package it.unive.lisa.symbolic.value.operator.binary;

import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.type.NumericType;

/**
 * Given two expressions that both evaluate to numeric values, a
 * {@link BinaryExpression} using this operator computes the arctangent of the
 * quotient of its arguments, i.e., {@code atan2(y, x)}.<br>
 * <br>
 * First argument expression type: {@link NumericType}<br>
 * Second argument expression type: {@link NumericType}<br>
 * Computed expression type: {@link NumericType}
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class NumericAtan2
		extends
		NumericOperation {

	/**
	 * The singleton instance of this class.
	 */
	public static final NumericAtan2 INSTANCE = new NumericAtan2();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected NumericAtan2() {
	}

	@Override
	public String toString() {
		return "atan2";
	}

}
