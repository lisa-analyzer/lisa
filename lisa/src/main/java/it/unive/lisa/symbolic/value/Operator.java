package it.unive.lisa.symbolic.value;

import it.unive.lisa.symbolic.SymbolicExpression;

/**
 * An operator that causes a transformation of one or more
 * {@link SymbolicExpression}s.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface Operator {

	/**
	 * Yields the string representation of the operator.
	 * 
	 * @return the string representation
	 */
	String getStringRepresentation();

	/**
	 * Yields the opposite operator of this operator (e.g., <= to >, < -> >=).
	 * 
	 * @return the opposite operator of this operator
	 */
	default Operator opposite() {
		return this;
	}
}
