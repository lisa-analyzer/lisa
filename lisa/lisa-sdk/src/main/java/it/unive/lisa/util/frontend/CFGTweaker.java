package it.unive.lisa.util.frontend;

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
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;

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
				pathIdx = addNormalFinallyEdges(cfg, pb.getTryBlock(), fin, pb.getClosing(), pathIdx);
			else
				pathIdx = addNormalFinallyEdges(cfg, pb.getElseBlock(), fin, pb.getClosing(), pathIdx);

			for (CatchBlock catchBody : pb.getCatchBlocks())
				pathIdx = addNormalFinallyEdges(cfg, catchBody.getBody(), fin, pb.getClosing(), pathIdx);
		}

		// we sort them for deterministic processing
		for (Statement yield : new TreeSet<>(cfg.getAllExitpoints())) {
			List<ProtectedBlock> fins = new LinkedList<>();
			for (ProtectionBlock pb : cfg.getDescriptor().getProtectionBlocks()) {
				if (pb.getFinallyBlock() != null
						&& !pb.getFinallyBlock().getBody().isEmpty()
						&& pb.getFullBody(false).contains(yield))
					fins.add(pb.getFinallyBlock());
			}
			if (fins.isEmpty())
				continue;

			fins.sort((
					a,
					b) -> a.getStart().getLocation().compareTo(b.getStart().getLocation()));
			for (Edge preEnd : cfg.getIngoingEdges(yield)) {
				cfg.getNodeList().removeEdge(preEnd);
				pathIdx = addFinallyPathInBetween(cfg, preEnd.getSource(), yield, fins, pathIdx);
			}
		}

		// we sort them for deterministic processing
		for (Statement st : new TreeSet<>(cfg.getNodes()))
			if (st.breaksControlFlow() || st.continuesControlFlow()) {
				List<ProtectedBlock> fins = new LinkedList<>();
				Collection<Edge> outs = cfg.getOutgoingEdges(st);
				Collection<Edge> toReplace = new TreeSet<>();
				for (ProtectionBlock pb : cfg.getDescriptor().getProtectionBlocks()) {
					if (pb.getFinallyBlock() == null
							|| pb.getFinallyBlock().getBody().isEmpty())
						continue;

					AtomicBoolean found = new AtomicBoolean(false);

					if (pb.getTryBlock().getBody().contains(st))
						outs.stream()
								.filter(e -> !pb.getTryBlock().getBody().contains(e.getDestination()))
								.forEach(e -> {
									found.set(true);
									toReplace.add(e);
								});
					if (pb.getElseBlock() != null && pb.getElseBlock().getBody().contains(st))
						outs.stream()
								.filter(e -> !pb.getElseBlock().getBody().contains(e.getDestination()))
								.forEach(e -> {
									found.set(true);
									toReplace.add(e);
								});
					for (CatchBlock catchBody : pb.getCatchBlocks())
						if (catchBody.getBody().getBody().contains(st))
							outs.stream()
									.filter(e -> !catchBody.getBody().getBody().contains(e.getDestination()))
									.forEach(e -> {
										found.set(true);
										toReplace.add(e);
									});

					if (found.get())
						fins.add(pb.getFinallyBlock());
				}
				if (fins.isEmpty())
					continue;

				fins.sort((
						a,
						b) -> a.getStart().getLocation().compareTo(b.getStart().getLocation()));
				for (Edge entry : toReplace) {
					cfg.getNodeList().removeEdge(entry);
					pathIdx = addFinallyPathInBetween(cfg, st, entry.getDestination(), fins, pathIdx);
				}
			}
	}

	private static int addNormalFinallyEdges(
			CFG cfg,
			ProtectedBlock pb,
			ProtectedBlock fin,
			Statement normalExit,
			int pathIdx) {
		// the edges are added as follows (BF: BeginFinallyEdge, EF:
		// EndFinallyEdge):
		// - if the block can be continued, a BF is added from the end of the
		// block to the start of the finally
		// - if both the block and the finally block can be continued, an EF is
		// added
		// from the end of the finally block to the closing
		if (!pb.canBeContinued())
			return pathIdx;
		cfg.addEdge(new BeginFinallyEdge(pb.getEnd(), fin.getStart(), pathIdx));
		if (fin.canBeContinued())
			cfg.addEdge(new EndFinallyEdge(fin.getEnd(), normalExit, pathIdx));
		return pathIdx + 1;
	}

	private static int addFinallyPathInBetween(
			CFG cfg,
			Statement start,
			Statement end,
			List<ProtectedBlock> fins,
			int pathIdx) {
		// we add:
		// - a BeginFinallyEdge from the predecessors of the yield to the
		// start of the first finally block
		// - a BeginFinallyEdge from the end each finally block to the beginning
		// of the next one
		// - an EndFinallyEdge from the end of the last finally block to the
		// yield if no yielders were found in the finally blocks
		// - an EndFinallyEdge from the end of the last finally block to the
		// last yielders found in the finally blocks otherwise
		if (fins.isEmpty())
			return pathIdx;

		int idx = 0;
		ProtectedBlock current = null;
		ProtectedBlock next = fins.get(idx++);
		Collection<Statement> lastYielders = Set.of(end);
		boolean yieldersInCurrentBlock = false;

		while (next != null) {
			yieldersInCurrentBlock = false;
			if (current == null)
				cfg.addEdge(new BeginFinallyEdge(start, next.getStart(), pathIdx));
			else {
				if (current.alwaysContinues())
					cfg.addEdge(new BeginFinallyEdge(current.getEnd(), next.getStart(), pathIdx));
				else
					for (Statement st : current.getBody())
						if (st.stopsExecution())
							for (Edge preEnd : cfg.getIngoingEdges(st)) {
								cfg.addEdge(new BeginFinallyEdge(preEnd.getSource(), next.getStart(), pathIdx));
								cfg.getNodeList().removeEdge(preEnd);
							}
			}

			if (!next.alwaysContinues())
				if (!next.canBeContinued()) {
					lastYielders = next.getBody().stream()
							.filter(Statement::stopsExecution)
							.toList();
					yieldersInCurrentBlock = true;
				} else {
					lastYielders = new LinkedList<>(lastYielders);
					next.getBody().stream()
							.filter(Statement::stopsExecution)
							.forEach(lastYielders::add);
				}

			current = next;
			next = idx < fins.size() ? fins.get(idx++) : null;
		}

		// current now holds the last finally block traversed:
		// all we have to do is connect it to the last yielding
		// block we found on our way there
		if (!yieldersInCurrentBlock)
			for (Statement st : lastYielders)
				cfg.addEdge(new EndFinallyEdge(current.getEnd(), st, pathIdx));
		return pathIdx + 1;
	}

	public static <E extends RuntimeException> void splitProtectedYields(
			CFG cfg,
			Function<String, E> exceptionFactory) {
		// we sort them for deterministic processing
		for (Statement yield : new TreeSet<>(cfg.getAllExitpoints())) {
			// the inner-most block containing the yield
			ProtectedBlock block = null;
			// all blocks containing the yield, to be updated if we add nodes
			List<ProtectedBlock> blocks = new LinkedList<>();
			// the catches that must be executed in case an error happens
			List<CatchBlock> catches = new LinkedList<>();

			// here we find the inner-most protected block that contains the
			// yield
			// to decide whether to add a noop before it or not; we collect all
			// catches
			// that the yield or the possible noop should be connected to, and
			// we
			// collect all protected blocks that contain the yield to update
			// them
			for (ProtectionBlock pb : cfg.getDescriptor().getProtectionBlocks()) {
				if (pb.getTryBlock().getBody().contains(yield)) {
					blocks.add(pb.getTryBlock());
					catches.addAll(pb.getCatchBlocks());
					if (block == null || block.getBody().containsAll(pb.getTryBlock().getBody()))
						block = pb.getTryBlock();
				}

				if (pb.getFinallyBlock() != null) {
					if (pb.getElseBlock() != null && pb.getElseBlock().getBody().contains(yield)) {
						blocks.add(pb.getElseBlock());
						if (block == null || block.getBody().containsAll(pb.getElseBlock().getBody()))
							block = pb.getElseBlock();
					}
					for (CatchBlock catchBody : pb.getCatchBlocks())
						if (catchBody.getBody().getBody().contains(yield)) {
							blocks.add(catchBody.getBody());
							if (block == null || block.getBody().containsAll(catchBody.getBody().getBody()))
								block = catchBody.getBody();
						}
				}
			}

			if (block != null)
				splitProtectedYield(cfg, yield, block.getBody().size() == 1, blocks, catches);
		}
	}

	private static <E extends RuntimeException> void splitProtectedYield(
			CFG cfg,
			Statement yielder,
			boolean isOnlyNode,
			List<ProtectedBlock> blocks,
			Collection<CatchBlock> catches) {
		// ret -> noop; ret
		// ret 0 -> noop; ret 0
		// if (b) { ret } -> if (b) { noop; ret }
		// if (b) { ret 0 } -> if (b) { noop; ret 0 }
		// A; ret -> A; ret
		// A; ret 0 -> A; ret 0
		// ret x+2 -> t=x+2; ret t
		// A; ret x+2 -> A; t=x+2; ret t

		Collection<Edge> ingoing = cfg.getIngoingEdges(yielder);
		boolean isBeginningOfBranch = ingoing.stream().anyMatch(Predicate.not(Edge::isUnconditional));
		boolean needsRewriting = yielder instanceof YieldsValue && !((YieldsValue) yielder).isAtomic();
		if (!needsRewriting && (isOnlyNode || isBeginningOfBranch)) {
			// first and second cases
			// third and fourth cases
			NoOp noop = addNoOp(cfg, yielder, ingoing);
			connectToCatches(cfg, noop, catches);
			removeOutgoingErrorEdges(cfg, yielder);
			updateBlocks(cfg, yielder, noop, null, blocks, false);
		} else if (!isOnlyNode && !needsRewriting && !isBeginningOfBranch) {
			// fifth and sixth cases
			for (Edge in : ingoing)
				connectToCatches(cfg, in.getSource(), catches);
			removeOutgoingErrorEdges(cfg, yielder);
		} else {
			// seventh and eighth cases
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

			connectToCatches(cfg, assign, catches);
			removeOutgoingErrorEdges(cfg, yielder);
			updateBlocks(cfg, yielder, assign, newYielder, blocks, true);

			cfg.getNodeList().removeNode(yielder);
		}
	}

	private static void updateBlocks(
			CFG cfg,
			Statement yielder,
			Statement first,
			Statement second,
			List<ProtectedBlock> blocks,
			boolean removeYielder) {
		if (cfg.getEntrypoints().contains(yielder)) {
			cfg.getEntrypoints().remove(yielder);
			cfg.getEntrypoints().add(first);
		}
		for (ProtectedBlock block : blocks) {
			block.getBody().add(first);
			if (second != null)
				block.getBody().add(second);
			if (removeYielder)
				block.getBody().remove(yielder);
			if (block.getStart() == yielder)
				block.setStart(first);
			if (second != null && block.getEnd() == yielder)
				block.setEnd(second);
		}
	}

	private static NoOp addNoOp(
			CFG cfg,
			Statement yielder,
			Collection<Edge> ingoing) {
		NoOp noop = new NoOp(cfg, yielder.getLocation());
		cfg.addNode(noop);
		cfg.addEdge(new SequentialEdge(noop, yielder));
		for (Edge in : ingoing) {
			cfg.addEdge(in.newInstance(in.getSource(), noop));
			cfg.getNodeList().removeEdge(in);
		}
		return noop;
	}

	private static void connectToCatches(
			CFG cfg,
			Statement target,
			Collection<CatchBlock> catches) {
		if (catches.isEmpty())
			return;
		for (CatchBlock cb : catches)
			cfg.addEdge(new ErrorEdge(target, cb.getBody().getStart(), cb.getIdentifier(), cb.getExceptions()));
	}

	private static void removeOutgoingErrorEdges(
			CFG cfg,
			Statement yielder) {
		for (Edge out : cfg.getOutgoingEdges(yielder))
			if (out.isErrorHandling())
				cfg.getNodeList().removeEdge(out);
	}
}
