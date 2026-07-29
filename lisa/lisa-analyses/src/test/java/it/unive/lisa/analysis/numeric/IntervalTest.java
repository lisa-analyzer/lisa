package it.unive.lisa.analysis.numeric;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestParameterProvider;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.events.EventQueue;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.BitwiseAnd;
import it.unive.lisa.symbolic.value.operator.binary.BitwiseOr;
import it.unive.lisa.symbolic.value.operator.binary.BitwiseShiftLeft;
import it.unive.lisa.symbolic.value.operator.binary.BitwiseShiftRight;
import it.unive.lisa.symbolic.value.operator.binary.BitwiseUnsignedShiftRight;
import it.unive.lisa.symbolic.value.operator.binary.BitwiseXor;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
import it.unive.lisa.symbolic.value.operator.binary.NumericAtan2;
import it.unive.lisa.symbolic.value.operator.binary.NumericMax;
import it.unive.lisa.symbolic.value.operator.binary.NumericMin;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingAdd;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingDiv;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingMod;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingMul;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingRem;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingSub;
import it.unive.lisa.symbolic.value.operator.binary.StringIndexOf;
import it.unive.lisa.symbolic.value.operator.binary.StringIndexOfChar;
import it.unive.lisa.symbolic.value.operator.binary.StringLastIndexOf;
import it.unive.lisa.symbolic.value.operator.binary.StringLastIndexOfChar;
import it.unive.lisa.symbolic.value.operator.binary.ValueComparison;
import it.unive.lisa.symbolic.value.operator.ternary.StringIndexOfCharFromIndex;
import it.unive.lisa.symbolic.value.operator.ternary.StringLastIndexOfCharFromIndex;
import it.unive.lisa.symbolic.value.operator.ternary.StringLastIndexOfFromIndex;
import it.unive.lisa.symbolic.value.operator.ternary.TernaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.BitwiseNegation;
import it.unive.lisa.symbolic.value.operator.unary.NumericAbs;
import it.unive.lisa.symbolic.value.operator.unary.NumericAcos;
import it.unive.lisa.symbolic.value.operator.unary.NumericAsin;
import it.unive.lisa.symbolic.value.operator.unary.NumericAtan;
import it.unive.lisa.symbolic.value.operator.unary.NumericCeil;
import it.unive.lisa.symbolic.value.operator.unary.NumericCos;
import it.unive.lisa.symbolic.value.operator.unary.NumericExp;
import it.unive.lisa.symbolic.value.operator.unary.NumericFloor;
import it.unive.lisa.symbolic.value.operator.unary.NumericLog;
import it.unive.lisa.symbolic.value.operator.unary.NumericLog10;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.symbolic.value.operator.unary.NumericRound;
import it.unive.lisa.symbolic.value.operator.unary.NumericSin;
import it.unive.lisa.symbolic.value.operator.unary.NumericSqrt;
import it.unive.lisa.symbolic.value.operator.unary.NumericTan;
import it.unive.lisa.symbolic.value.operator.unary.NumericToRadians;
import it.unive.lisa.symbolic.value.operator.unary.StringLength;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.numeric.InfiniteIterationException;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class IntervalTest {

	private final ProgramPoint pp = TestParameterProvider.provideParam(null, ProgramPoint.class);

	private final Interval domain = new Interval();

	private final Variable variable = new Variable(Int32Type.INSTANCE, "x", pp.getLocation());

	private final Variable varAux = new Variable(Int32Type.INSTANCE, "aux", pp.getLocation());

	private final SemanticOracle oracle = TestParameterProvider.provideParam(null, SemanticOracle.class);

	private final ValueEnvironment<IntInterval> env = new ValueEnvironment<>(IntInterval.TOP)
			.putState(variable, IntInterval.TOP);

	private BinaryExpression mkBin(
			BinaryOperator op) {
		return new BinaryExpression(Int32Type.INSTANCE, varAux, variable, op, pp.getLocation());
	}

	private UnaryExpression mkUnary(
			UnaryOperator op) {
		return new UnaryExpression(Int32Type.INSTANCE, varAux, op, pp.getLocation());
	}

	private IntInterval evalBin(
			BinaryOperator op,
			IntInterval left,
			IntInterval right)
			throws SemanticException {
		return domain.evalBinaryExpression(mkBin(op), left, right, pp, oracle);
	}

	private IntInterval evalUn(
			UnaryOperator op,
			IntInterval arg)
			throws SemanticException {
		return domain.evalUnaryExpression(mkUnary(op), arg, pp, oracle);
	}

	private Satisfiability sat(
			BinaryOperator op,
			IntInterval left,
			IntInterval right) {
		return domain.satisfiesBinaryExpression(mkBin(op), left, right, pp, oracle);
	}

	private IntInterval mk(
			int v) {
		return new IntInterval(v, v);
	}

	private IntInterval mk(
			int l,
			int h) {
		return new IntInterval(l, h);
	}

	// -----------------------------------------------------------------------
	// evalConstant
	// -----------------------------------------------------------------------

	@Test
	public void testEvalConstantInteger() {
		assertEquals(mk(5), domain.evalConstant(new Constant(Int32Type.INSTANCE, 5, pp.getLocation()), pp, oracle));
		assertEquals(mk(-3),
				domain.evalConstant(new Constant(Int32Type.INSTANCE, -3, pp.getLocation()), pp, oracle));
		assertEquals(mk(0), domain.evalConstant(new Constant(Int32Type.INSTANCE, 0, pp.getLocation()), pp, oracle));
	}

	@Test
	public void testEvalConstantFloat() {
		// floor(3.7)=3, ceil(3.7)=4 → [3,4]
		assertEquals(mk(3, 4),
				domain.evalConstant(new Constant(Int32Type.INSTANCE, 3.7f, pp.getLocation()), pp, oracle));
		// floor(3.0)=3, ceil(3.0)=3 → [3,3]
		assertEquals(mk(3),
				domain.evalConstant(new Constant(Int32Type.INSTANCE, 3.0f, pp.getLocation()), pp, oracle));
	}

	@Test
	public void testEvalConstantDouble() {
		// floor(2.5)=2, ceil(2.5)=3 → [2,3]
		assertEquals(mk(2, 3),
				domain.evalConstant(new Constant(Int32Type.INSTANCE, 2.5, pp.getLocation()), pp, oracle));
		// floor(7.0)=7, ceil(7.0)=7 → [7,7]
		assertEquals(mk(7),
				domain.evalConstant(new Constant(Int32Type.INSTANCE, 7.0, pp.getLocation()), pp, oracle));
	}

	@Test
	public void testEvalConstantNonNumeric() {
		// Non-numeric constant → TOP
		assertEquals(IntInterval.TOP,
				domain.evalConstant(new Constant(Int32Type.INSTANCE, "hello", pp.getLocation()), pp, oracle));
	}

	// -----------------------------------------------------------------------
	// evalUnaryExpression – BitwiseNegation (~x = -x-1)
	// -----------------------------------------------------------------------

	@Test
	public void testBitwiseNegation() throws SemanticException {
		// ~[3,7] = [-8,-4]
		assertEquals(mk(-8, -4), evalUn(BitwiseNegation.INSTANCE, mk(3, 7)));
		// ~[0,0] = [-1,-1]
		assertEquals(mk(-1), evalUn(BitwiseNegation.INSTANCE, mk(0)));
		// ~[-3,-1] = [0,2]
		assertEquals(mk(0, 2), evalUn(BitwiseNegation.INSTANCE, mk(-3, -1)));
		// ~TOP = TOP
		assertEquals(IntInterval.TOP, evalUn(BitwiseNegation.INSTANCE, IntInterval.TOP));
		// ~[-Inf, 5] = [-6, +Inf]
		IntInterval negInf5 = new IntInterval(MathNumber.MINUS_INFINITY, new MathNumber(5));
		IntInterval expected = new IntInterval(new MathNumber(-6), MathNumber.PLUS_INFINITY);
		assertEquals(expected, evalUn(BitwiseNegation.INSTANCE, negInf5));
		// ~[3, +Inf] = [-Inf, -4]
		IntInterval three_posInf = new IntInterval(new MathNumber(3), MathNumber.PLUS_INFINITY);
		IntInterval expected2 = new IntInterval(MathNumber.MINUS_INFINITY, new MathNumber(-4));
		assertEquals(expected2, evalUn(BitwiseNegation.INSTANCE, three_posInf));
	}

	// -----------------------------------------------------------------------
	// evalUnaryExpression – NumericNegation
	// -----------------------------------------------------------------------

	@Test
	public void testNumericNegation() throws SemanticException {
		// -[3,7] = [-7,-3]
		assertEquals(mk(-7, -3), evalUn(NumericNegation.INSTANCE, mk(3, 7)));
		// -[0,0] = [0,0]
		assertEquals(mk(0), evalUn(NumericNegation.INSTANCE, mk(0)));
		// -[-3,-1] = [1,3]
		assertEquals(mk(1, 3), evalUn(NumericNegation.INSTANCE, mk(-3, -1)));
		// -TOP = TOP
		assertEquals(IntInterval.TOP, evalUn(NumericNegation.INSTANCE, IntInterval.TOP));
	}

	// -----------------------------------------------------------------------
	// evalUnaryExpression – NumericAbs
	// -----------------------------------------------------------------------

	@Test
	public void testNumericAbs() throws SemanticException {
		// abs([3,7]) = [3,7] (already non-negative)
		assertEquals(mk(3, 7), evalUn(NumericAbs.INSTANCE, mk(3, 7)));
		// abs([-7,-3]) = [3,7]
		assertEquals(mk(3, 7), evalUn(NumericAbs.INSTANCE, mk(-7, -3)));
		// abs([-3,5]) = [0,5] (5 >= 3)
		assertEquals(mk(0, 5), evalUn(NumericAbs.INSTANCE, mk(-3, 5)));
		// abs([-5,3]) = [0,5] (-(-5)=5 > 3)
		assertEquals(mk(0, 5), evalUn(NumericAbs.INSTANCE, mk(-5, 3)));
		// abs([0,0]) = [0,0]
		assertEquals(mk(0), evalUn(NumericAbs.INSTANCE, mk(0)));
	}

	// -----------------------------------------------------------------------
	// evalUnaryExpression – floor/ceil/round (identity for integer intervals)
	// -----------------------------------------------------------------------

	@Test
	public void testFloorCeilRound() throws SemanticException {
		assertEquals(mk(2, 5), evalUn(NumericFloor.INSTANCE, mk(2, 5)));
		assertEquals(mk(-3, 7), evalUn(NumericCeil.INSTANCE, mk(-3, 7)));
		assertEquals(mk(1, 4), evalUn(NumericRound.INSTANCE, mk(1, 4)));
	}

	// -----------------------------------------------------------------------
	// evalUnaryExpression – NumericSqrt
	// -----------------------------------------------------------------------

	@Test
	public void testNumericSqrt() throws SemanticException {
		// sqrt([4,9]) = [2,3]
		assertEquals(mk(2, 3), evalUn(NumericSqrt.INSTANCE, mk(4, 9)));
		// sqrt([0,4]) = [0,2]
		assertEquals(mk(0, 2), evalUn(NumericSqrt.INSTANCE, mk(0, 4)));
		// sqrt([-5,4]) = [0,2]
		assertEquals(mk(0, 2), evalUn(NumericSqrt.INSTANCE, mk(-5, 4)));
		// sqrt([0,0]) = [0,0]
		assertEquals(IntInterval.ZERO, evalUn(NumericSqrt.INSTANCE, mk(0)));
		// sqrt([-5,0]) = [0,0]
		assertEquals(IntInterval.ZERO, evalUn(NumericSqrt.INSTANCE, mk(-5, 0)));
		// sqrt([-5,-1]) = BOTTOM (no non-negative values)
		assertEquals(IntInterval.BOTTOM, evalUn(NumericSqrt.INSTANCE, mk(-5, -1)));
		// sqrt([1,+Inf]) = [1,+Inf]
		IntInterval one_posInf = new IntInterval(new MathNumber(1), MathNumber.PLUS_INFINITY);
		assertEquals(one_posInf, evalUn(NumericSqrt.INSTANCE, one_posInf));
		// sqrt([0,+Inf]) = [0,+Inf]
		IntInterval zero_posInf = new IntInterval(MathNumber.ZERO, MathNumber.PLUS_INFINITY);
		assertEquals(zero_posInf, evalUn(NumericSqrt.INSTANCE, zero_posInf));
	}

	// -----------------------------------------------------------------------
	// evalUnaryExpression – NumericExp
	// -----------------------------------------------------------------------

	@Test
	public void testNumericExp() throws SemanticException {
		// exp([0,0]) = [1,1]
		assertEquals(mk(1), evalUn(NumericExp.INSTANCE, mk(0)));
		// exp([-Inf,0]) = [0,1]
		IntInterval negInf_0 = new IntInterval(MathNumber.MINUS_INFINITY, MathNumber.ZERO);
		IntInterval expNegInf0 = evalUn(NumericExp.INSTANCE, negInf_0);
		assertEquals(MathNumber.ZERO, expNegInf0.getLow());
		assertEquals(new MathNumber(Math.exp(0)), expNegInf0.getHigh());
		// exp([-Inf,2]) lower bound = 0
		IntInterval negInf_2 = new IntInterval(MathNumber.MINUS_INFINITY, new MathNumber(2));
		IntInterval expNegInf2 = evalUn(NumericExp.INSTANCE, negInf_2);
		assertEquals(MathNumber.ZERO, expNegInf2.getLow());
		assertFalse(expNegInf2.getHigh().isMinusInfinity());
		// exp(TOP) = [0,+Inf]
		IntInterval expTop = evalUn(NumericExp.INSTANCE, IntInterval.TOP);
		assertEquals(MathNumber.ZERO, expTop.getLow());
		assertTrue(expTop.highIsPlusInfinity());
		// exp([0,+Inf]) = [1,+Inf]
		IntInterval zero_posInf = new IntInterval(MathNumber.ZERO, MathNumber.PLUS_INFINITY);
		IntInterval expZeroPosInf = evalUn(NumericExp.INSTANCE, zero_posInf);
		assertEquals(new MathNumber(Math.exp(0)), expZeroPosInf.getLow());
		assertTrue(expZeroPosInf.highIsPlusInfinity());
	}

	// -----------------------------------------------------------------------
	// evalUnaryExpression – NumericLog
	// -----------------------------------------------------------------------

	@Test
	public void testNumericLog() throws SemanticException {
		// log([1,1]) = [0,0]
		IntInterval logOne = evalUn(NumericLog.INSTANCE, mk(1));
		assertEquals(new MathNumber(Math.log(1)), logOne.getLow());
		assertEquals(new MathNumber(Math.log(1)), logOne.getHigh());
		// log([-5,-1]) = BOTTOM
		assertEquals(IntInterval.BOTTOM, evalUn(NumericLog.INSTANCE, mk(-5, -1)));
		// log([-5,0]) = BOTTOM (log(0) is -Inf, undefined)
		assertEquals(IntInterval.BOTTOM, evalUn(NumericLog.INSTANCE, mk(-5, 0)));
		// log([0,+Inf]) = TOP
		IntInterval zero_posInf = new IntInterval(MathNumber.ZERO, MathNumber.PLUS_INFINITY);
		assertEquals(IntInterval.TOP, evalUn(NumericLog.INSTANCE, zero_posInf));
		// log([1,+Inf]) lower=0, high=+Inf
		IntInterval one_posInf = new IntInterval(new MathNumber(1), MathNumber.PLUS_INFINITY);
		IntInterval logOnePosInf = evalUn(NumericLog.INSTANCE, one_posInf);
		assertEquals(new MathNumber(Math.log(1)), logOnePosInf.getLow());
		assertTrue(logOnePosInf.highIsPlusInfinity());
	}

	// -----------------------------------------------------------------------
	// evalUnaryExpression – NumericLog10
	// -----------------------------------------------------------------------

	@Test
	public void testNumericLog10() throws SemanticException {
		// log10([10,100]) = [1,2]
		IntInterval log10_10_100 = evalUn(NumericLog10.INSTANCE, mk(10, 100));
		assertEquals(new MathNumber(Math.log10(10)), log10_10_100.getLow());
		assertEquals(new MathNumber(Math.log10(100)), log10_10_100.getHigh());
		// log10([-5,-1]) = BOTTOM
		assertEquals(IntInterval.BOTTOM, evalUn(NumericLog10.INSTANCE, mk(-5, -1)));
		// log10([-5,0]) = BOTTOM
		assertEquals(IntInterval.BOTTOM, evalUn(NumericLog10.INSTANCE, mk(-5, 0)));
		// log10([0,+Inf]) = TOP
		IntInterval zero_posInf = new IntInterval(MathNumber.ZERO, MathNumber.PLUS_INFINITY);
		assertEquals(IntInterval.TOP, evalUn(NumericLog10.INSTANCE, zero_posInf));
	}

	// -----------------------------------------------------------------------
	// evalUnaryExpression – NumericSin / NumericCos / NumericTan
	// -----------------------------------------------------------------------

	@Test
	public void testNumericSin() throws SemanticException {
		// sin([0,0]) = [0,0]
		assertEquals(mk(0), evalUn(NumericSin.INSTANCE, mk(0)));
		// sin(TOP) = [-1,1]
		assertEquals(mk(-1, 1), evalUn(NumericSin.INSTANCE, IntInterval.TOP));
		// interval wider than 4π → [-1,1]
		assertEquals(mk(-1, 1), evalUn(NumericSin.INSTANCE, mk(0, 20)));
	}

	@Test
	public void testNumericCos() throws SemanticException {
		// cos([0,0]) = [1,1]
		assertEquals(mk(1), evalUn(NumericCos.INSTANCE, mk(0)));
		// cos(TOP) = [-1,1]
		assertEquals(mk(-1, 1), evalUn(NumericCos.INSTANCE, IntInterval.TOP));
	}

	@Test
	public void testNumericTan() throws SemanticException {
		// tan([0,0]) = [0,0]
		assertEquals(mk(0), evalUn(NumericTan.INSTANCE, mk(0)));
		// tan(TOP) = [-1,1]
		assertEquals(mk(-1, 1), evalUn(NumericTan.INSTANCE, IntInterval.TOP));
	}

	// -----------------------------------------------------------------------
	// evalUnaryExpression – NumericAsin / NumericAcos / NumericAtan
	// -----------------------------------------------------------------------

	@Test
	public void testNumericAsin() throws SemanticException {
		// asin([-1,1]) = [asin(-1), asin(1)] = [-π/2, π/2]
		IntInterval result = evalUn(NumericAsin.INSTANCE, mk(-1, 1));
		assertEquals(new MathNumber(Math.asin(-1)), result.getLow());
		assertEquals(new MathNumber(Math.asin(1)), result.getHigh());
		// asin([0,1]) = [asin(0), asin(1)] = [0, π/2]
		IntInterval result2 = evalUn(NumericAsin.INSTANCE, mk(0, 1));
		assertEquals(new MathNumber(Math.asin(0)), result2.getLow());
		assertEquals(new MathNumber(Math.asin(1)), result2.getHigh());
		// asin([-2,2]) → clamped to asin domain: [asin(-1), asin(1)]
		IntInterval result3 = evalUn(NumericAsin.INSTANCE, mk(-2, 2));
		assertEquals(new MathNumber(Math.asin(-1)), result3.getLow());
		assertEquals(new MathNumber(Math.asin(1)), result3.getHigh());
	}

	@Test
	public void testNumericAcos() throws SemanticException {
		// acos([-1,1]) = [acos(1), acos(-1)] = [0, π]
		IntInterval result = evalUn(NumericAcos.INSTANCE, mk(-1, 1));
		assertEquals(new MathNumber(Math.acos(1)), result.getLow());
		assertEquals(new MathNumber(Math.acos(-1)), result.getHigh());
		// acos([0,1]) = [acos(1), acos(0)] = [0, π/2]
		IntInterval result2 = evalUn(NumericAcos.INSTANCE, mk(0, 1));
		assertEquals(new MathNumber(Math.acos(1)), result2.getLow());
		assertEquals(new MathNumber(Math.acos(0)), result2.getHigh());
	}

	@Test
	public void testNumericAtan() throws SemanticException {
		// atan([0,0]) = [0,0]
		IntInterval result = evalUn(NumericAtan.INSTANCE, mk(0));
		assertEquals(new MathNumber(Math.atan(0)), result.getLow());
		assertEquals(new MathNumber(Math.atan(0)), result.getHigh());
		// atan(TOP) = TOP
		assertEquals(IntInterval.TOP, evalUn(NumericAtan.INSTANCE, IntInterval.TOP));
		// atan([0,+Inf]) = [atan(0), +Inf]
		IntInterval zero_posInf = new IntInterval(MathNumber.ZERO, MathNumber.PLUS_INFINITY);
		IntInterval atanResult = evalUn(NumericAtan.INSTANCE, zero_posInf);
		assertEquals(new MathNumber(Math.atan(0)), atanResult.getLow());
		assertTrue(atanResult.highIsPlusInfinity());
		// atan([-Inf,0]) = [-Inf, atan(0)]
		IntInterval negInf_0 = new IntInterval(MathNumber.MINUS_INFINITY, MathNumber.ZERO);
		IntInterval atanResult2 = evalUn(NumericAtan.INSTANCE, negInf_0);
		assertTrue(atanResult2.lowIsMinusInfinity());
		assertEquals(new MathNumber(Math.atan(0)), atanResult2.getHigh());
	}

	// -----------------------------------------------------------------------
	// evalUnaryExpression – NumericToRadians
	// -----------------------------------------------------------------------

	@Test
	public void testNumericToRadians() throws SemanticException {
		// toRadians([0,0]) = [0,0]
		IntInterval result = evalUn(NumericToRadians.INSTANCE, mk(0));
		assertEquals(new MathNumber(Math.toRadians(0)), result.getLow());
		assertEquals(new MathNumber(Math.toRadians(0)), result.getHigh());
		// toRadians([90,180]) = [π/2, π]
		IntInterval result2 = evalUn(NumericToRadians.INSTANCE, mk(90, 180));
		assertEquals(new MathNumber(Math.toRadians(90)), result2.getLow());
		assertEquals(new MathNumber(Math.toRadians(180)), result2.getHigh());
	}

	// -----------------------------------------------------------------------
	// evalBinaryExpression – Addition
	// -----------------------------------------------------------------------

	@Test
	public void testAddition() throws SemanticException {
		assertEquals(mk(3, 7), evalBin(NumericNonOverflowingAdd.INSTANCE, mk(1, 3), mk(2, 4)));
		assertEquals(mk(-3, 1), evalBin(NumericNonOverflowingAdd.INSTANCE, mk(1, 3), mk(-4, -2)));
		assertEquals(mk(-9, -5), evalBin(NumericNonOverflowingAdd.INSTANCE, mk(-5, -3), mk(-4, -2)));
		assertEquals(IntInterval.TOP, evalBin(NumericNonOverflowingAdd.INSTANCE, IntInterval.TOP, mk(1, 3)));
		assertEquals(IntInterval.TOP, evalBin(NumericNonOverflowingAdd.INSTANCE, mk(1, 3), IntInterval.TOP));
	}

	// -----------------------------------------------------------------------
	// evalBinaryExpression – Subtraction
	// -----------------------------------------------------------------------

	@Test
	public void testSubtraction() throws SemanticException {
		assertEquals(mk(1, 4), evalBin(NumericNonOverflowingSub.INSTANCE, mk(3, 5), mk(1, 2)));
		assertEquals(mk(-3, 1), evalBin(NumericNonOverflowingSub.INSTANCE, mk(1, 3), mk(2, 4)));
		assertEquals(IntInterval.TOP, evalBin(NumericNonOverflowingSub.INSTANCE, IntInterval.TOP, mk(1, 3)));
	}

	// -----------------------------------------------------------------------
	// evalBinaryExpression – Multiplication
	// -----------------------------------------------------------------------

	@Test
	public void testMultiplication() throws SemanticException {
		assertEquals(mk(8, 15), evalBin(NumericNonOverflowingMul.INSTANCE, mk(2, 3), mk(4, 5)));
		assertEquals(IntInterval.ZERO, evalBin(NumericNonOverflowingMul.INSTANCE, mk(2, 3), mk(0)));
		assertEquals(IntInterval.ZERO, evalBin(NumericNonOverflowingMul.INSTANCE, mk(0), mk(-5, 3)));
		assertEquals(mk(-10, 15), evalBin(NumericNonOverflowingMul.INSTANCE, mk(-2, 3), mk(4, 5)));
		assertEquals(mk(8, 15), evalBin(NumericNonOverflowingMul.INSTANCE, mk(-3, -2), mk(-5, -4)));
		assertEquals(IntInterval.TOP, evalBin(NumericNonOverflowingMul.INSTANCE, IntInterval.TOP, mk(1)));
	}

	// -----------------------------------------------------------------------
	// evalBinaryExpression – Division
	// -----------------------------------------------------------------------

	@Test
	public void testDivision() throws SemanticException {
		assertEquals(mk(3, 4), evalBin(NumericNonOverflowingDiv.INSTANCE, mk(6, 8), mk(2)));
		assertEquals(IntInterval.BOTTOM, evalBin(NumericNonOverflowingDiv.INSTANCE, mk(6, 8), mk(0)));
		assertEquals(IntInterval.ZERO, evalBin(NumericNonOverflowingDiv.INSTANCE, mk(0), mk(3, 5)));
		assertEquals(mk(-4, -3), evalBin(NumericNonOverflowingDiv.INSTANCE, mk(-8, -6), mk(2)));
		assertEquals(IntInterval.TOP, evalBin(NumericNonOverflowingDiv.INSTANCE, IntInterval.TOP, mk(2)));
	}

	// -----------------------------------------------------------------------
	// evalBinaryExpression – Modulo
	// -----------------------------------------------------------------------

	@Test
	public void testModulo() throws SemanticException {
		// [7,7] % [3,3]: divisor > 0 → [0, divisor.high-1] = [0,2]
		assertEquals(mk(0, 2), evalBin(NumericNonOverflowingMod.INSTANCE, mk(7), mk(3)));
		// any % [0,0] → BOTTOM
		assertEquals(IntInterval.BOTTOM, evalBin(NumericNonOverflowingMod.INSTANCE, mk(3, 5), mk(0)));
		// [0,0] % any → ZERO
		assertEquals(IntInterval.ZERO, evalBin(NumericNonOverflowingMod.INSTANCE, mk(0), mk(3, 5)));
		// [6,6] % [0,3]: else branch, right.low=0 (not < 0) → lower=0, upper=2
		assertEquals(mk(0, 2), evalBin(NumericNonOverflowingMod.INSTANCE, mk(6), mk(0, 3)));
		// [6,9] % [-3,-1]: fully negative divisor → [right.low+1, 0] = [-2,0]
		assertEquals(mk(-2, 0), evalBin(NumericNonOverflowingMod.INSTANCE, mk(6, 9), mk(-3, -1)));
		// [6,9] % [-3,3]: spans zero, right.low=-3 < 0 → [-2, 2]
		assertEquals(mk(-2, 2), evalBin(NumericNonOverflowingMod.INSTANCE, mk(6, 9), mk(-3, 3)));
	}

	// -----------------------------------------------------------------------
	// evalBinaryExpression – Remainder (sign of dividend)
	// -----------------------------------------------------------------------

	@Test
	public void testRemainder() throws SemanticException {
		// [7,9] % [3,5]: left positive, M=5 → [0, M-1] = [0,4]
		assertEquals(mk(0, 4), evalBin(NumericNonOverflowingRem.INSTANCE, mk(7, 9), mk(3, 5)));
		// [-7,-5] % [3,5]: left negative, M=5 → [-4, 0]
		assertEquals(mk(-4, 0), evalBin(NumericNonOverflowingRem.INSTANCE, mk(-7, -5), mk(3, 5)));
		// any % [0,0] → BOTTOM
		assertEquals(IntInterval.BOTTOM, evalBin(NumericNonOverflowingRem.INSTANCE, mk(3, 5), mk(0)));
		// [0,0] % any → ZERO
		assertEquals(IntInterval.ZERO, evalBin(NumericNonOverflowingRem.INSTANCE, mk(0), mk(3, 5)));
	}

	// -----------------------------------------------------------------------
	// evalBinaryExpression – BitwiseAnd
	// -----------------------------------------------------------------------

	@Test
	public void testBitwiseAnd() throws SemanticException {
		// [0,5] & [0,3]: low=min(0,0,0)=0, high=max(5,3)=5
		assertEquals(mk(0, 5), evalBin(BitwiseAnd.INSTANCE, mk(0, 5), mk(0, 3)));
		// [2,5] & [2,5]: low=min(2,2,0)=0, high=max(5,5)=5
		assertEquals(mk(0, 5), evalBin(BitwiseAnd.INSTANCE, mk(2, 5), mk(2, 5)));
		// TOP & [1,3] → TOP (early-exit for TOP inputs)
		assertEquals(IntInterval.TOP, evalBin(BitwiseAnd.INSTANCE, IntInterval.TOP, mk(1, 3)));
	}

	// -----------------------------------------------------------------------
	// evalBinaryExpression – BitwiseOr
	// -----------------------------------------------------------------------

	@Test
	public void testBitwiseOr() throws SemanticException {
		// [0,2] | [0,3]: low=0, high=max(max(2,3), 2|3)=max(3,3)=3
		assertEquals(mk(0, 3), evalBin(BitwiseOr.INSTANCE, mk(0, 2), mk(0, 3)));
		// TOP | [1,3] → TOP
		assertEquals(IntInterval.TOP, evalBin(BitwiseOr.INSTANCE, IntInterval.TOP, mk(1, 3)));
	}

	// -----------------------------------------------------------------------
	// evalBinaryExpression – BitwiseXor
	// -----------------------------------------------------------------------

	@Test
	public void testBitwiseXor() throws SemanticException {
		// [0,3] ^ [0,3]: max=3, nbits=log(3)/log(2)≈1.58, k=2, ub=3 → [0,3]
		assertEquals(mk(0, 3), evalBin(BitwiseXor.INSTANCE, mk(0, 3), mk(0, 3)));
		// [0,7] ^ [0,15]: max=15, k=4, ub=15 → [0,15]
		assertEquals(mk(0, 15), evalBin(BitwiseXor.INSTANCE, mk(0, 7), mk(0, 15)));
		// [-1,3] ^ [0,3]: includes negative → TOP
		assertEquals(IntInterval.TOP, evalBin(BitwiseXor.INSTANCE, mk(-1, 3), mk(0, 3)));
	}

	// -----------------------------------------------------------------------
	// evalBinaryExpression – BitwiseShiftLeft
	// -----------------------------------------------------------------------

	@Test
	public void testBitwiseShiftLeft() throws SemanticException {
		// [3,5] << [2,2]: left * 2^2 = [3,5] * [4,4] = [12,20]
		assertEquals(mk(12, 20), evalBin(BitwiseShiftLeft.INSTANCE, mk(3, 5), mk(2)));
		// [1,2] << [0,1]: left * [2^0,2^1] = [1,2] * [1,2] = [1,4]
		assertEquals(mk(1, 4), evalBin(BitwiseShiftLeft.INSTANCE, mk(1, 2), mk(0, 1)));
		// [1,1] << [-1,-1]: right < 0 → BOTTOM
		assertEquals(IntInterval.BOTTOM, evalBin(BitwiseShiftLeft.INSTANCE, mk(1), mk(-1)));
	}

	// -----------------------------------------------------------------------
	// evalBinaryExpression – BitwiseShiftRight
	// -----------------------------------------------------------------------

	@Test
	public void testBitwiseShiftRight() throws SemanticException {
		// [12,20] >> [2,2]: left / 2^2 = [12,20] / 4 = [3,5]
		assertEquals(mk(3, 5), evalBin(BitwiseShiftRight.INSTANCE, mk(12, 20), mk(2)));
		// [1,1] >> [-1,-1] → BOTTOM
		assertEquals(IntInterval.BOTTOM, evalBin(BitwiseShiftRight.INSTANCE, mk(1), mk(-1)));
	}

	// -----------------------------------------------------------------------
	// evalBinaryExpression – BitwiseUnsignedShiftRight
	// -----------------------------------------------------------------------

	@Test
	public void testBitwiseUnsignedShiftRight() throws SemanticException {
		// [12,20] >>> [2,2]: positive, same as >> → [3,5]
		assertEquals(mk(3, 5), evalBin(BitwiseUnsignedShiftRight.INSTANCE, mk(12, 20), mk(2)));
		// [-1,-1] >>> [2,2]: negative left → TOP
		assertEquals(IntInterval.TOP, evalBin(BitwiseUnsignedShiftRight.INSTANCE, mk(-1), mk(2)));
		// [1,1] >>> [-1,-1]: right < 0 → BOTTOM
		assertEquals(IntInterval.BOTTOM, evalBin(BitwiseUnsignedShiftRight.INSTANCE, mk(1), mk(-1)));
	}

	// -----------------------------------------------------------------------
	// evalBinaryExpression – NumericMax / NumericMin
	// -----------------------------------------------------------------------

	@Test
	public void testNumericMax() throws SemanticException {
		// max([1,3], [2,5]) = [max(1,2), max(3,5)] = [2,5]
		assertEquals(mk(2, 5), evalBin(NumericMax.INSTANCE, mk(1, 3), mk(2, 5)));
		// max([5,7], [1,3]) = [max(5,1), max(7,3)] = [5,7]
		assertEquals(mk(5, 7), evalBin(NumericMax.INSTANCE, mk(5, 7), mk(1, 3)));
		// max([1,3], [1,3]) = [1,3]
		assertEquals(mk(1, 3), evalBin(NumericMax.INSTANCE, mk(1, 3), mk(1, 3)));
	}

	@Test
	public void testNumericMin() throws SemanticException {
		// min([1,3], [2,5]) = [min(1,2), min(3,5)] = [1,3]
		assertEquals(mk(1, 3), evalBin(NumericMin.INSTANCE, mk(1, 3), mk(2, 5)));
		// min([5,7], [1,3]) = [min(5,1), min(7,3)] = [1,3]
		assertEquals(mk(1, 3), evalBin(NumericMin.INSTANCE, mk(5, 7), mk(1, 3)));
		// min([1,3], [1,3]) = [1,3]
		assertEquals(mk(1, 3), evalBin(NumericMin.INSTANCE, mk(1, 3), mk(1, 3)));
	}

	// -----------------------------------------------------------------------
	// evalBinaryExpression – NumericAtan2
	// -----------------------------------------------------------------------

	@Test
	public void testNumericAtan2() throws SemanticException {
		// atan2 always returns [-π, π]
		IntInterval expected = new IntInterval(new MathNumber(-Math.PI), new MathNumber(Math.PI));
		assertEquals(expected, evalBin(NumericAtan2.INSTANCE, mk(1, 5), mk(2, 4)));
		assertEquals(expected, evalBin(NumericAtan2.INSTANCE, mk(-3, 3), mk(-3, 3)));
	}

	// -----------------------------------------------------------------------
	// evalBinaryExpression – ValueComparison
	// -----------------------------------------------------------------------

	@Test
	public void testValueComparison() throws SemanticException {
		// [1,2] compare [3,4]: high(2) < low(3) → MINUS_ONE
		assertEquals(IntInterval.MINUS_ONE, evalBin(ValueComparison.INSTANCE, mk(1, 2), mk(3, 4)));
		// [3,4] compare [1,2]: low(3) > high(2) → ONE
		assertEquals(IntInterval.ONE, evalBin(ValueComparison.INSTANCE, mk(3, 4), mk(1, 2)));
		// [3,3] compare [3,3]: singleton equal → ZERO
		assertEquals(IntInterval.ZERO, evalBin(ValueComparison.INSTANCE, mk(3), mk(3)));
		// [1,3] compare [2,4]: overlap → [-1,1]
		assertEquals(mk(-1, 1), evalBin(ValueComparison.INSTANCE, mk(1, 3), mk(2, 4)));
	}

	// -----------------------------------------------------------------------
	// satisfiesBinaryExpression – ComparisonEq
	// -----------------------------------------------------------------------

	@Test
	public void testSatisfiesEq() {
		// [3,3] == [3,3] → SATISFIED
		assertEquals(Satisfiability.SATISFIED, sat(ComparisonEq.INSTANCE, mk(3), mk(3)));
		// [3,3] == [4,4] → NOT_SATISFIED
		assertEquals(Satisfiability.NOT_SATISFIED, sat(ComparisonEq.INSTANCE, mk(3), mk(4)));
		// [1,3] == [2,4] → UNKNOWN (intersect, not singleton equal)
		assertEquals(Satisfiability.UNKNOWN, sat(ComparisonEq.INSTANCE, mk(1, 3), mk(2, 4)));
		// [1,2] == [3,4] → NOT_SATISFIED (no intersection)
		assertEquals(Satisfiability.NOT_SATISFIED, sat(ComparisonEq.INSTANCE, mk(1, 2), mk(3, 4)));
		// TOP → UNKNOWN
		assertEquals(Satisfiability.UNKNOWN, sat(ComparisonEq.INSTANCE, IntInterval.TOP, mk(3)));
	}

	// -----------------------------------------------------------------------
	// satisfiesBinaryExpression – ComparisonNe
	// -----------------------------------------------------------------------

	@Test
	public void testSatisfiesNe() {
		// [1,2] != [3,4] → SATISFIED (disjoint)
		assertEquals(Satisfiability.SATISFIED, sat(ComparisonNe.INSTANCE, mk(1, 2), mk(3, 4)));
		// [1,2] != [2,3] → UNKNOWN (intersect)
		assertEquals(Satisfiability.UNKNOWN, sat(ComparisonNe.INSTANCE, mk(1, 2), mk(2, 3)));
		// TOP → UNKNOWN
		assertEquals(Satisfiability.UNKNOWN, sat(ComparisonNe.INSTANCE, IntInterval.TOP, mk(3)));
	}

	// -----------------------------------------------------------------------
	// satisfiesBinaryExpression – ComparisonLe
	// -----------------------------------------------------------------------

	@Test
	public void testSatisfiesLe() {
		// [1,2] <= [3,4]: disjoint, high(2) <= low(3) → SATISFIED
		assertEquals(Satisfiability.SATISFIED, sat(ComparisonLe.INSTANCE, mk(1, 2), mk(3, 4)));
		// [3,4] <= [1,2]: disjoint, high(4) <= low(1)? No → NOT_SATISFIED
		assertEquals(Satisfiability.NOT_SATISFIED, sat(ComparisonLe.INSTANCE, mk(3, 4), mk(1, 2)));
		// [1,2] <= [2,3]: glb=[2,2] singleton, high(2)==low(2) → SATISFIED
		assertEquals(Satisfiability.SATISFIED, sat(ComparisonLe.INSTANCE, mk(1, 2), mk(2, 3)));
		// [1,3] <= [2,4]: overlap, glb non-singleton → UNKNOWN
		assertEquals(Satisfiability.UNKNOWN, sat(ComparisonLe.INSTANCE, mk(1, 3), mk(2, 4)));
	}

	// -----------------------------------------------------------------------
	// satisfiesBinaryExpression – ComparisonLt
	// -----------------------------------------------------------------------

	@Test
	public void testSatisfiesLt() {
		// [1,2] < [3,4]: disjoint, high(2) < low(3) → SATISFIED
		assertEquals(Satisfiability.SATISFIED, sat(ComparisonLt.INSTANCE, mk(1, 2), mk(3, 4)));
		// [3,4] < [1,2]: disjoint, high(4) < low(1)? No → NOT_SATISFIED
		assertEquals(Satisfiability.NOT_SATISFIED, sat(ComparisonLt.INSTANCE, mk(3, 4), mk(1, 2)));
		// [1,2] < [2,3]: glb=[2,2] non-empty → UNKNOWN
		assertEquals(Satisfiability.UNKNOWN, sat(ComparisonLt.INSTANCE, mk(1, 2), mk(2, 3)));
		// [1,3] < [2,4]: overlap → UNKNOWN
		assertEquals(Satisfiability.UNKNOWN, sat(ComparisonLt.INSTANCE, mk(1, 3), mk(2, 4)));
	}

	// -----------------------------------------------------------------------
	// satisfiesBinaryExpression – ComparisonGe / ComparisonGt (symmetric)
	// -----------------------------------------------------------------------

	@Test
	public void testSatisfiesGe() {
		// [3,4] >= [1,2] → SATISFIED (symmetric of <= [1,2] <= [3,4])
		assertEquals(Satisfiability.SATISFIED, sat(ComparisonGe.INSTANCE, mk(3, 4), mk(1, 2)));
		// [1,2] >= [3,4] → NOT_SATISFIED
		assertEquals(Satisfiability.NOT_SATISFIED, sat(ComparisonGe.INSTANCE, mk(1, 2), mk(3, 4)));
	}

	@Test
	public void testSatisfiesGt() {
		// [3,4] > [1,2] → SATISFIED (symmetric of < [1,2] < [3,4])
		assertEquals(Satisfiability.SATISFIED, sat(ComparisonGt.INSTANCE, mk(3, 4), mk(1, 2)));
		// [1,2] > [3,4] → NOT_SATISFIED
		assertEquals(Satisfiability.NOT_SATISFIED, sat(ComparisonGt.INSTANCE, mk(1, 2), mk(3, 4)));
	}

	// -----------------------------------------------------------------------
	// assumeBinaryExpression
	// -----------------------------------------------------------------------

	private BinaryExpression mkAssume(
			BinaryOperator op,
			int val) {
		return new BinaryExpression(BoolType.INSTANCE, variable,
				new Constant(Int32Type.INSTANCE, val, pp.getLocation()), op, pp.getLocation());
	}

	@Test
	public void testAssumeEq() throws SemanticException {
		// assume(x == 5) when x=TOP → x=[5,5]
		ValueEnvironment<IntInterval> result = domain.assumeBinaryExpression(
				env, mkAssume(ComparisonEq.INSTANCE, 5), pp, pp, oracle);
		assertEquals(mk(5), result.getState(variable));
	}

	@Test
	public void testAssumeGt() throws SemanticException {
		// assume(x > 3) when x=TOP → x=[4,+Inf]
		ValueEnvironment<IntInterval> result = domain.assumeBinaryExpression(
				env, mkAssume(ComparisonGt.INSTANCE, 3), pp, pp, oracle);
		IntInterval xVal = result.getState(variable);
		assertEquals(new MathNumber(4), xVal.getLow());
		assertTrue(xVal.highIsPlusInfinity());
	}

	@Test
	public void testAssumeGe() throws SemanticException {
		// assume(x >= 3) when x=TOP → x=[3,+Inf]
		ValueEnvironment<IntInterval> result = domain.assumeBinaryExpression(
				env, mkAssume(ComparisonGe.INSTANCE, 3), pp, pp, oracle);
		IntInterval xVal = result.getState(variable);
		assertEquals(new MathNumber(3), xVal.getLow());
		assertTrue(xVal.highIsPlusInfinity());
	}

	@Test
	public void testAssumeLt() throws SemanticException {
		// assume(x < 5) when x=TOP → x=[-Inf,4]
		ValueEnvironment<IntInterval> result = domain.assumeBinaryExpression(
				env, mkAssume(ComparisonLt.INSTANCE, 5), pp, pp, oracle);
		IntInterval xVal = result.getState(variable);
		assertTrue(xVal.lowIsMinusInfinity());
		assertEquals(new MathNumber(4), xVal.getHigh());
	}

	@Test
	public void testAssumeLe() throws SemanticException {
		// assume(x <= 5) when x=TOP → x=[-Inf,5]
		ValueEnvironment<IntInterval> result = domain.assumeBinaryExpression(
				env, mkAssume(ComparisonLe.INSTANCE, 5), pp, pp, oracle);
		IntInterval xVal = result.getState(variable);
		assertTrue(xVal.lowIsMinusInfinity());
		assertEquals(new MathNumber(5), xVal.getHigh());
	}

	@Test
	public void testAssumeEqWithBoundedEnv() throws SemanticException {
		// assume(x == 7) when x=[3,10] → x=[7,7]
		ValueEnvironment<IntInterval> start = env.putState(variable, mk(3, 10));
		ValueEnvironment<IntInterval> result = domain.assumeBinaryExpression(
				start, mkAssume(ComparisonEq.INSTANCE, 7), pp, pp, oracle);
		assertEquals(mk(7), result.getState(variable));
	}

	@Test
	public void testAssumeGtWithBoundedEnv() throws SemanticException {
		// assume(x > 8) when x=[3,10] → x=[9,10]
		ValueEnvironment<IntInterval> start = env.putState(variable, mk(3, 10));
		ValueEnvironment<IntInterval> result = domain.assumeBinaryExpression(
				start, mkAssume(ComparisonGt.INSTANCE, 8), pp, pp, oracle);
		assertEquals(mk(9, 10), result.getState(variable));
	}

	@Test
	public void testAssumeLtWithBoundedEnv() throws SemanticException {
		// assume(x < 5) when x=[3,10] → x=[3,4]
		ValueEnvironment<IntInterval> start = env.putState(variable, mk(3, 10));
		ValueEnvironment<IntInterval> result = domain.assumeBinaryExpression(
				start, mkAssume(ComparisonLt.INSTANCE, 5), pp, pp, oracle);
		assertEquals(mk(3, 4), result.getState(variable));
	}

	// -----------------------------------------------------------------------
	// Lattice operations
	// -----------------------------------------------------------------------

	@Test
	public void testLatticeOperations() throws SemanticException {
		// lub: [1,3] lub [2,5] = [1,5]
		assertEquals(mk(1, 5), mk(1, 3).lub(mk(2, 5)));
		// lub with disjoint: [1,2] lub [4,5] = [1,5]
		assertEquals(mk(1, 5), mk(1, 2).lub(mk(4, 5)));
		// glb: [1,3] glb [2,5] = [2,3]
		assertEquals(mk(2, 3), mk(1, 3).glb(mk(2, 5)));
		// glb disjoint: [1,2] glb [4,5] = BOTTOM
		assertEquals(IntInterval.BOTTOM, mk(1, 2).glb(mk(4, 5)));
		// widening: [1,3] widening [2,5] → high grows → [1,+Inf]
		IntInterval widen1 = mk(1, 3).widening(mk(2, 5));
		assertEquals(new MathNumber(1), widen1.getLow());
		assertTrue(widen1.highIsPlusInfinity());
		// widening: [2,5] widening [1,3] → low shrinks → [-Inf, 5]
		IntInterval widen2 = mk(2, 5).widening(mk(1, 3));
		assertTrue(widen2.lowIsMinusInfinity());
		assertEquals(new MathNumber(5), widen2.getHigh());
		// lessOrEqual
		assertTrue(mk(2, 3).lessOrEqual(mk(1, 5)));
		assertFalse(mk(1, 5).lessOrEqual(mk(2, 3)));
		assertTrue(mk(3).lessOrEqual(mk(3)));
	}

	// -----------------------------------------------------------------------
	// Iterator
	// -----------------------------------------------------------------------

	@Test
	public void testIteratorOnTopInterval() {
		assertThrows(InfiniteIterationException.class, () -> {
			for (Long l : IntInterval.TOP)
				System.out.println(l);
		});
	}

	@Test
	public void testIterator() throws SemanticException {
		IntInterval interval = mk(-1, 2);
		List<Long> values = new ArrayList<>();
		for (Long l : interval)
			values.add(l);
		assertEquals(List.of(-1L, 0L, 1L, 2L), values);
	}

	// -----------------------------------------------------------------------
	// WVA helper
	// -----------------------------------------------------------------------

	private BinaryExpression mkConstraint(
			int constant,
			BinaryOperator op,
			ValueExpression expr) {
		return new BinaryExpression(
				Int32Type.INSTANCE,
				new Constant(Int32Type.INSTANCE, constant, pp.getLocation()),
				expr,
				op,
				pp.getLocation());
	}

	private TernaryExpression mkTernary(
			TernaryOperator op) {
		return new TernaryExpression(Int32Type.INSTANCE, varAux, varAux, variable, op, pp.getLocation());
	}

	// -----------------------------------------------------------------------
	// generate() directly
	// -----------------------------------------------------------------------

	@Test
	public void testGenerateNull() throws SemanticException {
		assertEquals(IntInterval.BOTTOM, domain.generate(null, pp, oracle));
	}

	@Test
	public void testGenerateEmpty() throws SemanticException {
		assertEquals(IntInterval.TOP, domain.generate(Collections.emptySet(), pp, oracle));
	}

	@Test
	public void testGenerateEq() throws SemanticException {
		// 5 == varAux → [5,5]
		Set<BinaryExpression> cs = Set.of(mkConstraint(5, ComparisonEq.INSTANCE, varAux));
		assertEquals(mk(5), domain.generate(cs, pp, oracle));
	}

	@Test
	public void testGenerateUpperBound() throws SemanticException {
		// 7 >= varAux → expr ≤ 7 → [-Inf, 7]
		Set<BinaryExpression> cs = Set.of(mkConstraint(7, ComparisonGe.INSTANCE, varAux));
		IntInterval result = domain.generate(cs, pp, oracle);
		assertTrue(result.lowIsMinusInfinity());
		assertEquals(new MathNumber(7), result.getHigh());
	}

	@Test
	public void testGenerateLowerBound() throws SemanticException {
		// 3 <= varAux → expr ≥ 3 → [3, +Inf]
		Set<BinaryExpression> cs = Set.of(mkConstraint(3, ComparisonLe.INSTANCE, varAux));
		IntInterval result = domain.generate(cs, pp, oracle);
		assertEquals(new MathNumber(3), result.getLow());
		assertTrue(result.highIsPlusInfinity());
	}

	@Test
	public void testGenerateRange() throws SemanticException {
		// 3 <= varAux AND 7 >= varAux → [3, 7]
		Set<BinaryExpression> cs = new HashSet<>();
		cs.add(mkConstraint(3, ComparisonLe.INSTANCE, varAux));
		cs.add(mkConstraint(7, ComparisonGe.INSTANCE, varAux));
		assertEquals(mk(3, 7), domain.generate(cs, pp, oracle));
	}

	@Test
	public void testGenerateNonMatchingSkipped() throws SemanticException {
		// left side is not a Constant Integer → skipped → TOP
		BinaryExpression nonConst = new BinaryExpression(
				Int32Type.INSTANCE, varAux, varAux, ComparisonLe.INSTANCE, pp.getLocation());
		assertEquals(IntInterval.TOP, domain.generate(Set.of(nonConst), pp, oracle));
	}

	// -----------------------------------------------------------------------
	// StringLength with WVA
	// -----------------------------------------------------------------------

	@Test
	public void testStringLengthWVA_null() throws SemanticException {
		WVAOracle wva = new WVAOracle(null);
		UnaryExpression strlen = new UnaryExpression(Int32Type.INSTANCE, varAux, StringLength.INSTANCE,
				pp.getLocation());
		assertEquals(IntInterval.BOTTOM,
				domain.evalUnaryExpression(strlen, IntInterval.TOP, pp, wva));
	}

	@Test
	public void testStringLengthWVA_empty() throws SemanticException {
		WVAOracle wva = new WVAOracle(Collections.emptySet());
		UnaryExpression strlen = new UnaryExpression(Int32Type.INSTANCE, varAux, StringLength.INSTANCE,
				pp.getLocation());
		assertEquals(IntInterval.TOP,
				domain.evalUnaryExpression(strlen, IntInterval.TOP, pp, wva));
	}

	@Test
	public void testStringLengthWVA_exact() throws SemanticException {
		// oracle returns {5 == strlen(varAux)} → [5,5]
		UnaryExpression strlenVarAux = new UnaryExpression(Int32Type.INSTANCE, varAux, StringLength.INSTANCE,
				pp.getLocation());
		WVAOracle wva = new WVAOracle(Set.of(mkConstraint(5, ComparisonEq.INSTANCE, strlenVarAux)));
		UnaryExpression strlen = new UnaryExpression(Int32Type.INSTANCE, varAux, StringLength.INSTANCE,
				pp.getLocation());
		assertEquals(mk(5), domain.evalUnaryExpression(strlen, IntInterval.TOP, pp, wva));
	}

	@Test
	public void testStringLengthWVA_range() throws SemanticException {
		// oracle returns {3 <= strlen(varAux), 10 >= strlen(varAux)} → [3,10]
		UnaryExpression strlenVarAux = new UnaryExpression(Int32Type.INSTANCE, varAux, StringLength.INSTANCE,
				pp.getLocation());
		Set<BinaryExpression> cs = new HashSet<>();
		cs.add(mkConstraint(3, ComparisonLe.INSTANCE, strlenVarAux));
		cs.add(mkConstraint(10, ComparisonGe.INSTANCE, strlenVarAux));
		WVAOracle wva = new WVAOracle(cs);
		UnaryExpression strlen = new UnaryExpression(Int32Type.INSTANCE, varAux, StringLength.INSTANCE,
				pp.getLocation());
		assertEquals(mk(3, 10), domain.evalUnaryExpression(strlen, IntInterval.TOP, pp, wva));
	}

	@Test
	public void testStringLengthWVA_upperOnly() throws SemanticException {
		// oracle returns {5 >= strlen(varAux)} → [-Inf, 5]
		UnaryExpression strlenVarAux = new UnaryExpression(Int32Type.INSTANCE, varAux, StringLength.INSTANCE,
				pp.getLocation());
		WVAOracle wva = new WVAOracle(Set.of(mkConstraint(5, ComparisonGe.INSTANCE, strlenVarAux)));
		UnaryExpression strlen = new UnaryExpression(Int32Type.INSTANCE, varAux, StringLength.INSTANCE,
				pp.getLocation());
		IntInterval result = domain.evalUnaryExpression(strlen, IntInterval.TOP, pp, wva);
		assertTrue(result.lowIsMinusInfinity());
		assertEquals(new MathNumber(5), result.getHigh());
	}

	// -----------------------------------------------------------------------
	// StringIndexOfChar / StringLastIndexOfChar / StringIndexOf /
	// StringLastIndexOf
	// -----------------------------------------------------------------------

	private void assertWVABinary(
			BinaryOperator op,
			Set<BinaryExpression> cs,
			IntInterval expected)
			throws SemanticException {
		WVAOracle wva = new WVAOracle(cs);
		BinaryExpression expr = new BinaryExpression(Int32Type.INSTANCE, varAux, variable, op, pp.getLocation());
		assertEquals(expected, domain.evalBinaryExpression(expr, IntInterval.TOP, IntInterval.TOP, pp, wva));
	}

	@Test
	public void testStringIndexOfCharWVA_null() throws SemanticException {
		WVAOracle wva = new WVAOracle(null);
		BinaryExpression expr = new BinaryExpression(Int32Type.INSTANCE, varAux, variable,
				StringIndexOfChar.INSTANCE, pp.getLocation());
		assertEquals(IntInterval.BOTTOM,
				domain.evalBinaryExpression(expr, IntInterval.TOP, IntInterval.TOP, pp, wva));
	}

	@Test
	public void testStringIndexOfCharWVA_empty() throws SemanticException {
		assertWVABinary(StringIndexOfChar.INSTANCE, Collections.emptySet(), IntInterval.TOP);
	}

	@Test
	public void testStringIndexOfCharWVA_exact() throws SemanticException {
		assertWVABinary(StringIndexOfChar.INSTANCE,
				Set.of(mkConstraint(4, ComparisonEq.INSTANCE, varAux)),
				mk(4));
	}

	@Test
	public void testStringIndexOfCharWVA_range() throws SemanticException {
		Set<BinaryExpression> cs = new HashSet<>();
		cs.add(mkConstraint(1, ComparisonLe.INSTANCE, varAux));
		cs.add(mkConstraint(8, ComparisonGe.INSTANCE, varAux));
		assertWVABinary(StringIndexOfChar.INSTANCE, cs, mk(1, 8));
	}

	@Test
	public void testStringLastIndexOfCharWVA_range() throws SemanticException {
		Set<BinaryExpression> cs = new HashSet<>();
		cs.add(mkConstraint(2, ComparisonLe.INSTANCE, varAux));
		cs.add(mkConstraint(6, ComparisonGe.INSTANCE, varAux));
		assertWVABinary(StringLastIndexOfChar.INSTANCE, cs, mk(2, 6));
	}

	@Test
	public void testStringIndexOfWVA_range() throws SemanticException {
		Set<BinaryExpression> cs = new HashSet<>();
		cs.add(mkConstraint(0, ComparisonLe.INSTANCE, varAux));
		cs.add(mkConstraint(5, ComparisonGe.INSTANCE, varAux));
		assertWVABinary(StringIndexOf.INSTANCE, cs, mk(0, 5));
	}

	@Test
	public void testStringLastIndexOfWVA_range() throws SemanticException {
		Set<BinaryExpression> cs = new HashSet<>();
		cs.add(mkConstraint(3, ComparisonLe.INSTANCE, varAux));
		cs.add(mkConstraint(9, ComparisonGe.INSTANCE, varAux));
		assertWVABinary(StringLastIndexOf.INSTANCE, cs, mk(3, 9));
	}

	// -----------------------------------------------------------------------
	// ValueComparison with both TOP and WVA
	// -----------------------------------------------------------------------

	@Test
	public void testValueComparisonBothTopWVA_exact() throws SemanticException {
		assertWVABinary(ValueComparison.INSTANCE,
				Set.of(mkConstraint(0, ComparisonEq.INSTANCE, varAux)),
				mk(0));
	}

	@Test
	public void testValueComparisonBothTopWVA_range() throws SemanticException {
		Set<BinaryExpression> cs = new HashSet<>();
		cs.add(mkConstraint(-1, ComparisonLe.INSTANCE, varAux));
		cs.add(mkConstraint(1, ComparisonGe.INSTANCE, varAux));
		assertWVABinary(ValueComparison.INSTANCE, cs, mk(-1, 1));
	}

	// -----------------------------------------------------------------------
	// Ternary expressions (StringIndexOfCharFromIndex etc.)
	// -----------------------------------------------------------------------

	private void assertWVATernary(
			TernaryOperator op,
			Set<BinaryExpression> cs,
			IntInterval expected)
			throws SemanticException {
		WVAOracle wva = new WVAOracle(cs);
		TernaryExpression expr = mkTernary(op);
		assertEquals(expected,
				domain.evalTernaryExpression(expr, IntInterval.TOP, IntInterval.TOP, IntInterval.TOP, pp, wva));
	}

	@Test
	public void testStringIndexOfCharFromIndexWVA_null() throws SemanticException {
		WVAOracle wva = new WVAOracle(null);
		assertEquals(IntInterval.BOTTOM,
				domain.evalTernaryExpression(mkTernary(StringIndexOfCharFromIndex.INSTANCE),
						IntInterval.TOP, IntInterval.TOP, IntInterval.TOP, pp, wva));
	}

	@Test
	public void testStringIndexOfCharFromIndexWVA_empty() throws SemanticException {
		assertWVATernary(StringIndexOfCharFromIndex.INSTANCE, Collections.emptySet(), IntInterval.TOP);
	}

	@Test
	public void testStringIndexOfCharFromIndexWVA_range() throws SemanticException {
		Set<BinaryExpression> cs = new HashSet<>();
		cs.add(mkConstraint(0, ComparisonLe.INSTANCE, varAux));
		cs.add(mkConstraint(5, ComparisonGe.INSTANCE, varAux));
		assertWVATernary(StringIndexOfCharFromIndex.INSTANCE, cs, mk(0, 5));
	}

	@Test
	public void testStringLastIndexOfCharFromIndexWVA_range() throws SemanticException {
		Set<BinaryExpression> cs = new HashSet<>();
		cs.add(mkConstraint(1, ComparisonLe.INSTANCE, varAux));
		cs.add(mkConstraint(7, ComparisonGe.INSTANCE, varAux));
		assertWVATernary(StringLastIndexOfCharFromIndex.INSTANCE, cs, mk(1, 7));
	}

	@Test
	public void testStringLastIndexOfFromIndexWVA_range() throws SemanticException {
		Set<BinaryExpression> cs = new HashSet<>();
		cs.add(mkConstraint(2, ComparisonLe.INSTANCE, varAux));
		cs.add(mkConstraint(8, ComparisonGe.INSTANCE, varAux));
		assertWVATernary(StringLastIndexOfFromIndex.INSTANCE, cs, mk(2, 8));
	}

	// -----------------------------------------------------------------------
	// WVAOracle inner class
	// -----------------------------------------------------------------------

	private static class WVAOracle
			implements
			SemanticOracle {

		private final Set<BinaryExpression> constraints;

		WVAOracle(
				Set<BinaryExpression> constraints) {
			this.constraints = constraints;
		}

		@Override
		public boolean hasWholeValueAnlysis() {
			return true;
		}

		@Override
		public Set<BinaryExpression> constraints(
				ValueDomain<?> requesting,
				ValueExpression e,
				ProgramPoint pp) {
			return constraints;
		}

		@Override
		public EventQueue getEventQueue() {
			return null;
		}

		@Override
		public Set<Type> getRuntimeTypesOf(
				SymbolicExpression e,
				ProgramPoint pp) {
			return Collections.emptySet();
		}

		@Override
		public Type getDynamicTypeOf(
				SymbolicExpression e,
				ProgramPoint pp) {
			return null;
		}

		@Override
		public ExpressionSet rewrite(
				SymbolicExpression expression,
				ProgramPoint pp) {
			return new ExpressionSet();
		}

		@Override
		public ExpressionSet rewrite(
				ExpressionSet expressions,
				ProgramPoint pp) {
			return new ExpressionSet();
		}

		@Override
		public Satisfiability alias(
				SymbolicExpression x,
				SymbolicExpression y,
				ProgramPoint pp) {
			return Satisfiability.UNKNOWN;
		}

		@Override
		public ExpressionSet reachableFrom(
				SymbolicExpression e,
				ProgramPoint pp) {
			return new ExpressionSet();
		}

		@Override
		public Satisfiability isReachableFrom(
				SymbolicExpression x,
				SymbolicExpression y,
				ProgramPoint pp) {
			return Satisfiability.UNKNOWN;
		}

		@Override
		public Satisfiability areMutuallyReachable(
				SymbolicExpression x,
				SymbolicExpression y,
				ProgramPoint pp) {
			return Satisfiability.UNKNOWN;
		}

	}

}
