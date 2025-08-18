package it.unive.lisa.symbolic.value.operator.binary;

import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.operator.OverflowingOperator;
import it.unive.lisa.symbolic.value.operator.RemainderOperator;
import it.unive.lisa.type.NumericType;

/**
 * Given two expressions that both evaluate to numeric values, a
 * {@link BinaryExpression} using this operator computes the arithmetic
 * remainder (the remainder of the division between the two operands and taking
 * the sign of the dividend) of those values. Both arguments and results are
 * expected to be 16-bits numbers, and this operation thus can
 * overflow/underflow.<br>
 * <br>
 * First argument expression type: {@link NumericType} (16-bit)<br>
 * Second argument expression type: {@link NumericType} (16-bit)<br>
 * Computed expression type: {@link NumericType} (16-bit)
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Numeric16BitRem extends NumericOperation implements RemainderOperator, OverflowingOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final Numeric16BitRem INSTANCE = new Numeric16BitRem();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected Numeric16BitRem() {
	}

	@Override
	public String toString() {
		return "%";
	}

}
