package it.unive.lisa.symbolic.value;

import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.NumericType;

/**
 * A ternary operator that can be applied to a triple of
 * {@link SymbolicExpression}s.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public enum TernaryOperator implements Operator {
	/**
	 * Given an input string, a search string, and a replace string, all of type
	 * {@link it.unive.lisa.type.StringType}, this operator returns the input
	 * string where all the occurrences of the search string are replaced with
	 * the replace string. If the input string is empty, the empty string is
	 * returned. If the search string is empty, the replace string is added in
	 * any position of the input string: for example, strreplace("aaa", "", "b")
	 * = "bababab". The return type of this operator is
	 * {@link it.unive.lisa.type.StringType}.
	 */
	STRING_REPLACE("strreplace"),

	/**
	 * Given a string {@code s} of type {@link it.unive.lisa.type.StringType}
	 * and two unsigned integers {@code i} and {@code j} of type
	 * {@link NumericType}, returns the the substring between {@code i} and
	 * {@code j} (excluded) of the input string. Indexes must be included
	 * between 0 and the length of the string {@code s} -1 and {@code i} must be
	 * less or equal than {@code j}. When {@code i == j}, the empty string is
	 * returned. The return type of this operator is
	 * {@link it.unive.lisa.type.StringType}.
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
