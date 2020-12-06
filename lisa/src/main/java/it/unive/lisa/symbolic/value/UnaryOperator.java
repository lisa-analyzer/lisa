package it.unive.lisa.symbolic.value;

import it.unive.lisa.cfg.type.BooleanType;
import it.unive.lisa.cfg.type.NumericType;
import it.unive.lisa.cfg.type.StringType;
import it.unive.lisa.symbolic.SymbolicExpression;

/**
 * A unary operator that can be applied to a single {@link SymbolicExpression}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public enum UnaryOperator implements Operator {
	// TODO the semantics of the operators should be clearly stated
	/**
	 * The logical negation of a value of type {@link BooleanType}.
	 */
	LOGICAL_NOT("!"),

	/**
	 * The negation of a numerical value of type {@link NumericType}.
	 */
	NUMERIC_NEG("-"),

	/**
	 * The length of a variable of type {@link StringType}.
	 */
	STRING_LENGTH("strlen");

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
