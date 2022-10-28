package it.unive.lisa.symbolic.value.operator.binary;

import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.common.StringType;

/**
 * Given two expressions that both evaluate to string values, a
 * {@link BinaryExpression} using this operator computes the concatenation of
 * the string from the first argument with the one of the second argument.<br>
 * <br>
 * First argument expression type: {@link StringType}<br>
 * Second argument expression type: {@link StringType}<br>
 * Computed expression type: {@link StringType}
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class StringConcat extends StringOperation {

	/**
	 * The singleton instance of this class.
	 */
	public static final StringConcat INSTANCE = new StringConcat();

	private StringConcat() {
	}

	@Override
	public String toString() {
		return "strcat";
	}

	@Override
	protected Type resultType() {
		return StringType.INSTANCE;
	}
}