package it.unive.lisa.analysis;

import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.lattices.ConstantValue;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushInv;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.DivisionOperator;
import it.unive.lisa.symbolic.value.operator.ModuloOperator;
import it.unive.lisa.symbolic.value.operator.MultiplicationOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.BitwiseAnd;
import it.unive.lisa.symbolic.value.operator.binary.BitwiseOr;
import it.unive.lisa.symbolic.value.operator.binary.BitwiseShiftLeft;
import it.unive.lisa.symbolic.value.operator.binary.BitwiseShiftRight;
import it.unive.lisa.symbolic.value.operator.binary.BitwiseUnsignedShiftRight;
import it.unive.lisa.symbolic.value.operator.binary.BitwiseXor;
import it.unive.lisa.symbolic.value.operator.binary.CharacterEquals;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
import it.unive.lisa.symbolic.value.operator.binary.NumericAtan2;
import it.unive.lisa.symbolic.value.operator.binary.NumericMax;
import it.unive.lisa.symbolic.value.operator.binary.NumericMin;
import it.unive.lisa.symbolic.value.operator.binary.NumericPow;
import it.unive.lisa.symbolic.value.operator.binary.StringCharAt;
import it.unive.lisa.symbolic.value.operator.binary.StringConcat;
import it.unive.lisa.symbolic.value.operator.binary.StringContains;
import it.unive.lisa.symbolic.value.operator.binary.StringEndsWith;
import it.unive.lisa.symbolic.value.operator.binary.StringEquals;
import it.unive.lisa.symbolic.value.operator.binary.StringEqualsIgnoreCase;
import it.unive.lisa.symbolic.value.operator.binary.StringIndexOf;
import it.unive.lisa.symbolic.value.operator.binary.StringIndexOfChar;
import it.unive.lisa.symbolic.value.operator.binary.StringLastIndexOf;
import it.unive.lisa.symbolic.value.operator.binary.StringLastIndexOfChar;
import it.unive.lisa.symbolic.value.operator.binary.StringMatches;
import it.unive.lisa.symbolic.value.operator.binary.StringStartsWith;
import it.unive.lisa.symbolic.value.operator.binary.StringSubstringToEnd;
import it.unive.lisa.symbolic.value.operator.binary.ValueComparison;
import it.unive.lisa.symbolic.value.operator.ternary.StringIndexOfCharFromIndex;
import it.unive.lisa.symbolic.value.operator.ternary.StringIndexOfFromIndex;
import it.unive.lisa.symbolic.value.operator.ternary.StringLastIndexOfCharFromIndex;
import it.unive.lisa.symbolic.value.operator.ternary.StringLastIndexOfFromIndex;
import it.unive.lisa.symbolic.value.operator.ternary.StringReplace;
import it.unive.lisa.symbolic.value.operator.ternary.StringReplaceAll;
import it.unive.lisa.symbolic.value.operator.ternary.StringReplaceFirst;
import it.unive.lisa.symbolic.value.operator.ternary.StringStartsWithFromIndex;
import it.unive.lisa.symbolic.value.operator.ternary.StringSubstring;
import it.unive.lisa.symbolic.value.operator.ternary.TernaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.CharacterIsDefined;
import it.unive.lisa.symbolic.value.operator.unary.CharacterIsDigit;
import it.unive.lisa.symbolic.value.operator.unary.CharacterIsIdentifierPart;
import it.unive.lisa.symbolic.value.operator.unary.CharacterIsIdentifierStart;
import it.unive.lisa.symbolic.value.operator.unary.CharacterIsLetter;
import it.unive.lisa.symbolic.value.operator.unary.CharacterIsLetterOrDigit;
import it.unive.lisa.symbolic.value.operator.unary.CharacterIsLowerCase;
import it.unive.lisa.symbolic.value.operator.unary.CharacterIsUpperCase;
import it.unive.lisa.symbolic.value.operator.unary.CharacterToLowerCase;
import it.unive.lisa.symbolic.value.operator.unary.CharacterToUpperCase;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
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
import it.unive.lisa.symbolic.value.operator.unary.NumericToString;
import it.unive.lisa.symbolic.value.operator.unary.StringLength;
import it.unive.lisa.symbolic.value.operator.unary.StringReverse;
import it.unive.lisa.symbolic.value.operator.unary.StringToLowerCase;
import it.unive.lisa.symbolic.value.operator.unary.StringToUpperCase;
import it.unive.lisa.symbolic.value.operator.unary.StringTrim;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.Type;
import java.util.Set;

/**
 * A non-relational value domain tracking {@link ConstantValue}s of variables
 * for numeric, character, string and boolean values.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class ConstantValuePropagation
		implements
		BaseNonRelationalValueDomain<ConstantValue> {

	@Override
	public boolean canProcess(
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (expression instanceof PushInv)
			// the type approximation of a pushinv is bottom, so the below check
			// will always fail regardless of the kind of value we are tracking
			return expression.getStaticType().isValueType();

		Set<Type> rts = null;
		try {
			rts = oracle.getRuntimeTypesOf(expression, pp);
		} catch (SemanticException e) {
			return false;
		}

		if (rts == null || rts.isEmpty())
			// if we have no runtime types, either the type domain has no type
			// information for the given expression (thus it can be anything,
			// also something that we can track) or the computation returned
			// bottom (and the whole state is likely going to go to bottom
			// anyway).
			return true;

		return rts.stream().anyMatch(Type::isValueType) || rts.stream().anyMatch(t -> t.isStringType());
	}

	@Override
	public ConstantValue evalConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return new ConstantValue(constant.getValue());
	}

	@Override
	public ConstantValue evalUnaryExpression(
			UnaryExpression expression,
			ConstantValue arg,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		// if arg is top, top is returned
		if (arg.isTop())
			return top();

		UnaryOperator operator = expression.getOperator();
		Object value = arg.getValue();

		// numeric
		if (operator instanceof NumericNegation)
			if (value instanceof Byte)
				return new ConstantValue(-((byte) value));
			else if (value instanceof Short)
				return new ConstantValue(-((short) value));
			else if (value instanceof Integer)
				return new ConstantValue(-((int) value));
			else if (value instanceof Long)
				return new ConstantValue(-((long) value));
			else if (value instanceof Float)
				return new ConstantValue(-((float) value));
			else if (value instanceof Double)
				return new ConstantValue(-((double) value));

		if (operator instanceof NumericSin)
			if (value instanceof Number)
				return new ConstantValue(Math.sin(((Number) value).doubleValue()));

		if (operator instanceof NumericCos)
			if (value instanceof Number)
				return new ConstantValue(Math.cos(((Number) value).doubleValue()));

		if (operator instanceof NumericSqrt)
			if (value instanceof Number)
				return new ConstantValue(Math.sqrt(((Number) value).doubleValue()));

		if (operator instanceof NumericTan)
			if (value instanceof Number)
				return new ConstantValue(Math.tan(((Number) value).doubleValue()));

		if (operator instanceof NumericAtan)
			if (value instanceof Number)
				return new ConstantValue(Math.atan(((Number) value).doubleValue()));

		if (operator instanceof NumericLog)
			if (value instanceof Number)
				return new ConstantValue(Math.log(((Number) value).doubleValue()));

		if (operator instanceof NumericLog10)
			if (value instanceof Number)
				return new ConstantValue(Math.log10(((Number) value).doubleValue()));

		if (operator instanceof NumericAsin)
			if (value instanceof Number)
				return new ConstantValue(Math.asin(((Number) value).doubleValue()));

		if (operator instanceof NumericExp)
			if (value instanceof Number)
				return new ConstantValue(Math.exp(((Number) value).doubleValue()));

		if (operator instanceof NumericAcos)
			if (value instanceof Number)
				return new ConstantValue(Math.acos(((Number) value).doubleValue()));

		if (operator instanceof NumericFloor)
			if (value instanceof Number)
				return new ConstantValue(Math.floor(((Number) value).doubleValue()));

		if (operator instanceof NumericCeil)
			if (value instanceof Number)
				return new ConstantValue(Math.ceil(((Number) value).doubleValue()));

		if (operator instanceof NumericRound)
			if (value instanceof Number)
				return new ConstantValue(Math.round(((Number) value).doubleValue()));

		if (operator instanceof NumericToRadians)
			if (value instanceof Number)
				return new ConstantValue(Math.toRadians(((Number) value).doubleValue()));

		if (operator instanceof NumericAbs)
			if (value instanceof Integer || value instanceof Short || value instanceof Byte)
				return new ConstantValue(Math.abs((int) value));
			else if (value instanceof Long)
				return new ConstantValue(Math.abs((long) value));
			else if (value instanceof Float)
				return new ConstantValue(Math.abs((float) value));
			else if (value instanceof Double)
				return new ConstantValue(Math.abs((double) value));

		if (operator instanceof NumericToString)
			if (value instanceof Number)
				return new ConstantValue(value.toString());

		// char
		if (operator instanceof CharacterIsLetter)
			if (value instanceof Integer)
				return new ConstantValue(Character.isLetter((int) value));

		if (operator instanceof CharacterIsDigit)
			if (value instanceof Integer)
				return new ConstantValue(Character.isDigit((int) value));

		if (operator instanceof CharacterIsDefined)
			if (value instanceof Integer)
				return new ConstantValue(Character.isDefined((int) value));

		if (operator instanceof CharacterToLowerCase)
			if (value instanceof Integer)
				return new ConstantValue((char) Character.toLowerCase((int) value));

		if (operator instanceof CharacterToUpperCase)
			if (value instanceof Integer)
				return new ConstantValue((char) Character.toUpperCase((int) value));

		if (operator instanceof CharacterIsIdentifierPart)
			if (value instanceof Integer)
				return new ConstantValue(Character.isJavaIdentifierPart((int) value));

		if (operator instanceof CharacterIsIdentifierStart)
			if (value instanceof Integer)
				return new ConstantValue(Character.isJavaIdentifierStart((int) value));

		if (operator instanceof CharacterIsLetterOrDigit)
			if (value instanceof Integer)
				return new ConstantValue(Character.isLetterOrDigit((int) value));

		if (operator instanceof CharacterIsLowerCase)
			if (value instanceof Integer)
				return new ConstantValue(Character.isLowerCase((int) value));

		if (operator instanceof CharacterIsUpperCase)
			if (value instanceof Integer)
				return new ConstantValue(Character.isUpperCase((int) value));

		// strings
		if (operator instanceof StringLength)
			if (value instanceof String)
				return new ConstantValue(((String) value).length());
		if (operator instanceof StringToLowerCase)
			if (value instanceof String)
				return new ConstantValue(((String) value).toLowerCase());
		if (operator instanceof StringToUpperCase)
			if (value instanceof String)
				return new ConstantValue(((String) value).toUpperCase());
		if (operator instanceof StringTrim)
			if (value instanceof String)
				return new ConstantValue(((String) value).trim());

		if (operator instanceof StringReverse)
			if (value instanceof String)
				return new ConstantValue(new StringBuilder((String) value).reverse().toString());

		// boolean
		if (operator instanceof LogicalNegation)
			if (value instanceof Boolean)
				return new ConstantValue(!((boolean) value));

		return top();
	}

	@Override
	public ConstantValue evalBinaryExpression(
			BinaryExpression expression,
			ConstantValue left,
			ConstantValue right,
			ProgramPoint pp,
			SemanticOracle oracle) {
		// if left or right is top, top is returned
		if (left.isTop() || right.isTop())
			return top();

		BinaryOperator operator = expression.getOperator();
		Object lVal = left.getValue();
		Object rVal = right.getValue();

		if (operator instanceof AdditionOperator) {
			if (lVal instanceof Character)
				lVal = (int) ((Character) lVal).charValue();
			if (rVal instanceof Character)
				rVal = (int) ((Character) rVal).charValue();

			if (lVal instanceof Double || rVal instanceof Double)
				return new ConstantValue(((Number) lVal).doubleValue() + ((Number) rVal).doubleValue());
			else if (lVal instanceof Float || rVal instanceof Float)
				return new ConstantValue(((Number) lVal).floatValue() + ((Number) rVal).floatValue());
			else if (lVal instanceof Long || rVal instanceof Long)
				return new ConstantValue(((Number) lVal).longValue() + ((Number) rVal).longValue());
			else if (lVal instanceof Integer || rVal instanceof Integer)
				return new ConstantValue(((Number) lVal).intValue() + ((Number) rVal).intValue());
			else if (lVal instanceof Short || rVal instanceof Short)
				return new ConstantValue(((Number) lVal).shortValue() + ((Number) rVal).shortValue());
			else if (lVal instanceof Byte || rVal instanceof Byte)
				return new ConstantValue(((Number) lVal).byteValue() + ((Number) rVal).byteValue());
		}

		if (operator instanceof SubtractionOperator) {
			if (lVal instanceof Character)
				lVal = (int) ((Character) lVal).charValue();
			if (rVal instanceof Character)
				rVal = (int) ((Character) rVal).charValue();

			if (lVal instanceof Double || rVal instanceof Double)
				return new ConstantValue(((Number) lVal).doubleValue() - ((Number) rVal).doubleValue());
			else if (lVal instanceof Float || rVal instanceof Float)
				return new ConstantValue(((Number) lVal).floatValue() - ((Number) rVal).floatValue());
			else if (lVal instanceof Long || rVal instanceof Long)
				return new ConstantValue(((Number) lVal).longValue() - ((Number) rVal).longValue());
			else if (lVal instanceof Integer || rVal instanceof Integer)
				return new ConstantValue(((Number) lVal).intValue() - ((Number) rVal).intValue());
			else if (lVal instanceof Short || rVal instanceof Short)
				return new ConstantValue(((Number) lVal).shortValue() - ((Number) rVal).shortValue());
			else if (lVal instanceof Byte || rVal instanceof Byte)
				return new ConstantValue(((Number) lVal).byteValue() - ((Number) rVal).byteValue());
		}

		if (operator instanceof MultiplicationOperator) {
			if (lVal instanceof Character)
				lVal = (int) ((Character) lVal).charValue();
			if (rVal instanceof Character)
				rVal = (int) ((Character) rVal).charValue();

			if (lVal instanceof Double || rVal instanceof Double)
				return new ConstantValue(((Number) lVal).doubleValue() * ((Number) rVal).doubleValue());
			else if (lVal instanceof Float || rVal instanceof Float)
				return new ConstantValue(((Number) lVal).floatValue() * ((Number) rVal).floatValue());
			else if (lVal instanceof Long || rVal instanceof Long)
				return new ConstantValue(((Number) lVal).longValue() * ((Number) rVal).longValue());
			else if (lVal instanceof Integer || rVal instanceof Integer)
				return new ConstantValue(((Number) lVal).intValue() * ((Number) rVal).intValue());
			else if (lVal instanceof Short || rVal instanceof Short)
				return new ConstantValue(((Number) lVal).shortValue() * ((Number) rVal).shortValue());
			else if (lVal instanceof Byte || rVal instanceof Byte)
				return new ConstantValue(((Number) lVal).byteValue() * ((Number) rVal).byteValue());
		}

		if (operator instanceof DivisionOperator) {
			if (lVal instanceof Character)
				lVal = (int) ((Character) lVal).charValue();
			if (rVal instanceof Character)
				rVal = (int) ((Character) rVal).charValue();

			if (lVal instanceof Double || rVal instanceof Double)
				return new ConstantValue(((Number) lVal).doubleValue() / ((Number) rVal).doubleValue());
			else if (lVal instanceof Float || rVal instanceof Float)
				return new ConstantValue(((Number) lVal).floatValue() / ((Number) rVal).floatValue());
			else if (lVal instanceof Long || rVal instanceof Long)
				return new ConstantValue(((Number) lVal).longValue() / ((Number) rVal).longValue());
			else if (lVal instanceof Integer || rVal instanceof Integer)
				return new ConstantValue(((Number) lVal).intValue() / ((Number) rVal).intValue());
			else if (lVal instanceof Short || rVal instanceof Short)
				return new ConstantValue(((Number) lVal).shortValue() / ((Number) rVal).shortValue());
			else if (lVal instanceof Byte || rVal instanceof Byte)
				return new ConstantValue(((Number) lVal).byteValue() / ((Number) rVal).byteValue());
		}

		if (operator instanceof ModuloOperator) {
			if (lVal instanceof Character)
				lVal = (int) ((Character) lVal).charValue();
			if (rVal instanceof Character)
				rVal = (int) ((Character) rVal).charValue();

			if (lVal instanceof Double || rVal instanceof Double)
				return new ConstantValue(((Number) lVal).doubleValue() % ((Number) rVal).doubleValue());
			else if (lVal instanceof Float || rVal instanceof Float)
				return new ConstantValue(((Number) lVal).floatValue() % ((Number) rVal).floatValue());
			else if (lVal instanceof Long || rVal instanceof Long)
				return new ConstantValue(((Number) lVal).longValue() % ((Number) rVal).longValue());
			else if (lVal instanceof Integer || rVal instanceof Integer)
				return new ConstantValue(((Number) lVal).intValue() % ((Number) rVal).intValue());
			else if (lVal instanceof Short || rVal instanceof Short)
				return new ConstantValue(((Number) lVal).shortValue() % ((Number) rVal).shortValue());
			else if (lVal instanceof Byte || rVal instanceof Byte)
				return new ConstantValue(((Number) lVal).byteValue() % ((Number) rVal).byteValue());
		}

		if (operator instanceof BitwiseOr) {
			if (lVal instanceof Character)
				lVal = (int) ((Character) lVal).charValue();
			if (rVal instanceof Character)
				rVal = (int) ((Character) rVal).charValue();

			if (lVal instanceof Long || rVal instanceof Long)
				return new ConstantValue(((Number) lVal).longValue() | ((Number) rVal).longValue());
			else if (lVal instanceof Integer || rVal instanceof Integer)
				return new ConstantValue(((Number) lVal).intValue() | ((Number) rVal).intValue());
			else if (lVal instanceof Short || rVal instanceof Short)
				return new ConstantValue(((Number) lVal).shortValue() | ((Number) rVal).shortValue());
			else if (lVal instanceof Byte || rVal instanceof Byte)
				return new ConstantValue(((Number) lVal).byteValue() | ((Number) rVal).byteValue());
		}

		if (operator instanceof BitwiseShiftRight) {
			if (lVal instanceof Character)
				lVal = (int) ((Character) lVal).charValue();
			if (rVal instanceof Character)
				rVal = (int) ((Character) rVal).charValue();

			if (lVal instanceof Long || rVal instanceof Long)
				return new ConstantValue(((Number) lVal).longValue() >> ((Number) rVal).longValue());
			else if (lVal instanceof Integer || rVal instanceof Integer)
				return new ConstantValue(((Number) lVal).intValue() >> ((Number) rVal).intValue());
			else if (lVal instanceof Short || rVal instanceof Short)
				return new ConstantValue(((Number) lVal).shortValue() >> ((Number) rVal).shortValue());
			else if (lVal instanceof Byte || rVal instanceof Byte)
				return new ConstantValue(((Number) lVal).byteValue() >> ((Number) rVal).byteValue());
		}

		if (operator instanceof BitwiseUnsignedShiftRight) {
			if (lVal instanceof Character)
				lVal = (int) ((Character) lVal).charValue();
			if (rVal instanceof Character)
				rVal = (int) ((Character) rVal).charValue();

			if (lVal instanceof Long || rVal instanceof Long)
				return new ConstantValue(((Number) lVal).longValue() >>> ((Number) rVal).longValue());
			else if (lVal instanceof Integer || rVal instanceof Integer)
				return new ConstantValue(((Number) lVal).intValue() >>> ((Number) rVal).intValue());
			else if (lVal instanceof Short || rVal instanceof Short)
				return new ConstantValue(((Number) lVal).shortValue() >>> ((Number) rVal).shortValue());
			else if (lVal instanceof Byte || rVal instanceof Byte)
				return new ConstantValue(((Number) lVal).byteValue() >>> ((Number) rVal).byteValue());
		}

		if (operator instanceof BitwiseShiftLeft) {
			if (lVal instanceof Character)
				lVal = (int) ((Character) lVal).charValue();
			if (rVal instanceof Character)
				rVal = (int) ((Character) rVal).charValue();

			if (lVal instanceof Long || rVal instanceof Long)
				return new ConstantValue(((Number) lVal).longValue() << ((Number) rVal).longValue());
			else if (lVal instanceof Integer || rVal instanceof Integer)
				return new ConstantValue(((Number) lVal).intValue() << ((Number) rVal).intValue());
			else if (lVal instanceof Short || rVal instanceof Short)
				return new ConstantValue(((Number) lVal).shortValue() << ((Number) rVal).shortValue());
			else if (lVal instanceof Byte || rVal instanceof Byte)
				return new ConstantValue(((Number) lVal).byteValue() << ((Number) rVal).byteValue());
		}

		if (operator instanceof BitwiseXor) {
			if (lVal instanceof Character)
				lVal = (int) ((Character) lVal).charValue();
			if (rVal instanceof Character)
				rVal = (int) ((Character) rVal).charValue();

			if (lVal instanceof Long || rVal instanceof Long)
				return new ConstantValue(((Number) lVal).longValue() ^ ((Number) rVal).longValue());
			else if (lVal instanceof Integer || rVal instanceof Integer)
				return new ConstantValue(((Number) lVal).intValue() ^ ((Number) rVal).intValue());
			else if (lVal instanceof Short || rVal instanceof Short)
				return new ConstantValue(((Number) lVal).shortValue() ^ ((Number) rVal).shortValue());
			else if (lVal instanceof Byte || rVal instanceof Byte)
				return new ConstantValue(((Number) lVal).byteValue() ^ ((Number) rVal).byteValue());
		}

		if (operator instanceof BitwiseAnd) {
			if (lVal instanceof Character)
				lVal = (int) ((Character) lVal).charValue();
			if (rVal instanceof Character)
				rVal = (int) ((Character) rVal).charValue();

			if (lVal instanceof Long || rVal instanceof Long)
				return new ConstantValue(((Number) lVal).longValue() & ((Number) rVal).longValue());
			else if (lVal instanceof Integer || rVal instanceof Integer)
				return new ConstantValue(((Number) lVal).intValue() & ((Number) rVal).intValue());
			else if (lVal instanceof Short || rVal instanceof Short)
				return new ConstantValue(((Number) lVal).shortValue() & ((Number) rVal).shortValue());
			else if (lVal instanceof Byte || rVal instanceof Byte)
				return new ConstantValue(((Number) lVal).byteValue() & ((Number) rVal).byteValue());
		}

		if (operator instanceof ComparisonLt) {
			if (lVal instanceof Character)
				lVal = (int) ((Character) lVal).charValue();
			if (rVal instanceof Character)
				rVal = (int) ((Character) rVal).charValue();

			if (lVal instanceof Double || rVal instanceof Double)
				return new ConstantValue(((Number) lVal).doubleValue() < ((Number) rVal).doubleValue());
			else if (lVal instanceof Float || rVal instanceof Float)
				return new ConstantValue(((Number) lVal).floatValue() < ((Number) rVal).floatValue());
			else if (lVal instanceof Long || rVal instanceof Long)
				return new ConstantValue(((Number) lVal).longValue() < ((Number) rVal).longValue());
			else if (lVal instanceof Integer || rVal instanceof Integer)
				return new ConstantValue(((Number) lVal).intValue() < ((Number) rVal).intValue());
			else if (lVal instanceof Short || rVal instanceof Short)
				return new ConstantValue(((Number) lVal).shortValue() < ((Number) rVal).shortValue());
			else if (lVal instanceof Byte || rVal instanceof Byte)
				return new ConstantValue(((Number) lVal).byteValue() < ((Number) rVal).byteValue());
		}

		if (operator instanceof NumericPow) {
			if (lVal instanceof Character)
				lVal = (int) ((Character) lVal).charValue();
			if (rVal instanceof Character)
				rVal = (int) ((Character) rVal).charValue();

			if (lVal instanceof Number || rVal instanceof Number)
				return new ConstantValue(Math.pow(((Number) lVal).doubleValue(), ((Number) rVal).doubleValue()));
		}

		if (operator instanceof NumericMax) {
			if (lVal instanceof Character)
				lVal = (int) ((Character) lVal).charValue();
			if (rVal instanceof Character)
				rVal = (int) ((Character) rVal).charValue();

			if (lVal instanceof Number || rVal instanceof Number)
				return new ConstantValue(Math.max(((Number) lVal).doubleValue(), ((Number) rVal).doubleValue()));
		}

		if (operator instanceof NumericMin) {
			if (lVal instanceof Character)
				lVal = (int) ((Character) lVal).charValue();
			if (rVal instanceof Character)
				rVal = (int) ((Character) rVal).charValue();

			if (lVal instanceof Number || rVal instanceof Number)
				return new ConstantValue(Math.max(((Number) lVal).doubleValue(), ((Number) rVal).doubleValue()));
		}

		if (operator instanceof NumericAtan2) {
			if (lVal instanceof Character)
				lVal = (int) ((Character) lVal).charValue();
			if (rVal instanceof Character)
				rVal = (int) ((Character) rVal).charValue();

			if (lVal instanceof Number || rVal instanceof Number)
				return new ConstantValue(Math.atan2(((Number) lVal).doubleValue(), ((Number) rVal).doubleValue()));
		}

		// strings
		if (operator instanceof StringConcat)
			return new ConstantValue(((String) lVal) + ((String) rVal));

		if (operator instanceof StringContains) {
			String lv = ((String) lVal);
			String rv = ((String) rVal);
			return new ConstantValue(lv.contains(rv));
		}

		if (operator instanceof StringEquals) {
			String lv = ((String) lVal);
			String rv = ((String) rVal);
			return new ConstantValue(lv.equals(rv));
		}

		if (operator instanceof StringEqualsIgnoreCase) {
			String lv = ((String) lVal);
			String rv = ((String) rVal);
			return new ConstantValue(lv.equalsIgnoreCase(rv));
		}

		if (operator instanceof StringCharAt) {
			String lv = ((String) lVal);
			Integer rv = ((Integer) rVal);
			return new ConstantValue(lv.charAt(rv));
		}

		if (operator instanceof StringStartsWith) {
			String lv = ((String) lVal);
			String rv = ((String) rVal);
			return new ConstantValue(lv.startsWith(rv));
		}

		if (operator instanceof StringEndsWith) {
			String lv = ((String) lVal);
			String rv = ((String) rVal);
			return new ConstantValue(lv.endsWith(rv));
		}

		if (operator instanceof StringMatches) {
			String lv = ((String) lVal);
			String rv = ((String) rVal);
			return new ConstantValue(lv.matches(rv));
		}

		if (operator instanceof StringSubstringToEnd) {
			String lv = ((String) lVal);
			Integer rv = ((Integer) rVal);
			return new ConstantValue(lv.substring(rv));
		}

		if (operator instanceof StringIndexOf) {
			String lv = ((String) lVal);
			String rv = ((String) rVal);
			return new ConstantValue(lv.indexOf(rv));
		}

		if (operator instanceof StringIndexOfChar) {
			String lv = ((String) lVal);
			Integer rv = ((Integer) rVal);
			return new ConstantValue(lv.indexOf(rv));
		}

		if (operator instanceof StringLastIndexOfChar) {
			String lv = ((String) lVal);
			Integer rv = ((Integer) rVal);
			return new ConstantValue(lv.lastIndexOf(rv));
		}

		if (operator instanceof StringLastIndexOf) {
			String lv = ((String) lVal);
			String rv = ((String) rVal);
			return new ConstantValue(lv.lastIndexOf(rv));
		}

		// char
		if (operator instanceof CharacterEquals) {
			Integer lv = ((Integer) lVal);
			Integer rv = ((Integer) rVal);
			return new ConstantValue(lv.equals(rv));
		}

		if (operator instanceof ValueComparison)
			if (lVal instanceof Comparable && rVal instanceof Comparable) {
				@SuppressWarnings({ "unchecked", "rawtypes" })
				int cmp = ((Comparable) lVal).compareTo((Comparable) rVal);
				return new ConstantValue(cmp);
			}

		return top();
	}

	@Override
	public ConstantValue evalTernaryExpression(
			TernaryExpression expression,
			ConstantValue left,
			ConstantValue middle,
			ConstantValue right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		// if left, middle or right is top, top is returned
		if (left.isTop() || middle.isTop() || right.isTop())
			return top();

		TernaryOperator operator = expression.getOperator();
		Object lVal = left.getValue();
		Object mVal = middle.getValue();
		Object rVal = right.getValue();

		if (operator instanceof StringReplaceAll) {
			String lv = ((String) lVal);
			String mv = ((String) mVal);
			String rv = ((String) rVal);
			return new ConstantValue(lv.replaceAll(mv, rv));
		}

		if (operator instanceof StringReplace) {
			String lv = ((String) lVal);
			Integer mv = ((Integer) mVal);
			Integer rv = ((Integer) rVal);
			return new ConstantValue(lv.replace((char) mv.intValue(), (char) rv.intValue()));
		}

		if (operator instanceof StringReplaceFirst) {
			String lv = ((String) lVal);
			String mv = ((String) mVal);
			String rv = ((String) rVal);
			return new ConstantValue(lv.replaceFirst(mv, rv));
		}

		if (operator instanceof StringIndexOfCharFromIndex) {
			String lv = ((String) lVal);
			Integer mv = ((Integer) mVal);
			Integer rv = ((Integer) rVal);
			return new ConstantValue(lv.indexOf((char) mv.intValue(), rv));
		}

		if (operator instanceof StringLastIndexOfCharFromIndex) {
			String lv = ((String) lVal);
			Integer mv = ((Integer) mVal);
			Integer rv = ((Integer) rVal);
			return new ConstantValue(lv.lastIndexOf((char) mv.intValue(), rv));
		}

		if (operator instanceof StringLastIndexOfFromIndex) {
			String lv = ((String) lVal);
			String mv = ((String) mVal);
			Integer rv = ((Integer) rVal);
			return new ConstantValue(lv.lastIndexOf(mv, rv));
		}

		if (operator instanceof StringSubstring) {
			String lv = ((String) lVal);
			Integer mv = ((Integer) mVal);
			Integer rv = ((Integer) rVal);
			return new ConstantValue(lv.substring(mv, rv));
		}

		if (operator instanceof StringStartsWithFromIndex) {
			String lv = ((String) lVal);
			String mv = ((String) mVal);
			Integer rv = ((Integer) rVal);
			return new ConstantValue(lv.startsWith(mv, rv));
		}

		if (operator instanceof StringIndexOfFromIndex) {
			String lv = ((String) lVal);
			String mv = ((String) mVal);
			Integer rv = ((Integer) rVal);
			return new ConstantValue(lv.indexOf(mv, rv));
		}

		return top();

	}

	@Override
	public Satisfiability satisfiesAbstractValue(
			ConstantValue value,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (value.getValue() instanceof Boolean)
			return ((Boolean) value.getValue()).booleanValue() ? Satisfiability.SATISFIED
					: Satisfiability.NOT_SATISFIED;

		return Satisfiability.UNKNOWN;
	}

	@Override
	public Satisfiability satisfiesUnaryExpression(
			UnaryExpression expression,
			ConstantValue arg,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (arg.isTop())
			return Satisfiability.UNKNOWN;

		UnaryOperator operator = expression.getOperator();
		Object value = arg.getValue();
		if (operator instanceof CharacterIsLetter)
			if (value instanceof Integer)
				return Character.isLetter((int) value) ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;

		if (operator instanceof CharacterIsDigit)
			if (value instanceof Integer)
				return Character.isDigit((int) value) ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;

		if (operator instanceof CharacterIsDefined)
			if (value instanceof Integer)
				return Character.isDefined((int) value) ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;

		if (operator instanceof CharacterIsLetterOrDigit)
			if (value instanceof Integer)
				return Character.isLetterOrDigit((int) value) ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;

		if (operator instanceof CharacterIsLowerCase)
			if (value instanceof Integer)
				return Character.isLowerCase((int) value) ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;

		if (operator instanceof CharacterIsUpperCase)
			if (value instanceof Integer)
				return Character.isUpperCase((int) value) ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;

		return BaseNonRelationalValueDomain.super.satisfiesUnaryExpression(expression, arg, pp, oracle);
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(
			BinaryExpression expression,
			ConstantValue left,
			ConstantValue right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		BinaryOperator operator = expression.getOperator();
		if (left.isTop() || right.isTop())
			return Satisfiability.UNKNOWN;

		// character
		if (operator instanceof CharacterEquals) {
			Integer lv = ((Integer) left.getValue());
			Integer rv = ((Integer) right.getValue());
			return lv.equals(rv) ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
		}

		// string
		if (operator instanceof StringContains) {
			String lv = ((String) left.getValue());
			String rv = ((String) right.getValue());
			return lv.contains(rv) ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
		}

		if (operator instanceof StringEquals) {
			String lv = ((String) left.getValue());
			String rv = ((String) right.getValue());
			return lv.equals(rv) ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
		}

		if (operator instanceof StringEqualsIgnoreCase) {
			String lv = ((String) left.getValue());
			String rv = ((String) right.getValue());
			return lv.equalsIgnoreCase(rv) ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
		}

		if (operator instanceof StringMatches) {
			String lv = ((String) left.getValue());
			String rv = ((String) right.getValue());
			return lv.matches(rv) ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
		}

		if (operator instanceof StringEndsWith) {
			String lv = ((String) left.getValue());
			String rv = ((String) right.getValue());
			return lv.endsWith(rv) ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
		}

		if (operator instanceof StringStartsWith) {
			String lv = ((String) left.getValue());
			String rv = ((String) right.getValue());
			return lv.startsWith(rv) ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
		}

		if (operator instanceof StringEqualsIgnoreCase) {
			String lv = ((String) left.getValue());
			String rv = ((String) right.getValue());
			return lv.equalsIgnoreCase(rv) ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
		}

		if (operator instanceof ComparisonEq) {
			Object lVal = left.getValue();
			Object rVal = right.getValue();

			if (lVal instanceof Number && rVal instanceof Number)
				if (lVal instanceof Double || rVal instanceof Double)
					return ((Number) lVal).doubleValue() == ((Number) rVal).doubleValue() ? Satisfiability.SATISFIED
							: Satisfiability.NOT_SATISFIED;
				else if (lVal instanceof Float || rVal instanceof Float)
					return ((Number) lVal).floatValue() == ((Number) rVal).floatValue() ? Satisfiability.SATISFIED
							: Satisfiability.NOT_SATISFIED;
				else if (lVal instanceof Long || rVal instanceof Long)
					return ((Number) lVal).longValue() == ((Number) rVal).longValue() ? Satisfiability.SATISFIED
							: Satisfiability.NOT_SATISFIED;
				else
					return ((Number) lVal).intValue() == ((Number) rVal).intValue() ? Satisfiability.SATISFIED
							: Satisfiability.NOT_SATISFIED;
			else if (lVal instanceof Boolean && rVal instanceof Boolean)
				return ((Boolean) lVal).booleanValue() == ((Boolean) rVal).booleanValue() ? Satisfiability.SATISFIED
						: Satisfiability.NOT_SATISFIED;
			else if (lVal instanceof Character && rVal instanceof Character)
				return ((Character) lVal).charValue() == ((Character) rVal).charValue() ? Satisfiability.SATISFIED
						: Satisfiability.NOT_SATISFIED;
		}

		if (operator instanceof ComparisonNe) {
			Object lVal = left.getValue();
			Object rVal = right.getValue();

			if (lVal instanceof Number || rVal instanceof Number)
				if (lVal instanceof Double || rVal instanceof Double)
					return ((Number) lVal).doubleValue() != ((Number) rVal).doubleValue() ? Satisfiability.SATISFIED
							: Satisfiability.NOT_SATISFIED;
				else if (lVal instanceof Float || rVal instanceof Float)
					return ((Number) lVal).floatValue() != ((Number) rVal).floatValue() ? Satisfiability.SATISFIED
							: Satisfiability.NOT_SATISFIED;
				else if (lVal instanceof Long || rVal instanceof Long)
					return ((Number) lVal).longValue() != ((Number) rVal).longValue() ? Satisfiability.SATISFIED
							: Satisfiability.NOT_SATISFIED;
				else
					return ((Number) lVal).intValue() != ((Number) rVal).intValue() ? Satisfiability.SATISFIED
							: Satisfiability.NOT_SATISFIED;
			else if (lVal instanceof Boolean && rVal instanceof Boolean)
				return ((Boolean) lVal).booleanValue() != ((Boolean) rVal).booleanValue() ? Satisfiability.SATISFIED
						: Satisfiability.NOT_SATISFIED;
			else if (lVal instanceof Character && rVal instanceof Character)
				return ((Character) lVal).charValue() != ((Character) rVal).charValue() ? Satisfiability.SATISFIED
						: Satisfiability.NOT_SATISFIED;
		}

		if (operator instanceof ComparisonLt) {
			Object lVal = left.getValue();
			Object rVal = right.getValue();

			if (lVal instanceof Double || rVal instanceof Double)
				return ((Number) lVal).doubleValue() < ((Number) rVal).doubleValue() ? Satisfiability.SATISFIED
						: Satisfiability.NOT_SATISFIED;
			else if (lVal instanceof Float || rVal instanceof Float)
				return ((Number) lVal).floatValue() < ((Number) rVal).floatValue() ? Satisfiability.SATISFIED
						: Satisfiability.NOT_SATISFIED;
			else if (lVal instanceof Long || rVal instanceof Long)
				return ((Number) lVal).longValue() < ((Number) rVal).longValue() ? Satisfiability.SATISFIED
						: Satisfiability.NOT_SATISFIED;
			else
				return ((Number) lVal).intValue() < ((Number) rVal).intValue() ? Satisfiability.SATISFIED
						: Satisfiability.NOT_SATISFIED;
		}

		if (operator instanceof ComparisonLe) {
			Object lVal = left.getValue();
			Object rVal = right.getValue();

			if (lVal instanceof Double || rVal instanceof Double)
				return ((Number) lVal).doubleValue() <= ((Number) rVal).doubleValue() ? Satisfiability.SATISFIED
						: Satisfiability.NOT_SATISFIED;
			else if (lVal instanceof Float || rVal instanceof Float)
				return ((Number) lVal).floatValue() <= ((Number) rVal).floatValue() ? Satisfiability.SATISFIED
						: Satisfiability.NOT_SATISFIED;
			else if (lVal instanceof Long || rVal instanceof Long)
				return ((Number) lVal).longValue() <= ((Number) rVal).longValue() ? Satisfiability.SATISFIED
						: Satisfiability.NOT_SATISFIED;
			else
				return ((Number) lVal).intValue() <= ((Number) rVal).intValue() ? Satisfiability.SATISFIED
						: Satisfiability.NOT_SATISFIED;
		}

		if (operator instanceof ComparisonGt) {
			Object lVal = left.getValue();
			Object rVal = right.getValue();

			if (lVal instanceof Double || rVal instanceof Double)
				return ((Number) lVal).doubleValue() > ((Number) rVal).doubleValue() ? Satisfiability.SATISFIED
						: Satisfiability.NOT_SATISFIED;
			else if (lVal instanceof Float || rVal instanceof Float)
				return ((Number) lVal).floatValue() > ((Number) rVal).floatValue() ? Satisfiability.SATISFIED
						: Satisfiability.NOT_SATISFIED;
			else if (lVal instanceof Long || rVal instanceof Long)
				return ((Number) lVal).longValue() > ((Number) rVal).longValue() ? Satisfiability.SATISFIED
						: Satisfiability.NOT_SATISFIED;
			else
				return ((Number) lVal).intValue() > ((Number) rVal).intValue() ? Satisfiability.SATISFIED
						: Satisfiability.NOT_SATISFIED;
		}

		if (operator instanceof ComparisonGe) {
			Object lVal = left.getValue();
			Object rVal = right.getValue();

			if (lVal instanceof Double || rVal instanceof Double)
				return ((Number) lVal).doubleValue() >= ((Number) rVal).doubleValue() ? Satisfiability.SATISFIED
						: Satisfiability.NOT_SATISFIED;
			else if (lVal instanceof Float || rVal instanceof Float)
				return ((Number) lVal).floatValue() >= ((Number) rVal).floatValue() ? Satisfiability.SATISFIED
						: Satisfiability.NOT_SATISFIED;
			else if (lVal instanceof Long || rVal instanceof Long)
				return ((Number) lVal).longValue() >= ((Number) rVal).longValue() ? Satisfiability.SATISFIED
						: Satisfiability.NOT_SATISFIED;
			else
				return ((Number) lVal).intValue() >= ((Number) rVal).intValue() ? Satisfiability.SATISFIED
						: Satisfiability.NOT_SATISFIED;
		}

		if (operator instanceof StringStartsWith) {
			String lv = ((String) left.getValue());
			String rv = ((String) right.getValue());
			return lv.startsWith(rv) ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
		}

		if (operator instanceof StringEndsWith) {
			String lv = ((String) left.getValue());
			String rv = ((String) right.getValue());
			return lv.endsWith(rv) ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
		}

		if (operator instanceof StringMatches) {
			String lv = ((String) left.getValue());
			String rv = ((String) right.getValue());
			return lv.matches(rv) ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
		}

		return BaseNonRelationalValueDomain.super.satisfiesBinaryExpression(expression, left, right, pp, oracle);
	}

	@Override
	public Satisfiability satisfiesConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (constant.getValue() instanceof Boolean)
			return ((Boolean) constant.getValue()).booleanValue() ? Satisfiability.SATISFIED
					: Satisfiability.NOT_SATISFIED;

		return Satisfiability.UNKNOWN;
	}

	@Override
	public ValueEnvironment<ConstantValue> assume(
			ValueEnvironment<ConstantValue> environment,
			ValueExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		Satisfiability sat = satisfies(environment, expression, src, oracle);
		if (sat == Satisfiability.NOT_SATISFIED)
			return environment.bottom();
		if (sat == Satisfiability.SATISFIED)
			return environment;
		return BaseNonRelationalValueDomain.super.assume(environment, expression, src, dest, oracle);
	}

	@Override
	public ValueEnvironment<ConstantValue> assumeBinaryExpression(
			ValueEnvironment<ConstantValue> environment,
			BinaryExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		Identifier id;
		ConstantValue eval;
		ValueExpression left = (ValueExpression) expression.getLeft();
		ValueExpression right = (ValueExpression) expression.getRight();
		if (left instanceof Identifier) {
			eval = eval(environment, right, src, oracle);
			id = (Identifier) left;
		} else if (right instanceof Identifier) {
			eval = eval(environment, left, src, oracle);
			id = (Identifier) right;
		} else
			return environment;

		ConstantValue starting = environment.getState(id);
		if (eval.isBottom() || starting.isBottom())
			return environment.bottom();
		if (eval.isTop())
			// if the value is not constant, we cannot refine the state
			return environment;

		ConstantValue update = null;
		// note for eq and neq: since we are tracking constants,
		// exact equality/inequality is handled through satisfies
		// (eg, x == 5 is either satisfied or not satisfied if x is constant);
		// here we just refine the state when we have an unknown satisfiability
		// (eg, x == 5 when x is top). This means that for eq we can just
		// set x to 5, and for neq we must leave x to top.
		// The only exception is with booleans: since they
		// can have only two values, for neq we can invert the boolean value.
		if (expression.getOperator() instanceof ComparisonEq)
			update = eval;
		else if (expression.getOperator() instanceof ComparisonNe)
			if (eval.getValue() instanceof Boolean) {
				Boolean b = (Boolean) eval.getValue();
				update = new ConstantValue(!b.booleanValue());
			} else
				update = top();

		if (update == null)
			return environment;
		else if (update.isBottom())
			return environment.bottom();
		else
			return environment.putState(id, update);
	}

	@Override
	public ConstantValue top() {
		return ConstantValue.TOP;
	}

	@Override
	public ConstantValue bottom() {
		return ConstantValue.BOTTOM;
	}
}
