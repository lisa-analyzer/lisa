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
import it.unive.lisa.analysis.AnalysisState.Error;
import it.unive.lisa.analysis.ProgramState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.call.Call.CallType;
import it.unive.lisa.program.cfg.statement.call.OpenCall;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.VoidType;
import java.util.Set;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

public class WorstCasePolicyTest {

	private static AnalysisState<TestAbstractState> state() {
		return new AnalysisState<>(new ProgramState<>(new TestAbstractState(), new ExpressionSet()));
	}

	private static Analysis<TestAbstractState, TestAbstractDomain> analysis(
			Predicate<Type> shouldSmash) {
		return new Analysis<>(new TestAbstractDomain(), shouldSmash);
	}

	@Test
	public void instanceIsASingleton() {
		assertSame(WorstCasePolicy.INSTANCE, WorstCasePolicy.INSTANCE);
	}

	@Test
	public void voidCallsProduceASkip() throws SemanticException {
		CFG caller = newCfg("caller");
		OpenCall call = new OpenCall(caller, LOC, CallType.STATIC, null, "foo", VoidType.INSTANCE);

		AnalysisState<TestAbstractState> result = WorstCasePolicy.INSTANCE.apply(
				call, state(), analysis(null), new ExpressionSet[0]);

		ExpressionSet computed = result.getExecutionExpressions();
		assertEquals(1, computed.size());
		assertTrue(computed.iterator().next() instanceof Skip);
	}

	@Test
	public void nonVoidCallsComputeTheMetaVariableAsAnUnknownValue() throws SemanticException {
		CFG caller = newCfg("caller");
		OpenCall call = new OpenCall(caller, LOC, CallType.STATIC, null, "foo");

		AnalysisState<TestAbstractState> result = WorstCasePolicy.INSTANCE.apply(
				call, state(), analysis(null), new ExpressionSet[0]);

		ExpressionSet computed = result.getExecutionExpressions();
		assertEquals(1, computed.size());
		assertEquals(call.getMetaVariable(), computed.iterator().next());
	}

	@Test
	public void noErrorTypesRegisteredMeansNoErrorsRaised() throws SemanticException {
		CFG caller = newCfg("caller");
		OpenCall call = new OpenCall(caller, LOC, CallType.STATIC, null, "foo");

		AnalysisState<TestAbstractState> result = WorstCasePolicy.INSTANCE.apply(
				call, state(), analysis(null), new ExpressionSet[0]);

		assertTrue(result.getErrors().getKeys().isEmpty());
		assertTrue(result.getSmashedErrors().getKeys().isEmpty());
	}

	@Test
	public void everyRegisteredErrorTypeIsRaisedAtTheCallSite() throws SemanticException {
		// "All possible errors are assumed to be thrown"
		CFG caller = newCfg("caller");
		Type errorType = InterproceduralTestFixtures.errorType(caller, "err");
		caller.getProgram().getTypes().registerType(errorType);
		OpenCall call = new OpenCall(caller, LOC, CallType.STATIC, null, "foo");

		AnalysisState<TestAbstractState> result = WorstCasePolicy.INSTANCE.apply(
				call, state(), analysis(null), new ExpressionSet[0]);

		assertEquals(Set.of(new Error(errorType, call)), result.getErrors().getKeys());
		assertTrue(result.getSmashedErrors().getKeys().isEmpty());
	}

	@Test
	public void errorTypesForWhichSmashingIsRequestedAreRecordedAsSmashedInstead() throws SemanticException {
		CFG caller = newCfg("caller");
		Type errorType = InterproceduralTestFixtures.errorType(caller, "err");
		caller.getProgram().getTypes().registerType(errorType);
		OpenCall call = new OpenCall(caller, LOC, CallType.STATIC, null, "foo");

		AnalysisState<TestAbstractState> result = WorstCasePolicy.INSTANCE.apply(
				call, state(), analysis(t -> t == errorType), new ExpressionSet[0]);

		assertTrue(result.getErrors().getKeys().isEmpty());
		assertEquals(Set.of(errorType), result.getSmashedErrors().getKeys());
		assertEquals(Set.of(call), result.getSmashedErrors().getState(errorType).elements());
	}

	@Test
	public void smashedAndNonSmashedErrorsAttributeTheSameOriginalCallSiteEvenThroughAChainOfResolutions()
			throws SemanticException {
		// regression test: WorstCasePolicy used to attribute smashed errors
		// directly to the (possibly already-resolved) call, while non-smashed
		// errors are attributed to the original call site by
		// AnalysisState.Error's constructor, which walks the whole
		// getSource() chain back to the root - the two must agree
		CFG caller = newCfg("caller");
		Type smashed = InterproceduralTestFixtures.errorType(caller, "smashed");
		Type notSmashed = InterproceduralTestFixtures.errorType(caller, "notSmashed");
		caller.getProgram().getTypes().registerType(smashed);
		caller.getProgram().getTypes().registerType(notSmashed);

		UnresolvedCall root = new UnresolvedCall(caller, LOC, CallType.STATIC, null, "foo");
		UnresolvedCall intermediate = new UnresolvedCall(caller, LOC, CallType.STATIC, null, "foo");
		intermediate.setSource(root);
		OpenCall call = new OpenCall(caller, LOC, CallType.STATIC, null, "foo");
		call.setSource(intermediate);

		AnalysisState<TestAbstractState> result = WorstCasePolicy.INSTANCE.apply(
				call, state(), analysis(t -> t == smashed), new ExpressionSet[0]);

		assertEquals(Set.of(new Error(notSmashed, root)), result.getErrors().getKeys());
		assertEquals(Set.of(root), result.getSmashedErrors().getState(smashed).elements());
	}

}
