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
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.call.Call.CallType;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Untyped;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class MultiCallTest {

	private static AnalysisState<TestAbstractState> state() {
		return new AnalysisState<>(new ProgramState<>(new TestAbstractState(), new ExpressionSet()));
	}

	private static class ControlledInterprocedural
			extends
			TestInterproceduralAnalysis<TestAbstractState, AbstractDomain<TestAbstractState>> {

		private final Analysis<TestAbstractState, AbstractDomain<TestAbstractState>> analysis = new Analysis<>(
				new TestAbstractDomain());

		@Override
		public Analysis<TestAbstractState, AbstractDomain<TestAbstractState>> getAnalysis() {
			return analysis;
		}

		@Override
		public AnalysisState<TestAbstractState> getAbstractResultOf(
				CFGCall call,
				AnalysisState<TestAbstractState> entryState,
				ExpressionSet[] parameters,
				StatementStore<TestAbstractState> expressions) {
			// a and b are structurally identical CFGCalls (same qualifier,
			// target name and no parameters), so Call#toString() alone cannot
			// tell them apart - use object identity instead to get two
			// distinguishable pushed identifiers
			return entryState.withExecutionExpression(
					new Variable(Untyped.INSTANCE, "ret@" + System.identityHashCode(call), LOC));
		}
	}

	@Test
	public void constructorRejectsAnUnderlyingCallThatWasNotResolved() {
		CFG caller = newCfg("caller");
		UnresolvedCall notResolved = new UnresolvedCall(caller, LOC, CallType.STATIC, null, "foo");
		UnresolvedCall source = new UnresolvedCall(caller, LOC, CallType.STATIC, null, "foo");
		assertThrows(IllegalArgumentException.class, () -> new MultiCall(source, notResolved));
	}

	@Test
	public void staticTypeIsTheCommonSupertypeOfEachUnderlyingCall() {
		CFG caller = newCfg("caller");
		CFG voidTarget = newCfg("v", it.unive.lisa.type.VoidType.INSTANCE);
		CFGCall a = new CFGCall(caller, LOC, CallType.STATIC, null, "foo", List.of(voidTarget));
		CFGCall b = new CFGCall(caller, LOC, CallType.STATIC, null, "foo", List.of(voidTarget));
		UnresolvedCall source = new UnresolvedCall(caller, LOC, CallType.STATIC, null, "foo");
		MultiCall multi = new MultiCall(source, a, b);
		assertSame(it.unive.lisa.type.VoidType.INSTANCE, multi.getStaticType());
	}

	@Test
	public void getCallsExposesTheUnderlyingCallsInOrder() {
		CFG caller = newCfg("caller");
		CFGCall a = new CFGCall(caller, LOC, CallType.STATIC, null, "foo", List.of(newCfg("t1")));
		CFGCall b = new CFGCall(caller, LOC, CallType.STATIC, null, "foo", List.of(newCfg("t2")));
		UnresolvedCall source = new UnresolvedCall(caller, LOC, CallType.STATIC, null, "foo");
		MultiCall multi = new MultiCall(source, a, b);
		assertEquals(List.of(a, b), List.copyOf(multi.getCalls()));
	}

	@Test
	public void getTargetsIsTheUnionOfEveryUnderlyingCallsTargets() {
		CFG caller = newCfg("caller");
		CFG t1 = newCfg("t1");
		CFG t2 = newCfg("t2");
		CFGCall a = new CFGCall(caller, LOC, CallType.STATIC, null, "foo", List.of(t1));
		CFGCall b = new CFGCall(caller, LOC, CallType.STATIC, null, "foo", List.of(t2));
		UnresolvedCall source = new UnresolvedCall(caller, LOC, CallType.STATIC, null, "foo");
		MultiCall multi = new MultiCall(source, a, b);
		assertEquals(Set.of(t1, t2), Set.copyOf(multi.getTargets()));
	}

	@Test
	public void setSourcePropagatesToEveryUnderlyingCall() {
		CFG caller = newCfg("caller");
		CFGCall a = new CFGCall(caller, LOC, CallType.STATIC, null, "foo", List.of(newCfg("t1")));
		CFGCall b = new CFGCall(caller, LOC, CallType.STATIC, null, "foo", List.of(newCfg("t2")));
		UnresolvedCall source = new UnresolvedCall(caller, LOC, CallType.STATIC, null, "foo");
		MultiCall multi = new MultiCall(source, a, b);

		UnresolvedCall newSource = new UnresolvedCall(caller, LOC, CallType.STATIC, null, "foo");
		multi.setSource(newSource);
		assertSame(newSource, multi.getSource());
		assertSame(newSource, a.getSource());
		assertSame(newSource, b.getSource());
	}

	@Test
	public void equalsAndHashCodeAreBasedOnTheUnderlyingCalls() {
		CFG caller = newCfg("caller");
		CFG t1 = newCfg("t1");
		UnresolvedCall source = new UnresolvedCall(caller, LOC, CallType.STATIC, null, "foo");
		MultiCall x = new MultiCall(source, new CFGCall(caller, LOC, CallType.STATIC, null, "foo", List.of(t1)));
		MultiCall y = new MultiCall(source, new CFGCall(caller, LOC, CallType.STATIC, null, "foo", List.of(t1)));
		assertEquals(x, y);
		assertEquals(x.hashCode(), y.hashCode());
	}

	@Test
	public void compareCallAuxComparesByNumberOfCallsThenPairwise() {
		CFG caller = newCfg("caller");
		CFG t1 = newCfg("t1");
		CFG t2 = newCfg("t2");
		UnresolvedCall source = new UnresolvedCall(caller, LOC, CallType.STATIC, null, "foo");
		MultiCall one = new MultiCall(source, new CFGCall(caller, LOC, CallType.STATIC, null, "foo", List.of(t1)));
		MultiCall two = new MultiCall(source,
				new CFGCall(caller, LOC, CallType.STATIC, null, "foo", List.of(t1)),
				new CFGCall(caller, LOC, CallType.STATIC, null, "foo", List.of(t2)));
		assertTrue(one.compareCallAux(two) < 0);
		assertTrue(two.compareCallAux(one) > 0);
	}

	@Test
	public void forwardSemanticsAuxJoinsTheResultsOfEveryUnderlyingCallStartingFromTheSameEntryState()
			throws Exception {
		// give both targets a Return exitpoint so Call#returnsVoid is false and
		// CallWithResult#forwardSemanticsAux takes the "push a meta variable"
		// branch, whose pushed identifier comes straight from this test's
		// controlled compute() result
		CFG t1 = newCfg("t1");
		t1.addNode(new it.unive.lisa.program.cfg.statement.Return(t1, LOC,
				new it.unive.lisa.program.cfg.statement.VariableRef(t1, LOC, "x")), true);
		CFG t2 = newCfg("t2");
		t2.addNode(new it.unive.lisa.program.cfg.statement.Return(t2, LOC,
				new it.unive.lisa.program.cfg.statement.VariableRef(t2, LOC, "y")), true);

		CFG caller = newCfg("caller");
		CFGCall a = new CFGCall(caller, LOC, CallType.STATIC, null, "foo", List.of(t1));
		CFGCall b = new CFGCall(caller, LOC, CallType.STATIC, null, "foo", List.of(t2));
		UnresolvedCall source = new UnresolvedCall(caller, LOC, CallType.STATIC, null, "foo");
		MultiCall multi = new MultiCall(source, a, b);

		ControlledInterprocedural interprocedural = new ControlledInterprocedural();
		AnalysisState<TestAbstractState> entry = state();
		AnalysisState<TestAbstractState> result = multi.forwardSemanticsAux(
				interprocedural, entry, new ExpressionSet[0], new StatementStore<>(entry));

		// each underlying call pushed a differently-named identifier ("ret@a",
		// "ret@b"); joining (lub-ing) their two post-states must retain both
		assertEquals(2, result.getExecutionExpressions().size());
		assertTrue(multi.getMetaVariables().containsAll(a.getMetaVariables()));
		assertTrue(multi.getMetaVariables().containsAll(b.getMetaVariables()));
	}

}
