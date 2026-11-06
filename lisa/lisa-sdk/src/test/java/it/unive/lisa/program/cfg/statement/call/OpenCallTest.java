package it.unive.lisa.program.cfg.statement.call;

import static it.unive.lisa.program.cfg.statement.call.CallFixtures.LOC;
import static it.unive.lisa.program.cfg.statement.call.CallFixtures.newCfg;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestAbstractDomain;
import it.unive.lisa.TestAbstractState;
import it.unive.lisa.TestInterproceduralAnalysis;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.ProgramState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.call.Call.CallType;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.type.VoidType;
import org.junit.jupiter.api.Test;

public class OpenCallTest {

	private static AnalysisState<TestAbstractState> state() {
		return new AnalysisState<>(new ProgramState<>(new TestAbstractState(), new ExpressionSet()));
	}

	/**
	 * An interprocedural analysis backed by a real {@link Analysis} (so that
	 * {@code smallStepSemantics} actually runs), whose
	 * {@code getAbstractResultOf(OpenCall, ...)} is fully controlled by the
	 * test.
	 */
	private static class ControlledInterprocedural
			extends
			TestInterproceduralAnalysis<TestAbstractState, AbstractDomain<TestAbstractState>> {

		private final Analysis<TestAbstractState, AbstractDomain<TestAbstractState>> analysis = new Analysis<>(
				new TestAbstractDomain());
		private AnalysisState<TestAbstractState> toReturn;

		@Override
		public Analysis<TestAbstractState, AbstractDomain<TestAbstractState>> getAnalysis() {
			return analysis;
		}

		@Override
		public AnalysisState<TestAbstractState> getAbstractResultOf(
				OpenCall call,
				AnalysisState<TestAbstractState> entryState,
				ExpressionSet[] parameters,
				StatementStore<TestAbstractState> expressions) {
			return toReturn;
		}
	}

	@Test
	public void defaultConstructorsUseLeftToRightEvaluationAndUntypedStaticType() {
		OpenCall call = new OpenCall(newCfg("caller"), LOC, CallType.STATIC, null, "foo");
		assertSame(Untyped.INSTANCE, call.getStaticType());
	}

	@Test
	public void getTargetsIsAlwaysEmpty() {
		OpenCall call = new OpenCall(newCfg("caller"), LOC, CallType.STATIC, null, "foo");
		assertTrue(call.getTargets().isEmpty());
	}

	@Test
	public void toStringIsTaggedAsOpen() {
		OpenCall call = new OpenCall(newCfg("caller"), LOC, CallType.STATIC, null, "foo");
		assertTrue(call.toString().startsWith("[open]"));
	}

	@Test
	public void getMetaVariableIsNamedAfterTheLocation() {
		OpenCall call = new OpenCall(newCfg("caller"), LOC, CallType.STATIC, null, "foo");
		Identifier meta = call.getMetaVariable();
		assertTrue(((Variable) meta).getName().startsWith("open_call_ret_value@"));
	}

	@Test
	public void whenTheStaticTypeIsVoidForwardSemanticsPushesASkipAndNoMetaVariable() throws SemanticException {
		CFG caller = newCfg("caller");
		OpenCall call = new OpenCall(caller, LOC, CallType.STATIC, null, "foo", VoidType.INSTANCE);
		ControlledInterprocedural interprocedural = new ControlledInterprocedural();
		interprocedural.toReturn = state();

		AnalysisState<TestAbstractState> entry = state();
		AnalysisState<TestAbstractState> result = call.forwardSemanticsAux(
				interprocedural, entry, new ExpressionSet[0], new StatementStore<>(entry));

		ExpressionSet computed = result.getExecutionExpressions();
		assertEquals(1, computed.size());
		assertTrue(computed.iterator().next() instanceof Skip);
		assertTrue(call.getMetaVariables().isEmpty());
	}

	@Test
	public void whenTheCallProducesAValueForwardSemanticsRegistersTheMetaVariable() throws SemanticException {
		CFG caller = newCfg("caller");
		// untyped static type + a non-empty, non-Skip result from compute() ->
		// Call#returnsVoid falls through to "false"; compute()'s contract
		// requires the pushed expression to be an Identifier (see
		// CallWithResult#compute's javadoc), since it is stored verbatim as a
		// meta variable
		OpenCall call = new OpenCall(caller, LOC, CallType.STATIC, null, "foo");
		ControlledInterprocedural interprocedural = new ControlledInterprocedural();
		AnalysisState<TestAbstractState> computed = state()
				.withExecutionExpression(new Variable(Untyped.INSTANCE, "ret_value", LOC));
		interprocedural.toReturn = computed;

		AnalysisState<TestAbstractState> entry = state();
		AnalysisState<TestAbstractState> result = call.forwardSemanticsAux(
				interprocedural, entry, new ExpressionSet[0], new StatementStore<>(entry));

		assertSame(computed, result);
		assertTrue(call.getMetaVariables().contains(call.getMetaVariable()));
	}

	@Test
	public void backwardSemanticsAuxIsUnsupported() {
		OpenCall call = new OpenCall(newCfg("caller"), LOC, CallType.STATIC, null, "foo");
		ControlledInterprocedural interprocedural = new ControlledInterprocedural();
		AnalysisState<TestAbstractState> entry = state();
		assertThrows(UnsupportedOperationException.class,
				() -> call.backwardSemanticsAux(interprocedural, entry, new ExpressionSet[0],
						new StatementStore<>(entry)));
	}

}
