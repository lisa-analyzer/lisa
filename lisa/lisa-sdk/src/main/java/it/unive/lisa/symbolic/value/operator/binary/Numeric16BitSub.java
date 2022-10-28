package it.unive.lisa.symbolic.value.operator.binary;

import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.operator.OverflowingOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.type.NumericType;

/**
 * Given two expressions that both evaluate to numeric values, a
 * {@link BinaryExpression} using this operator computes the arithmetic
 * subtraction of those values. Both arguments and results are expected to be
 * 16-bits numbers, and this operation thus can overflow/underflow.<br>
 * <br>
 * First argument expression type: {@link NumericType} (16-bit)<br>
 * Second argument expression type: {@link NumericType} (16-bit)<br>
 * Computed expression type: {@link NumericType} (16-bit)
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Numeric16BitSub extends NumericOperation
		implements SubtractionOperator, OverflowingOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final Numeric16BitSub INSTANCE = new Numeric16BitSub();

	private Numeric16BitSub() {
	}

	@Override
	public String toString() {
		return "-";
	}
}