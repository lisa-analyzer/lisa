package it.unive.lisa.symbolic.value;

import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.BooleanType;
import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.StringType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeTokenType;

/**
 * A unary {@link Operator} that can be applied to a single
 * {@link SymbolicExpression}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public enum UnaryOperator implements Operator {

	/**
	 * Given an expression that evaluates to a Boolean value, a
	 * {@link UnaryExpression} using this operator computes the logical negation
	 * of the expression: if it evaluates to {@code true}, it is transformed to
	 * {@code false}, and vice versa.<br>
	 * <br>
	 * Argument expression type: {@link BooleanType}<br>
	 * Computed expression type: {@link BooleanType}
	 */
	LOGICAL_NOT("!"),

	/**
	 * Given an expression that evaluates to a numeric value, a
	 * {@link UnaryExpression} using this operator computes the arithmetic
	 * negation (i.e., multiplication by {@code -1}) of that value.<br>
	 * <br>
	 * Argument expression type: {@link NumericType}<br>
	 * Computed expression type: {@link NumericType} (same of the argument, but
	 * signed)
	 */
	NUMERIC_NEG("-"),

	/**
	 * Given an expression that evaluates to a string value, a
	 * {@link UnaryExpression} using this operator computes an integral value,
	 * from {@code 0} upwards, representing the length of that value.<br>
	 * <br>
	 * Argument expression type: {@link StringType}<br>
	 * Computed expression type: {@link NumericType} (integral)
	 */
	STRING_LENGTH("strlen"),

	/**
	 * Given any expression, a {@link UnaryExpression} using this operator
	 * computes the type(s) of that expression.<br>
	 * <br>
	 * Argument expression type: any {@link Type}<br>
	 * Computed expression type: {@link TypeTokenType} containing all types of
	 * argument
	 */
	TYPEOF("typeof"),

	/**
	 * Given an expression that evaluates to a numeric value, a
	 * {@link UnaryExpression} using this operator computes the bitwise negation
	 * (i.e., flipping every single bit independently) of that value.<br>
	 * <br>
	 * Argument expression type: {@link NumericType}<br>
	 * Computed expression type: {@link NumericType} (same as argument)
	 */
	BITWISE_NEGATION("~");

	private final String representation;

	private UnaryOperator(String representation) {
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
