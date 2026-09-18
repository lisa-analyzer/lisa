package it.unive.lisa.program.cfg.statement.numeric;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.ProgramState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.program.testsupport.FakeInterproceduralAnalysis;
import it.unive.lisa.program.testsupport.RecordingDomain;
import it.unive.lisa.program.testsupport.TestFixtures;
import it.unive.lisa.program.testsupport.UnitLattice;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.program.type.StringType;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingAdd;
import it.unive.lisa.type.Untyped;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class AdditionTest {

	private final Variable leftOperand = new Variable(Untyped.INSTANCE, "left", TestFixtures.LOCATION);

	private final Variable rightOperand = new Variable(Untyped.INSTANCE, "right", TestFixtures.LOCATION);

	private RecordingDomain domain;

	private FakeInterproceduralAnalysis interprocedural;

	private AnalysisState<UnitLattice> state;

	private StatementStore<UnitLattice> expressions;

	private Addition addition() {
		domain = new RecordingDomain();
		interprocedural = new FakeInterproceduralAnalysis(domain);
		ProgramState<UnitLattice> programState = new ProgramState<>(UnitLattice.INSTANCE, new ExpressionSet());
		// the single-arg AnalysisState constructor starts at top; withExecution
		// is needed to actually install our (non-top) starting lattice value
		state = new AnalysisState<>(programState).withExecution(programState);
		expressions = new StatementStore<>(state);
		VariableRef left = new VariableRef(TestFixtures.CFG, TestFixtures.LOCATION, "left");
		VariableRef right = new VariableRef(TestFixtures.CFG, TestFixtures.LOCATION, "right");
		return new Addition(TestFixtures.CFG, TestFixtures.LOCATION, left, right);
	}

	@Test
	public void bothOperandsNumericBuildsTheAdditionOperatorAndDelegatesToSmallStepSemantics()
			throws SemanticException {
		Addition add = addition();
		domain.setRuntimeTypes(leftOperand, Set.of(Int32Type.INSTANCE));
		domain.setRuntimeTypes(rightOperand, Set.of(Int32Type.INSTANCE));

		AnalysisState<UnitLattice> result = add.fwdBinarySemantics(
				interprocedural, state, leftOperand, rightOperand, expressions);

		assertEquals(1, domain.smallStepCalls.size());
		BinaryExpression built = (BinaryExpression) domain.smallStepCalls.get(0);
		assertEquals(NumericNonOverflowingAdd.INSTANCE, built.getOperator());
		assertEquals(add.getStaticType(), built.getStaticType());
		assertEquals(leftOperand, built.getLeft());
		assertEquals(rightOperand, built.getRight());
		assertEquals(add.getLocation(), built.getCodeLocation());
		// the recording domain leaves the underlying lattice value untouched:
		// this checks the real result is the one returned by
		// smallStepSemantics (its tracked computed expression), not bottom
		assertEquals(UnitLattice.INSTANCE, result.getExecutionState());
		assertEquals(built, result.getExecutionExpressions().elements().iterator().next());
	}

	@Test
	public void leftOperandNotNumericYieldsBottomWithoutComputingTheAddition()
			throws SemanticException {
		Addition add = addition();
		domain.setRuntimeTypes(leftOperand, Set.of(StringType.INSTANCE));
		domain.setRuntimeTypes(rightOperand, Set.of(Int32Type.INSTANCE));

		AnalysisState<UnitLattice> result = add.fwdBinarySemantics(
				interprocedural, state, leftOperand, rightOperand, expressions);

		assertTrue(result.getExecutionState().isBottom());
		assertTrue(domain.smallStepCalls.isEmpty());
	}

	@Test
	public void rightOperandNotNumericYieldsBottomWithoutComputingTheAddition()
			throws SemanticException {
		Addition add = addition();
		domain.setRuntimeTypes(leftOperand, Set.of(Int32Type.INSTANCE));
		domain.setRuntimeTypes(rightOperand, Set.of(StringType.INSTANCE));

		AnalysisState<UnitLattice> result = add.fwdBinarySemantics(
				interprocedural, state, leftOperand, rightOperand, expressions);

		assertTrue(result.getExecutionState().isBottom());
		assertTrue(domain.smallStepCalls.isEmpty());
	}

}
