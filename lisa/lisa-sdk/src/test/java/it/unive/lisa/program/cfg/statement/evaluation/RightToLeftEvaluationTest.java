package it.unive.lisa.program.cfg.statement.evaluation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import it.unive.lisa.TestAbstractState;
import it.unive.lisa.TestInterproceduralAnalysis;
import it.unive.lisa.TestLanguageFeatures;
import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.ProgramState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.statement.Expression;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class RightToLeftEvaluationTest {

	private static final SourceCodeLocation LOC = new SourceCodeLocation("fake", 0, 0);

	private static CFG newCfg() {
		ClassUnit unit = new ClassUnit(
				LOC, new Program(new TestLanguageFeatures(), new TestTypeSystem()), "unit", false);
		return new CFG(new CodeMemberDescriptor(LOC, unit, false, "m"));
	}

	private static AnalysisState<TestAbstractState> state() {
		return new AnalysisState<>(new ProgramState<>(new TestAbstractState(), new ExpressionSet()));
	}

	@Test
	public void previousIsTheImmediatelyFollowingIndexAndNegativeAtTheLastOne() {
		// R2L visits len-1 first, so the "index visited right before pos" is
		// pos+1 - mirroring L2R's previous()/next() around the midpoint
		assertEquals(1, RightToLeftEvaluation.INSTANCE.previous(0, 3));
		assertEquals(2, RightToLeftEvaluation.INSTANCE.previous(1, 3));
		assertEquals(-1, RightToLeftEvaluation.INSTANCE.previous(2, 3));
	}

	@Test
	public void nextIsTheImmediatelyPrecedingIndexAndNegativeAtIndexZero() {
		assertEquals(-1, RightToLeftEvaluation.INSTANCE.next(0, 3));
		assertEquals(0, RightToLeftEvaluation.INSTANCE.next(1, 3));
		assertEquals(1, RightToLeftEvaluation.INSTANCE.next(2, 3));
	}

	@Test
	public void firstIsTheHighestIndexAndLastIsIndexZero() {
		assertEquals(3, RightToLeftEvaluation.INSTANCE.first(4));
		assertEquals(0, RightToLeftEvaluation.INSTANCE.last(4));
	}

	@Test
	public void evaluateVisitsSubExpressionsInReverseDeclarationOrderUsingForwardSemantics()
			throws SemanticException {
		List<String> log = new ArrayList<>();
		CFG cfg = newCfg();
		Expression[] subs = {
				new RecordingExpression(cfg, "a", log),
				new RecordingExpression(cfg, "b", log),
				new RecordingExpression(cfg, "c", log) };

		ExpressionSet[] computed = new ExpressionSet[3];
		AnalysisState<TestAbstractState> entry = state();
		AnalysisState<TestAbstractState> result = RightToLeftEvaluation.INSTANCE.evaluate(
				subs, entry, new TestInterproceduralAnalysis<>(), new StatementStore<>(entry), computed);

		assertEquals(List.of("fwd:c", "fwd:b", "fwd:a"), log);
		assertSame(entry, result);
	}

	@Test
	public void bwdEvaluateVisitsSubExpressionsInDeclarationOrderUsingBackwardSemantics() throws SemanticException {
		// bwdEvaluate reverses R2L's own (reverse) order, landing back on plain
		// declaration order - this is what mirrors LeftToRightEvaluation
		List<String> log = new ArrayList<>();
		CFG cfg = newCfg();
		Expression[] subs = {
				new RecordingExpression(cfg, "a", log),
				new RecordingExpression(cfg, "b", log),
				new RecordingExpression(cfg, "c", log) };

		ExpressionSet[] computed = new ExpressionSet[3];
		AnalysisState<TestAbstractState> entry = state();
		RightToLeftEvaluation.INSTANCE.bwdEvaluate(
				subs, entry, new TestInterproceduralAnalysis<>(), new StatementStore<>(entry), computed);

		assertEquals(List.of("bwd:a", "bwd:b", "bwd:c"), log);
	}

	@Test
	public void bwdEvaluateOnEmptySubExpressionsReturnsTheEntryStateUnchanged() throws SemanticException {
		AnalysisState<TestAbstractState> entry = state();
		AnalysisState<TestAbstractState> result = RightToLeftEvaluation.INSTANCE.bwdEvaluate(
				new Expression[0], entry, new TestInterproceduralAnalysis<>(), new StatementStore<>(entry),
				new ExpressionSet[0]);
		assertSame(entry, result);
	}

}
