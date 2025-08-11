package it.unive.lisa.util.frontend;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.function.Function;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.VariableTableEntry;
import it.unive.lisa.program.cfg.edge.BeginFinallyEdge;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.edge.EndFinallyEdge;
import it.unive.lisa.program.cfg.edge.ErrorEdge;
import it.unive.lisa.program.cfg.edge.SequentialEdge;
import it.unive.lisa.program.cfg.protection.CatchBlock;
import it.unive.lisa.program.cfg.protection.ProtectedBlock;
import it.unive.lisa.program.cfg.protection.ProtectionBlock;
import it.unive.lisa.program.cfg.statement.Assignment;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NoOp;
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

	public static <E extends RuntimeException> void addReturns(
			CFG cfg,
			Function<String, E> exceptionFactory) {
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
				throw exceptionFactory.apply("Return statement at " + st.getLocation()
						+ " should return something, since other returns do it");

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

	public static <E extends RuntimeException> void addFinallyEdges(
			CFG cfg,
			Function<String, E> exceptionFactory) {
		int pathIdx = 0;
		for (ProtectionBlock pb : cfg.getDescriptor().getProtectionBlocks()) {
			ProtectedBlock fin = pb.getFinallyBlock();
			if (fin == null || fin.getBody().isEmpty())
				continue;

			if (pb.getElseBlock() == null)
				addFinallyEdges(cfg, pb.getTryBlock(), fin, pb.getClosing(), pathIdx++);
			else
				addFinallyEdges(cfg, pb.getElseBlock(), fin, pb.getClosing(), pathIdx++);

			for (CatchBlock catchBody : pb.getCatchBlocks())
				addFinallyEdges(cfg, catchBody.getBody(), fin, pb.getClosing(), pathIdx++);
			

			Map<Statement, ProtectedBlock> alterer = new HashMap<>();
			pb.getTryBlock().getBody().stream()
				.filter(s -> s.breaksControlFlow() || s.continuesControlFlow())
				.forEach(s -> alterer.put(s, pb.getTryBlock()));
			if (pb.getElseBlock() != null)
				pb.getElseBlock().getBody().stream()
					.filter(s -> s.breaksControlFlow() || s.continuesControlFlow())
					.forEach(s -> alterer.put(s, pb.getElseBlock()));
			for (CatchBlock catchBody : pb.getCatchBlocks())
				catchBody.getBody().getBody().stream()
					.filter(s -> s.breaksControlFlow() || s.continuesControlFlow())
					.forEach(s -> alterer.put(s, catchBody.getBody()));

			for (Map.Entry<Statement, ProtectedBlock> entry : alterer.entrySet()) {
				Statement st = entry.getKey();
				ProtectedBlock block = entry.getValue();
				for (Edge out : cfg.getOutgoingEdges(st)) {
					if (!block.getBody().contains(out.getDestination())) {
						// the destination is outside the protected block,
						// so we must execute the finally before going there
						cfg.getNodeList().removeEdge(out);
						cfg.addEdge(new BeginFinallyEdge(st, fin.getStart(), pathIdx++));
						if (fin.canBeContinued())
							cfg.addEdge(new EndFinallyEdge(fin.getEnd(), out.getDestination(), pathIdx - 1));
					}
				}
			}
		}
	}

	private static void addFinallyEdges(
			CFG cfg,
			ProtectedBlock pb,
			ProtectedBlock fin,
			Statement normalExit,
			int pathIdx) {
		// the edges are added as follows (BF: BeginFinallyEdge, EF:
		// EndFinallyEdge):
		// - if a block can be continued, a BF is added from the end of the
		// block to the start of the finally
		// - if a block cannot be continued, a BF is added from the pre-end of
		// the block to the start of the finally
		// - if a block and the finally block can be continued, an EF is added
		// from the end of the finally block to the closing
		// - if a block cannot be continued and the finally block can be
		// continued, an EF is added from the end of the finally block to the
		// statement(s) returning

		if (pb.canBeContinued()) {
			cfg.addEdge(new BeginFinallyEdge(pb.getEnd(), fin.getStart(), pathIdx));
			if (fin.canBeContinued())
				cfg.addEdge(new EndFinallyEdge(fin.getEnd(), normalExit, pathIdx));
		} else
			for (Statement st : pb.getBody())
				if (st.stopsExecution()) {
					for (Edge preEnd : cfg.getIngoingEdges(pb.getEnd())) {
						cfg.addEdge(new BeginFinallyEdge(preEnd.getSource(), fin.getStart(), pathIdx));
						cfg.getNodeList().removeEdge(preEnd);
					}
					if (fin.canBeContinued())
						cfg.addEdge(new EndFinallyEdge(fin.getEnd(), st, pathIdx));
				}
	}

	public static <E extends RuntimeException> void splitProtectedYields(
			CFG cfg,
			Function<String, E> exceptionFactory) {
		for (ProtectionBlock pb : cfg.getDescriptor().getProtectionBlocks()) {
			// we make copies of the bodies since we are modifying them
			// while iterating over them, and we want to (i) avoid exceptions
			// and (ii) iterate only on the original statements
			for (Statement st : new ArrayList<>(pb.getTryBlock().getBody()))
				if (st.stopsExecution())
					splitProtectedYield(
							cfg,
							st,
							pb.getTryBlock().getBody().size() == 1,
							true,
							pb.getFinallyBlock() != null,
							pb.getTryBlock(),
							pb.getCatchBlocks());

			if (pb.getElseBlock() != null)
				for (Statement st : new ArrayList<>(pb.getElseBlock().getBody()))
					if (st.stopsExecution())
						splitProtectedYield(
								cfg,
								st,
								pb.getElseBlock().getBody().size() == 1,
								false,
								pb.getFinallyBlock() != null,
								pb.getElseBlock(),
								pb.getCatchBlocks());

			for (CatchBlock catchBody : pb.getCatchBlocks())
				for (Statement st : new ArrayList<>(catchBody.getBody().getBody()))
					if (st.stopsExecution())
						splitProtectedYield(
								cfg,
								st,
								catchBody.getBody().getBody().size() == 1,
								false,
								pb.getFinallyBlock() != null,
								catchBody.getBody(),
								pb.getCatchBlocks());
		}
	}

	private static <E extends RuntimeException> void splitProtectedYield(
			CFG cfg,
			Statement yielder,
			boolean isOnlyNode,
			boolean needsErrors,
			boolean hasFinally,
			ProtectedBlock pb,
			Collection<CatchBlock> catches) {
		// ret -> noop; ret
		// ret 0 -> noop; ret 0
		// A; ret -> A; ret
		// A; ret 0 -> A; ret 0
		// ret x+2 -> t=x+2; ret t
		// A; ret x+2 -> A; t=x+2; ret t

		Collection<Edge> ingoing = cfg.getIngoingEdges(yielder);
		boolean needsRewriting = yielder instanceof YieldsValue && !((YieldsValue) yielder).isAtomic();
		if ((isOnlyNode && !needsRewriting)) {
			// first two cases
			NoOp noop = new NoOp(cfg, yielder.getLocation());
			cfg.addNode(noop);
			cfg.addEdge(new SequentialEdge(noop, yielder));
			for (Edge in : ingoing) {
				cfg.addEdge(in.newInstance(in.getSource(), noop));
				cfg.getNodeList().removeEdge(in);
			}
			if (cfg.getEntrypoints().contains(yielder)) {
				cfg.getEntrypoints().remove(yielder);
				cfg.getEntrypoints().add(noop);
			}
			if (needsErrors) {
				for (CatchBlock cb : catches)
					cfg.addEdge(new ErrorEdge(noop, cb.getBody().getStart(), cb.getIdentifier(), cb.getExceptions()));
				for (Edge out : cfg.getOutgoingEdges(yielder))
					if (out.isErrorHandling())
						cfg.getNodeList().removeEdge(out);
			}
			pb.getBody().add(noop);
			pb.setStart(noop);
		} else if (!isOnlyNode && !needsRewriting) {
			// third and fourth cases
			if (needsErrors) {
				for (Edge in : ingoing)
					for (CatchBlock cb : catches)
						cfg.addEdge(new ErrorEdge(in.getSource(), cb.getBody().getStart(), cb.getIdentifier(),
								cb.getExceptions()));
				for (Edge out : cfg.getOutgoingEdges(yielder))
					if (out.isErrorHandling())
						cfg.getNodeList().removeEdge(out);
			}
		} else {
			YieldsValue vyielder = (YieldsValue) yielder;
			Expression value = vyielder.yieldedValue();
			needsRewriting = !(value instanceof VariableRef || value instanceof Literal);
			VariableRef tmpVar1 = new VariableRef(cfg, value.getLocation(), "$val_to_yield", value.getStaticType());
			VariableRef tmpVar2 = new VariableRef(cfg, yielder.getLocation(), "$val_to_yield", value.getStaticType());
			Assignment assign = new Assignment(
					cfg,
					yielder.getLocation(),
					tmpVar1,
					value);
			Statement newYielder = vyielder.withValue(tmpVar2);

			cfg.addNode(assign);
			cfg.addNode(newYielder);
			cfg.addEdge(new SequentialEdge(assign, newYielder));
			for (Edge in : ingoing) {
				cfg.addEdge(in.newInstance(in.getSource(), assign));
				cfg.getNodeList().removeEdge(in);
			}
			if (cfg.getEntrypoints().contains(yielder)) {
				cfg.getEntrypoints().remove(yielder);
				cfg.getEntrypoints().add(assign);
			}
			if (needsErrors) {
				for (CatchBlock cb : catches)
					cfg.addEdge(new ErrorEdge(assign, cb.getBody().getStart(), cb.getIdentifier(), cb.getExceptions()));
				for (Edge out : cfg.getOutgoingEdges(yielder))
					if (out.isErrorHandling())
						cfg.getNodeList().removeEdge(out);
			}
			pb.getBody().add(assign);
			pb.getBody().add(newYielder);
			pb.getBody().remove(yielder);
			if (pb.getStart() == yielder)
				pb.setStart(assign);
			if (pb.getEnd() == yielder)
				pb.setEnd(newYielder);

			cfg.getNodeList().removeNode(yielder);
		}
	}
}
