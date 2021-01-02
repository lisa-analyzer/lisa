package it.unive.lisa.symbolic.value;

import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.BooleanType;
import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.StringType;

/**
 * A binary operator that can be applied to a pair of
 * {@link SymbolicExpression}s.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public enum BinaryOperator implements Operator {
	/**
	 * Given two integers values {@code n1} and {@code n2} of type
	 * {@link NumericType}, this operator returns the subtraction of {@code n1}
	 * with {@code n2}. The return type of this operator is {@link NumericType}.
	 */
	NUMERIC_SUB("-"),

	/**
	 * Given two integers values {@code n1} and {@code n2} of type
	 * {@link NumericType}, this operator returns the addition of {@code n1}
	 * with {@code n2}. The return type of this operator is {@link NumericType}.
	 */
	NUMERIC_ADD("+"),

	/**
	 * Given two integers values {@code n1} and {@code n2}, with {@code n2} not
	 * equal to zero, of type {@link NumericType}, this operator returns the
	 * division of {@code i1} with {@code n2}. The return type of this operator
	 * is {@code NumericType}. If {@code n1} is divisible by {@code n2}, the
	 * return type is the {@link NumericType#commonSupertype} between the types
	 * of {@code n1} and {@code n2}.
	 */
	NUMERIC_DIV("/"),

	/**
	 * Given two integers values {@code n1} and {@code n2} of type
	 * {@link NumericType}, this operator returns the multiplication of
	 * {@code i1} with {@code n2}. The return type of this operator is
	 * {@link NumericType}.
	 */
	NUMERIC_MUL("*"),

	/**
	 * Given two integers values {@code n1} and {@code n2} of type
	 * {@link NumericType}, this operator returns the modulo of {@code n1} with
	 * {@code n2}. The return type of this operator is a 32 bit unsigned integer
	 * {@link NumericType}.
	 */
	NUMERIC_MOD("%"),

	/**
	 * Given two Boolean values {@code b1} and {@code b2} of type
	 * {@link BooleanType}, this operator returns the logical or between
	 * {@code b1} and {@code b2}. No short-circuit mechanism is provided, hence
	 * both {@code b1} and {@code b2} are evaluated The return type of this
	 * operator is {@link BooleanType}.
	 */
	LOGICAL_OR("||"),

	/**
	 * Given two Boolean values {@code b1} and {@code b2} of type
	 * {@link BooleanType}, this operator returns the logical and between
	 * {@code b1} and {@code b2}. No short-circuit mechanism is provided, hence
	 * both {@code b1} and {@code b2} are evaluated The return type of this
	 * operator is {@link BooleanType}.
	 */
	LOGICAL_AND("&&"),

	/**
	 * Given two values, check whether they have different values or different
	 * type. If so, true value is returned, false otherwise. This operator
	 * corresponds to the logical negation of
	 * {@link BinaryOperator#COMPARISON_EQ}. The return type of this operator is
	 * {@link BooleanType}.
	 */
	COMPARISON_NE("!="),

	/**
	 * Given two values, check whether they have same values and same type. If
	 * so, true value is returned, false otherwise. This operator corresponds to
	 * the logical negation of {@link BinaryOperator#COMPARISON_NE}. The return
	 * type of this operator is {@link BooleanType}.
	 */
	COMPARISON_EQ("=="),

	/**
	 * Given two numeric values {@code n1} and {@code n2} of type
	 * {@link NumericType}, this operator checks if {@code n1} is in relation
	 * through the "greater or equal than" relation with {@code n2}. If so, true
	 * value is returned, false otherwise. The return type of this operator is
	 * {@link BooleanType}.
	 */
	COMPARISON_GE(">="),

	/**
	 * Given two numeric values {@code n1} and {@code n2}, this operator checks
	 * if {@code n1} is in relation through the "greater than" relation with
	 * {@code n2}. If so, true value is returned, false otherwise. The return
	 * type of this operator is {@link BooleanType}.
	 */
	COMPARISON_GT(">"),

	/**
	 * Given two numeric values {@code n1} and {@code n2}, this operator checks
	 * if {@code n1} is in relation through the "less or equal than" relation
	 * with {@code n2}. If so, true value is returned, false otherwise. The
	 * return type of this operator is {@link BooleanType}.
	 */
	COMPARISON_LE("<="),

	/**
	 * Given two numeric values {@code n1} and {@code n2}, this operator checks
	 * if {@code n1} is in relation through the "less than" relation with
	 * {@code n2}. If so, true value is returned, false otherwise. The return
	 * type of this operator is {@link BooleanType}.
	 */
	COMPARISON_LT("<"),

	/**
	 * Given two string values {@code s1} and {@code s2} of type
	 * {@link StringType}, this operator return the concatenation of {@code s1}
	 * with {@code s2}. The neutral element is the empty string. The return type
	 * of this operator is {@link StringType}.
	 */
	STRING_CONCAT("strcat"),

	/**
	 * Given two string values {@code s1} and {@code s2} of type
	 * {@link StringType}, this operator checks whether {@code s1} contains
	 * {@code s2}. If {@code s2} is the empty string, then true is returned. The
	 * return type of this operator is {@link BooleanType}.
	 */
	STRING_CONTAINS("strcontains"),

	/**
	 * Given two string values {@code s1} and {@code s2} of type
	 * {@link StringType}, this operator checks whether {@code s1} starts with
	 * {@code s2}, namely if {@code s2} is the prefix of {@code s1}. The empty
	 * string is the prefix of any string, hence if {@code s2} is the empty
	 * string, true is returned. The return type of this operator is
	 * {@link BooleanType}.
	 */
	STRING_STARTS_WITH("strstarts"),

	/**
	 * Given two string values {@code s1} and {@code s2} of type
	 * {@link StringType}, this operator checks whether {@code s1} ends with
	 * {@code s2}, namely if {@code s2} is the suffix of {@code s1}. The empty
	 * string is the suffix of any string, hence if {@code s2} is the empty
	 * string, true is returned. The return type of this operator is
	 * {@link BooleanType}.
	 */
	STRING_ENDS_WITH("strends"),

	/**
	 * Given two string values {@code s1} and {@code s2} of type
	 * {@link StringType}, this operator return the position of the first
	 * occurrence of {@code s1} in {@code s2}. If {@code s1} does not contains
	 * {@code s2}, then -1 is returned. The return type of this operator is a 32
	 * bit signed {@link NumericType}.
	 */
	STRING_INDEX_OF("strindexof"),

	/**
	 * Given two string values {@code s1} and {@code s2} of type
	 * {@link StringType}, this operator checks if the two strings are equals.
	 * The return type of this operator is {@link StringType}.
	 */
	STRING_EQUALS("strcmp"),

	/**
	 * Casts the type of the left-hand side of this expression to the type of
	 * the right-hand side. The returned value is exactly the left-hand side,
	 * but with its runtime types filtered to be instances of the right-hand
	 * side type.
	 */
	TYPE_CAST("as"),

	/**
	 * Tests if the type of the left-hand side of this expression is the same
	 * as, or a subtype of, the type of the right-hand side. The returned value
	 * is a boolean of type {@link BooleanType} expressing this relation.
	 */
	TYPE_CHECK("is");

	private final String representation;

	private BinaryOperator(String representation) {
		this.representation = representation;
	}

	@Override
	public String getStringRepresentation() {
		return representation;
	}

	@Override
	public String toString() {
		return getStringRepresentation();
	}
}
