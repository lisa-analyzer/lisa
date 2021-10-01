package it.unive.lisa.symbolic.value;

import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.StringType;

/**
 * A ternary {@link Operator} that can be applied to a triple of
 * {@link SymbolicExpression}s.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public enum TernaryOperator implements Operator {

	/**
	 * Given three expressions that all evaluate to string values, a
	 * {@link TernaryExpression} using this operator computes a new string
	 * corresponding to the first argument's string where all occurrences of the
	 * second argument's string have been replaced with the third argument's
	 * string.<br>
	 * Note that:
	 * <ul>
	 * <li>if the first argument's string is empty, the empty string is
	 * returned</li>
	 * <li>if the second argument's string is empty, the third argument's string
	 * is added in any position of the first argument's string</li>
	 * <li>if the third argument's string is empty, occurrences of the second
	 * argument's string are simply removed from the first argument's
	 * string</li>
	 * </ul>
	 * <br>
	 * First argument expression type: {@link StringType}<br>
	 * Second argument expression type: {@link StringType}<br>
	 * Third argument expression type: {@link StringType}<br>
	 * Computed expression type: {@link StringType}
	 */
	STRING_REPLACE("strreplace"),

	/**
	 * Given three expressions, with the first one evaluating to a string value
	 * and the second and third one evaluating to integral numerical values, a
	 * {@link TernaryExpression} using this operator computes a new string
	 * corresponding to the portion of first argument's string starting at the
	 * second argument's number position (inclusive) and ending at the third
	 * argument's number position (exclusive).<br>
	 * Note that:
	 * <ul>
	 * <li>both second and third argument's numbers must be non-negative and
	 * less than the length of the first argument's string, with the second
	 * one's less or equal than the third one's</li>
	 * <li>if the second and third argument's numbers are equal, the empty
	 * string is returned</li>
	 * </ul>
	 * <br>
	 * First argument expression type: {@link StringType}<br>
	 * Second argument expression type: {@link NumericType}<br>
	 * Third argument expression type: {@link NumericType}<br>
	 * Computed expression type: {@link StringType}
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
