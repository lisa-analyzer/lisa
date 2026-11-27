package it.unive.lisa.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;

import it.unive.lisa.TestAbstractDomain;
import it.unive.lisa.TestAbstractState;
import it.unive.lisa.TestInterproceduralAnalysis;
import it.unive.lisa.TestLanguageFeatures;
import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.interprocedural.UniqueScope;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.edge.SequentialEdge;
import it.unive.lisa.program.cfg.statement.Assignment;
import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.program.cfg.statement.Return;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.VariableRef;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class AnalyzedCFGTest {

	private static final ClassUnit unit = new ClassUnit(
			new SourceCodeLocation("unknown", 0, 0),
			new Program(new TestLanguageFeatures(), new TestTypeSystem()),
			"Testing",
			false);

	@Test
	public void testIssue189()
			throws SemanticException {
		SourceCodeLocation unknown = new SourceCodeLocation("unknown", 0, 0);
		CFG cfg = new CFG(new CodeMemberDescriptor(unknown, unit, false, "emptyIf"));
		VariableRef x = new VariableRef(cfg, unknown, "x");
		Return y = new Return(cfg, unknown, x);
		cfg.addNode(y, true);

		AnalysisState<TestAbstractState> state = new AnalysisState<>(
				new ProgramState<>(new TestAbstractState(), new ExpressionSet()));

		Map<Statement, AnalysisState<TestAbstractState>> entries = Map.of(y, state);
		Map<Statement, AnalysisState<TestAbstractState>> results = Map.of(y, state, x, state);

		AnalyzedCFG<TestAbstractState> res = new AnalyzedCFG<>(cfg, new UniqueScope<>(), state, entries, results);

		assertEquals(state, res.getAnalysisStateAfter(y));
		assertEquals(state, res.getAnalysisStateBefore(y));
		assertEquals(state, res.getAnalysisStateAfter(x));
		assertEquals(state, res.getAnalysisStateBefore(x));
	}

	@Test
	public void testIssue189Optimized()
			throws SemanticException {
		SourceCodeLocation unknown = new SourceCodeLocation("unknown", 0, 0);
		CFG cfg = new CFG(new CodeMemberDescriptor(unknown, unit, false, "emptyIf"));
		VariableRef x = new VariableRef(cfg, unknown, "x");
		Return y = new Return(cfg, unknown, x);
		cfg.addNode(y, true);

		AnalysisState<TestAbstractState> state = new AnalysisState<>(
				new ProgramState<>(new TestAbstractState(), new ExpressionSet()));

		Map<Statement, AnalysisState<TestAbstractState>> entries = Map.of(y, state);
		Map<Statement, AnalysisState<TestAbstractState>> results = Map.of(y, state, x, state);

		OptimizedAnalyzedCFG<TestAbstractState,
				TestAbstractDomain> res = new OptimizedAnalyzedCFG<>(
						cfg,
						new UniqueScope<>(),
						state,
						entries,
						results,
						new TestInterproceduralAnalysis<>());

		assertEquals(state, res.getAnalysisStateAfter(y));
		assertEquals(state, res.getAnalysisStateBefore(y));
		assertEquals(state, res.getAnalysisStateAfter(x));
		assertEquals(state, res.getAnalysisStateBefore(x));
	}

	@Test
	public void testIssue189Backward()
			throws SemanticException {
		SourceCodeLocation unknown = new SourceCodeLocation("unknown", 0, 0);
		CFG cfg = new CFG(new CodeMemberDescriptor(unknown, unit, false, "emptyIf"));
		VariableRef x = new VariableRef(cfg, unknown, "x");
		Return y = new Return(cfg, unknown, x);
		cfg.addNode(y, true);

		AnalysisState<TestAbstractState> state = new AnalysisState<>(
				new ProgramState<>(new TestAbstractState(), new ExpressionSet()));

		Map<Statement, AnalysisState<TestAbstractState>> entries = Map.of(y, state);
		Map<Statement, AnalysisState<TestAbstractState>> results = Map.of(y, state, x, state);

		BackwardAnalyzedCFG<
				TestAbstractState> res = new BackwardAnalyzedCFG<>(cfg, new UniqueScope<>(), state, entries, results);

		assertEquals(state, res.getAnalysisStateAfter(y));
		assertEquals(state, res.getAnalysisStateBefore(y));
		assertEquals(state, res.getAnalysisStateAfter(x));
		assertEquals(state, res.getAnalysisStateBefore(x));
	}

	@Test
	public void testIssue189BackwardOptimized()
			throws SemanticException {
		SourceCodeLocation unknown = new SourceCodeLocation("unknown", 0, 0);
		CFG cfg = new CFG(new CodeMemberDescriptor(unknown, unit, false, "emptyIf"));
		VariableRef x = new VariableRef(cfg, unknown, "x");
		Return y = new Return(cfg, unknown, x);
		cfg.addNode(y, true);

		AnalysisState<TestAbstractState> state = new AnalysisState<>(
				new ProgramState<>(new TestAbstractState(), new ExpressionSet()));

		Map<Statement, AnalysisState<TestAbstractState>> entries = Map.of(y, state);
		Map<Statement, AnalysisState<TestAbstractState>> results = Map.of(y, state, x, state);

		BackwardOptimizedAnalyzedCFG<TestAbstractState,
				TestAbstractDomain> res = new BackwardOptimizedAnalyzedCFG<>(
						cfg,
						new UniqueScope<>(),
						state,
						entries,
						results,
						new TestInterproceduralAnalysis<>());

		assertEquals(state, res.getAnalysisStateAfter(y));
		assertEquals(state, res.getAnalysisStateBefore(y));
		assertEquals(state, res.getAnalysisStateAfter(x));
		assertEquals(state, res.getAnalysisStateBefore(x));
	}

	/**
	 * Regression test for a bug introduced while adding support for backward
	 * analyses (commit 88217bfc6): when querying the state before an expression
	 * that is the first one evaluated within its statement (and thus has no
	 * {@link Statement#getEvaluationPredecessor()}), and that statement's root
	 * is not an entrypoint of the cfg, the state has to be recovered as the
	 * least upper bound of the states after the root statement's actual
	 * predecessors in the graph. The buggy implementation returned
	 * {@code bottom} instead, silently losing information.
	 */
	@Test
	public void testAnalysisStateBeforeFallsBackToPredecessorsForFirstEvaluatedSubExpression()
			throws SemanticException {
		SourceCodeLocation unknown = new SourceCodeLocation("unknown", 0, 0);
		CFG cfg = new CFG(new CodeMemberDescriptor(unknown, unit, false, "seq"));
		NoOp a = new NoOp(cfg, unknown);
		VariableRef target = new VariableRef(cfg, unknown, "t");
		VariableRef value = new VariableRef(cfg, unknown, "v");
		// right-to-left evaluation order (the default for Assignment) means
		// that 'value' is evaluated before 'target', and thus has no
		// evaluation predecessor of its own
		Assignment b = new Assignment(cfg, unknown, target, value);
		cfg.addNode(a, true);
		cfg.addNode(b, false);
		cfg.addEdge(new SequentialEdge(a, b));

		AnalysisState<TestAbstractState> stateA = new AnalysisState<>(
				new ProgramState<>(new TestAbstractState(), new ExpressionSet()));
		AnalysisState<TestAbstractState> stateB = new AnalysisState<>(
				new ProgramState<>(new TestAbstractState(), new ExpressionSet()));

		Map<Statement, AnalysisState<TestAbstractState>> entries = Map.of();
		Map<Statement, AnalysisState<TestAbstractState>> results = Map.of(a, stateA, b, stateB);

		AnalyzedCFG<TestAbstractState> res = new AnalyzedCFG<>(cfg, new UniqueScope<>(), stateA, entries, results);

		assertEquals(stateA, res.getAnalysisStateBefore(value));
	}

	/**
	 * Same as
	 * {@link #testAnalysisStateBeforeFallsBackToPredecessorsForFirstEvaluatedSubExpression()},
	 * but for {@link OptimizedAnalyzedCFG}, which inherits the buggy behavior
	 * from {@link AnalyzedCFG} without overriding it.
	 */
	@Test
	public void testAnalysisStateBeforeFallsBackToPredecessorsForFirstEvaluatedSubExpressionOptimized()
			throws SemanticException {
		SourceCodeLocation unknown = new SourceCodeLocation("unknown", 0, 0);
		CFG cfg = new CFG(new CodeMemberDescriptor(unknown, unit, false, "seq"));
		NoOp a = new NoOp(cfg, unknown);
		VariableRef target = new VariableRef(cfg, unknown, "t");
		VariableRef value = new VariableRef(cfg, unknown, "v");
		Assignment b = new Assignment(cfg, unknown, target, value);
		cfg.addNode(a, true);
		cfg.addNode(b, false);
		cfg.addEdge(new SequentialEdge(a, b));

		AnalysisState<TestAbstractState> stateA = new AnalysisState<>(
				new ProgramState<>(new TestAbstractState(), new ExpressionSet()));
		AnalysisState<TestAbstractState> stateB = new AnalysisState<>(
				new ProgramState<>(new TestAbstractState(), new ExpressionSet()));

		Map<Statement, AnalysisState<TestAbstractState>> entries = Map.of();
		Map<Statement, AnalysisState<TestAbstractState>> results = Map.of(a, stateA, b, stateB);

		OptimizedAnalyzedCFG<TestAbstractState,
				TestAbstractDomain> res = new OptimizedAnalyzedCFG<>(
						cfg,
						new UniqueScope<>(),
						stateA,
						entries,
						results,
						new TestInterproceduralAnalysis<>());

		assertEquals(stateA, res.getAnalysisStateBefore(value));
	}

	/**
	 * Symmetric counterpart, on {@link BackwardAnalyzedCFG}, of the root-level
	 * fallback performed by
	 * {@link AnalyzedCFG#getAnalysisStateBefore(Statement)} when the queried
	 * statement is not an entrypoint: when a statement is not an exitpoint of
	 * the cfg, the state after it must be recovered as the least upper bound of
	 * the states before its followers in the graph. This behavior was correct
	 * both before and after the fix applied for {@link AnalyzedCFG}, and this
	 * test locks it in as a regression guard.
	 */
	@Test
	public void testAnalysisStateAfterFallsBackToFollowersWhenNotExitpoint()
			throws SemanticException {
		SourceCodeLocation unknown = new SourceCodeLocation("unknown", 0, 0);
		CFG cfg = new CFG(new CodeMemberDescriptor(unknown, unit, false, "seq"));
		// distinct locations, as NoOp's equals/hashCode are location-based
		NoOp a = new NoOp(cfg, unknown);
		NoOp b = new NoOp(cfg, new SourceCodeLocation("unknown", 1, 0));
		cfg.addNode(a, true);
		cfg.addNode(b, false);
		cfg.addEdge(new SequentialEdge(a, b));

		AnalysisState<TestAbstractState> stateA = new AnalysisState<>(
				new ProgramState<>(new TestAbstractState(), new ExpressionSet()));
		AnalysisState<TestAbstractState> stateB = new AnalysisState<>(
				new ProgramState<>(new TestAbstractState(), new ExpressionSet()));

		Map<Statement, AnalysisState<TestAbstractState>> exits = Map.of();
		Map<Statement, AnalysisState<TestAbstractState>> results = Map.of(a, stateA, b, stateB);

		BackwardAnalyzedCFG<TestAbstractState> res = new BackwardAnalyzedCFG<>(
				cfg,
				new UniqueScope<>(),
				stateA,
				exits,
				results);

		assertEquals(stateB, res.getAnalysisStateAfter(a));
	}

	/**
	 * Same as
	 * {@link #testAnalysisStateAfterFallsBackToFollowersWhenNotExitpoint()},
	 * but for {@link BackwardOptimizedAnalyzedCFG}.
	 */
	@Test
	public void testAnalysisStateAfterFallsBackToFollowersWhenNotExitpointOptimized()
			throws SemanticException {
		SourceCodeLocation unknown = new SourceCodeLocation("unknown", 0, 0);
		CFG cfg = new CFG(new CodeMemberDescriptor(unknown, unit, false, "seq"));
		// distinct locations, as NoOp's equals/hashCode are location-based
		NoOp a = new NoOp(cfg, unknown);
		NoOp b = new NoOp(cfg, new SourceCodeLocation("unknown", 1, 0));
		cfg.addNode(a, true);
		cfg.addNode(b, false);
		cfg.addEdge(new SequentialEdge(a, b));

		AnalysisState<TestAbstractState> stateA = new AnalysisState<>(
				new ProgramState<>(new TestAbstractState(), new ExpressionSet()));
		AnalysisState<TestAbstractState> stateB = new AnalysisState<>(
				new ProgramState<>(new TestAbstractState(), new ExpressionSet()));

		Map<Statement, AnalysisState<TestAbstractState>> exits = Map.of();
		Map<Statement, AnalysisState<TestAbstractState>> results = Map.of(a, stateA, b, stateB);

		BackwardOptimizedAnalyzedCFG<TestAbstractState,
				TestAbstractDomain> res = new BackwardOptimizedAnalyzedCFG<>(
						cfg,
						new UniqueScope<>(),
						stateA,
						exits,
						results,
						new TestInterproceduralAnalysis<>());

		assertEquals(stateB, res.getAnalysisStateAfter(a));
	}

}
