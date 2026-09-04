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
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingDiv;
import it.unive.lisa.type.Untyped;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class DivisionTest {

	private final Variable leftOperand = new Variable(Untyped.INSTANCE, "left", TestFixtures.LOCATION);

	private final Variable rightOperand = new Variable(Untyped.INSTANCE, "right", TestFixtures.LOCATION);

	private RecordingDomain domain;

	private FakeInterproceduralAnalysis interprocedural;

	private AnalysisState<UnitLattice> state;

	private StatementStore<UnitLattice> expressions;

	private Division division() {
		domain = new RecordingDomain();
		interprocedural = new FakeInterproceduralAnalysis(domain);
		ProgramState<UnitLattice> programState = new ProgramState<>(UnitLattice.INSTANCE, new ExpressionSet());
		state = new AnalysisState<>(programState).withExecution(programState);
		expressions = new StatementStore<>(state);
		VariableRef left = new VariableRef(TestFixtures.CFG, TestFixtures.LOCATION, "left");
		VariableRef right = new VariableRef(TestFixtures.CFG, TestFixtures.LOCATION, "right");
		return new Division(TestFixtures.CFG, TestFixtures.LOCATION, left, right);
	}

	@Test
	public void bothOperandsNumericBuildsTheDivisionOperatorAndDelegatesToSmallStepSemantics()
			throws SemanticException {
		Division div = division();
		domain.setRuntimeTypes(leftOperand, Set.of(Int32Type.INSTANCE));
		domain.setRuntimeTypes(rightOperand, Set.of(Int32Type.INSTANCE));

		AnalysisState<UnitLattice> result = div.fwdBinarySemantics(
				interprocedural, state, leftOperand, rightOperand, expressions);

		assertEquals(1, domain.smallStepCalls.size());
		BinaryExpression built = (BinaryExpression) domain.smallStepCalls.get(0);
		assertEquals(NumericNonOverflowingDiv.INSTANCE, built.getOperator());
		assertEquals(div.getStaticType(), built.getStaticType());
		assertEquals(leftOperand, built.getLeft());
		assertEquals(rightOperand, built.getRight());
		assertEquals(div.getLocation(), built.getCodeLocation());
		assertEquals(UnitLattice.INSTANCE, result.getExecutionState());
		assertEquals(built, result.getExecutionExpressions().elements().iterator().next());
	}

	@Test
	public void leftOperandNotNumericYieldsBottomWithoutComputingTheDivision()
			throws SemanticException {
		Division div = division();
		domain.setRuntimeTypes(leftOperand, Set.of(StringType.INSTANCE));
		domain.setRuntimeTypes(rightOperand, Set.of(Int32Type.INSTANCE));

		AnalysisState<UnitLattice> result = div.fwdBinarySemantics(
				interprocedural, state, leftOperand, rightOperand, expressions);

		assertTrue(result.getExecutionState().isBottom());
		assertTrue(domain.smallStepCalls.isEmpty());
	}

	@Test
	public void rightOperandNotNumericYieldsBottomWithoutComputingTheDivision()
			throws SemanticException {
		Division div = division();
		domain.setRuntimeTypes(leftOperand, Set.of(Int32Type.INSTANCE));
		domain.setRuntimeTypes(rightOperand, Set.of(StringType.INSTANCE));

		AnalysisState<UnitLattice> result = div.fwdBinarySemantics(
				interprocedural, state, leftOperand, rightOperand, expressions);

		assertTrue(result.getExecutionState().isBottom());
		assertTrue(domain.smallStepCalls.isEmpty());
	}

}
