package it.unive.lisa.analysis.numeric;

import static org.junit.jupiter.api.Assertions.assertEquals;

import it.unive.lisa.TestParameterProvider;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.events.EventQueue;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.lattices.numeric.ParityLattice;
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
import it.unive.lisa.symbolic.value.operator.binary.ValueComparison;
import it.unive.lisa.symbolic.value.operator.ternary.StringIndexOfCharFromIndex;
import it.unive.lisa.symbolic.value.operator.unary.BitwiseNegation;
import it.unive.lisa.symbolic.value.operator.unary.NumericAbs;
import it.unive.lisa.symbolic.value.operator.unary.NumericCeil;
import it.unive.lisa.symbolic.value.operator.unary.NumericCos;
import it.unive.lisa.symbolic.value.operator.unary.NumericFloor;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.symbolic.value.operator.unary.NumericRound;
import it.unive.lisa.symbolic.value.operator.unary.NumericSqrt;
import it.unive.lisa.symbolic.value.operator.unary.StringLength;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.Type;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class ParityTest {

	private final ProgramPoint pp = TestParameterProvider.provideParam(null, ProgramPoint.class);
	private final Parity domain = new Parity();
	private final Variable variable = new Variable(Int32Type.INSTANCE, "x", pp.getLocation());
	private final Variable varAux = new Variable(Int32Type.INSTANCE, "aux", pp.getLocation());
	private final SemanticOracle oracle = TestParameterProvider.provideParam(null, SemanticOracle.class);
	private final ValueEnvironment<
			ParityLattice> env = new ValueEnvironment<>(ParityLattice.TOP).putState(variable, ParityLattice.TOP);

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

	private ParityLattice evalBin(
			BinaryOperator op,
			ParityLattice left,
			ParityLattice right)
			throws SemanticException {
		return domain.evalBinaryExpression(mkBin(op), left, right, pp, oracle);
	}

	private ParityLattice evalUn(
			UnaryOperator op,
			ParityLattice arg)
			throws SemanticException {
		return domain.evalUnaryExpression(mkUnary(op), arg, pp, oracle);
	}

	private Satisfiability satisfiesBin(
			BinaryOperator op,
			ParityLattice left,
			ParityLattice right)
			throws SemanticException {
		return domain.satisfiesBinaryExpression(mkBin(op), left, right, pp, oracle);
	}

	private BinaryExpression mkConstraintEq(
			int constant) {
		return new BinaryExpression(Int32Type.INSTANCE,
				new Constant(Int32Type.INSTANCE, constant, pp.getLocation()),
				variable, ComparisonEq.INSTANCE, pp.getLocation());
	}

	private BinaryExpression mkConstraintEqStrLen(
			int constant) {
		// constraint: constant == StringLength(variable)
		return new BinaryExpression(Int32Type.INSTANCE,
				new Constant(Int32Type.INSTANCE, constant, pp.getLocation()),
				new UnaryExpression(Int32Type.INSTANCE, variable, StringLength.INSTANCE, pp.getLocation()),
				ComparisonEq.INSTANCE, pp.getLocation());
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
		assertEquals(ParityLattice.EVEN, domain.evalConstant(mkConst((byte) 4), pp, oracle));
		assertEquals(ParityLattice.ODD, domain.evalConstant(mkConst((byte) 3), pp, oracle));
		assertEquals(ParityLattice.EVEN, domain.evalConstant(mkConst((byte) 0), pp, oracle));
	}

	@Test
	void evalConstantShort() throws SemanticException {
		assertEquals(ParityLattice.EVEN, domain.evalConstant(mkConst((short) 6), pp, oracle));
		assertEquals(ParityLattice.ODD, domain.evalConstant(mkConst((short) 7), pp, oracle));
	}

	@Test
	void evalConstantInteger() throws SemanticException {
		assertEquals(ParityLattice.EVEN, domain.evalConstant(mkConst(10), pp, oracle));
		assertEquals(ParityLattice.ODD, domain.evalConstant(mkConst(11), pp, oracle));
		assertEquals(ParityLattice.ODD, domain.evalConstant(mkConst(-3), pp, oracle));
		assertEquals(ParityLattice.EVEN, domain.evalConstant(mkConst(-4), pp, oracle));
	}

	@Test
	void evalConstantLong() throws SemanticException {
		assertEquals(ParityLattice.EVEN, domain.evalConstant(mkConst(100L), pp, oracle));
		assertEquals(ParityLattice.ODD, domain.evalConstant(mkConst(101L), pp, oracle));
	}

	@Test
	void evalConstantFloatDoubleReturnTop() throws SemanticException {
		assertEquals(ParityLattice.TOP, domain.evalConstant(mkConst(3.14), pp, oracle));
		assertEquals(ParityLattice.TOP, domain.evalConstant(mkConst(2.0f), pp, oracle));
		assertEquals(ParityLattice.TOP, domain.evalConstant(mkConst("hello"), pp, oracle));
	}

	// --- evalUnaryExpression ---

	@Test
	void evalUnaryBitwiseNegationFlipsParity() throws SemanticException {
		assertEquals(ParityLattice.ODD, evalUn(BitwiseNegation.INSTANCE, ParityLattice.EVEN));
		assertEquals(ParityLattice.EVEN, evalUn(BitwiseNegation.INSTANCE, ParityLattice.ODD));
	}

	@Test
	void evalUnaryIdentityOpsPreserveParity() throws SemanticException {
		for (UnaryOperator op : new UnaryOperator[] {
				NumericAbs.INSTANCE, NumericCeil.INSTANCE,
				NumericFloor.INSTANCE, NumericNegation.INSTANCE, NumericRound.INSTANCE
		}) {
			assertEquals(ParityLattice.EVEN, evalUn(op, ParityLattice.EVEN), op.toString());
			assertEquals(ParityLattice.ODD, evalUn(op, ParityLattice.ODD), op.toString());
		}
	}

	@Test
	void evalUnaryTranscendentalsReturnTop() throws SemanticException {
		assertEquals(ParityLattice.TOP, evalUn(NumericSqrt.INSTANCE, ParityLattice.EVEN));
		assertEquals(ParityLattice.TOP, evalUn(NumericCos.INSTANCE, ParityLattice.ODD));
	}

	@Test
	void evalUnaryTopArgReturnsTop() throws SemanticException {
		// When argument is TOP, result is always TOP
		assertEquals(ParityLattice.TOP, evalUn(NumericAbs.INSTANCE, ParityLattice.TOP));
		assertEquals(ParityLattice.TOP, evalUn(BitwiseNegation.INSTANCE, ParityLattice.TOP));
	}

	// --- evalBinaryExpression: Arithmetic ---

	@Test
	void evalAddition() throws SemanticException {
		// same parity → EVEN (even + even = even, odd + odd = even)
		assertEquals(ParityLattice.EVEN,
				evalBin(NumericNonOverflowingAdd.INSTANCE, ParityLattice.EVEN, ParityLattice.EVEN));
		assertEquals(ParityLattice.EVEN,
				evalBin(NumericNonOverflowingAdd.INSTANCE, ParityLattice.ODD, ParityLattice.ODD));
		// different parity → ODD
		assertEquals(ParityLattice.ODD,
				evalBin(NumericNonOverflowingAdd.INSTANCE, ParityLattice.EVEN, ParityLattice.ODD));
		assertEquals(ParityLattice.ODD,
				evalBin(NumericNonOverflowingAdd.INSTANCE, ParityLattice.ODD, ParityLattice.EVEN));
		// TOP propagates
		assertEquals(ParityLattice.TOP,
				evalBin(NumericNonOverflowingAdd.INSTANCE, ParityLattice.TOP, ParityLattice.EVEN));
	}

	@Test
	void evalSubtraction() throws SemanticException {
		assertEquals(ParityLattice.EVEN,
				evalBin(NumericNonOverflowingSub.INSTANCE, ParityLattice.EVEN, ParityLattice.EVEN));
		assertEquals(ParityLattice.EVEN,
				evalBin(NumericNonOverflowingSub.INSTANCE, ParityLattice.ODD, ParityLattice.ODD));
		assertEquals(ParityLattice.ODD,
				evalBin(NumericNonOverflowingSub.INSTANCE, ParityLattice.EVEN, ParityLattice.ODD));
		assertEquals(ParityLattice.ODD,
				evalBin(NumericNonOverflowingSub.INSTANCE, ParityLattice.ODD, ParityLattice.EVEN));
	}

	@Test
	void evalMultiplication() throws SemanticException {
		// any even operand → EVEN
		assertEquals(ParityLattice.EVEN,
				evalBin(NumericNonOverflowingMul.INSTANCE, ParityLattice.EVEN, ParityLattice.ODD));
		assertEquals(ParityLattice.EVEN,
				evalBin(NumericNonOverflowingMul.INSTANCE, ParityLattice.ODD, ParityLattice.EVEN));
		assertEquals(ParityLattice.EVEN,
				evalBin(NumericNonOverflowingMul.INSTANCE, ParityLattice.EVEN, ParityLattice.EVEN));
		// odd * odd → ODD
		assertEquals(ParityLattice.ODD,
				evalBin(NumericNonOverflowingMul.INSTANCE, ParityLattice.ODD, ParityLattice.ODD));
	}

	@Test
	void evalDivision() throws SemanticException {
		// ODD/ODD → ODD (for exact division a/b where both odd)
		assertEquals(ParityLattice.ODD,
				evalBin(NumericNonOverflowingDiv.INSTANCE, ParityLattice.ODD, ParityLattice.ODD));
		// ODD/EVEN → EVEN
		assertEquals(ParityLattice.EVEN,
				evalBin(NumericNonOverflowingDiv.INSTANCE, ParityLattice.ODD, ParityLattice.EVEN));
		// EVEN/ODD → EVEN (even/odd = even for exact division)
		assertEquals(ParityLattice.EVEN,
				evalBin(NumericNonOverflowingDiv.INSTANCE, ParityLattice.EVEN, ParityLattice.ODD));
		// EVEN/EVEN → TOP (can be either parity)
		assertEquals(ParityLattice.TOP,
				evalBin(NumericNonOverflowingDiv.INSTANCE, ParityLattice.EVEN, ParityLattice.EVEN));
	}

	@Test
	void evalModuloAndRemainderReturnTop() throws SemanticException {
		assertEquals(ParityLattice.TOP,
				evalBin(NumericNonOverflowingMod.INSTANCE, ParityLattice.EVEN, ParityLattice.ODD));
		assertEquals(ParityLattice.TOP,
				evalBin(NumericNonOverflowingRem.INSTANCE, ParityLattice.ODD, ParityLattice.EVEN));
	}

	// --- evalBinaryExpression: Bitwise ---

	@Test
	void evalBitwiseAnd() throws SemanticException {
		// at least one EVEN → EVEN (last bit is 0 if either has 0 in last bit)
		assertEquals(ParityLattice.EVEN, evalBin(BitwiseAnd.INSTANCE, ParityLattice.EVEN, ParityLattice.EVEN));
		assertEquals(ParityLattice.EVEN, evalBin(BitwiseAnd.INSTANCE, ParityLattice.EVEN, ParityLattice.ODD));
		assertEquals(ParityLattice.EVEN, evalBin(BitwiseAnd.INSTANCE, ParityLattice.ODD, ParityLattice.EVEN));
		// both ODD → ODD
		assertEquals(ParityLattice.ODD, evalBin(BitwiseAnd.INSTANCE, ParityLattice.ODD, ParityLattice.ODD));
	}

	@Test
	void evalBitwiseOr() throws SemanticException {
		// at least one ODD → ODD
		assertEquals(ParityLattice.ODD, evalBin(BitwiseOr.INSTANCE, ParityLattice.ODD, ParityLattice.ODD));
		assertEquals(ParityLattice.ODD, evalBin(BitwiseOr.INSTANCE, ParityLattice.ODD, ParityLattice.EVEN));
		assertEquals(ParityLattice.ODD, evalBin(BitwiseOr.INSTANCE, ParityLattice.EVEN, ParityLattice.ODD));
		// both EVEN → EVEN
		assertEquals(ParityLattice.EVEN, evalBin(BitwiseOr.INSTANCE, ParityLattice.EVEN, ParityLattice.EVEN));
	}

	@Test
	void evalBitwiseShiftLeftAlwaysEven() throws SemanticException {
		assertEquals(ParityLattice.EVEN, evalBin(BitwiseShiftLeft.INSTANCE, ParityLattice.ODD, ParityLattice.ODD));
		assertEquals(ParityLattice.EVEN, evalBin(BitwiseShiftLeft.INSTANCE, ParityLattice.EVEN, ParityLattice.EVEN));
		assertEquals(ParityLattice.EVEN, evalBin(BitwiseShiftLeft.INSTANCE, ParityLattice.ODD, ParityLattice.EVEN));
	}

	@Test
	void evalBitwiseXor() throws SemanticException {
		// different parities → ODD (last bits differ)
		assertEquals(ParityLattice.ODD, evalBin(BitwiseXor.INSTANCE, ParityLattice.EVEN, ParityLattice.ODD));
		assertEquals(ParityLattice.ODD, evalBin(BitwiseXor.INSTANCE, ParityLattice.ODD, ParityLattice.EVEN));
		// same parities → EVEN
		assertEquals(ParityLattice.EVEN, evalBin(BitwiseXor.INSTANCE, ParityLattice.EVEN, ParityLattice.EVEN));
		assertEquals(ParityLattice.EVEN, evalBin(BitwiseXor.INSTANCE, ParityLattice.ODD, ParityLattice.ODD));
	}

	@Test
	void evalNumericMaxMin() throws SemanticException {
		assertEquals(ParityLattice.EVEN, evalBin(NumericMax.INSTANCE, ParityLattice.EVEN, ParityLattice.EVEN));
		assertEquals(ParityLattice.ODD, evalBin(NumericMax.INSTANCE, ParityLattice.ODD, ParityLattice.ODD));
		assertEquals(ParityLattice.TOP, evalBin(NumericMax.INSTANCE, ParityLattice.EVEN, ParityLattice.ODD));
		assertEquals(ParityLattice.EVEN, evalBin(NumericMin.INSTANCE, ParityLattice.EVEN, ParityLattice.EVEN));
		assertEquals(ParityLattice.TOP, evalBin(NumericMin.INSTANCE, ParityLattice.ODD, ParityLattice.EVEN));
	}

	@Test
	void evalBinaryOtherOpsReturnTop() throws SemanticException {
		assertEquals(ParityLattice.TOP,
				evalBin(NumericAtan2.INSTANCE, ParityLattice.EVEN, ParityLattice.ODD));
		assertEquals(ParityLattice.TOP,
				evalBin(BitwiseShiftRight.INSTANCE, ParityLattice.ODD, ParityLattice.ODD));
	}

	// --- evalTernaryExpression ---

	@Test
	void evalTernaryWithoutWvaReturnsTop() throws SemanticException {
		TernaryExpression expr = new TernaryExpression(
				Int32Type.INSTANCE, variable, varAux, variable,
				StringIndexOfCharFromIndex.INSTANCE, pp.getLocation());
		assertEquals(ParityLattice.TOP,
				domain.evalTernaryExpression(expr, ParityLattice.EVEN, ParityLattice.ODD, ParityLattice.EVEN, pp,
						oracle));
	}

	// --- satisfiesBinaryExpression ---

	@Test
	void satisfiesEq() throws SemanticException {
		// same parity: might be equal (e.g., 2==2) or not (2==4) → UNKNOWN
		assertEquals(Satisfiability.UNKNOWN,
				satisfiesBin(ComparisonEq.INSTANCE, ParityLattice.EVEN, ParityLattice.EVEN));
		assertEquals(Satisfiability.UNKNOWN, satisfiesBin(ComparisonEq.INSTANCE, ParityLattice.ODD, ParityLattice.ODD));
		// different parity: can never be equal (even ≠ odd) → NOT_SATISFIED
		assertEquals(Satisfiability.NOT_SATISFIED,
				satisfiesBin(ComparisonEq.INSTANCE, ParityLattice.EVEN, ParityLattice.ODD));
		// TOP on either side → UNKNOWN
		assertEquals(Satisfiability.UNKNOWN,
				satisfiesBin(ComparisonEq.INSTANCE, ParityLattice.TOP, ParityLattice.EVEN));
	}

	@Test
	void satisfiesNe() throws SemanticException {
		// same parity: two even (or odd) numbers might or might not be equal →
		// UNKNOWN
		assertEquals(Satisfiability.UNKNOWN,
				satisfiesBin(ComparisonNe.INSTANCE, ParityLattice.EVEN, ParityLattice.EVEN));
		assertEquals(Satisfiability.UNKNOWN,
				satisfiesBin(ComparisonNe.INSTANCE, ParityLattice.ODD, ParityLattice.ODD));
		// different parity: even ≠ odd always → SATISFIED
		assertEquals(Satisfiability.SATISFIED,
				satisfiesBin(ComparisonNe.INSTANCE, ParityLattice.EVEN, ParityLattice.ODD));
		assertEquals(Satisfiability.SATISFIED,
				satisfiesBin(ComparisonNe.INSTANCE, ParityLattice.ODD, ParityLattice.EVEN));
	}

	@Test
	void satisfiesOtherOpsReturnUnknown() throws SemanticException {
		// Ordering comparisons are not handled → UNKNOWN
		assertEquals(Satisfiability.UNKNOWN,
				satisfiesBin(it.unive.lisa.symbolic.value.operator.binary.ComparisonLt.INSTANCE,
						ParityLattice.EVEN, ParityLattice.ODD));
	}

	// --- assumeBinaryExpression ---

	@Test
	void assumeEqSetsIdentifierParity() throws SemanticException {
		// assume variable == even constant: variable becomes EVEN
		BinaryExpression expr = new BinaryExpression(
				Int32Type.INSTANCE, variable, mkConst(4), ComparisonEq.INSTANCE, pp.getLocation());
		ValueEnvironment<ParityLattice> result = domain.assumeBinaryExpression(env, expr, pp, pp, oracle);
		assertEquals(ParityLattice.EVEN, result.getState(variable));
	}

	@Test
	void assumeEqRightIdentifier() throws SemanticException {
		// assume even_constant == variable (right is identifier)
		BinaryExpression expr = new BinaryExpression(
				Int32Type.INSTANCE, mkConst(3), variable, ComparisonEq.INSTANCE, pp.getLocation());
		ValueEnvironment<ParityLattice> result = domain.assumeBinaryExpression(env, expr, pp, pp, oracle);
		assertEquals(ParityLattice.ODD, result.getState(variable));
	}

	@Test
	void assumeNonEqLeavesEnvUnchanged() throws SemanticException {
		// assume with NE: no refinement
		BinaryExpression expr = new BinaryExpression(
				Int32Type.INSTANCE, variable, mkConst(4), ComparisonNe.INSTANCE, pp.getLocation());
		ValueEnvironment<ParityLattice> result = domain.assumeBinaryExpression(env, expr, pp, pp, oracle);
		assertEquals(ParityLattice.TOP, result.getState(variable));
	}

	// --- WVA tests: PushFromConstraints ---

	@Test
	void pushFromConstraintsEqEven() throws SemanticException {
		// constraint: 4 == variable → EVEN
		PushFromConstraints push = new PushFromConstraints(
				Int32Type.INSTANCE, pp.getLocation(), mkConstraintEq(4));
		assertEquals(ParityLattice.EVEN, domain.evalPushAny(push, pp, oracle));
	}

	@Test
	void pushFromConstraintsEqOdd() throws SemanticException {
		// constraint: 7 == variable → ODD
		PushFromConstraints push = new PushFromConstraints(
				Int32Type.INSTANCE, pp.getLocation(), mkConstraintEq(7));
		assertEquals(ParityLattice.ODD, domain.evalPushAny(push, pp, oracle));
	}

	@Test
	void pushFromConstraintsNoEqConstraintReturnsTop() throws SemanticException {
		// ge/le constraints are ignored by Parity → TOP
		BinaryExpression c = new BinaryExpression(Int32Type.INSTANCE,
				mkConst(5), variable, ComparisonGe.INSTANCE, pp.getLocation());
		PushFromConstraints push = new PushFromConstraints(
				Int32Type.INSTANCE, pp.getLocation(), c);
		assertEquals(ParityLattice.TOP, domain.evalPushAny(push, pp, oracle));
	}

	@Test
	void pushFromConstraintsEmptyReturnsTop() throws SemanticException {
		PushFromConstraints push = new PushFromConstraints(
				Int32Type.INSTANCE, pp.getLocation(), Collections.emptySet());
		assertEquals(ParityLattice.TOP, domain.evalPushAny(push, pp, oracle));
	}

	// --- WVA tests: StringLength oracle ---

	@Test
	void wvaStringLengthEvenConstraintReturnsEven() throws SemanticException {
		// oracle returns: 4 == StringLength(variable)
		// filter passes because right is a StringLength UnaryExpression
		// generate() finds 4 == ... → EVEN
		WVAOracle wva = new WVAOracle(Set.of(mkConstraintEqStrLen(4)));
		UnaryExpression strLen = mkUnaryOn(StringLength.INSTANCE, variable);
		assertEquals(ParityLattice.EVEN,
				domain.evalUnaryExpression(strLen, ParityLattice.TOP, pp, wva));
	}

	@Test
	void wvaStringLengthOddConstraintReturnsOdd() throws SemanticException {
		// oracle returns: 5 == StringLength(variable)
		WVAOracle wva = new WVAOracle(Set.of(mkConstraintEqStrLen(5)));
		UnaryExpression strLen = mkUnaryOn(StringLength.INSTANCE, variable);
		assertEquals(ParityLattice.ODD,
				domain.evalUnaryExpression(strLen, ParityLattice.TOP, pp, wva));
	}

	@Test
	void wvaStringLengthNoMatchingConstraintReturnsTop() throws SemanticException {
		// oracle returns a constraint where right is NOT StringLength →
		// filtered out → TOP
		BinaryExpression nonStrLen = new BinaryExpression(Int32Type.INSTANCE,
				mkConst(4), variable, ComparisonEq.INSTANCE, pp.getLocation());
		WVAOracle wva = new WVAOracle(Set.of(nonStrLen));
		UnaryExpression strLen = mkUnaryOn(StringLength.INSTANCE, variable);
		assertEquals(ParityLattice.TOP,
				domain.evalUnaryExpression(strLen, ParityLattice.TOP, pp, wva));
	}

	// --- WVA tests: StringIndexOf oracle ---

	@Test
	void wvaStringIndexOfEvenConstraintReturnsEven() throws SemanticException {
		// oracle returns: 6 == expr for a StringIndexOf expression → EVEN
		WVAOracle wva = new WVAOracle(Set.of(mkConstraintEq(6)));
		BinaryExpression strIdx = new BinaryExpression(
				Int32Type.INSTANCE, variable, varAux, StringIndexOfChar.INSTANCE, pp.getLocation());
		assertEquals(ParityLattice.EVEN,
				domain.evalBinaryExpression(strIdx, ParityLattice.TOP, ParityLattice.TOP, pp, wva));
	}

	@Test
	void wvaStringIndexOfOddConstraintReturnsOdd() throws SemanticException {
		WVAOracle wva = new WVAOracle(Set.of(mkConstraintEq(5)));
		BinaryExpression strIdx = new BinaryExpression(
				Int32Type.INSTANCE, variable, varAux, StringIndexOf.INSTANCE, pp.getLocation());
		assertEquals(ParityLattice.ODD,
				domain.evalBinaryExpression(strIdx, ParityLattice.TOP, ParityLattice.TOP, pp, wva));
	}

	@Test
	void wvaValueComparisonBothTopUsesOracle() throws SemanticException {
		// ValueComparison with both TOP and WVA → queries oracle
		WVAOracle wva = new WVAOracle(Set.of(mkConstraintEq(8)));
		BinaryExpression cmp = new BinaryExpression(
				Int32Type.INSTANCE, variable, varAux, ValueComparison.INSTANCE, pp.getLocation());
		assertEquals(ParityLattice.EVEN,
				domain.evalBinaryExpression(cmp, ParityLattice.TOP, ParityLattice.TOP, pp, wva));
	}

	// --- WVA tests: TernaryExpression oracle ---

	@Test
	void wvaTernaryStringIndexOfCharFromIndexOdd() throws SemanticException {
		// oracle returns: 7 == expr → ODD
		WVAOracle wva = new WVAOracle(Set.of(mkConstraintEq(7)));
		TernaryExpression expr = new TernaryExpression(
				Int32Type.INSTANCE, variable, varAux, variable,
				StringIndexOfCharFromIndex.INSTANCE, pp.getLocation());
		assertEquals(ParityLattice.ODD,
				domain.evalTernaryExpression(expr,
						ParityLattice.TOP, ParityLattice.TOP, ParityLattice.TOP, pp, wva));
	}
}
