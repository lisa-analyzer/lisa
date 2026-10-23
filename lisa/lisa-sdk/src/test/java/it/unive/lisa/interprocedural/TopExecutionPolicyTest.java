package it.unive.lisa.interprocedural;

import static it.unive.lisa.interprocedural.InterproceduralTestFixtures.LOC;
import static it.unive.lisa.interprocedural.InterproceduralTestFixtures.newCfg;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestAbstractDomain;
import it.unive.lisa.TestAbstractState;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.ProgramState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.call.Call.CallType;
import it.unive.lisa.program.cfg.statement.call.OpenCall;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import it.unive.lisa.type.VoidType;
import org.junit.jupiter.api.Test;

public class TopExecutionPolicyTest {

	private static AnalysisState<TestAbstractState> state() {
		return new AnalysisState<>(new ProgramState<>(new TestAbstractState(), new ExpressionSet()));
	}

	private static Analysis<TestAbstractState, TestAbstractDomain> analysis() {
		return new Analysis<>(new TestAbstractDomain());
	}

	@Test
	public void instanceIsASingleton() {
		assertSame(TopExecutionPolicy.INSTANCE, TopExecutionPolicy.INSTANCE);
	}

	@Test
	public void voidCallsProduceASkip() throws SemanticException {
		CFG caller = newCfg("caller");
		OpenCall call = new OpenCall(caller, LOC, CallType.STATIC, null, "foo", VoidType.INSTANCE);

		AnalysisState<TestAbstractState> result = TopExecutionPolicy.INSTANCE.apply(
				call, state(), analysis(), new ExpressionSet[0]);

		ExpressionSet computed = result.getExecutionExpressions();
		assertEquals(1, computed.size());
		assertTrue(computed.iterator().next() instanceof Skip);
	}

	@Test
	public void nonVoidCallsComputeTheMetaVariableAsAnUnknownValue() throws SemanticException {
		CFG caller = newCfg("caller");
		OpenCall call = new OpenCall(caller, LOC, CallType.STATIC, null, "foo");

		AnalysisState<TestAbstractState> result = TopExecutionPolicy.INSTANCE.apply(
				call, state(), analysis(), new ExpressionSet[0]);

		ExpressionSet computed = result.getExecutionExpressions();
		assertEquals(1, computed.size());
		assertEquals(call.getMetaVariable(), computed.iterator().next());
	}

	@Test
	public void noErrorsAreAssumedUnlikeWorstCasePolicy() throws SemanticException {
		// TopExecutionPolicy's contract states that "no errors are assumed to
		// be thrown": only the whole execution state becomes top, contrarily
		// to WorstCasePolicy which also raises every possible error
		CFG caller = newCfg("caller");
		TypeSystem types = caller.getProgram().getTypes();
		Type errorType = InterproceduralTestFixtures.errorType(caller, "err");
		types.registerType(errorType);
		OpenCall call = new OpenCall(caller, LOC, CallType.STATIC, null, "foo");

		AnalysisState<TestAbstractState> result = TopExecutionPolicy.INSTANCE.apply(
				call, state(), analysis(), new ExpressionSet[0]);

		assertTrue(result.getErrors().getKeys().isEmpty());
		assertTrue(result.getSmashedErrors().getKeys().isEmpty());
	}

}
