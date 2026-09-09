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
import it.unive.lisa.program.type.Int64Type;
import it.unive.lisa.program.type.StringType;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingAdd;
import it.unive.lisa.symbolic.value.operator.binary.StringConcat;
import it.unive.lisa.type.Untyped;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class IMPAddOrConcatTest {

	private final Variable leftOperand = new Variable(Untyped.INSTANCE, "left", TestFixtures.LOCATION);

	private final Variable rightOperand = new Variable(Untyped.INSTANCE, "right", TestFixtures.LOCATION);

	private RecordingDomain domain;

	private FakeInterproceduralAnalysis interprocedural;

	private AnalysisState<UnitLattice> state;

	private StatementStore<UnitLattice> expressions;

	private IMPAddOrConcat addOrConcat() {
		domain = new RecordingDomain();
		interprocedural = new FakeInterproceduralAnalysis(domain);
		ProgramState<UnitLattice> programState = new ProgramState<>(UnitLattice.INSTANCE, new ExpressionSet());
		state = new AnalysisState<>(programState).withExecution(programState);
		expressions = new StatementStore<>(state);
		VariableRef left = new VariableRef(TestFixtures.CFG, TestFixtures.LOCATION, "left");
		VariableRef right = new VariableRef(TestFixtures.CFG, TestFixtures.LOCATION, "right");
		return new IMPAddOrConcat(TestFixtures.CFG, "test", 1, 1, left, right);
	}

	@Test
	public void bothOperandsStringConcatenates()
			throws SemanticException {
		IMPAddOrConcat add = addOrConcat();
		domain.setRuntimeTypes(leftOperand, Set.of(StringType.INSTANCE));
		domain.setRuntimeTypes(rightOperand, Set.of(StringType.INSTANCE));

		add.fwdBinarySemantics(interprocedural, state, leftOperand, rightOperand, expressions);

		assertEquals(1, domain.smallStepCalls.size());
		BinaryExpression built = (BinaryExpression) domain.smallStepCalls.get(0);
		assertEquals(StringConcat.INSTANCE, built.getOperator());
	}

	@Test
	public void bothOperandsNumericAdds()
			throws SemanticException {
		IMPAddOrConcat add = addOrConcat();
		domain.setRuntimeTypes(leftOperand, Set.of(Int32Type.INSTANCE));
		domain.setRuntimeTypes(rightOperand, Set.of(Int32Type.INSTANCE));

		add.fwdBinarySemantics(interprocedural, state, leftOperand, rightOperand, expressions);

		assertEquals(1, domain.smallStepCalls.size());
		BinaryExpression built = (BinaryExpression) domain.smallStepCalls.get(0);
		assertEquals(NumericNonOverflowingAdd.INSTANCE, built.getOperator());
	}

	@Test
	public void stringAndNumericIsRejected()
			throws SemanticException {
		IMPAddOrConcat add = addOrConcat();
		domain.setRuntimeTypes(leftOperand, Set.of(StringType.INSTANCE));
		domain.setRuntimeTypes(rightOperand, Set.of(Int32Type.INSTANCE));

		AnalysisState<UnitLattice> result = add.fwdBinarySemantics(
				interprocedural, state, leftOperand, rightOperand, expressions);

		// neither string-concat (right is not string/untyped) nor numeric-add
		// (left is not numeric/untyped) applies to this combination, so no
		// operation is ever attempted for it
		assertTrue(domain.smallStepCalls.isEmpty());
		assertTrue(result.getExecutionState().isBottom());
	}

	@Test
	public void numericAndStringIsRejected()
			throws SemanticException {
		IMPAddOrConcat add = addOrConcat();
		domain.setRuntimeTypes(leftOperand, Set.of(Int32Type.INSTANCE));
		domain.setRuntimeTypes(rightOperand, Set.of(StringType.INSTANCE));

		AnalysisState<UnitLattice> result = add.fwdBinarySemantics(
				interprocedural, state, leftOperand, rightOperand, expressions);

		assertTrue(domain.smallStepCalls.isEmpty());
		assertTrue(result.getExecutionState().isBottom());
	}

	@Test
	public void neitherStringNorNumericIsRejected()
			throws SemanticException {
		IMPAddOrConcat add = addOrConcat();
		domain.setRuntimeTypes(leftOperand, Set.of(BoolType.INSTANCE));
		domain.setRuntimeTypes(rightOperand, Set.of(BoolType.INSTANCE));

		AnalysisState<UnitLattice> result = add.fwdBinarySemantics(
				interprocedural, state, leftOperand, rightOperand, expressions);

		assertTrue(domain.smallStepCalls.isEmpty());
		assertTrue(result.getExecutionState().isBottom());
	}

	@Test
	public void untypedLeftWithStringRightConcatenates()
			throws SemanticException {
		IMPAddOrConcat add = addOrConcat();
		domain.setRuntimeTypes(leftOperand, Set.of(Untyped.INSTANCE));
		domain.setRuntimeTypes(rightOperand, Set.of(StringType.INSTANCE));

		add.fwdBinarySemantics(interprocedural, state, leftOperand, rightOperand, expressions);

		assertEquals(1, domain.smallStepCalls.size());
		BinaryExpression built = (BinaryExpression) domain.smallStepCalls.get(0);
		assertEquals(StringConcat.INSTANCE, built.getOperator());
	}

	@Test
	public void bothOperandsUntypedIsTreatedAsNumericAddition()
			throws SemanticException {
		// per the class's own javadoc-adjacent comment ("arbitrary choice"),
		// two untyped operands are treated as a numeric sum
		IMPAddOrConcat add = addOrConcat();
		domain.setRuntimeTypes(leftOperand, Set.of(Untyped.INSTANCE));
		domain.setRuntimeTypes(rightOperand, Set.of(Untyped.INSTANCE));

		add.fwdBinarySemantics(interprocedural, state, leftOperand, rightOperand, expressions);

		assertEquals(1, domain.smallStepCalls.size());
		BinaryExpression built = (BinaryExpression) domain.smallStepCalls.get(0);
		assertEquals(NumericNonOverflowingAdd.INSTANCE, built.getOperator());
	}

	@Test
	public void numericLeftWithUntypedRightAdds()
			throws SemanticException {
		IMPAddOrConcat add = addOrConcat();
		domain.setRuntimeTypes(leftOperand, Set.of(Int32Type.INSTANCE));
		domain.setRuntimeTypes(rightOperand, Set.of(Untyped.INSTANCE));

		add.fwdBinarySemantics(interprocedural, state, leftOperand, rightOperand, expressions);

		assertEquals(1, domain.smallStepCalls.size());
		BinaryExpression built = (BinaryExpression) domain.smallStepCalls.get(0);
		assertEquals(NumericNonOverflowingAdd.INSTANCE, built.getOperator());
	}

	@Test
	public void multipleRuntimeTypesTriggerOneOperationPerValidCombination()
			throws SemanticException {
		// left might be Int32 or Int64 at runtime: both are numeric, so both
		// combinations with a numeric right operand must be attempted
		IMPAddOrConcat add = addOrConcat();
		domain.setRuntimeTypes(leftOperand, Set.of(Int32Type.INSTANCE, Int64Type.INSTANCE));
		domain.setRuntimeTypes(rightOperand, Set.of(Int32Type.INSTANCE));

		add.fwdBinarySemantics(interprocedural, state, leftOperand, rightOperand, expressions);

		assertEquals(2, domain.smallStepCalls.size());
		for (var call : domain.smallStepCalls)
			assertEquals(NumericNonOverflowingAdd.INSTANCE, ((BinaryExpression) call).getOperator());
	}

}
