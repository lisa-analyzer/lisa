package it.unive.lisa.program.cfg.statement.call;

import static it.unive.lisa.program.cfg.statement.call.CallFixtures.LOC;
import static it.unive.lisa.program.cfg.statement.call.CallFixtures.newCfg;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestAbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.ProgramState;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.program.cfg.statement.call.Call.CallType;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class TruncatedParamsCallTest {

	@Test
	public void constructorRejectsACallThatWasNotAlreadyResolved() {
		CFG caller = newCfg("caller");
		UnresolvedCall notResolved = new UnresolvedCall(caller, LOC, CallType.STATIC, null, "foo");
		assertThrows(IllegalArgumentException.class, () -> new TruncatedParamsCall(notResolved));
	}

	@Test
	public void wrapsAResolvedCallReusingItsAlreadyTruncatedParameters() {
		CFG caller = newCfg("caller");
		CFG t1 = newCfg("t1");
		VariableRef arg = new VariableRef(caller, LOC, "arg");
		// CFGCall built directly with a single parameter, simulating what
		// CanRemoveReceiver#removeFirstParameter already produced
		CFGCall inner = new CFGCall(caller, LOC, CallType.UNKNOWN, null, "foo", List.of(t1), arg);
		TruncatedParamsCall wrapper = new TruncatedParamsCall(inner);

		assertSame(inner, wrapper.getInnerCall());
		assertEquals(1, wrapper.getParameters().length);
		assertSame(arg, wrapper.getParameters()[0]);
		assertEquals(Set.of(t1), Set.copyOf(wrapper.getTargets()));
	}

	@Test
	public void equalsAndHashCodeAreBasedOnTheInnerCall() {
		CFG caller = newCfg("caller");
		CFG t1 = newCfg("t1");
		CFGCall inner1 = new CFGCall(caller, LOC, CallType.UNKNOWN, null, "foo", List.of(t1));
		CFGCall inner2 = new CFGCall(caller, LOC, CallType.UNKNOWN, null, "foo", List.of(t1));
		TruncatedParamsCall a = new TruncatedParamsCall(inner1);
		TruncatedParamsCall b = new TruncatedParamsCall(inner2);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void setSourcePropagatesToTheInnerCall() {
		CFG caller = newCfg("caller");
		CFG t1 = newCfg("t1");
		CFGCall inner = new CFGCall(caller, LOC, CallType.UNKNOWN, null, "foo", List.of(t1));
		TruncatedParamsCall wrapper = new TruncatedParamsCall(inner);
		UnresolvedCall source = new UnresolvedCall(caller, LOC, CallType.UNKNOWN, null, "foo");

		wrapper.setSource(source);
		assertSame(source, wrapper.getSource());
		assertSame(source, inner.getSource());
	}

	@Test
	public void forwardSemanticsAuxForwardsTruncatedParamsUnchangedWhenAlreadyMatchingInLength() throws Exception {
		CFG caller = newCfg("caller");
		CFG t1 = newCfg("t1");
		VariableRef arg = new VariableRef(caller, LOC, "arg");
		CFGCall inner = new CFGCall(caller, LOC, CallType.UNKNOWN, null, "foo", List.of(t1), arg);
		TruncatedParamsCall wrapper = new TruncatedParamsCall(inner);

		RecordingResolvedCallInterprocedural interprocedural = new RecordingResolvedCallInterprocedural();
		AnalysisState<TestAbstractState> entry = new AnalysisState<>(
				new ProgramState<>(new TestAbstractState(), new ExpressionSet()));
		ExpressionSet[] params = new ExpressionSet[] { new ExpressionSet() };

		wrapper.forwardSemanticsAux(interprocedural, entry, params,
				new it.unive.lisa.analysis.StatementStore<>(entry));

		assertTrue(wrapper.getMetaVariables().isEmpty());
	}

	private static class RecordingResolvedCallInterprocedural
			extends
			it.unive.lisa.TestInterproceduralAnalysis<TestAbstractState,
					it.unive.lisa.analysis.AbstractDomain<TestAbstractState>> {

		private final it.unive.lisa.analysis.Analysis<TestAbstractState,
				it.unive.lisa.analysis.AbstractDomain<
						TestAbstractState>> analysis = new it.unive.lisa.analysis.Analysis<>(
								new it.unive.lisa.TestAbstractDomain());

		@Override
		public it.unive.lisa.analysis.Analysis<TestAbstractState,
				it.unive.lisa.analysis.AbstractDomain<TestAbstractState>> getAnalysis() {
			return analysis;
		}

		@Override
		public AnalysisState<TestAbstractState> getAbstractResultOf(
				CFGCall call,
				AnalysisState<TestAbstractState> entryState,
				ExpressionSet[] parameters,
				it.unive.lisa.analysis.StatementStore<TestAbstractState> expressions) {
			return entryState;
		}
	}

}
