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
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.program.type.StringType;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.TypeCast;
import it.unive.lisa.symbolic.value.operator.binary.TypeConv;
import it.unive.lisa.type.TypeTokenType;
import it.unive.lisa.type.Untyped;
import java.util.Collections;
import org.junit.jupiter.api.Test;

public class CastTest {

	private final Variable leftOperand = new Variable(Untyped.INSTANCE, "left", TestFixtures.LOCATION);

	private RecordingDomain domain;

	private FakeInterproceduralAnalysis interprocedural;

	private AnalysisState<UnitLattice> state;

	private StatementStore<UnitLattice> expressions;

	private Cast cast() {
		domain = new RecordingDomain();
		interprocedural = new FakeInterproceduralAnalysis(domain);
		ProgramState<UnitLattice> programState = new ProgramState<>(UnitLattice.INSTANCE, new ExpressionSet());
		state = new AnalysisState<>(programState).withExecution(programState);
		expressions = new StatementStore<>(state);
		VariableRef left = new VariableRef(TestFixtures.CFG, TestFixtures.LOCATION, "left");
		TypeLiteral right = new TypeLiteral(TestFixtures.CFG, TestFixtures.LOCATION, Int32Type.INSTANCE);
		return new Cast(TestFixtures.CFG, TestFixtures.LOCATION, left, right);
	}

	// per Cast's own 5-arg BinaryExpression super call (no explicit static
	// type given), the static type of a Cast expression is always Untyped:
	// this was previously hidden by a bug that hardcoded the built
	// expression's type to BooleanType (copy-pasted from IsInstance) instead
	// of using getStaticType() - fixed to use getStaticType(), which for
	// Cast is always Untyped.INSTANCE
	@Test
	public void castingToANumericTypeBuildsATypeConvOperatorSinceItIsAConversion()
			throws SemanticException {
		Cast cast = cast();
		Constant target = new Constant(
				new TypeTokenType(Collections.singleton(Int32Type.INSTANCE)), Int32Type.INSTANCE,
				TestFixtures.LOCATION);

		AnalysisState<UnitLattice> result = cast.fwdBinarySemantics(
				interprocedural, state, leftOperand, target, expressions);

		assertEquals(1, domain.smallStepCalls.size());
		BinaryExpression built = (BinaryExpression) domain.smallStepCalls.get(0);
		assertEquals(TypeConv.INSTANCE, built.getOperator());
		assertEquals(Untyped.INSTANCE, built.getStaticType());
		assertEquals(cast.getStaticType(), built.getStaticType());
		assertEquals(leftOperand, built.getLeft());
		assertEquals(target, built.getRight());
		assertEquals(UnitLattice.INSTANCE, result.getExecutionState());
	}

	// StringType does not override castIsConversion(), so it stays at Type's
	// default (false): casting to it must use TypeCast, not TypeConv
	@Test
	public void castingToANonConvertibleTypeBuildsATypeCastOperator()
			throws SemanticException {
		Cast cast = cast();
		Constant target = new Constant(
				new TypeTokenType(Collections.singleton(StringType.INSTANCE)), StringType.INSTANCE,
				TestFixtures.LOCATION);

		cast.fwdBinarySemantics(interprocedural, state, leftOperand, target, expressions);

		assertEquals(1, domain.smallStepCalls.size());
		BinaryExpression built = (BinaryExpression) domain.smallStepCalls.get(0);
		assertEquals(TypeCast.INSTANCE, built.getOperator());
		assertEquals(leftOperand, built.getLeft());
		assertEquals(target, built.getRight());
	}

	@Test
	public void rightOperandNotATypeConstantYieldsBottomWithoutComputingTheCast()
			throws SemanticException {
		Cast cast = cast();
		Variable notAType = new Variable(Untyped.INSTANCE, "right", TestFixtures.LOCATION);

		AnalysisState<UnitLattice> result = cast.fwdBinarySemantics(
				interprocedural, state, leftOperand, notAType, expressions);

		assertTrue(result.getExecutionState().isBottom());
		assertTrue(domain.smallStepCalls.isEmpty());
	}

	@Test
	public void rightOperandConstantNotWrappingATypeYieldsBottomWithoutComputingTheCast()
			throws SemanticException {
		Cast cast = cast();
		Constant notAType = new Constant(Int32Type.INSTANCE, 5, TestFixtures.LOCATION);

		AnalysisState<UnitLattice> result = cast.fwdBinarySemantics(
				interprocedural, state, leftOperand, notAType, expressions);

		assertTrue(result.getExecutionState().isBottom());
		assertTrue(domain.smallStepCalls.isEmpty());
	}

}
