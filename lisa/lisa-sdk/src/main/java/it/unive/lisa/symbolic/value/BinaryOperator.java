package it.unive.lisa.symbolic.value;

import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.BooleanType;
import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.StringType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeTokenType;

/**
 * A binary {@link Operator} that can be applied to a pair of
 * {@link SymbolicExpression}s.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public enum BinaryOperator implements Operator {

	/**
	 * Given two expressions that both evaluate to numeric values, a
	 * {@link BinaryExpression} using this operator computes the arithmetic
	 * subtraction of those values.<br>
	 * <br>
	 * First argument expression type: {@link NumericType}<br>
	 * Second argument expression type: {@link NumericType}<br>
	 * Computed expression type: {@link NumericType}
	 */
	NUMERIC_SUB("-"),

	/**
	 * Given two expressions that both evaluate to numeric values, a
	 * {@link BinaryExpression} using this operator computes the arithmetic
	 * addition of those values.<br>
	 * <br>
	 * First argument expression type: {@link NumericType}<br>
	 * Second argument expression type: {@link NumericType}<br>
	 * Computed expression type: {@link NumericType}
	 */
	NUMERIC_ADD("+"),

	/**
	 * Given two expressions that both evaluate to numeric values, a
	 * {@link BinaryExpression} using this operator computes the arithmetic
	 * division of those values.<br>
	 * <br>
	 * First argument expression type: {@link NumericType}<br>
	 * Second argument expression type: {@link NumericType}<br>
	 * Computed expression type: {@link NumericType}
	 */
	NUMERIC_DIV("/"),

	/**
	 * Given two expressions that both evaluate to numeric values, a
	 * {@link BinaryExpression} using this operator computes the arithmetic
	 * multiplication of those values.<br>
	 * <br>
	 * First argument expression type: {@link NumericType}<br>
	 * Second argument expression type: {@link NumericType}<br>
	 * Computed expression type: {@link NumericType}
	 */
	NUMERIC_MUL("*"),

	/**
	 * Given two expressions that both evaluate to numeric values, a
	 * {@link BinaryExpression} using this operator computes the arithmetic
	 * remainder of those values.<br>
	 * <br>
	 * First argument expression type: {@link NumericType}<br>
	 * Second argument expression type: {@link NumericType}<br>
	 * Computed expression type: {@link NumericType}
	 */
	NUMERIC_MOD("%"),

	/**
	 * Given two expressions that both evaluate to Boolean values, a
	 * {@link BinaryExpression} using this operator computes the logical
	 * disjunction of those values, without short-circuiting.<br>
	 * <br>
	 * First argument expression type: {@link BooleanType}<br>
	 * Second argument expression type: {@link BooleanType}<br>
	 * Computed expression type: {@link BooleanType}
	 */
	LOGICAL_OR("||") {
		@Override
		public BinaryOperator opposite() {
			return LOGICAL_AND;
		}
	},

	/**
	 * Given two expressions that both evaluate to Boolean values, a
	 * {@link BinaryExpression} using this operator computes the logical
	 * conjunction of those values, without short-circuiting.<br>
	 * <br>
	 * First argument expression type: {@link BooleanType}<br>
	 * Second argument expression type: {@link BooleanType}<br>
	 * Computed expression type: {@link BooleanType}
	 */
	LOGICAL_AND("&&") {
		@Override
		public BinaryOperator opposite() {
			return LOGICAL_OR;
		}
	},

	/**
	 * Given two expressions, a {@link BinaryExpression} using this operator
	 * checks if the values those expressions compute to are different. This
	 * operator corresponds to the logical negation of
	 * {@link #COMPARISON_EQ}.<br>
	 * <br>
	 * First argument expression type: any {@link Type}<br>
	 * Second argument expression type: any {@link Type}<br>
	 * Computed expression type: {@link BooleanType}
	 */
	COMPARISON_NE("!=") {
		@Override
		public BinaryOperator opposite() {
			return COMPARISON_EQ;
		}
	},

	/**
	 * Given two expressions, a {@link BinaryExpression} using this operator
	 * checks if the values those expressions compute to are different. This
	 * operator corresponds to the logical negation of
	 * {@link #COMPARISON_NE}.<br>
	 * <br>
	 * First argument expression type: any {@link Type}<br>
	 * Second argument expression type: any {@link Type}<br>
	 * Computed expression type: {@link BooleanType}
	 */
	COMPARISON_EQ("==") {
		@Override
		public BinaryOperator opposite() {
			return COMPARISON_NE;
		}
	},

	/**
	 * Given two expressions that both evaluate to numeric values, a
	 * {@link BinaryExpression} using this operator checks if the value of the
	 * first argument is greater or equal than the value of the right-hand
	 * side.<br>
	 * <br>
	 * First argument expression type: {@link NumericType}<br>
	 * Second argument expression type: {@link NumericType}<br>
	 * Computed expression type: {@link BooleanType}
	 */
	COMPARISON_GE(">=") {
		@Override
		public BinaryOperator opposite() {
			return COMPARISON_LT;
		}
	},

	/**
	 * Given two expressions that both evaluate to numeric values, a
	 * {@link BinaryExpression} using this operator checks if the value of the
	 * first argument is greater than the value of the second argument.<br>
	 * <br>
	 * First argument expression type: {@link NumericType}<br>
	 * Second argument expression type: {@link NumericType}<br>
	 * Computed expression type: {@link BooleanType}
	 */
	COMPARISON_GT(">") {
		@Override
		public BinaryOperator opposite() {
			return COMPARISON_LE;
		}
	},

	/**
	 * Given two expressions that both evaluate to numeric values, a
	 * {@link BinaryExpression} using this operator checks if the value of the
	 * first argument is less or equal than the value of the right-hand
	 * side.<br>
	 * <br>
	 * First argument expression type: {@link NumericType}<br>
	 * Second argument expression type: {@link NumericType}<br>
	 * Computed expression type: {@link BooleanType}
	 */
	COMPARISON_LE("<=") {
		@Override
		public BinaryOperator opposite() {
			return COMPARISON_GT;
		}
	},

	/**
	 * Given two expressions that both evaluate to numeric values, a
	 * {@link BinaryExpression} using this operator checks if the value of the
	 * first argument is less than the value of the second argument.<br>
	 * <br>
	 * First argument expression type: {@link NumericType}<br>
	 * Second argument expression type: {@link NumericType}<br>
	 * Computed expression type: {@link BooleanType}
	 */
	COMPARISON_LT("<") {
		@Override
		public BinaryOperator opposite() {
			return COMPARISON_GE;
		}
	},

	/**
	 * Given two expressions that both evaluate to string values, a
	 * {@link BinaryExpression} using this operator computes the concatenation
	 * of the string from the first argument with the one of the second
	 * argument.<br>
	 * <br>
	 * First argument expression type: {@link StringType}<br>
	 * Second argument expression type: {@link StringType}<br>
	 * Computed expression type: {@link StringType}
	 */
	STRING_CONCAT("strcat"),

	/**
	 * Given two expressions that both evaluate to string values, a
	 * {@link BinaryExpression} using this operator checks if the string from
	 * the first argument contains the one of the second argument.<br>
	 * <br>
	 * First argument expression type: {@link StringType}<br>
	 * Second argument expression type: {@link StringType}<br>
	 * Computed expression type: {@link BooleanType}
	 */
	STRING_CONTAINS("strcontains"),

	/**
	 * Given two expressions that both evaluate to string values, a
	 * {@link BinaryExpression} using this operator checks if the string from
	 * the first argument is prefixed by the one of the second argument.<br>
	 * <br>
	 * First argument expression type: {@link StringType}<br>
	 * Second argument expression type: {@link StringType}<br>
	 * Computed expression type: {@link BooleanType}
	 */
	STRING_STARTS_WITH("strstarts"),

	/**
	 * Given two expressions that both evaluate to string values, a
	 * {@link BinaryExpression} using this operator checks if the string from
	 * the first argument is suffixed by the one of the second argument.<br>
	 * <br>
	 * First argument expression type: {@link StringType}<br>
	 * Second argument expression type: {@link StringType}<br>
	 * Computed expression type: {@link BooleanType}
	 */
	STRING_ENDS_WITH("strends"),

	/**
	 * Given two expressions that both evaluate to string values, a
	 * {@link BinaryExpression} using this operator computes the starting index
	 * <i>of the first occurrence</i> of the string from the second argument
	 * inside the one of the first argument, producing {@code -1} if no
	 * occurrence can be found.<br>
	 * <br>
	 * First argument expression type: {@link StringType}<br>
	 * Second argument expression type: {@link StringType}<br>
	 * Computed expression type: {@link NumericType} (integral)
	 */
	STRING_INDEX_OF("strindexof"),

	/**
	 * Given two expressions that both evaluate to string values, a
	 * {@link BinaryExpression} using this operator checks if the string from
	 * the first argument is equal (in terms of contents, that is different from
	 * {@link #COMPARISON_EQ}) to the one of the second argument.<br>
	 * <br>
	 * First argument expression type: {@link StringType}<br>
	 * Second argument expression type: {@link StringType}<br>
	 * Computed expression type: {@link BooleanType}
	 */
	STRING_EQUALS("strcmp"),

	/**
	 * Given two expressions, with the second one evaluating to a type token, a
	 * {@link BinaryExpression} using this operator casts the type of the first
	 * argument to the type of the second argument. The resulting value is
	 * exactly the first argument, but with its runtime types <i>filtered</i> to
	 * be instances of the right-hand side type. Indeed, this operation on types
	 * is a narrowing operator, that is, the destination type is smaller than
	 * the source type.<br>
	 * This operator resembles the Java cast operation between reference types:
	 * when an object is cast to another type, its runtime types do not change,
	 * but the cast fails of the runtime type of the object is not a subtype of
	 * the desired type.<br>
	 * <br>
	 * First argument expression type: any {@link Type}<br>
	 * Second argument expression type: {@link TypeTokenType}<br>
	 * Computed expression type: first argument {@link Type}s filtered
	 */
	TYPE_CAST("cast-as"),

	/**
	 * Given two expressions, with the second one evaluating to a type token, a
	 * {@link BinaryExpression} using this operator converts the type of the
	 * first argument to the type of the second argument. The resulting value is
	 * exactly the first argument, but with its runtime types <i>changed</i> to
	 * be instances of the right-hand side type. Indeed, this operation on types
	 * is a widening operator, that is, the destination type is greater than the
	 * source type.<br>
	 * This operator resembles the Java cast operation between primitive types:
	 * when, for instance, an int is cast to a float, its runtime types does
	 * change, possibly with loss of information during the conversion.<br>
	 * <br>
	 * First argument expression type: any {@link Type}<br>
	 * Second argument expression type: {@link TypeTokenType}<br>
	 * Computed expression type: second argument inner {@link Type}s
	 */
	TYPE_CONV("conv-as"),

	/**
	 * Given two expressions, with the second one evaluating to a type token, a
	 * {@link BinaryExpression} using this operator checks if the runtime types
	 * of the value the first argument evaluates to are subtypes of the ones
	 * contained in the token.<br>
	 * <br>
	 * First argument expression type: any {@link Type}<br>
	 * Second argument expression type: {@link TypeTokenType}<br>
	 * Computed expression type: {@link BooleanType}
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
