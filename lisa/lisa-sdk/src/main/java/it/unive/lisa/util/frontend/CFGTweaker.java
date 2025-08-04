package it.unive.lisa.util.frontend;

import java.util.Collection;
import java.util.LinkedList;
import java.util.function.Function;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.VariableTableEntry;
import it.unive.lisa.program.cfg.edge.BeginFinallyEdge;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.edge.EndFinallyEdge;
import it.unive.lisa.program.cfg.edge.ErrorEdge;
import it.unive.lisa.program.cfg.edge.SequentialEdge;
import it.unive.lisa.program.cfg.protection.ProtectionBlock;
import it.unive.lisa.program.cfg.statement.Assignment;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Ret;
import it.unive.lisa.program.cfg.statement.Return;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.program.cfg.statement.YieldsValue;
import it.unive.lisa.program.cfg.statement.literal.Literal;

public class CFGTweaker {

    private CFGTweaker() {
        // utility class, no instances allowed
    }

    public static <E extends RuntimeException> void addReturns(CFG cfg, Function<String, E> exceptionFactory) {
        Ret ret = new Ret(cfg, cfg.getDescriptor().getLocation());

		if (cfg.getNodesCount() == 0) {
			// empty method, so the ret is also the entrypoint
			cfg.addNode(ret, true);
			return;
		}

		// every non-throwing instruction that does not have a follower
		// is ending the method
		Collection<Statement> preExits = new LinkedList<>();
		for (Statement st : cfg.getNodes())
			if (!st.stopsExecution() && cfg.followersOf(st).isEmpty())
				preExits.add(st);
		if (preExits.isEmpty()) 
			return;

		// if the other returns do return a value, we cannot add
		// a ret as we should return a value as well
		boolean returnsValue = false;
		for (Statement st : cfg.getNormalExitpoints())
			if (st instanceof Return)
				returnsValue = true;
			else if (returnsValue)
				throw exceptionFactory.apply("Return statement at " + st.getLocation() + " should return something, since other returns do it");

		cfg.addNode(ret);
		for (Statement st : preExits) {
			if (returnsValue)
				throw exceptionFactory.apply("Missing return statement at " + st.getLocation());
			cfg.addEdge(new SequentialEdge(st, ret));
		}

		// adjust scopes
		for (VariableTableEntry entry : cfg.getDescriptor().getVariables())
			if (preExits.contains(entry.getScopeEnd()))
				entry.setScopeEnd(ret);
    }

    public static <E extends RuntimeException> void fixFinallyEdges(CFG cfg, Function<String, E> exceptionFactory) {
        for (ProtectionBlock pb : cfg.getDescriptor().getProtectionBlocks()) {
            Collection<Edge> goingOutside = new LinkedList<>();
            Collection<Statement> terminating = new LinkedList<>();

            Collection<Statement> protectedBody = pb.getFullBody(false);
            Collection<Statement> fullBody = pb.getFullBody(true);

            for (Statement st : protectedBody) {
                cfg.getOutgoingEdges(st).stream()
                    .filter(e -> !fullBody.contains(e.getDestination()))
                    .forEach(goingOutside::add);
                if (st.stopsExecution())
                    terminating.add(st);
            }

            // TODO: what if we have nested protection blocks?
            
            for (Statement st : terminating) 
                if (st instanceof YieldsValue)
                    fixYielding(cfg, exceptionFactory, st);
                else
                    fixNonYielding(cfg, exceptionFactory, st);

            if (pb.getFinallyBlock() == null || pb.getFinallyBlock().isEmpty()) 
                // no finally block, edges going outside are fine
                return;

            for (Edge e : goingOutside) 
                if (e instanceof SequentialEdge) {
                    // - SequentialEdge: we direct it to the assign
                    cfg.addEdge(new BeginFinallyEdge(e.getSource(), pb.getFinallyHead(), e.getSource()));
                    if (pb.getClosing() != null)
                        cfg.addEdge(new EndFinallyEdge(pb.getClosing(), e.getDestination(), e.getSource()));
                    cfg.getNodeList().removeEdge(e);
                } else
                    // - EndFinallyEdge: we direct it to the new yielder
                    // - BeginFinallyEdge: they should not happen (only outgoing)
                    // - ErrorEdge: we direct it to the assign
                    // - TrueEdge: we direct it to the assign
                    // - FalseEdge: we direct it to the assign
                    exceptionFactory.apply(e.getSource() + " has an outgoing edge going outside of the protection block that cannot be redirected");
        }
    }

    private static <E extends RuntimeException> void fixYielding(CFG cfg, Function<String, E> exceptionFactory, Statement st) {
        // we make terminating statements that yield an expression
        // into ones that yield a variable, so that the execution
        // of the finally block can happen after the expression
        // to yield has been computed
        
        YieldsValue yielder = (YieldsValue) st;
        Expression value = yielder.yieldedValue();
        if (value instanceof VariableRef 
            || value instanceof Literal)
            // no need to fix this, as it is already a variable
            return; 

        VariableRef tmpVar1 = new VariableRef(cfg, value.getLocation(), "$yielded", value.getStaticType());
        VariableRef tmpVar2 = new VariableRef(cfg, st.getLocation(), "$yielded", value.getStaticType());
        Assignment assign = new Assignment(
                cfg,
                st.getLocation(),
                tmpVar1,
                value);
        Statement newYielder = yielder.withValue(tmpVar2);

        cfg.addNode(assign);
        cfg.addNode(newYielder);
        cfg.addEdge(new SequentialEdge(assign, newYielder));

        for (Edge e : cfg.getIngoingEdges(st))
            if (e instanceof EndFinallyEdge)
                // - EndFinallyEdge: we direct it to the new yielder
                cfg.addEdge(new EndFinallyEdge(e.getSource(), newYielder, assign));
            else if (e instanceof BeginFinallyEdge)
                // - BeginFinallyEdge: they should not happen (only outgoing)
                exceptionFactory.apply(st + " has an ingoing BeginFinallyEdge while not being in a finally block");
            else
                // - ErrorEdge: we direct it to the assign
                // - SequentialEdge: we direct it to the assign
                // - TrueEdge: we direct it to the assign
                // - FalseEdge: we direct it to the assign
                cfg.addEdge(e.newInstance(e.getSource(), assign));

        for (Edge e : cfg.getOutgoingEdges(st))
            if (e instanceof ErrorEdge)
                // - ErrorEdge: we make it start from the assign
                cfg.addEdge(e.newInstance(assign, e.getDestination()));
            else if (e instanceof BeginFinallyEdge)
                // - BeginFinallyEdge: we make it start from the assign
                cfg.addEdge(new BeginFinallyEdge(assign, e.getDestination(), assign));
            else if (e instanceof EndFinallyEdge)
                // - EndFinallyEdge: they should not happen (only ingoing)
                exceptionFactory.apply(st + " has an outgoing EndFinallyEdge while not being in a finally block");
            else
                // - SequentialEdge: they should not happen (deadcode)
                // - TrueEdge: they should not happen (deadcode)
                // - FalseEdge: they should not happen (deadcode)
                exceptionFactory.apply("Execution-terminating statement " + st + " has non-error outgoing edges");

        cfg.getNodeList().removeNode(st);
    }

    private static <E extends RuntimeException> void fixNonYielding(CFG cfg, Function<String, E> exceptionFactory,
            Statement st) {
        // for terminating statements that do not yield an expression
        // we just ensure that the edges going from/to the finally block
        // and the ones going to the catch blocks are correct
        
        Collection<Edge> toRemove = new LinkedList<>();

        for (Edge e : cfg.getIngoingEdges(st))
            if (e instanceof BeginFinallyEdge)
                // - BeginFinallyEdge: they should not happen (only outgoing)
                exceptionFactory.apply(st + " has an ingoing BeginFinallyEdge while not being in a finally block");
            else if (e instanceof BeginFinallyEdge) {
                // - EndFinallyEdge: we make them start from the predecessors
                for (Statement pred : cfg.predecessorsOf(st)) 
                    cfg.addEdge(new EndFinallyEdge(e.getSource(), e.getDestination(), pred));
                toRemove.add(e);
            } else
                // - ErrorEdge: we keep them
                // - SequentialEdge: we keep them
                // - TrueEdge: we keep them
                // - FalseEdge: we keep them
                continue;

        for (Edge e : cfg.getOutgoingEdges(st))
            if (e instanceof ErrorEdge)
                // - ErrorEdge: we remove it
                toRemove.add(e);
            else if (e instanceof BeginFinallyEdge) {
                // - BeginFinallyEdge: we make them start from the predecessors
                for (Statement pred : cfg.predecessorsOf(st)) 
                    cfg.addEdge(new BeginFinallyEdge(pred, e.getDestination(), pred));
                toRemove.add(e);
            } else if (e instanceof EndFinallyEdge)
                // - EndFinallyEdge: they should not happen (only ingoing)
                exceptionFactory.apply(st + " has an outgoing EndFinallyEdge while not being in a finally block");
            else
                // - SequentialEdge: they should not happen (deadcode)
                // - TrueEdge: they should not happen (deadcode)
                // - FalseEdge: they should not happen (deadcode)
                exceptionFactory.apply("Execution-terminating statement " + st + " has non-error outgoing edges");
        
        toRemove.forEach(cfg.getNodeList()::removeEdge);
    }
}
