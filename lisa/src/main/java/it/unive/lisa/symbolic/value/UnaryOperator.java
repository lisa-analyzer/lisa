package it.unive.lisa.symbolic.value;

import it.unive.lisa.cfg.type.BooleanType;
import it.unive.lisa.cfg.type.NumericType;
import it.unive.lisa.cfg.type.StringType;
import it.unive.lisa.cfg.type.Type;
import it.unive.lisa.symbolic.SymbolicExpression;

/**
 * A unary operator that can be applied to a single {@link SymbolicExpression}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public enum UnaryOperator implements Operator {
	/**
	 * Given a Boolean value of type {@link BooleanType}, this operator returns
	 * the logical negation of the value: if the Boolean value represents true,
	 * it returns false, and vice versa. The return type of this operation is
	 * {@link BooleanType}.
	 */
	LOGICAL_NOT("!"),

	/**
	 * Given a numeric value of type {@link NumericType}, this operator returns
	 * the negation of the numerical value. The return type of this operator is
	 * a signed {@link NumericType}.
	 */
	NUMERIC_NEG("-"),

	/**
	 * Given a string of type {@link StringType} returns the length of that
	 * string. If the value represents the empty string, it returns 0. The
	 * return type of this operator is a 32 bit unsigned {@link NumericType}.
	 */
	STRING_LENGTH("strlen"),

	/**
	 * Yields the {@link Type} of an expression.
	 */
	TYPEOF("typeof");

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
