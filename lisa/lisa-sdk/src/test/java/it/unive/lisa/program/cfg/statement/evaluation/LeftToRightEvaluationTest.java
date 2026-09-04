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

public class LeftToRightEvaluationTest {

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
	public void previousIsAlwaysTheImmediatelyPrecedingIndex() {
		assertEquals(-1, LeftToRightEvaluation.INSTANCE.previous(0, 3));
		assertEquals(0, LeftToRightEvaluation.INSTANCE.previous(1, 3));
		assertEquals(1, LeftToRightEvaluation.INSTANCE.previous(2, 3));
	}

	@Test
	public void nextIsTheImmediatelyFollowingIndexAndNegativeAtTheLastOne() {
		assertEquals(1, LeftToRightEvaluation.INSTANCE.next(0, 3));
		assertEquals(2, LeftToRightEvaluation.INSTANCE.next(1, 3));
		assertEquals(-1, LeftToRightEvaluation.INSTANCE.next(2, 3));
	}

	@Test
	public void firstIsIndexZeroAndLastIsTheHighestIndex() {
		assertEquals(0, LeftToRightEvaluation.INSTANCE.first(4));
		assertEquals(3, LeftToRightEvaluation.INSTANCE.last(4));
	}

	@Test
	public void evaluateVisitsSubExpressionsInDeclarationOrderUsingForwardSemantics() throws SemanticException {
		List<String> log = new ArrayList<>();
		CFG cfg = newCfg();
		Expression[] subs = {
				new RecordingExpression(cfg, "a", log),
				new RecordingExpression(cfg, "b", log),
				new RecordingExpression(cfg, "c", log) };

		ExpressionSet[] computed = new ExpressionSet[3];
		AnalysisState<TestAbstractState> entry = state();
		AnalysisState<TestAbstractState> result = LeftToRightEvaluation.INSTANCE.evaluate(
				subs, entry, new TestInterproceduralAnalysis<>(), new StatementStore<>(entry), computed);

		assertEquals(List.of("fwd:a", "fwd:b", "fwd:c"), log);
		assertSame(entry, result);
	}

	@Test
	public void bwdEvaluateVisitsSubExpressionsInReverseDeclarationOrderUsingBackwardSemantics()
			throws SemanticException {
		List<String> log = new ArrayList<>();
		CFG cfg = newCfg();
		Expression[] subs = {
				new RecordingExpression(cfg, "a", log),
				new RecordingExpression(cfg, "b", log),
				new RecordingExpression(cfg, "c", log) };

		ExpressionSet[] computed = new ExpressionSet[3];
		AnalysisState<TestAbstractState> entry = state();
		LeftToRightEvaluation.INSTANCE.bwdEvaluate(
				subs, entry, new TestInterproceduralAnalysis<>(), new StatementStore<>(entry), computed);

		assertEquals(List.of("bwd:c", "bwd:b", "bwd:a"), log);
	}

	@Test
	public void evaluateOnEmptySubExpressionsReturnsTheEntryStateUnchanged() throws SemanticException {
		AnalysisState<TestAbstractState> entry = state();
		AnalysisState<TestAbstractState> result = LeftToRightEvaluation.INSTANCE.evaluate(
				new Expression[0], entry, new TestInterproceduralAnalysis<>(), new StatementStore<>(entry),
				new ExpressionSet[0]);
		assertSame(entry, result);
	}

}
