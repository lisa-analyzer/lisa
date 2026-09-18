package it.unive.lisa.program.cfg.statement.string;

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
import it.unive.lisa.symbolic.value.operator.binary.StringEndsWith;
import it.unive.lisa.type.Untyped;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class EndsWithTest {

	private final Variable leftOperand = new Variable(Untyped.INSTANCE, "left", TestFixtures.LOCATION);

	private final Variable rightOperand = new Variable(Untyped.INSTANCE, "right", TestFixtures.LOCATION);

	private RecordingDomain domain;

	private FakeInterproceduralAnalysis interprocedural;

	private AnalysisState<UnitLattice> state;

	private StatementStore<UnitLattice> expressions;

	private EndsWith endsWith() {
		domain = new RecordingDomain();
		interprocedural = new FakeInterproceduralAnalysis(domain);
		ProgramState<UnitLattice> programState = new ProgramState<>(UnitLattice.INSTANCE, new ExpressionSet());
		state = new AnalysisState<>(programState).withExecution(programState);
		expressions = new StatementStore<>(state);
		VariableRef left = new VariableRef(TestFixtures.CFG, TestFixtures.LOCATION, "left");
		VariableRef right = new VariableRef(TestFixtures.CFG, TestFixtures.LOCATION, "right");
		return new EndsWith(TestFixtures.CFG, TestFixtures.LOCATION, left, right);
	}

	@Test
	public void bothOperandsStringBuildsTheEndsWithOperatorAndDelegatesToSmallStepSemantics()
			throws SemanticException {
		EndsWith endsWith = endsWith();
		domain.setRuntimeTypes(leftOperand, Set.of(StringType.INSTANCE));
		domain.setRuntimeTypes(rightOperand, Set.of(StringType.INSTANCE));

		AnalysisState<UnitLattice> result = endsWith.fwdBinarySemantics(
				interprocedural, state, leftOperand, rightOperand, expressions);

		assertEquals(1, domain.smallStepCalls.size());
		BinaryExpression built = (BinaryExpression) domain.smallStepCalls.get(0);
		assertEquals(StringEndsWith.INSTANCE, built.getOperator());
		assertEquals(endsWith.getStaticType(), built.getStaticType());
		assertEquals(leftOperand, built.getLeft());
		assertEquals(rightOperand, built.getRight());
		assertEquals(UnitLattice.INSTANCE, result.getExecutionState());
	}

	@Test
	public void leftOperandNotStringYieldsBottomWithoutComputingTheEndsWith()
			throws SemanticException {
		EndsWith endsWith = endsWith();
		domain.setRuntimeTypes(leftOperand, Set.of(Int32Type.INSTANCE));
		domain.setRuntimeTypes(rightOperand, Set.of(StringType.INSTANCE));

		AnalysisState<UnitLattice> result = endsWith.fwdBinarySemantics(
				interprocedural, state, leftOperand, rightOperand, expressions);

		assertTrue(result.getExecutionState().isBottom());
		assertTrue(domain.smallStepCalls.isEmpty());
	}

	@Test
	public void rightOperandNotStringYieldsBottomWithoutComputingTheEndsWith()
			throws SemanticException {
		EndsWith endsWith = endsWith();
		domain.setRuntimeTypes(leftOperand, Set.of(StringType.INSTANCE));
		domain.setRuntimeTypes(rightOperand, Set.of(Int32Type.INSTANCE));

		AnalysisState<UnitLattice> result = endsWith.fwdBinarySemantics(
				interprocedural, state, leftOperand, rightOperand, expressions);

		assertTrue(result.getExecutionState().isBottom());
		assertTrue(domain.smallStepCalls.isEmpty());
	}

}
