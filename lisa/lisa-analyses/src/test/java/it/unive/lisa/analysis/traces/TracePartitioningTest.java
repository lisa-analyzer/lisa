package it.unive.lisa.analysis.traces;

import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SimpleAbstractDomain;
import it.unive.lisa.analysis.nonrelational.type.TypeEnvironment;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.imp.ParsingException;
import it.unive.lisa.lattices.SimpleAbstractState;
import it.unive.lisa.lattices.heap.Monolith;
import it.unive.lisa.lattices.traces.Branching;
import it.unive.lisa.lattices.traces.ExecutionTrace;
import it.unive.lisa.lattices.traces.LoopIteration;
import it.unive.lisa.lattices.traces.LoopSummary;
import it.unive.lisa.lattices.traces.TraceLattice;
import it.unive.lisa.lattices.types.TypeSet;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.cfg.controlFlow.ControlFlowStructure;
import it.unive.lisa.program.cfg.controlFlow.IfThenElse;
import it.unive.lisa.program.cfg.controlFlow.Loop;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.util.numeric.IntInterval;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TracePartitioningTest {

	private static Statement ifCondition;

	private static Statement ifTrueBranchTarget;

	private static Statement ifFalseBranchTarget;

	private static Statement loopCondition;

	private static Statement loopBodyTarget;

	@BeforeAll
	public static void init()
			throws ParsingException {
		Program ifProgram = IMPFrontend
				.processText("class c { foo() { if (true) { this.foo(); } else { this.foo(); } } }");
		CFG ifCfg = ifProgram.getAllCFGs().iterator().next();
		ControlFlowStructure ifStruct = null;
		for (ControlFlowStructure s : ifCfg.getDescriptor().getControlFlowStructures())
			if (s instanceof IfThenElse)
				ifStruct = s;
		ifCondition = ifStruct.getCondition();
		ifTrueBranchTarget = ((IfThenElse) ifStruct).getTrueBranch().iterator().next();
		ifFalseBranchTarget = ((IfThenElse) ifStruct).getFalseBranch().iterator().next();

		Program loopProgram = IMPFrontend.processText("class c { foo() { while (true) { this.foo(); } } }");
		CFG loopCfg = loopProgram.getAllCFGs().iterator().next();
		ControlFlowStructure loopStruct = null;
		for (ControlFlowStructure s : loopCfg.getDescriptor().getControlFlowStructures())
			if (s instanceof Loop)
				loopStruct = s;
		loopCondition = loopStruct.getCondition();
		loopBodyTarget = ((Loop) loopStruct).getBody().iterator().next();
	}

	private SimpleAbstractDomain<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>> mkDomain() {
		return DefaultConfiguration.defaultAbstractDomain();
	}

	private Constant alwaysTrue(
			ProgramPoint pp) {
		return new Constant(BoolType.INSTANCE, true, pp.getLocation());
	}

	@Test
	public void assumeOnAnIfConditionFromTopPushesABranchingToken()
			throws SemanticException {
		SimpleAbstractDomain<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>> domain = mkDomain();
		TracePartitioning<SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>,
				SimpleAbstractDomain<Monolith,
						ValueEnvironment<IntInterval>,
						TypeEnvironment<TypeSet>>> tp = new TracePartitioning<>(domain);

		TraceLattice<
				SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>> result = tp
						.assume(tp.makeLattice(), alwaysTrue(ifCondition), ifCondition, ifTrueBranchTarget);

		ExecutionTrace expected = ExecutionTrace.EMPTY.push(new Branching(ifCondition, true));
		assertTrue(result.getKeys().contains(expected));
	}

	@Test
	public void assumeOnTheFalseBranchPushesAFalseBranchingToken()
			throws SemanticException {
		SimpleAbstractDomain<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>> domain = mkDomain();
		TracePartitioning<SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>,
				SimpleAbstractDomain<Monolith,
						ValueEnvironment<IntInterval>,
						TypeEnvironment<TypeSet>>> tp = new TracePartitioning<>(domain);

		TraceLattice<
				SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>> result = tp
						.assume(tp.makeLattice(), alwaysTrue(ifCondition), ifCondition, ifFalseBranchTarget);

		ExecutionTrace expected = ExecutionTrace.EMPTY.push(new Branching(ifCondition, false));
		assertTrue(result.getKeys().contains(expected));
	}

	@Test
	public void assumeOnAnIfConditionNeverPushesWhenMaxConditionsIsZero() {
		// with a limit of zero trackable conditions, a branch traversal must
		// not split the trace at all: it should stay the empty trace
		SimpleAbstractDomain<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>> domain = mkDomain();
		TracePartitioning<SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>,
				SimpleAbstractDomain<Monolith,
						ValueEnvironment<IntInterval>,
						TypeEnvironment<TypeSet>>> tp = new TracePartitioning<>(5, 0, domain);

		TraceLattice<
				SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>> result;
		try {
			result = tp.assume(tp.makeLattice(), alwaysTrue(ifCondition), ifCondition, ifTrueBranchTarget);
		} catch (SemanticException e) {
			throw new IllegalStateException(e);
		}

		assertTrue(result.getKeys().contains(ExecutionTrace.EMPTY));
	}

	@Test
	public void assumeOnALoopConditionFromTopPushesTheFirstIterationToken()
			throws SemanticException {
		SimpleAbstractDomain<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>> domain = mkDomain();
		TracePartitioning<SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>,
				SimpleAbstractDomain<Monolith,
						ValueEnvironment<IntInterval>,
						TypeEnvironment<TypeSet>>> tp = new TracePartitioning<>(domain);

		TraceLattice<
				SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>> result = tp
						.assume(tp.makeLattice(), alwaysTrue(loopCondition), loopCondition, loopBodyTarget);

		ExecutionTrace expected = ExecutionTrace.EMPTY.push(new LoopIteration(loopCondition, 0));
		assertTrue(result.getKeys().contains(expected));
	}

	@Test
	public void assumeOnASubsequentLoopIterationIncrementsTheIterationCounter()
			throws SemanticException {
		SimpleAbstractDomain<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>> domain = mkDomain();
		TracePartitioning<SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>,
				SimpleAbstractDomain<Monolith,
						ValueEnvironment<IntInterval>,
						TypeEnvironment<TypeSet>>> tp = new TracePartitioning<>(domain);

		Map<ExecutionTrace,
				SimpleAbstractState<Monolith,
						ValueEnvironment<IntInterval>,
						TypeEnvironment<TypeSet>>> function = new HashMap<>();
		ExecutionTrace atIterationTwo = ExecutionTrace.EMPTY.push(new LoopIteration(loopCondition, 2));
		function.put(atIterationTwo, domain.makeLattice());
		TraceLattice<
				SimpleAbstractState<Monolith,
						ValueEnvironment<IntInterval>,
						TypeEnvironment<TypeSet>>> state = new TraceLattice<>(domain.makeLattice(), function);

		TraceLattice<
				SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>> result = tp
						.assume(state, alwaysTrue(loopCondition), loopCondition, loopBodyTarget);

		ExecutionTrace expected = atIterationTwo.push(new LoopIteration(loopCondition, 3));
		assertTrue(result.getKeys().contains(expected));
	}

	@Test
	public void assumeCollapsesIntoALoopSummaryOnceTheIterationLimitIsReached() {
		// with a limit of one trackable iteration, entering the loop body for
		// a trace that is already at iteration 1 must summarize the rest of
		// the iterations instead of tracking iteration 2 individually
		SimpleAbstractDomain<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>> domain = mkDomain();
		TracePartitioning<SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>,
				SimpleAbstractDomain<Monolith,
						ValueEnvironment<IntInterval>,
						TypeEnvironment<TypeSet>>> tp = new TracePartitioning<>(1, 5, domain);

		Map<ExecutionTrace,
				SimpleAbstractState<Monolith,
						ValueEnvironment<IntInterval>,
						TypeEnvironment<TypeSet>>> function = new HashMap<>();
		ExecutionTrace atIterationOne = ExecutionTrace.EMPTY.push(new LoopIteration(loopCondition, 1));
		function.put(atIterationOne, domain.makeLattice());
		TraceLattice<
				SimpleAbstractState<Monolith,
						ValueEnvironment<IntInterval>,
						TypeEnvironment<TypeSet>>> state = new TraceLattice<>(domain.makeLattice(), function);

		TraceLattice<
				SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>> result;
		try {
			result = tp.assume(state, alwaysTrue(loopCondition), loopCondition, loopBodyTarget);
		} catch (SemanticException e) {
			throw new IllegalStateException(e);
		}

		ExecutionTrace expected = atIterationOne.push(new LoopSummary(loopCondition));
		assertTrue(result.getKeys().contains(expected));
	}

	@Test
	public void assumeOnALoopConditionThatDoesNotLeadIntoTheBodyDoesNotPushALoopToken()
			throws SemanticException {
		// per the javadoc, tokens are only pushed for edges that stay inside
		// the loop body: an edge leaving the loop (e.g. towards the
		// condition's own program point, simulating a loop exit) must not
		// split the trace
		SimpleAbstractDomain<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>> domain = mkDomain();
		TracePartitioning<SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>,
				SimpleAbstractDomain<Monolith,
						ValueEnvironment<IntInterval>,
						TypeEnvironment<TypeSet>>> tp = new TracePartitioning<>(domain);

		TraceLattice<
				SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>> result = tp
						.assume(tp.makeLattice(), alwaysTrue(loopCondition), loopCondition, loopCondition);

		assertTrue(result.getKeys().contains(ExecutionTrace.EMPTY));
	}

	@Test
	public void assumeOnBottomIsANoOp()
			throws SemanticException {
		SimpleAbstractDomain<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>> domain = mkDomain();
		TracePartitioning<SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>,
				SimpleAbstractDomain<Monolith,
						ValueEnvironment<IntInterval>,
						TypeEnvironment<TypeSet>>> tp = new TracePartitioning<>(domain);

		TraceLattice<
				SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>> bottom = tp
						.makeLattice().bottom();

		assertTrue(tp.assume(bottom, alwaysTrue(ifCondition), ifCondition, ifTrueBranchTarget).isBottom());
	}

	@Test
	public void makeLatticeStartsAtTop() {
		SimpleAbstractDomain<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>> domain = mkDomain();
		TracePartitioning<SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>,
				SimpleAbstractDomain<Monolith,
						ValueEnvironment<IntInterval>,
						TypeEnvironment<TypeSet>>> tp = new TracePartitioning<>(domain);

		assertTrue(tp.makeLattice().isTop());
	}

}
