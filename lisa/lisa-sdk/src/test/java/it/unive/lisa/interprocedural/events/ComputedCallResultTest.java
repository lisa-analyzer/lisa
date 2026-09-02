package it.unive.lisa.interprocedural.events;

import static it.unive.lisa.interprocedural.InterproceduralTestFixtures.LOC;
import static it.unive.lisa.interprocedural.InterproceduralTestFixtures.newCfg;
import static org.junit.jupiter.api.Assertions.assertSame;

import it.unive.lisa.TestAbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.ProgramState;
import it.unive.lisa.interprocedural.UniqueScope;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.program.cfg.statement.call.Call.CallType;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ComputedCallResultTest {

	private static AnalysisState<TestAbstractState> state() {
		return new AnalysisState<>(new ProgramState<>(new TestAbstractState(), new ExpressionSet()));
	}

	@Test
	public void getProgramPointReturnsTheCallItselfWhenItHasNoSource() {
		CFG caller = newCfg("caller");
		CFG callee = newCfg("callee");
		CFGCall call = new CFGCall(caller, LOC, CallType.STATIC, null, "callee", List.of(callee));

		ComputedCallResult<TestAbstractState> event = new ComputedCallResult<>(
				call, new UniqueScope<>(), state(), new ExpressionSet[0], state());

		assertSame(call, event.getProgramPoint());
	}

	@Test
	public void getProgramPointWalksTheWholeSourceChainBackToTheOriginalCallSite() {
		// a call can originate from a chain of more than one unresolved call
		// (e.g. when a call's semantics generates another call that gets
		// resolved in turn): the reported program point must always be the
		// original one, consistently with, e.g.,
		// Statement#getEvaluationPredecessor() and AnalysisState.Error
		CFG caller = newCfg("caller");
		CFG callee = newCfg("callee");

		UnresolvedCall root = new UnresolvedCall(caller, LOC, CallType.STATIC, null, "callee");
		UnresolvedCall intermediate = new UnresolvedCall(caller, LOC, CallType.STATIC, null, "callee");
		intermediate.setSource(root);

		CFGCall call = new CFGCall(caller, LOC, CallType.STATIC, null, "callee", List.of(callee));
		call.setSource(intermediate);

		ComputedCallResult<TestAbstractState> event = new ComputedCallResult<>(
				call, new UniqueScope<>(), state(), new ExpressionSet[0], state());

		assertSame(root, event.getProgramPoint());
	}

}
