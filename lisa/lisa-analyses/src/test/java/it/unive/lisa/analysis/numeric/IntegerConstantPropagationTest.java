package it.unive.lisa.analysis.numeric;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestParameterProvider;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.events.EventQueue;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.lattices.numeric.IntegerConstant;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.PushFromConstraints;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.BitwiseAnd;
import it.unive.lisa.symbolic.value.operator.binary.BitwiseOr;
import it.unive.lisa.symbolic.value.operator.binary.BitwiseShiftLeft;
import it.unive.lisa.symbolic.value.operator.binary.BitwiseShiftRight;
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
import it.unive.lisa.symbolic.value.operator.binary.NumericPow;
import it.unive.lisa.symbolic.value.operator.binary.StringIndexOf;
import it.unive.lisa.symbolic.value.operator.binary.StringIndexOfChar;
import it.unive.lisa.symbolic.value.operator.binary.ValueComparison;
import it.unive.lisa.symbolic.value.operator.ternary.StringIndexOfCharFromIndex;
import it.unive.lisa.symbolic.value.operator.unary.BitwiseNegation;
import it.unive.lisa.symbolic.value.operator.unary.NumericAbs;
import it.unive.lisa.symbolic.value.operator.unary.NumericCos;
import it.unive.lisa.symbolic.value.operator.unary.NumericExp;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.symbolic.value.operator.unary.NumericSqrt;
import it.unive.lisa.symbolic.value.operator.unary.StringLength;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.Type;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class IntegerConstantPropagationTest {

	private final ProgramPoint pp = TestParameterProvider.provideParam(null, ProgramPoint.class);
	private final IntegerConstantPropagation domain = new IntegerConstantPropagation();
	private final Variable variable = new Variable(Int32Type.INSTANCE, "x", pp.getLocation());
	private final Variable varAux = new Variable(Int32Type.INSTANCE, "aux", pp.getLocation());
	private final SemanticOracle oracle = TestParameterProvider.provideParam(null, SemanticOracle.class);
	private final ValueEnvironment<
			IntegerConstant> env = new ValueEnvironment<>(IntegerConstant.TOP).putState(variable, IntegerConstant.TOP);

	private BinaryExpression mkBin(
			BinaryOperator op) {
		return new BinaryExpression(Int32Type.INSTANCE, varAux, variable, op, pp.getLocation());
	}

	private UnaryExpression mkUnary(
			UnaryOperator op) {
		return new UnaryExpression(Int32Type.INSTANCE, varAux, op, pp.getLocation());
	}

	private UnaryExpression mkUnaryOn(
			UnaryOperator op,
			ValueExpression inner) {
		return new UnaryExpression(Int32Type.INSTANCE, inner, op, pp.getLocation());
	}

	private Constant mkConst(
			Object val) {
		return new Constant(Int32Type.INSTANCE, val, pp.getLocation());
	}

	private IntegerConstant mk(
			int v) {
		return new IntegerConstant(v);
	}

	private IntegerConstant evalBin(
			BinaryOperator op,
			IntegerConstant left,
			IntegerConstant right)
			throws SemanticException {
		return domain.evalBinaryExpression(mkBin(op), left, right, pp, oracle);
	}

	private IntegerConstant evalUn(
			UnaryOperator op,
			IntegerConstant arg)
			throws SemanticException {
		return domain.evalUnaryExpression(mkUnary(op), arg, pp, oracle);
	}

	private Satisfiability satisfiesBin(
			BinaryOperator op,
			IntegerConstant left,
			IntegerConstant right)
			throws SemanticException {
		return domain.satisfiesBinaryExpression(mkBin(op), left, right, pp, oracle);
	}

	private BinaryExpression mkConstraintEq(
			int constant) {
		return new BinaryExpression(Int32Type.INSTANCE,
				new Constant(Int32Type.INSTANCE, constant, pp.getLocation()),
				variable, ComparisonEq.INSTANCE, pp.getLocation());
	}

	private BinaryExpression mkConstraintGe(
			int constant) {
		return new BinaryExpression(Int32Type.INSTANCE,
				new Constant(Int32Type.INSTANCE, constant, pp.getLocation()),
				variable, ComparisonGe.INSTANCE, pp.getLocation());
	}

	private BinaryExpression mkConstraintLe(
			int constant) {
		return new BinaryExpression(Int32Type.INSTANCE,
				new Constant(Int32Type.INSTANCE, constant, pp.getLocation()),
				variable, ComparisonLe.INSTANCE, pp.getLocation());
	}

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

	// --- evalConstant ---

	@Test
	void evalConstantByte() throws SemanticException {
		assertEquals(mk(5), domain.evalConstant(mkConst((byte) 5), pp, oracle));
		assertEquals(mk(0), domain.evalConstant(mkConst((byte) 0), pp, oracle));
		assertEquals(mk(-3), domain.evalConstant(mkConst((byte) -3), pp, oracle));
	}

	@Test
	void evalConstantShort() throws SemanticException {
		assertEquals(mk(100), domain.evalConstant(mkConst((short) 100), pp, oracle));
		assertEquals(mk(-50), domain.evalConstant(mkConst((short) -50), pp, oracle));
	}

	@Test
	void evalConstantInteger() throws SemanticException {
		assertEquals(mk(42), domain.evalConstant(mkConst(42), pp, oracle));
		assertEquals(mk(-7), domain.evalConstant(mkConst(-7), pp, oracle));
		assertEquals(mk(0), domain.evalConstant(mkConst(0), pp, oracle));
	}

	@Test
	void evalConstantLongAndFloatAndDoubleReturnTop() throws SemanticException {
		// Long, Float, Double are not tracked exactly
		assertEquals(IntegerConstant.TOP, domain.evalConstant(mkConst(10L), pp, oracle));
		assertEquals(IntegerConstant.TOP, domain.evalConstant(mkConst(3.14), pp, oracle));
		assertEquals(IntegerConstant.TOP, domain.evalConstant(mkConst(2.0f), pp, oracle));
		assertEquals(IntegerConstant.TOP, domain.evalConstant(mkConst("hello"), pp, oracle));
	}

	// --- evalUnaryExpression ---

	@Test
	void evalUnaryBitwiseNegation() throws SemanticException {
		assertEquals(mk(~5), evalUn(BitwiseNegation.INSTANCE, mk(5))); // ~5 =
																		// -6
		assertEquals(mk(~(-3)), evalUn(BitwiseNegation.INSTANCE, mk(-3))); // ~-3
																			// =
																			// 2
	}

	@Test
	void evalUnaryNumericNegation() throws SemanticException {
		assertEquals(mk(-7), evalUn(NumericNegation.INSTANCE, mk(7)));
		assertEquals(mk(4), evalUn(NumericNegation.INSTANCE, mk(-4)));
	}

	@Test
	void evalUnaryNumericAbs() throws SemanticException {
		assertEquals(mk(7), evalUn(NumericAbs.INSTANCE, mk(-7)));
		assertEquals(mk(5), evalUn(NumericAbs.INSTANCE, mk(5)));
	}

	@Test
	void evalUnaryMathOps() throws SemanticException {
		// sqrt(9) = 3
		assertEquals(mk(3), evalUn(NumericSqrt.INSTANCE, mk(9)));
		// exp(0) = 1
		assertEquals(mk(1), evalUn(NumericExp.INSTANCE, mk(0)));
		// cos(0) = (int)1.0 = 1
		assertEquals(mk(1), evalUn(NumericCos.INSTANCE, mk(0)));
	}

	@Test
	void evalUnaryTopArgReturnsTop() throws SemanticException {
		assertEquals(IntegerConstant.TOP, evalUn(NumericAbs.INSTANCE, IntegerConstant.TOP));
		assertEquals(IntegerConstant.TOP, evalUn(NumericNegation.INSTANCE, IntegerConstant.TOP));
	}

	// --- evalBinaryExpression: Arithmetic ---

	@Test
	void evalAddition() throws SemanticException {
		assertEquals(mk(7), evalBin(NumericNonOverflowingAdd.INSTANCE, mk(3), mk(4)));
		assertEquals(mk(-1), evalBin(NumericNonOverflowingAdd.INSTANCE, mk(3), mk(-4)));
		assertEquals(IntegerConstant.TOP, evalBin(NumericNonOverflowingAdd.INSTANCE, mk(3), IntegerConstant.TOP));
	}

	@Test
	void evalSubtraction() throws SemanticException {
		assertEquals(mk(7), evalBin(NumericNonOverflowingSub.INSTANCE, mk(10), mk(3)));
		assertEquals(mk(-5), evalBin(NumericNonOverflowingSub.INSTANCE, mk(2), mk(7)));
		assertEquals(IntegerConstant.TOP, evalBin(NumericNonOverflowingSub.INSTANCE, IntegerConstant.TOP, mk(1)));
	}

	@Test
	void evalMultiplication() throws SemanticException {
		assertEquals(mk(20), evalBin(NumericNonOverflowingMul.INSTANCE, mk(4), mk(5)));
		assertEquals(mk(-6), evalBin(NumericNonOverflowingMul.INSTANCE, mk(2), mk(-3)));
		// zero absorbs
		assertEquals(mk(0), evalBin(NumericNonOverflowingMul.INSTANCE, mk(0), mk(100)));
		assertEquals(mk(0), evalBin(NumericNonOverflowingMul.INSTANCE, IntegerConstant.TOP, mk(0)));
		// TOP propagates
		assertEquals(IntegerConstant.TOP, evalBin(NumericNonOverflowingMul.INSTANCE, mk(5), IntegerConstant.TOP));
	}

	@Test
	void evalDivision() throws SemanticException {
		// exact division
		assertEquals(mk(5), evalBin(NumericNonOverflowingDiv.INSTANCE, mk(10), mk(2)));
		assertEquals(mk(-3), evalBin(NumericNonOverflowingDiv.INSTANCE, mk(-6), mk(2)));
		// 0 / x = 0
		assertEquals(mk(0), evalBin(NumericNonOverflowingDiv.INSTANCE, mk(0), mk(7)));
		// division by zero → BOTTOM
		assertEquals(IntegerConstant.BOTTOM, evalBin(NumericNonOverflowingDiv.INSTANCE, mk(5), mk(0)));
		// non-exact division → TOP
		assertEquals(IntegerConstant.TOP, evalBin(NumericNonOverflowingDiv.INSTANCE, mk(7), mk(2)));
		// any TOP operand → TOP
		assertEquals(IntegerConstant.TOP, evalBin(NumericNonOverflowingDiv.INSTANCE, IntegerConstant.TOP, mk(2)));
	}

	@Test
	void evalModulo() throws SemanticException {
		// 7 mod 3 = 1
		assertEquals(mk(1), evalBin(NumericNonOverflowingMod.INSTANCE, mk(7), mk(3)));
		// -7 mod 3 = 2 (floor-based: result has same sign as divisor)
		assertEquals(mk(2), evalBin(NumericNonOverflowingMod.INSTANCE, mk(-7), mk(3)));
		// 7 mod -3 = -2 (result has same sign as divisor -3)
		assertEquals(mk(-2), evalBin(NumericNonOverflowingMod.INSTANCE, mk(7), mk(-3)));
		// -7 mod -3 = -1
		assertEquals(mk(-1), evalBin(NumericNonOverflowingMod.INSTANCE, mk(-7), mk(-3)));
	}

	@Test
	void evalRemainder() throws SemanticException {
		// Java-style %: result sign matches dividend
		assertEquals(mk(1), evalBin(NumericNonOverflowingRem.INSTANCE, mk(7), mk(3)));
		assertEquals(mk(-1), evalBin(NumericNonOverflowingRem.INSTANCE, mk(-7), mk(3)));
		assertEquals(mk(1), evalBin(NumericNonOverflowingRem.INSTANCE, mk(7), mk(-3)));
	}

	// --- evalBinaryExpression: Bitwise ---

	@Test
	void evalBitwiseOps() throws SemanticException {
		assertEquals(mk(5 & 3), evalBin(BitwiseAnd.INSTANCE, mk(5), mk(3))); // 1
		assertEquals(mk(5 | 3), evalBin(BitwiseOr.INSTANCE, mk(5), mk(3))); // 7
		assertEquals(mk(5 ^ 3), evalBin(BitwiseXor.INSTANCE, mk(5), mk(3))); // 6
		assertEquals(mk(1 << 4), evalBin(BitwiseShiftLeft.INSTANCE, mk(1), mk(4))); // 16
		assertEquals(mk(16 >> 2), evalBin(BitwiseShiftRight.INSTANCE, mk(16), mk(2))); // 4
	}

	// --- evalBinaryExpression: Math functions ---

	@Test
	void evalNumericMaxMin() throws SemanticException {
		assertEquals(mk(7), evalBin(NumericMax.INSTANCE, mk(3), mk(7)));
		assertEquals(mk(3), evalBin(NumericMin.INSTANCE, mk(3), mk(7)));
	}

	@Test
	void evalNumericPow() throws SemanticException {
		assertEquals(mk(8), evalBin(NumericPow.INSTANCE, mk(2), mk(3))); // 2^3=8
	}

	@Test
	void evalValueComparison() throws SemanticException {
		assertEquals(mk(-1), evalBin(ValueComparison.INSTANCE, mk(3), mk(5)));
		assertEquals(mk(0), evalBin(ValueComparison.INSTANCE, mk(5), mk(5)));
		assertEquals(mk(1), evalBin(ValueComparison.INSTANCE, mk(7), mk(3)));
	}

	@Test
	void evalBinaryTopOperandsReturnTop() throws SemanticException {
		assertEquals(IntegerConstant.TOP, evalBin(NumericNonOverflowingAdd.INSTANCE, IntegerConstant.TOP, mk(5)));
		assertEquals(IntegerConstant.TOP, evalBin(NumericNonOverflowingAdd.INSTANCE, mk(5), IntegerConstant.TOP));
		assertEquals(IntegerConstant.TOP, evalBin(NumericAtan2.INSTANCE, IntegerConstant.TOP, mk(0)));
	}

	@Test
	void evalAtan2ConcretValues() throws SemanticException {
		// atan2(0, 0) = 0.0 → (int) 0.0 = 0
		assertEquals(mk(0), evalBin(NumericAtan2.INSTANCE, mk(0), mk(0)));
	}

	// --- evalTernaryExpression (without WVA) ---

	@Test
	void evalTernaryWithoutWvaReturnsTop() throws SemanticException {
		TernaryExpression expr = new TernaryExpression(
				Int32Type.INSTANCE, variable, varAux, variable,
				StringIndexOfCharFromIndex.INSTANCE, pp.getLocation());
		assertEquals(IntegerConstant.TOP,
				domain.evalTernaryExpression(expr, mk(1), mk(2), mk(3), pp, oracle));
	}

	// --- satisfiesBinaryExpression ---

	@Test
	void satisfiesEq() throws SemanticException {
		assertEquals(Satisfiability.SATISFIED, satisfiesBin(ComparisonEq.INSTANCE, mk(5), mk(5)));
		assertEquals(Satisfiability.NOT_SATISFIED, satisfiesBin(ComparisonEq.INSTANCE, mk(5), mk(6)));
		assertEquals(Satisfiability.UNKNOWN, satisfiesBin(ComparisonEq.INSTANCE, IntegerConstant.TOP, mk(5)));
	}

	@Test
	void satisfiesOrdering() throws SemanticException {
		assertEquals(Satisfiability.SATISFIED, satisfiesBin(ComparisonGt.INSTANCE, mk(5), mk(3)));
		assertEquals(Satisfiability.NOT_SATISFIED, satisfiesBin(ComparisonGt.INSTANCE, mk(3), mk(5)));
		assertEquals(Satisfiability.SATISFIED, satisfiesBin(ComparisonGe.INSTANCE, mk(5), mk(5)));
		assertEquals(Satisfiability.NOT_SATISFIED, satisfiesBin(ComparisonGe.INSTANCE, mk(3), mk(5)));
		assertEquals(Satisfiability.SATISFIED, satisfiesBin(ComparisonLt.INSTANCE, mk(2), mk(5)));
		assertEquals(Satisfiability.NOT_SATISFIED, satisfiesBin(ComparisonLt.INSTANCE, mk(5), mk(2)));
		assertEquals(Satisfiability.SATISFIED, satisfiesBin(ComparisonLe.INSTANCE, mk(5), mk(5)));
		assertEquals(Satisfiability.NOT_SATISFIED, satisfiesBin(ComparisonLe.INSTANCE, mk(6), mk(5)));
		assertEquals(Satisfiability.SATISFIED, satisfiesBin(ComparisonNe.INSTANCE, mk(5), mk(6)));
		assertEquals(Satisfiability.NOT_SATISFIED, satisfiesBin(ComparisonNe.INSTANCE, mk(5), mk(5)));
	}

	@Test
	void satisfiesTopReturnsUnknown() throws SemanticException {
		assertEquals(Satisfiability.UNKNOWN, satisfiesBin(ComparisonLt.INSTANCE, IntegerConstant.TOP, mk(5)));
		assertEquals(Satisfiability.UNKNOWN, satisfiesBin(ComparisonEq.INSTANCE, mk(5), IntegerConstant.TOP));
	}

	// --- assumeBinaryExpression ---

	@Test
	void assumeEqSetsIdentifier() throws SemanticException {
		BinaryExpression expr = new BinaryExpression(
				Int32Type.INSTANCE, variable, mkConst(7), ComparisonEq.INSTANCE, pp.getLocation());
		ValueEnvironment<IntegerConstant> result = domain.assumeBinaryExpression(env, expr, pp, pp, oracle);
		assertEquals(mk(7), result.getState(variable));
	}

	@Test
	void assumeEqRightIdentifier() throws SemanticException {
		BinaryExpression expr = new BinaryExpression(
				Int32Type.INSTANCE, mkConst(42), variable, ComparisonEq.INSTANCE, pp.getLocation());
		ValueEnvironment<IntegerConstant> result = domain.assumeBinaryExpression(env, expr, pp, pp, oracle);
		assertEquals(mk(42), result.getState(variable));
	}

	@Test
	void assumeContradictionReturnsBottom() throws SemanticException {
		// variable = 5, assume variable == 7 → contradiction
		ValueEnvironment<
				IntegerConstant> envWith5 = new ValueEnvironment<>(IntegerConstant.TOP).putState(variable, mk(5));
		BinaryExpression expr = new BinaryExpression(
				Int32Type.INSTANCE, variable, mkConst(7), ComparisonEq.INSTANCE, pp.getLocation());
		ValueEnvironment<IntegerConstant> result = domain.assumeBinaryExpression(envWith5, expr, pp, pp, oracle);
		assertTrue(result.isBottom());
	}

	@Test
	void assumeSatisfiedLeaveEnvUnchanged() throws SemanticException {
		// variable = 5, assume variable == 5 → already satisfied
		ValueEnvironment<
				IntegerConstant> envWith5 = new ValueEnvironment<>(IntegerConstant.TOP).putState(variable, mk(5));
		BinaryExpression expr = new BinaryExpression(
				Int32Type.INSTANCE, variable, mkConst(5), ComparisonEq.INSTANCE, pp.getLocation());
		ValueEnvironment<IntegerConstant> result = domain.assumeBinaryExpression(envWith5, expr, pp, pp, oracle);
		assertEquals(mk(5), result.getState(variable));
	}

	// --- generate() directly ---

	@Test
	void generateEqConstraint() throws SemanticException {
		assertEquals(mk(5), domain.generate(Set.of(mkConstraintEq(5)), pp, oracle));
		assertEquals(mk(-3), domain.generate(Set.of(mkConstraintEq(-3)), pp, oracle));
	}

	@Test
	void generateGeEqualsLeConstraint() throws SemanticException {
		// ge == le → singleton constant
		assertEquals(mk(7), domain.generate(Set.of(mkConstraintGe(7), mkConstraintLe(7)), pp, oracle));
	}

	@Test
	void generateGeLeDifferentReturnsTop() throws SemanticException {
		// ge ≠ le → range, not pinned → TOP
		assertEquals(IntegerConstant.TOP,
				domain.generate(Set.of(mkConstraintGe(5), mkConstraintLe(3)), pp, oracle));
	}

	@Test
	void generateEmptyReturnsTop() throws SemanticException {
		assertEquals(IntegerConstant.TOP, domain.generate(Collections.emptySet(), pp, oracle));
	}

	@Test
	void generateNullReturnsBottom() throws SemanticException {
		assertEquals(IntegerConstant.BOTTOM, domain.generate(null, pp, oracle));
	}

	// --- WVA tests: PushFromConstraints ---

	@Test
	void pushFromConstraintsEq() throws SemanticException {
		PushFromConstraints push = new PushFromConstraints(
				Int32Type.INSTANCE, pp.getLocation(), mkConstraintEq(13));
		assertEquals(mk(13), domain.evalPushAny(push, pp, oracle));
	}

	@Test
	void pushFromConstraintsSingleton() throws SemanticException {
		PushFromConstraints push = new PushFromConstraints(
				Int32Type.INSTANCE, pp.getLocation(),
				Set.of(mkConstraintGe(9), mkConstraintLe(9)));
		assertEquals(mk(9), domain.evalPushAny(push, pp, oracle));
	}

	// --- WVA tests: StringLength oracle ---

	@Test
	void wvaStringLengthConstraintReturnsConstant() throws SemanticException {
		// ICP queries oracle.constraints(expression, pp) with the full
		// StringLength expr
		// oracle returns 5 == variable → IntegerConstant(5)
		WVAOracle wva = new WVAOracle(Set.of(mkConstraintEq(5)));
		UnaryExpression strLen = mkUnaryOn(StringLength.INSTANCE, variable);
		assertEquals(mk(5),
				domain.evalUnaryExpression(strLen, IntegerConstant.TOP, pp, wva));
	}

	@Test
	void wvaStringLengthNoConstraintReturnsTop() throws SemanticException {
		WVAOracle wva = new WVAOracle(Collections.emptySet());
		UnaryExpression strLen = mkUnaryOn(StringLength.INSTANCE, variable);
		assertEquals(IntegerConstant.TOP,
				domain.evalUnaryExpression(strLen, IntegerConstant.TOP, pp, wva));
	}

	// --- WVA tests: StringIndexOf oracle ---

	@Test
	void wvaStringIndexOfConstraintReturnsConstant() throws SemanticException {
		WVAOracle wva = new WVAOracle(Set.of(mkConstraintEq(3)));
		BinaryExpression strIdx = new BinaryExpression(
				Int32Type.INSTANCE, variable, varAux, StringIndexOfChar.INSTANCE, pp.getLocation());
		assertEquals(mk(3),
				domain.evalBinaryExpression(strIdx, IntegerConstant.TOP, IntegerConstant.TOP, pp, wva));
	}

	@Test
	void wvaStringIndexOfPinnedByGeLeConstraint() throws SemanticException {
		// ge = 7 and le = 7 → singleton 7
		WVAOracle wva = new WVAOracle(Set.of(mkConstraintGe(7), mkConstraintLe(7)));
		BinaryExpression strIdx = new BinaryExpression(
				Int32Type.INSTANCE, variable, varAux, StringIndexOf.INSTANCE, pp.getLocation());
		assertEquals(mk(7),
				domain.evalBinaryExpression(strIdx, IntegerConstant.TOP, IntegerConstant.TOP, pp, wva));
	}

	// --- WVA tests: TernaryExpression oracle ---

	@Test
	void wvaTernaryStringIndexOfFromIndexReturnsConstant() throws SemanticException {
		WVAOracle wva = new WVAOracle(Set.of(mkConstraintEq(-1)));
		TernaryExpression expr = new TernaryExpression(
				Int32Type.INSTANCE, variable, varAux, variable,
				StringIndexOfCharFromIndex.INSTANCE, pp.getLocation());
		assertEquals(mk(-1),
				domain.evalTernaryExpression(expr,
						IntegerConstant.TOP, IntegerConstant.TOP, IntegerConstant.TOP, pp, wva));
	}
}
