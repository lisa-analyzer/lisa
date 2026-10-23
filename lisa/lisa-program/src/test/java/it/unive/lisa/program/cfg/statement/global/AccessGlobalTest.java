package it.unive.lisa.program.cfg.statement.global;

import static org.junit.jupiter.api.Assertions.assertEquals;

import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.ProgramState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.ConstantGlobal;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.testsupport.FakeInterproceduralAnalysis;
import it.unive.lisa.program.testsupport.RecordingDomain;
import it.unive.lisa.program.testsupport.TestFixtures;
import it.unive.lisa.program.testsupport.UnitLattice;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.GlobalVariable;
import org.junit.jupiter.api.Test;

public class AccessGlobalTest {

	private RecordingDomain domain;

	private FakeInterproceduralAnalysis interprocedural;

	private AnalysisState<UnitLattice> state;

	private StatementStore<UnitLattice> expressions;

	private void setup() {
		domain = new RecordingDomain();
		interprocedural = new FakeInterproceduralAnalysis(domain);
		ProgramState<UnitLattice> programState = new ProgramState<>(UnitLattice.INSTANCE, new ExpressionSet());
		state = new AnalysisState<>(programState).withExecution(programState);
		expressions = new StatementStore<>(state);
	}

	// a plain (non-constant) global is accessed by building a GlobalVariable
	// carrying the global's own static type and a name derived from
	// "container::target" (see AccessGlobal#toString)
	@Test
	public void plainGlobalIsAccessedThroughAGlobalVariableNamedAfterItsContainer()
			throws SemanticException {
		setup();
		ClassUnit unit = new ClassUnit(TestFixtures.LOCATION, TestFixtures.PROGRAM, "Holder", false);
		Global target = new Global(TestFixtures.LOCATION, unit, "field", false, Int32Type.INSTANCE);

		AccessGlobal access = new AccessGlobal(TestFixtures.CFG, TestFixtures.LOCATION, unit, target);
		AnalysisState<UnitLattice> result = access.forwardSemantics(state, interprocedural, expressions);

		assertEquals(1, domain.smallStepCalls.size());
		GlobalVariable built = (GlobalVariable) domain.smallStepCalls.get(0);
		assertEquals(Int32Type.INSTANCE, built.getStaticType());
		assertEquals("Holder::field", built.getName());
		assertEquals(UnitLattice.INSTANCE, result.getExecutionState());
	}

	// a ConstantGlobal is accessed by directly evaluating its own constant
	// value, bypassing the GlobalVariable machinery entirely
	@Test
	public void constantGlobalIsAccessedThroughItsOwnConstantValue()
			throws SemanticException {
		setup();
		ClassUnit unit = new ClassUnit(TestFixtures.LOCATION, TestFixtures.PROGRAM, "Holder", false);
		Constant value = new Constant(Int32Type.INSTANCE, 42, TestFixtures.LOCATION);
		ConstantGlobal target = new ConstantGlobal(TestFixtures.LOCATION, unit, "PI", value);

		AccessGlobal access = new AccessGlobal(TestFixtures.CFG, TestFixtures.LOCATION, unit, target);
		AnalysisState<UnitLattice> result = access.forwardSemantics(state, interprocedural, expressions);

		assertEquals(1, domain.smallStepCalls.size());
		assertEquals(value, domain.smallStepCalls.get(0));
		assertEquals(UnitLattice.INSTANCE, result.getExecutionState());
	}

}
