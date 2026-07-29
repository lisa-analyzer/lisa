package it.unive.lisa.analysis.numeric;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestParameterProvider;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.lattices.numeric.SignLattice;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.PushFromConstraints;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingAdd;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingDiv;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingMod;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingMul;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingRem;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingSub;
import it.unive.lisa.symbolic.value.operator.unary.NumericAbs;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class SignTest {

	private final ProgramPoint pp = TestParameterProvider.provideParam(null, ProgramPoint.class);
	private final Sign domain = new Sign();
	private final Variable variable = new Variable(Int32Type.INSTANCE, "x", pp.getLocation());
	private final Variable varAux = new Variable(Int32Type.INSTANCE, "aux", pp.getLocation());
	private final SemanticOracle oracle = TestParameterProvider.provideParam(null, SemanticOracle.class);

	private BinaryExpression mkBin(
			BinaryOperator op) {
		return new BinaryExpression(Int32Type.INSTANCE, varAux, variable, op, pp.getLocation());
	}

	private UnaryExpression mkUnary(
			UnaryOperator op) {
		return new UnaryExpression(Int32Type.INSTANCE, varAux, op, pp.getLocation());
	}

	private Constant mkConst(
			Object val) {
		return new Constant(Int32Type.INSTANCE, val, pp.getLocation());
	}

	private SignLattice evalBin(
			BinaryOperator op,
			SignLattice left,
			SignLattice right)
			throws SemanticException {
		return domain.evalBinaryExpression(mkBin(op), left, right, pp, oracle);
	}

	private SignLattice evalUn(
			UnaryOperator op,
			SignLattice arg)
			throws SemanticException {
		return domain.evalUnaryExpression(mkUnary(op), arg, pp, oracle);
	}

	private Satisfiability satisfiesBin(
			BinaryOperator op,
			SignLattice left,
			SignLattice right)
			throws SemanticException {
		return domain.satisfiesBinaryExpression(mkBin(op), left, right, pp, oracle);
	}

	private ValueEnvironment<SignLattice> envWith(
			SignLattice sign) {
		return new ValueEnvironment<>(SignLattice.TOP).putState(variable, sign);
	}

	// assume variable op constant
	private ValueEnvironment<SignLattice> assume(
			SignLattice starting,
			BinaryOperator op,
			int constant)
			throws SemanticException {
		BinaryExpression expr = new BinaryExpression(
				Int32Type.INSTANCE, variable, mkConst(constant), op, pp.getLocation());
		return domain.assumeBinaryExpression(envWith(starting), expr, pp, pp, oracle);
	}

	private BinaryExpression mkConstraint(
			int constant,
			BinaryOperator op) {
		return new BinaryExpression(Int32Type.INSTANCE,
				new Constant(Int32Type.INSTANCE, constant, pp.getLocation()),
				variable, op, pp.getLocation());
	}

	// --- evalConstant ---

	@Test
	void evalConstantByte() throws SemanticException {
		assertEquals(SignLattice.POS, domain.evalConstant(mkConst((byte) 5), pp, oracle));
		assertEquals(SignLattice.ZERO, domain.evalConstant(mkConst((byte) 0), pp, oracle));
		assertEquals(SignLattice.NEG, domain.evalConstant(mkConst((byte) -3), pp, oracle));
	}

	@Test
	void evalConstantShort() throws SemanticException {
		assertEquals(SignLattice.POS, domain.evalConstant(mkConst((short) 100), pp, oracle));
		assertEquals(SignLattice.ZERO, domain.evalConstant(mkConst((short) 0), pp, oracle));
		assertEquals(SignLattice.NEG, domain.evalConstant(mkConst((short) -100), pp, oracle));
	}

	@Test
	void evalConstantInteger() throws SemanticException {
		assertEquals(SignLattice.POS, domain.evalConstant(mkConst(42), pp, oracle));
		assertEquals(SignLattice.ZERO, domain.evalConstant(mkConst(0), pp, oracle));
		assertEquals(SignLattice.NEG, domain.evalConstant(mkConst(-42), pp, oracle));
	}

	@Test
	void evalConstantLong() throws SemanticException {
		assertEquals(SignLattice.POS, domain.evalConstant(mkConst(10L), pp, oracle));
		assertEquals(SignLattice.ZERO, domain.evalConstant(mkConst(0L), pp, oracle));
		assertEquals(SignLattice.NEG, domain.evalConstant(mkConst(-10L), pp, oracle));
	}

	@Test
	void evalConstantFloat() throws SemanticException {
		assertEquals(SignLattice.POS, domain.evalConstant(mkConst(1.5f), pp, oracle));
		assertEquals(SignLattice.ZERO, domain.evalConstant(mkConst(0.0f), pp, oracle));
		assertEquals(SignLattice.NEG, domain.evalConstant(mkConst(-1.5f), pp, oracle));
	}

	@Test
	void evalConstantDouble() throws SemanticException {
		assertEquals(SignLattice.POS, domain.evalConstant(mkConst(3.14), pp, oracle));
		assertEquals(SignLattice.ZERO, domain.evalConstant(mkConst(0.0), pp, oracle));
		assertEquals(SignLattice.NEG, domain.evalConstant(mkConst(-3.14), pp, oracle));
	}

	@Test
	void evalConstantStringReturnsTop() throws SemanticException {
		assertEquals(SignLattice.TOP, domain.evalConstant(mkConst("hello"), pp, oracle));
	}

	// --- evalUnaryExpression ---

	@Test
	void evalUnaryNegation() throws SemanticException {
		// negation flips the sign
		assertEquals(SignLattice.NEG, evalUn(NumericNegation.INSTANCE, SignLattice.POS));
		assertEquals(SignLattice.POS, evalUn(NumericNegation.INSTANCE, SignLattice.NEG));
		assertEquals(SignLattice.ZERO, evalUn(NumericNegation.INSTANCE, SignLattice.ZERO));
		assertEquals(SignLattice.TOP, evalUn(NumericNegation.INSTANCE, SignLattice.TOP));
	}

	@Test
	void evalUnaryNonNegationReturnsTop() throws SemanticException {
		// only NumericNegation is handled; everything else → TOP
		assertEquals(SignLattice.TOP, evalUn(NumericAbs.INSTANCE, SignLattice.NEG));
		assertEquals(SignLattice.TOP, evalUn(NumericAbs.INSTANCE, SignLattice.POS));
	}

	// --- evalBinaryExpression: Addition ---

	@Test
	void evalAddition() throws SemanticException {
		assertEquals(SignLattice.POS, evalBin(NumericNonOverflowingAdd.INSTANCE, SignLattice.ZERO, SignLattice.POS));
		assertEquals(SignLattice.NEG, evalBin(NumericNonOverflowingAdd.INSTANCE, SignLattice.ZERO, SignLattice.NEG));
		assertEquals(SignLattice.POS, evalBin(NumericNonOverflowingAdd.INSTANCE, SignLattice.POS, SignLattice.ZERO));
		assertEquals(SignLattice.POS, evalBin(NumericNonOverflowingAdd.INSTANCE, SignLattice.POS, SignLattice.POS));
		assertEquals(SignLattice.NEG, evalBin(NumericNonOverflowingAdd.INSTANCE, SignLattice.NEG, SignLattice.NEG));
		// mixed signs → TOP
		assertEquals(SignLattice.TOP, evalBin(NumericNonOverflowingAdd.INSTANCE, SignLattice.POS, SignLattice.NEG));
		assertEquals(SignLattice.TOP, evalBin(NumericNonOverflowingAdd.INSTANCE, SignLattice.NEG, SignLattice.POS));
		// TOP + something
		assertEquals(SignLattice.TOP, evalBin(NumericNonOverflowingAdd.INSTANCE, SignLattice.TOP, SignLattice.POS));
	}

	// --- evalBinaryExpression: Subtraction ---

	@Test
	void evalSubtraction() throws SemanticException {
		// 0 - x = opposite of x
		assertEquals(SignLattice.NEG, evalBin(NumericNonOverflowingSub.INSTANCE, SignLattice.ZERO, SignLattice.POS));
		assertEquals(SignLattice.POS, evalBin(NumericNonOverflowingSub.INSTANCE, SignLattice.ZERO, SignLattice.NEG));
		assertEquals(SignLattice.ZERO, evalBin(NumericNonOverflowingSub.INSTANCE, SignLattice.ZERO, SignLattice.ZERO));
		// x - 0 = x
		assertEquals(SignLattice.POS, evalBin(NumericNonOverflowingSub.INSTANCE, SignLattice.POS, SignLattice.ZERO));
		assertEquals(SignLattice.NEG, evalBin(NumericNonOverflowingSub.INSTANCE, SignLattice.NEG, SignLattice.ZERO));
		// same sign → TOP (can't determine)
		assertEquals(SignLattice.TOP, evalBin(NumericNonOverflowingSub.INSTANCE, SignLattice.POS, SignLattice.POS));
		assertEquals(SignLattice.TOP, evalBin(NumericNonOverflowingSub.INSTANCE, SignLattice.NEG, SignLattice.NEG));
		// different non-zero signs → left sign
		assertEquals(SignLattice.POS, evalBin(NumericNonOverflowingSub.INSTANCE, SignLattice.POS, SignLattice.NEG));
		assertEquals(SignLattice.NEG, evalBin(NumericNonOverflowingSub.INSTANCE, SignLattice.NEG, SignLattice.POS));
	}

	// --- evalBinaryExpression: Division ---

	@Test
	void evalDivision() throws SemanticException {
		// division by zero → BOTTOM
		assertEquals(SignLattice.BOTTOM, evalBin(NumericNonOverflowingDiv.INSTANCE, SignLattice.POS, SignLattice.ZERO));
		assertEquals(SignLattice.BOTTOM, evalBin(NumericNonOverflowingDiv.INSTANCE, SignLattice.NEG, SignLattice.ZERO));
		// 0 / x = 0
		assertEquals(SignLattice.ZERO, evalBin(NumericNonOverflowingDiv.INSTANCE, SignLattice.ZERO, SignLattice.POS));
		assertEquals(SignLattice.ZERO, evalBin(NumericNonOverflowingDiv.INSTANCE, SignLattice.ZERO, SignLattice.NEG));
		// same sign / same sign → POS (+/+ or -/-)
		assertEquals(SignLattice.POS, evalBin(NumericNonOverflowingDiv.INSTANCE, SignLattice.POS, SignLattice.POS));
		assertEquals(SignLattice.POS, evalBin(NumericNonOverflowingDiv.INSTANCE, SignLattice.NEG, SignLattice.NEG));
		// TOP / TOP → TOP
		assertEquals(SignLattice.TOP, evalBin(NumericNonOverflowingDiv.INSTANCE, SignLattice.TOP, SignLattice.TOP));
		// opposite signs → NEG (+/- or -/+)
		assertEquals(SignLattice.NEG, evalBin(NumericNonOverflowingDiv.INSTANCE, SignLattice.POS, SignLattice.NEG));
		assertEquals(SignLattice.NEG, evalBin(NumericNonOverflowingDiv.INSTANCE, SignLattice.NEG, SignLattice.POS));
		// TOP / non-zero → TOP
		assertEquals(SignLattice.TOP, evalBin(NumericNonOverflowingDiv.INSTANCE, SignLattice.TOP, SignLattice.POS));
	}

	// --- evalBinaryExpression: Modulo and Remainder ---

	@Test
	void evalModulo() throws SemanticException {
		// modulo returns the sign of the right operand
		assertEquals(SignLattice.POS, evalBin(NumericNonOverflowingMod.INSTANCE, SignLattice.NEG, SignLattice.POS));
		assertEquals(SignLattice.NEG, evalBin(NumericNonOverflowingMod.INSTANCE, SignLattice.POS, SignLattice.NEG));
		assertEquals(SignLattice.TOP, evalBin(NumericNonOverflowingMod.INSTANCE, SignLattice.NEG, SignLattice.TOP));
	}

	@Test
	void evalRemainder() throws SemanticException {
		// remainder returns the sign of the left operand
		assertEquals(SignLattice.POS, evalBin(NumericNonOverflowingRem.INSTANCE, SignLattice.POS, SignLattice.NEG));
		assertEquals(SignLattice.NEG, evalBin(NumericNonOverflowingRem.INSTANCE, SignLattice.NEG, SignLattice.POS));
		assertEquals(SignLattice.TOP, evalBin(NumericNonOverflowingRem.INSTANCE, SignLattice.TOP, SignLattice.POS));
	}

	// --- evalBinaryExpression: Multiplication ---

	@Test
	void evalMultiplication() throws SemanticException {
		// zero absorbs
		assertEquals(SignLattice.ZERO, evalBin(NumericNonOverflowingMul.INSTANCE, SignLattice.ZERO, SignLattice.POS));
		assertEquals(SignLattice.ZERO, evalBin(NumericNonOverflowingMul.INSTANCE, SignLattice.POS, SignLattice.ZERO));
		assertEquals(SignLattice.ZERO, evalBin(NumericNonOverflowingMul.INSTANCE, SignLattice.ZERO, SignLattice.TOP));
		// TOP propagates (when no zero)
		assertEquals(SignLattice.TOP, evalBin(NumericNonOverflowingMul.INSTANCE, SignLattice.TOP, SignLattice.POS));
		assertEquals(SignLattice.TOP, evalBin(NumericNonOverflowingMul.INSTANCE, SignLattice.POS, SignLattice.TOP));
		// same sign → POS
		assertEquals(SignLattice.POS, evalBin(NumericNonOverflowingMul.INSTANCE, SignLattice.POS, SignLattice.POS));
		assertEquals(SignLattice.POS, evalBin(NumericNonOverflowingMul.INSTANCE, SignLattice.NEG, SignLattice.NEG));
		// different non-zero signs → NEG
		assertEquals(SignLattice.NEG, evalBin(NumericNonOverflowingMul.INSTANCE, SignLattice.POS, SignLattice.NEG));
		assertEquals(SignLattice.NEG, evalBin(NumericNonOverflowingMul.INSTANCE, SignLattice.NEG, SignLattice.POS));
	}

	@Test
	void evalBinaryUnknownOperatorReturnsTop() throws SemanticException {
		// ComparisonEq is not an arithmetic op → TOP
		assertEquals(SignLattice.TOP,
				evalBin(ComparisonEq.INSTANCE, SignLattice.POS, SignLattice.POS));
	}

	// --- satisfiesBinaryExpression ---

	@Test
	void satisfiesEq() throws SemanticException {
		// ZERO == ZERO is always satisfied
		assertEquals(Satisfiability.SATISFIED, satisfiesBin(ComparisonEq.INSTANCE, SignLattice.ZERO, SignLattice.ZERO));
		// POS == POS is unknown (two positive numbers might or might not be
		// equal)
		assertEquals(Satisfiability.UNKNOWN, satisfiesBin(ComparisonEq.INSTANCE, SignLattice.POS, SignLattice.POS));
		// ZERO == POS is never satisfied (0 ≠ positive)
		assertEquals(Satisfiability.NOT_SATISFIED,
				satisfiesBin(ComparisonEq.INSTANCE, SignLattice.ZERO, SignLattice.POS));
		// POS == NEG is never satisfied
		assertEquals(Satisfiability.NOT_SATISFIED,
				satisfiesBin(ComparisonEq.INSTANCE, SignLattice.POS, SignLattice.NEG));
		// TOP on either side → UNKNOWN
		assertEquals(Satisfiability.UNKNOWN, satisfiesBin(ComparisonEq.INSTANCE, SignLattice.TOP, SignLattice.POS));
		assertEquals(Satisfiability.UNKNOWN, satisfiesBin(ComparisonEq.INSTANCE, SignLattice.POS, SignLattice.TOP));
	}

	@Test
	void satisfiesGt() throws SemanticException {
		// POS > ZERO → SAT
		assertEquals(Satisfiability.SATISFIED, satisfiesBin(ComparisonGt.INSTANCE, SignLattice.POS, SignLattice.ZERO));
		// POS > NEG → SAT
		assertEquals(Satisfiability.SATISFIED, satisfiesBin(ComparisonGt.INSTANCE, SignLattice.POS, SignLattice.NEG));
		// ZERO > NEG → SAT
		assertEquals(Satisfiability.SATISFIED, satisfiesBin(ComparisonGt.INSTANCE, SignLattice.ZERO, SignLattice.NEG));
		// NEG > POS → NOT_SAT
		assertEquals(Satisfiability.NOT_SATISFIED,
				satisfiesBin(ComparisonGt.INSTANCE, SignLattice.NEG, SignLattice.POS));
		// POS > POS → UNKNOWN (could be 3>5 or 5>3)
		assertEquals(Satisfiability.UNKNOWN, satisfiesBin(ComparisonGt.INSTANCE, SignLattice.POS, SignLattice.POS));
	}

	@Test
	void satisfiesGe() throws SemanticException {
		// POS >= ZERO → SAT (positive > 0)
		assertEquals(Satisfiability.SATISFIED, satisfiesBin(ComparisonGe.INSTANCE, SignLattice.POS, SignLattice.ZERO));
		// ZERO >= POS → NOT_SAT (0 < positive)
		assertEquals(Satisfiability.NOT_SATISFIED,
				satisfiesBin(ComparisonGe.INSTANCE, SignLattice.ZERO, SignLattice.POS));
		// POS >= POS → UNKNOWN
		assertEquals(Satisfiability.UNKNOWN, satisfiesBin(ComparisonGe.INSTANCE, SignLattice.POS, SignLattice.POS));
	}

	@Test
	void satisfiesLe() throws SemanticException {
		// ZERO <= POS → SAT (0 < positive)
		assertEquals(Satisfiability.SATISFIED, satisfiesBin(ComparisonLe.INSTANCE, SignLattice.ZERO, SignLattice.POS));
		// POS <= ZERO → NOT_SAT (positive > 0)
		assertEquals(Satisfiability.NOT_SATISFIED,
				satisfiesBin(ComparisonLe.INSTANCE, SignLattice.POS, SignLattice.ZERO));
		// POS <= POS → UNKNOWN
		assertEquals(Satisfiability.UNKNOWN, satisfiesBin(ComparisonLe.INSTANCE, SignLattice.POS, SignLattice.POS));
	}

	@Test
	void satisfiesLt() throws SemanticException {
		// ZERO < POS → SAT
		assertEquals(Satisfiability.SATISFIED, satisfiesBin(ComparisonLt.INSTANCE, SignLattice.ZERO, SignLattice.POS));
		// NEG < POS → SAT
		assertEquals(Satisfiability.SATISFIED, satisfiesBin(ComparisonLt.INSTANCE, SignLattice.NEG, SignLattice.POS));
		// POS < ZERO → NOT_SAT
		assertEquals(Satisfiability.NOT_SATISFIED,
				satisfiesBin(ComparisonLt.INSTANCE, SignLattice.POS, SignLattice.ZERO));
		// ZERO < ZERO → NOT_SAT (0 < 0 is false)
		assertEquals(Satisfiability.NOT_SATISFIED,
				satisfiesBin(ComparisonLt.INSTANCE, SignLattice.ZERO, SignLattice.ZERO));
		// POS < POS → UNKNOWN
		assertEquals(Satisfiability.UNKNOWN, satisfiesBin(ComparisonLt.INSTANCE, SignLattice.POS, SignLattice.POS));
	}

	@Test
	void satisfiesNe() throws SemanticException {
		// ZERO != ZERO → NOT_SAT (0 == 0 always)
		assertEquals(Satisfiability.NOT_SATISFIED,
				satisfiesBin(ComparisonNe.INSTANCE, SignLattice.ZERO, SignLattice.ZERO));
		// POS != NEG → SAT (positive ≠ negative)
		assertEquals(Satisfiability.SATISFIED, satisfiesBin(ComparisonNe.INSTANCE, SignLattice.POS, SignLattice.NEG));
		// POS != POS → UNKNOWN
		assertEquals(Satisfiability.UNKNOWN, satisfiesBin(ComparisonNe.INSTANCE, SignLattice.POS, SignLattice.POS));
	}

	// --- assumeBinaryExpression ---

	@Test
	void assumeEqPositiveConstant() throws SemanticException {
		// assume variable == 5: variable must be positive
		ValueEnvironment<SignLattice> result = assume(SignLattice.TOP, ComparisonEq.INSTANCE, 5);
		assertEquals(SignLattice.POS, result.getState(variable));
	}

	@Test
	void assumeEqNegativeConstant() throws SemanticException {
		// assume variable == -3: variable must be negative
		ValueEnvironment<SignLattice> result = assume(SignLattice.TOP, ComparisonEq.INSTANCE, -3);
		assertEquals(SignLattice.NEG, result.getState(variable));
	}

	@Test
	void assumeEqZero() throws SemanticException {
		// assume variable == 0: variable must be zero
		ValueEnvironment<SignLattice> result = assume(SignLattice.TOP, ComparisonEq.INSTANCE, 0);
		assertEquals(SignLattice.ZERO, result.getState(variable));
	}

	@Test
	void assumeGtPositiveConstantRefinesSign() throws SemanticException {
		// assume variable > 0: variable must be positive
		ValueEnvironment<SignLattice> result = assume(SignLattice.TOP, ComparisonGt.INSTANCE, 0);
		assertEquals(SignLattice.POS, result.getState(variable));
	}

	@Test
	void assumeLtZeroRefinesSign() throws SemanticException {
		// assume variable < 0: variable must be negative
		ValueEnvironment<SignLattice> result = assume(SignLattice.TOP, ComparisonLt.INSTANCE, 0);
		assertEquals(SignLattice.NEG, result.getState(variable));
	}

	@Test
	void assumeContradictionReturnsBottom() throws SemanticException {
		// assume variable < 0, but variable is POS → contradiction
		ValueEnvironment<SignLattice> result = assume(SignLattice.POS, ComparisonLt.INSTANCE, 0);
		assertTrue(result.isBottom());
	}

	@Test
	void assumeSatisfiedConditionUnchanged() throws SemanticException {
		// variable is POS, assume variable > 0 → already satisfied, env
		// unchanged
		ValueEnvironment<SignLattice> result = assume(SignLattice.POS, ComparisonGt.INSTANCE, 0);
		assertEquals(SignLattice.POS, result.getState(variable));
		assertTrue(!result.isBottom());
	}

	// --- WVA tests via PushFromConstraints ---

	@Test
	void pushFromConstraintsEqPositive() throws SemanticException {
		// constraint: 5 == variable → sign is POS
		PushFromConstraints push = new PushFromConstraints(
				Int32Type.INSTANCE, pp.getLocation(),
				mkConstraint(5, ComparisonEq.INSTANCE));
		assertEquals(SignLattice.POS, domain.evalPushAny(push, pp, oracle));
	}

	@Test
	void pushFromConstraintsEqZero() throws SemanticException {
		// constraint: 0 == variable → sign is ZERO
		PushFromConstraints push = new PushFromConstraints(
				Int32Type.INSTANCE, pp.getLocation(),
				mkConstraint(0, ComparisonEq.INSTANCE));
		assertEquals(SignLattice.ZERO, domain.evalPushAny(push, pp, oracle));
	}

	@Test
	void pushFromConstraintsEqNegative() throws SemanticException {
		// constraint: -5 == variable → sign is NEG
		PushFromConstraints push = new PushFromConstraints(
				Int32Type.INSTANCE, pp.getLocation(),
				mkConstraint(-5, ComparisonEq.INSTANCE));
		assertEquals(SignLattice.NEG, domain.evalPushAny(push, pp, oracle));
	}

	@Test
	void pushFromConstraintsUpperBoundNegative() throws SemanticException {
		// constraint: -3 >= variable → upper bound is negative → sign is NEG
		PushFromConstraints push = new PushFromConstraints(
				Int32Type.INSTANCE, pp.getLocation(),
				mkConstraint(-3, ComparisonGe.INSTANCE));
		assertEquals(SignLattice.NEG, domain.evalPushAny(push, pp, oracle));
	}

	@Test
	void pushFromConstraintsLowerBoundPositive() throws SemanticException {
		// constraint: 3 <= variable → lower bound is positive → sign is POS
		PushFromConstraints push = new PushFromConstraints(
				Int32Type.INSTANCE, pp.getLocation(),
				mkConstraint(3, ComparisonLe.INSTANCE));
		assertEquals(SignLattice.POS, domain.evalPushAny(push, pp, oracle));
	}

	@Test
	void pushFromConstraintsBothBoundsPositiveRange() throws SemanticException {
		// 3 >= variable (upper=3) and 1 <= variable (lower=1) → interval [1,3]
		// → POS
		PushFromConstraints push = new PushFromConstraints(
				Int32Type.INSTANCE, pp.getLocation(),
				Set.of(mkConstraint(3, ComparisonGe.INSTANCE),
						mkConstraint(1, ComparisonLe.INSTANCE)));
		assertEquals(SignLattice.POS, domain.evalPushAny(push, pp, oracle));
	}

	@Test
	void pushFromConstraintsSingletonPositive() throws SemanticException {
		// ge==le==5: constant is 5 → POS
		PushFromConstraints push = new PushFromConstraints(
				Int32Type.INSTANCE, pp.getLocation(),
				Set.of(mkConstraint(5, ComparisonGe.INSTANCE),
						mkConstraint(5, ComparisonLe.INSTANCE)));
		assertEquals(SignLattice.POS, domain.evalPushAny(push, pp, oracle));
	}

	@Test
	void pushFromConstraintsSingletonNegative() throws SemanticException {
		// ge==le==-3: constant is -3 → NEG
		PushFromConstraints push = new PushFromConstraints(
				Int32Type.INSTANCE, pp.getLocation(),
				Set.of(mkConstraint(-3, ComparisonGe.INSTANCE),
						mkConstraint(-3, ComparisonLe.INSTANCE)));
		assertEquals(SignLattice.NEG, domain.evalPushAny(push, pp, oracle));
	}

	@Test
	void pushFromConstraintsEmptyReturnsTop() throws SemanticException {
		// no constraints → TOP
		PushFromConstraints push = new PushFromConstraints(
				Int32Type.INSTANCE, pp.getLocation(), Collections.emptySet());
		assertEquals(SignLattice.TOP, domain.evalPushAny(push, pp, oracle));
	}

	@Test
	void pushFromConstraintsUpperBoundNonNegativeNoLowerReturnsTop() throws SemanticException {
		// constraint: 5 >= variable (upper bound = 5, no lower bound) → could
		// be
		// negative, zero, or a small positive → TOP
		PushFromConstraints push = new PushFromConstraints(
				Int32Type.INSTANCE, pp.getLocation(),
				mkConstraint(5, ComparisonGe.INSTANCE));
		assertEquals(SignLattice.TOP, domain.evalPushAny(push, pp, oracle));
	}
}
