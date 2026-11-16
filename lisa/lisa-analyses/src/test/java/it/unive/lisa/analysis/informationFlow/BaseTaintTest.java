package it.unive.lisa.analysis.informationFlow;

import static org.junit.jupiter.api.Assertions.assertEquals;

import it.unive.lisa.TestParameterProvider;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.lattices.informationFlow.SimpleTaint;
import it.unive.lisa.lattices.informationFlow.ThreeTaint;
import it.unive.lisa.program.annotations.Annotation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingAdd;
import it.unive.lisa.symbolic.value.operator.ternary.StringSubstring;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import org.junit.jupiter.api.Test;

public class BaseTaintTest {

	private final ProgramPoint pp = TestParameterProvider.provideParam(null, ProgramPoint.class);
	private final SemanticOracle oracle = TestParameterProvider.provideParam(null, SemanticOracle.class);

	private final ThreeLevelsTaint threeLevels = new ThreeLevelsTaint();
	private final TwoLevelsTaint twoLevels = new TwoLevelsTaint();

	private Variable annotated(
			Annotation ann) {
		Variable v = new Variable(Int32Type.INSTANCE, "x", pp.getLocation());
		v.addAnnotation(ann);
		return v;
	}

	@Test
	public void constantsAreAlwaysClean()
			throws SemanticException {
		Constant c = new Constant(Int32Type.INSTANCE, 5, pp.getLocation());
		assertEquals(ThreeTaint.CLEAN, threeLevels.evalConstant(c, pp, oracle));
		assertEquals(SimpleTaint.CLEAN, twoLevels.evalConstant(c, pp, oracle));
	}

	@Test
	public void aVariableAnnotatedAsTaintedSourceEvaluatesToTainted()
			throws SemanticException {
		Variable v = annotated(BaseTaint.TAINTED_ANNOTATION);
		assertEquals(ThreeTaint.TAINTED, threeLevels.fixedVariable(v, pp, oracle));
		assertEquals(SimpleTaint.TAINTED, twoLevels.fixedVariable(v, pp, oracle));
	}

	@Test
	public void aVariableAnnotatedAsSanitizerEvaluatesToClean()
			throws SemanticException {
		Variable v = annotated(BaseTaint.CLEAN_ANNOTATION);
		assertEquals(ThreeTaint.CLEAN, threeLevels.fixedVariable(v, pp, oracle));
		assertEquals(SimpleTaint.CLEAN, twoLevels.fixedVariable(v, pp, oracle));
	}

	@Test
	public void anUnaryExpressionPreservesTheTaintOfItsOperand()
			throws SemanticException {
		UnaryExpression exp = new UnaryExpression(
				Int32Type.INSTANCE,
				new Variable(Int32Type.INSTANCE, "x", pp.getLocation()),
				NumericNegation.INSTANCE,
				pp.getLocation());
		assertEquals(ThreeTaint.TAINTED, threeLevels.evalUnaryExpression(exp, ThreeTaint.TAINTED, pp, oracle));
		assertEquals(ThreeTaint.CLEAN, threeLevels.evalUnaryExpression(exp, ThreeTaint.CLEAN, pp, oracle));
	}

	@Test
	public void aBinaryExpressionIsTaintedIfEitherOperandIs()
			throws SemanticException {
		BinaryExpression exp = new BinaryExpression(
				Int32Type.INSTANCE,
				new Variable(Int32Type.INSTANCE, "x", pp.getLocation()),
				new Variable(Int32Type.INSTANCE, "y", pp.getLocation()),
				NumericNonOverflowingAdd.INSTANCE,
				pp.getLocation());

		assertEquals(ThreeTaint.TAINTED,
				threeLevels.evalBinaryExpression(exp, ThreeTaint.TAINTED, ThreeTaint.CLEAN, pp, oracle));
		assertEquals(ThreeTaint.TAINTED,
				threeLevels.evalBinaryExpression(exp, ThreeTaint.CLEAN, ThreeTaint.TAINTED, pp, oracle));
		assertEquals(ThreeTaint.CLEAN,
				threeLevels.evalBinaryExpression(exp, ThreeTaint.CLEAN, ThreeTaint.CLEAN, pp, oracle));
	}

	@Test
	public void aTernaryExpressionIsTaintedIfAnyOperandIs()
			throws SemanticException {
		TernaryExpression exp = new TernaryExpression(
				Int32Type.INSTANCE,
				new Variable(Int32Type.INSTANCE, "x", pp.getLocation()),
				new Variable(Int32Type.INSTANCE, "y", pp.getLocation()),
				new Variable(Int32Type.INSTANCE, "z", pp.getLocation()),
				StringSubstring.INSTANCE,
				pp.getLocation());

		assertEquals(ThreeTaint.TAINTED,
				threeLevels.evalTernaryExpression(exp, ThreeTaint.CLEAN, ThreeTaint.CLEAN, ThreeTaint.TAINTED, pp,
						oracle));
		assertEquals(ThreeTaint.CLEAN,
				threeLevels.evalTernaryExpression(exp, ThreeTaint.CLEAN, ThreeTaint.CLEAN, ThreeTaint.CLEAN, pp,
						oracle));
	}

	@Test
	public void topAndBottomAreLatticeSpecific() {
		assertEquals(ThreeTaint.TOP, threeLevels.top());
		assertEquals(ThreeTaint.BOTTOM, threeLevels.bottom());
		// with only two levels, TAINTED doubles as the top element
		assertEquals(SimpleTaint.TAINTED, twoLevels.top());
		assertEquals(SimpleTaint.BOTTOM, twoLevels.bottom());
	}

}
