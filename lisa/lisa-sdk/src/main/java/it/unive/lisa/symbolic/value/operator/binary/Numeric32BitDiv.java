package it.unive.lisa.symbolic.value.operator.binary;

import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.operator.DivisionOperator;
import it.unive.lisa.symbolic.value.operator.OverflowingOperator;
import it.unive.lisa.type.NumericType;

/**
 * Given two expressions that both evaluate to numeric values, a
 * {@link BinaryExpression} using this operator computes the arithmetic division
 * of those values. Both arguments and results are expected to be 32-bits
 * numbers, and this operation thus can overflow/underflow.<br>
 * <br>
 * First argument expression type: {@link NumericType} (32-bit)<br>
 * Second argument expression type: {@link NumericType} (32-bit)<br>
 * Computed expression type: {@link NumericType} (32-bit)
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Numeric32BitDiv extends NumericOperation implements DivisionOperator, OverflowingOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final Numeric32BitDiv INSTANCE = new Numeric32BitDiv();

	private Numeric32BitDiv() {
	}

	@Override
	public String toString() {
		return "/";
	}
}
