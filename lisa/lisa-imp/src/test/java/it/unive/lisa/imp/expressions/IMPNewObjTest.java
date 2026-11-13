package it.unive.lisa.imp.expressions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.ProgramState;
import it.unive.lisa.imp.testsupport.FakeInterproceduralAnalysis;
import it.unive.lisa.imp.testsupport.RecordingDomain;
import it.unive.lisa.imp.testsupport.TestFixtures;
import it.unive.lisa.imp.testsupport.UnitLattice;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.symbolic.heap.MemoryAllocation;
import org.junit.jupiter.api.Test;

public class IMPNewObjTest {

	private RecordingDomain domain;

	private FakeInterproceduralAnalysis interprocedural;

	private AnalysisState<UnitLattice> state;

	private ExpressionSet[] emptyParams;

	private it.unive.lisa.analysis.StatementStore<UnitLattice> expressions;

	private IMPNewObj newObj(
			boolean staticallyAllocated) {
		domain = new RecordingDomain();
		interprocedural = new FakeInterproceduralAnalysis(domain);
		ProgramState<UnitLattice> programState = new ProgramState<>(UnitLattice.INSTANCE, new ExpressionSet());
		state = new AnalysisState<>(programState).withExecution(programState);
		expressions = new it.unive.lisa.analysis.StatementStore<>(state);
		emptyParams = new ExpressionSet[0];
		return new IMPNewObj(TestFixtures.CFG, "test", 1, 1,
				it.unive.lisa.imp.types.ClassType.register("TestUnit", TestFixtures.UNIT), staticallyAllocated,
				new Expression[0]);
	}

	@Test
	public void allocatesTheMemoryRegionBeforeAttemptingTheConstructorCall() {
		// IMPNewObj's forwardSemanticsAux allocates the object and wires up
		// the instrumented receiver BEFORE dispatching to the (unresolved)
		// constructor call; since this harness has no real interprocedural
		// call-resolution machinery available (lisa-imp has no concrete
		// domain to build one with, same limitation as lisa-program), the
		// constructor-call phase itself cannot be exercised here and is
		// expected to fail - but the allocation phase that happens first
		// must still have run and been recorded correctly
		IMPNewObj obj = newObj(false);

		assertThrows(UnsupportedOperationException.class,
				() -> obj.forwardSemanticsAux(interprocedural, state, emptyParams, expressions));

		assertFalse(domain.smallStepCalls.isEmpty(), "the allocation must have been attempted");
		MemoryAllocation allocation = (MemoryAllocation) domain.smallStepCalls.get(0);
		assertEquals(it.unive.lisa.imp.types.ClassType.register("TestUnit", TestFixtures.UNIT),
				allocation.getStaticType());
		assertFalse(domain.assignCalls.isEmpty(), "the receiver must have been bound to the allocation");
	}

	@Test
	public void staticAndDynamicAllocationAreNotEqual() {
		IMPNewObj dyn = newObj(false);
		IMPNewObj stat = newObj(true);
		assertNotEquals(dyn, stat);
	}

}
