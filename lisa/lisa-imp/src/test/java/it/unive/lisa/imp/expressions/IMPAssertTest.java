package it.unive.lisa.imp.expressions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.ProgramState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.imp.testsupport.FakeInterproceduralAnalysis;
import it.unive.lisa.imp.testsupport.RecordingDomain;
import it.unive.lisa.imp.testsupport.TestFixtures;
import it.unive.lisa.imp.testsupport.UnitLattice;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Untyped;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class IMPAssertTest {

	private final Variable operand = new Variable(Untyped.INSTANCE, "cond", TestFixtures.LOCATION);

	private RecordingDomain domain;

	private FakeInterproceduralAnalysis interprocedural;

	private AnalysisState<UnitLattice> state;

	private StatementStore<UnitLattice> expressions;

	private IMPAssert impAssert() {
		domain = new RecordingDomain();
		interprocedural = new FakeInterproceduralAnalysis(domain);
		ProgramState<UnitLattice> programState = new ProgramState<>(UnitLattice.INSTANCE, new ExpressionSet());
		state = new AnalysisState<>(programState).withExecution(programState);
		expressions = new StatementStore<>(state);
		VariableRef expr = new VariableRef(TestFixtures.CFG, TestFixtures.LOCATION, "cond");
		return new IMPAssert(TestFixtures.CFG, "test", 1, 1, expr);
	}

	@Test
	public void booleanOperandProducesASkipInsteadOfTrackingTheAssertedValue()
			throws SemanticException {
		IMPAssert stmt = impAssert();
		domain.setRuntimeTypes(operand, Set.of(BoolType.INSTANCE));

		stmt.fwdUnarySemantics(interprocedural, state, operand, expressions);

		// per the actual implementation, a well-typed assert does not keep
		// track of the asserted expression at all: it just delegates to a
		// Skip, regardless of what was being asserted
		assertEquals(1, domain.smallStepCalls.size());
		assertEquals(Skip.class, domain.smallStepCalls.get(0).getClass());
	}

	@Test
	public void nonBooleanOperandYieldsBottomWithoutComputingAnything()
			throws SemanticException {
		IMPAssert stmt = impAssert();
		domain.setRuntimeTypes(operand, Set.of(Int32Type.INSTANCE));

		AnalysisState<UnitLattice> result = stmt.fwdUnarySemantics(
				interprocedural, state, operand, expressions);

		assertTrue(domain.smallStepCalls.isEmpty());
		assertTrue(result.getExecutionState().isBottom());
	}

	@Test
	public void anyBooleanAmongMultipleRuntimeTypesIsEnough()
			throws SemanticException {
		// the guard is "none match boolean", so as long as ONE runtime type
		// is boolean, the assert is considered well-typed
		IMPAssert stmt = impAssert();
		domain.setRuntimeTypes(operand, Set.of(Int32Type.INSTANCE, BoolType.INSTANCE));

		stmt.fwdUnarySemantics(interprocedural, state, operand, expressions);

		assertEquals(1, domain.smallStepCalls.size());
	}

}
