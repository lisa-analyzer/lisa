package it.unive.lisa.symbolic.value;

import it.unive.lisa.symbolic.SymbolicExpression;

/**
 * A ternary operator that can be applied to a triple of
 * {@link SymbolicExpression}s.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public enum TernaryOperator implements Operator {
	// TODO the semantics of the operators should be clearly stated
	/**
	 * Replaces all occurrences of a string into another with the given one.
	 */
	STRING_REPLACE("strreplace"),

	/**
	 * Computes the substring of a string between two indexes.
	 */
	STRING_SUBSTRING("substr");

	private final String representation;

	private TernaryOperator(String representation) {
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
