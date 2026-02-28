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

}
