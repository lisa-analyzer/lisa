package it.unive.lisa.program.cfg.statement.types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.ProgramState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.program.cfg.statement.literal.TypeLiteral;
import it.unive.lisa.program.testsupport.FakeInterproceduralAnalysis;
import it.unive.lisa.program.testsupport.RecordingDomain;
import it.unive.lisa.program.testsupport.TestFixtures;
import it.unive.lisa.program.testsupport.UnitLattice;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.TypeCheck;
import it.unive.lisa.type.TypeTokenType;
import it.unive.lisa.type.Untyped;
import java.util.Collections;
import org.junit.jupiter.api.Test;

public class IsInstanceTest {

	private final Variable leftOperand = new Variable(Untyped.INSTANCE, "left", TestFixtures.LOCATION);

	private RecordingDomain domain;

	private FakeInterproceduralAnalysis interprocedural;

	private AnalysisState<UnitLattice> state;

	private StatementStore<UnitLattice> expressions;

	private IsInstance isInstance() {
		domain = new RecordingDomain();
		interprocedural = new FakeInterproceduralAnalysis(domain);
		ProgramState<UnitLattice> programState = new ProgramState<>(UnitLattice.INSTANCE, new ExpressionSet());
		state = new AnalysisState<>(programState).withExecution(programState);
		expressions = new StatementStore<>(state);
		VariableRef left = new VariableRef(TestFixtures.CFG, TestFixtures.LOCATION, "left");
		TypeLiteral right = new TypeLiteral(TestFixtures.CFG, TestFixtures.LOCATION, Int32Type.INSTANCE);
		return new IsInstance(TestFixtures.CFG, TestFixtures.LOCATION, left, right);
	}

	// unlike Cast (whose built expression's static type is getStaticType(),
	// always Untyped), a type check always yields a boolean value: the built
	// expression's static type here is the program's boolean type, not
	// getStaticType() of the IsInstance node itself (which is also Untyped,
	// per the same 5-arg BinaryExpression super constructor)
	@Test
	public void buildsTheTypeCheckOperatorWithABooleanStaticType()
			throws SemanticException {
		IsInstance isInstance = isInstance();
		Constant target = new Constant(
				new TypeTokenType(Collections.singleton(Int32Type.INSTANCE)), Int32Type.INSTANCE,
				TestFixtures.LOCATION);

		AnalysisState<UnitLattice> result = isInstance.fwdBinarySemantics(
				interprocedural, state, leftOperand, target, expressions);

		assertEquals(1, domain.smallStepCalls.size());
		BinaryExpression built = (BinaryExpression) domain.smallStepCalls.get(0);
		assertEquals(TypeCheck.INSTANCE, built.getOperator());
		assertEquals(BoolType.INSTANCE, built.getStaticType());
		assertEquals(leftOperand, built.getLeft());
		assertEquals(target, built.getRight());
		assertEquals(UnitLattice.INSTANCE, result.getExecutionState());
	}

	@Test
	public void rightOperandNotATypeConstantYieldsBottomWithoutComputingTheCheck()
			throws SemanticException {
		IsInstance isInstance = isInstance();
		Variable notAType = new Variable(Untyped.INSTANCE, "right", TestFixtures.LOCATION);

		AnalysisState<UnitLattice> result = isInstance.fwdBinarySemantics(
				interprocedural, state, leftOperand, notAType, expressions);

		assertTrue(result.getExecutionState().isBottom());
		assertTrue(domain.smallStepCalls.isEmpty());
	}

	@Test
	public void rightOperandConstantNotWrappingATypeYieldsBottomWithoutComputingTheCheck()
			throws SemanticException {
		IsInstance isInstance = isInstance();
		Constant notAType = new Constant(Int32Type.INSTANCE, 5, TestFixtures.LOCATION);

		AnalysisState<UnitLattice> result = isInstance.fwdBinarySemantics(
				interprocedural, state, leftOperand, notAType, expressions);

		assertTrue(result.getExecutionState().isBottom());
		assertTrue(domain.smallStepCalls.isEmpty());
	}

}
