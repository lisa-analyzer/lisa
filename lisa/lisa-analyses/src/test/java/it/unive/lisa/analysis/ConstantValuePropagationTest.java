package it.unive.lisa.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestParameterProvider;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.lattices.ConstantValue;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.program.type.StringType;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.BitwiseAnd;
import it.unive.lisa.symbolic.value.operator.binary.BitwiseOr;
import it.unive.lisa.symbolic.value.operator.binary.BitwiseShiftLeft;
import it.unive.lisa.symbolic.value.operator.binary.BitwiseShiftRight;
import it.unive.lisa.symbolic.value.operator.binary.BitwiseXor;
import it.unive.lisa.symbolic.value.operator.binary.CharacterEquals;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
import it.unive.lisa.symbolic.value.operator.binary.NumericMax;
import it.unive.lisa.symbolic.value.operator.binary.NumericMin;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingAdd;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingDiv;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingMul;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingRem;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingSub;
import it.unive.lisa.symbolic.value.operator.binary.StringConcat;
import it.unive.lisa.symbolic.value.operator.binary.StringContains;
import it.unive.lisa.symbolic.value.operator.binary.StringEndsWith;
import it.unive.lisa.symbolic.value.operator.binary.StringEquals;
import it.unive.lisa.symbolic.value.operator.binary.StringIndexOf;
import it.unive.lisa.symbolic.value.operator.binary.StringStartsWith;
import it.unive.lisa.symbolic.value.operator.binary.ValueComparison;
import it.unive.lisa.symbolic.value.operator.ternary.StringIndexOfFromIndex;
import it.unive.lisa.symbolic.value.operator.ternary.StringReplace;
import it.unive.lisa.symbolic.value.operator.ternary.StringReplaceAll;
import it.unive.lisa.symbolic.value.operator.ternary.StringSubstring;
import it.unive.lisa.symbolic.value.operator.unary.CharacterIsDigit;
import it.unive.lisa.symbolic.value.operator.unary.CharacterIsLetter;
import it.unive.lisa.symbolic.value.operator.unary.CharacterIsUpperCase;
import it.unive.lisa.symbolic.value.operator.unary.CharacterToLowerCase;
import it.unive.lisa.symbolic.value.operator.unary.CharacterToUpperCase;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.symbolic.value.operator.unary.NumericAbs;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.symbolic.value.operator.unary.NumericSqrt;
import it.unive.lisa.symbolic.value.operator.unary.NumericToString;
import it.unive.lisa.symbolic.value.operator.unary.StringLength;
import it.unive.lisa.symbolic.value.operator.unary.StringToUpperCase;
import it.unive.lisa.symbolic.value.operator.unary.StringTrim;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class ConstantValuePropagationTest {

	private final ProgramPoint pp = TestParameterProvider.provideParam(null, ProgramPoint.class);
	private final ConstantValuePropagation domain = new ConstantValuePropagation();
	private final SemanticOracle oracle = TestParameterProvider.provideParam(null, SemanticOracle.class);

	private final Variable variable = new Variable(Int32Type.INSTANCE, "x", pp.getLocation());
	private final Variable varAux = new Variable(Int32Type.INSTANCE, "aux", pp.getLocation());

	private ConstantValue mk(
			Object value) {
		return new ConstantValue(value);
	}

	private BinaryExpression mkBin(
			it.unive.lisa.symbolic.value.operator.binary.BinaryOperator op) {
		return new BinaryExpression(Int32Type.INSTANCE, variable, varAux, op, pp.getLocation());
	}

	private UnaryExpression mkUnary(
			it.unive.lisa.symbolic.value.operator.unary.UnaryOperator op) {
		return new UnaryExpression(Int32Type.INSTANCE, variable, op, pp.getLocation());
	}

	private ValueEnvironment<ConstantValue> envWith(
			ConstantValue value) {
		return domain.makeLattice().putState(variable, value);
	}

	// --- evalConstant ---

	@Test
	void evalConstantInteger() throws SemanticException {
		assertEquals(mk(42), domain.evalConstant(new Constant(Int32Type.INSTANCE, 42, pp.getLocation()), pp, oracle));
	}

	@Test
	void evalConstantString() throws SemanticException {
		assertEquals(mk("hello"),
				domain.evalConstant(new Constant(StringType.INSTANCE, "hello", pp.getLocation()), pp, oracle));
	}

	@Test
	void evalConstantBoolean() throws SemanticException {
		assertEquals(mk(Boolean.TRUE),
				domain.evalConstant(new Constant(BoolType.INSTANCE, Boolean.TRUE, pp.getLocation()), pp, oracle));
	}

	@Test
	void evalConstantDouble() throws SemanticException {
		assertEquals(mk(3.14),
				domain.evalConstant(new Constant(Int32Type.INSTANCE, 3.14, pp.getLocation()), pp, oracle));
	}

	// --- evalUnaryExpression: numeric ---

	@Test
	void evalNegationInteger() throws SemanticException {
		assertEquals(mk(-7), domain.evalUnaryExpression(mkUnary(NumericNegation.INSTANCE), mk(7), pp, oracle));
	}

	@Test
	void evalNegationLong() throws SemanticException {
		assertEquals(mk(-100L), domain.evalUnaryExpression(mkUnary(NumericNegation.INSTANCE), mk(100L), pp, oracle));
	}

	@Test
	void evalNegationFloat() throws SemanticException {
		assertEquals(mk(-1.5f), domain.evalUnaryExpression(mkUnary(NumericNegation.INSTANCE), mk(1.5f), pp, oracle));
	}

	@Test
	void evalNegationDouble() throws SemanticException {
		assertEquals(mk(-2.5), domain.evalUnaryExpression(mkUnary(NumericNegation.INSTANCE), mk(2.5), pp, oracle));
	}

	@Test
	void evalAbsOnInteger() throws SemanticException {
		assertEquals(mk(5), domain.evalUnaryExpression(mkUnary(NumericAbs.INSTANCE), mk(-5), pp, oracle));
		assertEquals(mk(0), domain.evalUnaryExpression(mkUnary(NumericAbs.INSTANCE), mk(0), pp, oracle));
	}

	@Test
	void evalAbsOnShort() throws SemanticException {
		// NumericAbs must handle Short without ClassCastException
		ConstantValue result = domain.evalUnaryExpression(
				mkUnary(NumericAbs.INSTANCE), mk(Short.valueOf((short) -5)), pp, oracle);
		assertEquals(mk(5), result);
	}

	@Test
	void evalAbsOnByte() throws SemanticException {
		// NumericAbs must handle Byte without ClassCastException
		ConstantValue result = domain.evalUnaryExpression(
				mkUnary(NumericAbs.INSTANCE), mk(Byte.valueOf((byte) -3)), pp, oracle);
		assertEquals(mk(3), result);
	}

	@Test
	void evalAbsOnLong() throws SemanticException {
		assertEquals(mk(7L), domain.evalUnaryExpression(mkUnary(NumericAbs.INSTANCE), mk(-7L), pp, oracle));
	}

	@Test
	void evalAbsOnDouble() throws SemanticException {
		assertEquals(mk(1.5), domain.evalUnaryExpression(mkUnary(NumericAbs.INSTANCE), mk(-1.5), pp, oracle));
	}

	@Test
	void evalSqrtOnDouble() throws SemanticException {
		assertEquals(mk(Math.sqrt(4.0)),
				domain.evalUnaryExpression(mkUnary(NumericSqrt.INSTANCE), mk(4.0), pp, oracle));
	}

	@Test
	void evalNumericToStringOnInteger() throws SemanticException {
		assertEquals(mk("42"), domain.evalUnaryExpression(mkUnary(NumericToString.INSTANCE), mk(42), pp, oracle));
	}

	@Test
	void evalUnaryTopArgReturnsTop() throws SemanticException {
		assertEquals(ConstantValue.TOP,
				domain.evalUnaryExpression(mkUnary(NumericNegation.INSTANCE), ConstantValue.TOP, pp, oracle));
	}

	@Test
	void evalUnaryUnsupportedTypeReturnsTop() throws SemanticException {
		// String with NumericNegation → TOP (no rule matches)
		assertEquals(ConstantValue.TOP,
				domain.evalUnaryExpression(mkUnary(NumericNegation.INSTANCE), mk("hello"), pp, oracle));
	}

	// --- evalUnaryExpression: string ---

	@Test
	void evalStringLength() throws SemanticException {
		assertEquals(mk(5),
				domain.evalUnaryExpression(mkUnary(StringLength.INSTANCE), mk("hello"), pp, oracle));
	}

	@Test
	void evalStringToUpperCase() throws SemanticException {
		assertEquals(mk("HELLO"),
				domain.evalUnaryExpression(mkUnary(StringToUpperCase.INSTANCE), mk("hello"), pp, oracle));
	}

	@Test
	void evalStringTrim() throws SemanticException {
		assertEquals(mk("hi"),
				domain.evalUnaryExpression(mkUnary(StringTrim.INSTANCE), mk("  hi  "), pp, oracle));
	}

	// --- evalUnaryExpression: character ---

	@Test
	void evalCharacterIsLetterTrue() throws SemanticException {
		assertEquals(mk(Boolean.TRUE),
				domain.evalUnaryExpression(mkUnary(CharacterIsLetter.INSTANCE), mk((int) 'A'), pp, oracle));
	}

	@Test
	void evalCharacterIsLetterFalse() throws SemanticException {
		assertEquals(mk(Boolean.FALSE),
				domain.evalUnaryExpression(mkUnary(CharacterIsLetter.INSTANCE), mk((int) '5'), pp, oracle));
	}

	@Test
	void evalCharacterIsDigitTrue() throws SemanticException {
		assertEquals(mk(Boolean.TRUE),
				domain.evalUnaryExpression(mkUnary(CharacterIsDigit.INSTANCE), mk((int) '7'), pp, oracle));
	}

	@Test
	void evalCharacterToLowerCase() throws SemanticException {
		ConstantValue result = domain.evalUnaryExpression(
				mkUnary(CharacterToUpperCase.INSTANCE), mk((int) 'a'), pp, oracle);
		assertEquals(mk((char) 'A'), result);
	}

	@Test
	void evalCharacterToUpperCase() throws SemanticException {
		ConstantValue result = domain.evalUnaryExpression(
				mkUnary(CharacterToLowerCase.INSTANCE), mk((int) 'A'), pp, oracle);
		assertEquals(mk((char) 'a'), result);
	}

	// --- evalUnaryExpression: boolean ---

	@Test
	void evalLogicalNegationTrue() throws SemanticException {
		assertEquals(mk(Boolean.FALSE),
				domain.evalUnaryExpression(mkUnary(LogicalNegation.INSTANCE), mk(Boolean.TRUE), pp, oracle));
	}

	@Test
	void evalLogicalNegationFalse() throws SemanticException {
		assertEquals(mk(Boolean.TRUE),
				domain.evalUnaryExpression(mkUnary(LogicalNegation.INSTANCE), mk(Boolean.FALSE), pp, oracle));
	}

	// --- evalBinaryExpression: arithmetic ---

	@Test
	void evalAddIntegers() throws SemanticException {
		assertEquals(mk(8),
				domain.evalBinaryExpression(mkBin(NumericNonOverflowingAdd.INSTANCE), mk(3), mk(5), pp, oracle));
	}

	@Test
	void evalAddLongs() throws SemanticException {
		assertEquals(mk(10L),
				domain.evalBinaryExpression(mkBin(NumericNonOverflowingAdd.INSTANCE), mk(4L), mk(6L), pp, oracle));
	}

	@Test
	void evalAddDoubles() throws SemanticException {
		assertEquals(mk(5.5),
				domain.evalBinaryExpression(mkBin(NumericNonOverflowingAdd.INSTANCE), mk(2.0), mk(3.5), pp, oracle));
	}

	@Test
	void evalSubtractIntegers() throws SemanticException {
		assertEquals(mk(3),
				domain.evalBinaryExpression(mkBin(NumericNonOverflowingSub.INSTANCE), mk(8), mk(5), pp, oracle));
	}

	@Test
	void evalMultiplyIntegers() throws SemanticException {
		assertEquals(mk(12),
				domain.evalBinaryExpression(mkBin(NumericNonOverflowingMul.INSTANCE), mk(3), mk(4), pp, oracle));
	}

	@Test
	void evalDivideIntegers() throws SemanticException {
		assertEquals(mk(3),
				domain.evalBinaryExpression(mkBin(NumericNonOverflowingDiv.INSTANCE), mk(9), mk(3), pp, oracle));
	}

	@Test
	void evalRemainderIntegers() throws SemanticException {
		assertEquals(mk(1),
				domain.evalBinaryExpression(mkBin(NumericNonOverflowingRem.INSTANCE), mk(10), mk(3), pp, oracle));
	}

	@Test
	void evalBinaryTopOperandReturnsTop() throws SemanticException {
		assertEquals(ConstantValue.TOP,
				domain.evalBinaryExpression(mkBin(NumericNonOverflowingAdd.INSTANCE), ConstantValue.TOP, mk(5), pp,
						oracle));
		assertEquals(ConstantValue.TOP,
				domain.evalBinaryExpression(mkBin(NumericNonOverflowingAdd.INSTANCE), mk(5), ConstantValue.TOP, pp,
						oracle));
	}

	// --- evalBinaryExpression: bitwise ---

	@Test
	void evalBitwiseOrIntegers() throws SemanticException {
		assertEquals(mk(0b1010 | 0b0110),
				domain.evalBinaryExpression(mkBin(BitwiseOr.INSTANCE), mk(0b1010), mk(0b0110), pp, oracle));
	}

	@Test
	void evalBitwiseAndIntegers() throws SemanticException {
		assertEquals(mk(0b1010 & 0b1100),
				domain.evalBinaryExpression(mkBin(BitwiseAnd.INSTANCE), mk(0b1010), mk(0b1100), pp, oracle));
	}

	@Test
	void evalBitwiseXorIntegers() throws SemanticException {
		assertEquals(mk(0b1010 ^ 0b1100),
				domain.evalBinaryExpression(mkBin(BitwiseXor.INSTANCE), mk(0b1010), mk(0b1100), pp, oracle));
	}

	@Test
	void evalBitwiseShiftLeft() throws SemanticException {
		assertEquals(mk(1 << 3),
				domain.evalBinaryExpression(mkBin(BitwiseShiftLeft.INSTANCE), mk(1), mk(3), pp, oracle));
	}

	@Test
	void evalBitwiseShiftRight() throws SemanticException {
		assertEquals(mk(16 >> 2),
				domain.evalBinaryExpression(mkBin(BitwiseShiftRight.INSTANCE), mk(16), mk(2), pp, oracle));
	}

	// --- evalBinaryExpression: comparison operators ---

	@Test
	void evalComparisonLtTrue() throws SemanticException {
		assertEquals(mk(Boolean.TRUE),
				domain.evalBinaryExpression(mkBin(ComparisonLt.INSTANCE), mk(3), mk(5), pp, oracle));
	}

	@Test
	void evalComparisonLtFalse() throws SemanticException {
		assertEquals(mk(Boolean.FALSE),
				domain.evalBinaryExpression(mkBin(ComparisonLt.INSTANCE), mk(7), mk(5), pp, oracle));
	}

	@Test
	void evalNumericMax() throws SemanticException {
		assertEquals(mk(Math.max(3.0, 7.0)),
				domain.evalBinaryExpression(mkBin(NumericMax.INSTANCE), mk(3), mk(7), pp, oracle));
	}

	@Test
	void evalNumericMin() throws SemanticException {
		assertEquals(mk(Math.min(3.0, 7.0)),
				domain.evalBinaryExpression(mkBin(NumericMin.INSTANCE), mk(3), mk(7), pp, oracle));
	}

	@Test
	void evalValueComparison() throws SemanticException {
		// Integer.compare(3, 5) = negative; compare(5, 5) = 0
		assertEquals(mk(Integer.compare(5, 5)),
				domain.evalBinaryExpression(mkBin(ValueComparison.INSTANCE), mk(5), mk(5), pp, oracle));
		assertTrue(((Integer) domain.evalBinaryExpression(
				mkBin(ValueComparison.INSTANCE), mk(3), mk(5), pp, oracle).getValue()) < 0);
	}

	// --- evalBinaryExpression: string ---

	@Test
	void evalStringConcat() throws SemanticException {
		assertEquals(mk("helloworld"),
				domain.evalBinaryExpression(mkBin(StringConcat.INSTANCE), mk("hello"), mk("world"), pp, oracle));
	}

	@Test
	void evalStringContainsTrue() throws SemanticException {
		assertEquals(mk(Boolean.TRUE),
				domain.evalBinaryExpression(mkBin(StringContains.INSTANCE), mk("hello world"), mk("world"), pp,
						oracle));
	}

	@Test
	void evalStringContainsFalse() throws SemanticException {
		assertEquals(mk(Boolean.FALSE),
				domain.evalBinaryExpression(mkBin(StringContains.INSTANCE), mk("hello"), mk("xyz"), pp, oracle));
	}

	@Test
	void evalStringEqualsTrue() throws SemanticException {
		assertEquals(mk(Boolean.TRUE),
				domain.evalBinaryExpression(mkBin(StringEquals.INSTANCE), mk("hi"), mk("hi"), pp, oracle));
	}

	@Test
	void evalStringEqualsFalse() throws SemanticException {
		assertEquals(mk(Boolean.FALSE),
				domain.evalBinaryExpression(mkBin(StringEquals.INSTANCE), mk("hi"), mk("bye"), pp, oracle));
	}

	@Test
	void evalStringStartsWith() throws SemanticException {
		assertEquals(mk(Boolean.TRUE),
				domain.evalBinaryExpression(mkBin(StringStartsWith.INSTANCE), mk("hello"), mk("hel"), pp, oracle));
	}

	@Test
	void evalStringEndsWith() throws SemanticException {
		assertEquals(mk(Boolean.TRUE),
				domain.evalBinaryExpression(mkBin(StringEndsWith.INSTANCE), mk("hello"), mk("llo"), pp, oracle));
	}

	@Test
	void evalStringIndexOf() throws SemanticException {
		assertEquals(mk(6),
				domain.evalBinaryExpression(mkBin(StringIndexOf.INSTANCE), mk("hello world"), mk("world"), pp, oracle));
	}

	// --- evalBinaryExpression: character ---

	@Test
	void evalCharacterEqualsTrue() throws SemanticException {
		assertEquals(mk(Boolean.TRUE),
				domain.evalBinaryExpression(mkBin(CharacterEquals.INSTANCE), mk(65), mk(65), pp, oracle));
	}

	@Test
	void evalCharacterEqualsFalse() throws SemanticException {
		assertEquals(mk(Boolean.FALSE),
				domain.evalBinaryExpression(mkBin(CharacterEquals.INSTANCE), mk(65), mk(66), pp, oracle));
	}

	// --- evalBinaryExpression: ComparisonNe ---

	@Test
	void evalComparisonNeIntegers() throws SemanticException {
		assertEquals(mk(Boolean.TRUE),
				domain.evalBinaryExpression(mkBin(ComparisonNe.INSTANCE), mk(3), mk(5), pp, oracle));
		assertEquals(mk(Boolean.FALSE),
				domain.evalBinaryExpression(mkBin(ComparisonNe.INSTANCE), mk(5), mk(5), pp, oracle));
	}

	@Test
	void evalComparisonNeBooleans() throws SemanticException {
		assertEquals(mk(Boolean.TRUE),
				domain.evalBinaryExpression(mkBin(ComparisonNe.INSTANCE), mk(Boolean.TRUE), mk(Boolean.FALSE), pp,
						oracle));
		assertEquals(mk(Boolean.FALSE),
				domain.evalBinaryExpression(mkBin(ComparisonNe.INSTANCE), mk(Boolean.TRUE), mk(Boolean.TRUE), pp,
						oracle));
	}

	@Test
	void evalComparisonNeCharacters() throws SemanticException {
		assertEquals(mk(Boolean.TRUE),
				domain.evalBinaryExpression(mkBin(ComparisonNe.INSTANCE), mk('A'), mk('B'), pp, oracle));
	}

	// --- evalTernaryExpression ---

	@Test
	void evalStringSubstring() throws SemanticException {
		TernaryExpression expr = new TernaryExpression(
				StringType.INSTANCE, variable, varAux, variable, StringSubstring.INSTANCE, pp.getLocation());
		assertEquals(mk("el"),
				domain.evalTernaryExpression(expr, mk("hello"), mk(1), mk(3), pp, oracle));
	}

	@Test
	void evalStringReplaceAll() throws SemanticException {
		TernaryExpression expr = new TernaryExpression(
				StringType.INSTANCE, variable, varAux, variable, StringReplaceAll.INSTANCE, pp.getLocation());
		assertEquals(mk("aXbXc"),
				domain.evalTernaryExpression(expr, mk("a1b2c"), mk("[0-9]"), mk("X"), pp, oracle));
	}

	@Test
	void evalStringReplace() throws SemanticException {
		TernaryExpression expr = new TernaryExpression(
				StringType.INSTANCE, variable, varAux, variable, StringReplace.INSTANCE, pp.getLocation());
		// "hello".replace('l', 'r') = "herro"
		assertEquals(mk("herro"),
				domain.evalTernaryExpression(expr, mk("hello"), mk((int) 'l'), mk((int) 'r'), pp, oracle));
	}

	@Test
	void evalStringIndexOfFromIndex() throws SemanticException {
		TernaryExpression expr = new TernaryExpression(
				Int32Type.INSTANCE, variable, varAux, variable, StringIndexOfFromIndex.INSTANCE, pp.getLocation());
		// "abcabc".indexOf("bc", 2) = 4
		assertEquals(mk(4),
				domain.evalTernaryExpression(expr, mk("abcabc"), mk("bc"), mk(2), pp, oracle));
	}

	@Test
	void evalTernaryTopOperandReturnsTop() throws SemanticException {
		TernaryExpression expr = new TernaryExpression(
				StringType.INSTANCE, variable, varAux, variable, StringSubstring.INSTANCE, pp.getLocation());
		assertEquals(ConstantValue.TOP,
				domain.evalTernaryExpression(expr, ConstantValue.TOP, mk(1), mk(3), pp, oracle));
	}

	// --- satisfiesAbstractValue ---

	@Test
	void satisfiesAbstractValueBooleanTrue() throws SemanticException {
		assertEquals(Satisfiability.SATISFIED,
				domain.satisfiesAbstractValue(mk(Boolean.TRUE), pp, oracle));
	}

	@Test
	void satisfiesAbstractValueBooleanFalse() throws SemanticException {
		assertEquals(Satisfiability.NOT_SATISFIED,
				domain.satisfiesAbstractValue(mk(Boolean.FALSE), pp, oracle));
	}

	@Test
	void satisfiesAbstractValueInteger() throws SemanticException {
		assertEquals(Satisfiability.UNKNOWN,
				domain.satisfiesAbstractValue(mk(42), pp, oracle));
	}

	// --- satisfiesUnaryExpression ---

	@Test
	void satisfiesUnaryCharIsLetterTrue() throws SemanticException {
		assertEquals(Satisfiability.SATISFIED,
				domain.satisfiesUnaryExpression(mkUnary(CharacterIsLetter.INSTANCE), mk((int) 'A'), pp, oracle));
	}

	@Test
	void satisfiesUnaryCharIsLetterFalse() throws SemanticException {
		assertEquals(Satisfiability.NOT_SATISFIED,
				domain.satisfiesUnaryExpression(mkUnary(CharacterIsLetter.INSTANCE), mk((int) '3'), pp, oracle));
	}

	@Test
	void satisfiesUnaryCharIsDigitTrue() throws SemanticException {
		assertEquals(Satisfiability.SATISFIED,
				domain.satisfiesUnaryExpression(mkUnary(CharacterIsDigit.INSTANCE), mk((int) '9'), pp, oracle));
	}

	@Test
	void satisfiesUnaryCharIsUpperCaseTrue() throws SemanticException {
		assertEquals(Satisfiability.SATISFIED,
				domain.satisfiesUnaryExpression(mkUnary(CharacterIsUpperCase.INSTANCE), mk((int) 'Z'), pp, oracle));
	}

	@Test
	void satisfiesUnaryCharIsUpperCaseFalse() throws SemanticException {
		assertEquals(Satisfiability.NOT_SATISFIED,
				domain.satisfiesUnaryExpression(mkUnary(CharacterIsUpperCase.INSTANCE), mk((int) 'a'), pp, oracle));
	}

	@Test
	void satisfiesUnaryTopArgReturnsUnknown() throws SemanticException {
		assertEquals(Satisfiability.UNKNOWN,
				domain.satisfiesUnaryExpression(mkUnary(CharacterIsLetter.INSTANCE), ConstantValue.TOP, pp, oracle));
	}

	// --- satisfiesBinaryExpression ---

	@Test
	void satisfiesBinaryTopOperandReturnsUnknown() throws SemanticException {
		assertEquals(Satisfiability.UNKNOWN,
				domain.satisfiesBinaryExpression(mkBin(ComparisonEq.INSTANCE), ConstantValue.TOP, mk(5), pp, oracle));
	}

	@Test
	void satisfiesBinaryEqIntegersSatisfied() throws SemanticException {
		assertEquals(Satisfiability.SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(ComparisonEq.INSTANCE), mk(5), mk(5), pp, oracle));
	}

	@Test
	void satisfiesBinaryEqIntegersNotSatisfied() throws SemanticException {
		assertEquals(Satisfiability.NOT_SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(ComparisonEq.INSTANCE), mk(3), mk(5), pp, oracle));
	}

	@Test
	void satisfiesBinaryEqBooleans() throws SemanticException {
		assertEquals(Satisfiability.SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(ComparisonEq.INSTANCE), mk(Boolean.TRUE), mk(Boolean.TRUE), pp,
						oracle));
		assertEquals(Satisfiability.NOT_SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(ComparisonEq.INSTANCE), mk(Boolean.TRUE), mk(Boolean.FALSE), pp,
						oracle));
	}

	@Test
	void satisfiesBinaryLtSatisfied() throws SemanticException {
		assertEquals(Satisfiability.SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(ComparisonLt.INSTANCE), mk(3), mk(5), pp, oracle));
	}

	@Test
	void satisfiesBinaryLtNotSatisfied() throws SemanticException {
		assertEquals(Satisfiability.NOT_SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(ComparisonLt.INSTANCE), mk(5), mk(3), pp, oracle));
	}

	@Test
	void satisfiesBinaryLeOnEqual() throws SemanticException {
		assertEquals(Satisfiability.SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(ComparisonLe.INSTANCE), mk(5), mk(5), pp, oracle));
	}

	@Test
	void satisfiesBinaryGtSatisfied() throws SemanticException {
		assertEquals(Satisfiability.SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(ComparisonGt.INSTANCE), mk(7), mk(3), pp, oracle));
	}

	@Test
	void satisfiesBinaryGeSatisfied() throws SemanticException {
		assertEquals(Satisfiability.SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(ComparisonGe.INSTANCE), mk(5), mk(5), pp, oracle));
	}

	@Test
	void satisfiesBinaryStringEquals() throws SemanticException {
		assertEquals(Satisfiability.SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(StringEquals.INSTANCE), mk("hi"), mk("hi"), pp, oracle));
		assertEquals(Satisfiability.NOT_SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(StringEquals.INSTANCE), mk("hi"), mk("bye"), pp, oracle));
	}

	@Test
	void satisfiesBinaryStringContains() throws SemanticException {
		assertEquals(Satisfiability.SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(StringContains.INSTANCE), mk("hello"), mk("ell"), pp, oracle));
	}

	@Test
	void satisfiesBinaryStringStartsWith() throws SemanticException {
		assertEquals(Satisfiability.SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(StringStartsWith.INSTANCE), mk("hello"), mk("he"), pp, oracle));
		assertEquals(Satisfiability.NOT_SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(StringStartsWith.INSTANCE), mk("hello"), mk("lo"), pp, oracle));
	}

	@Test
	void satisfiesBinaryStringEndsWith() throws SemanticException {
		assertEquals(Satisfiability.SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(StringEndsWith.INSTANCE), mk("hello"), mk("lo"), pp, oracle));
	}

	@Test
	void satisfiesBinaryCharacterEquals() throws SemanticException {
		assertEquals(Satisfiability.SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(CharacterEquals.INSTANCE), mk(65), mk(65), pp, oracle));
		assertEquals(Satisfiability.NOT_SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(CharacterEquals.INSTANCE), mk(65), mk(66), pp, oracle));
	}

	// --- satisfiesConstant ---

	@Test
	void satisfiesConstantBooleanTrue() throws SemanticException {
		assertEquals(Satisfiability.SATISFIED,
				domain.satisfiesConstant(new Constant(BoolType.INSTANCE, Boolean.TRUE, pp.getLocation()), pp, oracle));
	}

	@Test
	void satisfiesConstantBooleanFalse() throws SemanticException {
		assertEquals(Satisfiability.NOT_SATISFIED,
				domain.satisfiesConstant(new Constant(BoolType.INSTANCE, Boolean.FALSE, pp.getLocation()), pp, oracle));
	}

	@Test
	void satisfiesConstantInteger() throws SemanticException {
		assertEquals(Satisfiability.UNKNOWN,
				domain.satisfiesConstant(new Constant(Int32Type.INSTANCE, 42, pp.getLocation()), pp, oracle));
	}

	// --- assume ---

	@Test
	void assumeOnSatisfiedExpressionReturnsEnvironmentUnchanged() throws SemanticException {
		// x = 5 and expression is x == 5 → SATISFIED → environment unchanged
		ValueEnvironment<ConstantValue> env = envWith(mk(5));
		BinaryExpression expr = new BinaryExpression(
				BoolType.INSTANCE, variable,
				new Constant(Int32Type.INSTANCE, 5, pp.getLocation()),
				ComparisonEq.INSTANCE, pp.getLocation());
		assertEquals(env, domain.assume(env, expr, pp, pp, oracle));
	}

	@Test
	void assumeOnNotSatisfiedExpressionReturnsBottom() throws SemanticException {
		// x = 3 and expression is x == 5 → NOT_SATISFIED → bottom
		ValueEnvironment<ConstantValue> env = envWith(mk(3));
		BinaryExpression expr = new BinaryExpression(
				BoolType.INSTANCE, variable,
				new Constant(Int32Type.INSTANCE, 5, pp.getLocation()),
				ComparisonEq.INSTANCE, pp.getLocation());
		assertTrue(domain.assume(env, expr, pp, pp, oracle).isBottom());
	}

	// --- assumeBinaryExpression ---

	@Test
	void assumeEqRefinesTopToConcreteValue() throws SemanticException {
		// x is TOP, assume x == 7 → x becomes 7
		ValueEnvironment<ConstantValue> env = domain.makeLattice();
		BinaryExpression expr = new BinaryExpression(
				BoolType.INSTANCE, variable,
				new Constant(Int32Type.INSTANCE, 7, pp.getLocation()),
				ComparisonEq.INSTANCE, pp.getLocation());
		ValueEnvironment<ConstantValue> result = domain.assumeBinaryExpression(env, expr, pp, pp, oracle);
		assertEquals(mk(7), result.getState(variable));
	}

	@Test
	void assumeNeFlipsBooleanValue() throws SemanticException {
		// x is TOP, assume x != true → x becomes false
		ValueEnvironment<ConstantValue> env = domain.makeLattice();
		BinaryExpression expr = new BinaryExpression(
				BoolType.INSTANCE, variable,
				new Constant(BoolType.INSTANCE, Boolean.TRUE, pp.getLocation()),
				ComparisonNe.INSTANCE, pp.getLocation());
		ValueEnvironment<ConstantValue> result = domain.assumeBinaryExpression(env, expr, pp, pp, oracle);
		assertEquals(mk(Boolean.FALSE), result.getState(variable));
	}

	@Test
	void assumeNeOnNonBooleanLeavesStateUnchanged() throws SemanticException {
		// x != 5 when x is TOP → no useful refinement, x stays TOP
		ValueEnvironment<ConstantValue> env = domain.makeLattice();
		BinaryExpression expr = new BinaryExpression(
				BoolType.INSTANCE, variable,
				new Constant(Int32Type.INSTANCE, 5, pp.getLocation()),
				ComparisonNe.INSTANCE, pp.getLocation());
		ValueEnvironment<ConstantValue> result = domain.assumeBinaryExpression(env, expr, pp, pp, oracle);
		assertTrue(result.getState(variable).isTop());
	}

	@Test
	void assumeGtDoesNotRefineState() throws SemanticException {
		// x > 5 → no useful refinement for constant propagation
		ValueEnvironment<ConstantValue> env = domain.makeLattice();
		BinaryExpression expr = new BinaryExpression(
				BoolType.INSTANCE, variable,
				new Constant(Int32Type.INSTANCE, 5, pp.getLocation()),
				ComparisonGt.INSTANCE, pp.getLocation());
		ValueEnvironment<ConstantValue> result = domain.assumeBinaryExpression(env, expr, pp, pp, oracle);
		assertTrue(result.getState(variable).isTop());
	}

	// --- constraints ---

	@Test
	void constraintsOnTopStateReturnsEmpty() throws SemanticException {
		assertTrue(domain.constraints(null, domain.makeLattice(), variable, pp, oracle).isEmpty());
	}

	@Test
	void constraintsOnBottomStateReturnsNull() throws SemanticException {
		assertNull(domain.constraints(null, domain.makeLattice().bottom(), variable, pp, oracle));
	}

	@Test
	void constraintsOnConcreteValueReturnsEqConstraint() throws SemanticException {
		// x = 42 → constraint should be "42 == x" where left Constant holds
		// Integer 42
		ValueEnvironment<ConstantValue> env = envWith(mk(42));
		Set<BinaryExpression> constraints = domain.constraints(null, env, variable, pp, oracle);
		assertNotNull(constraints);
		assertEquals(1, constraints.size());
		BinaryExpression c = constraints.iterator().next();
		assertEquals(ComparisonEq.INSTANCE, c.getOperator());
		// The left side must be a Constant holding the raw Java value 42,
		// not a ConstantValue wrapping 42
		Constant leftConst = (Constant) c.getLeft();
		assertEquals(42, leftConst.getValue());
	}

	@Test
	void constraintsOnTopVariableReturnsEmpty() throws SemanticException {
		// x is TOP (unknown) → no constraint can be generated
		ValueEnvironment<ConstantValue> env = domain.makeLattice();
		assertTrue(domain.constraints(null, env, variable, pp, oracle).isEmpty());
	}
}
