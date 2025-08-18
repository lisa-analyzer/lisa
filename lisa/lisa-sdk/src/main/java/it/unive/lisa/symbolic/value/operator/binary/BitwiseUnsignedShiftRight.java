package it.unive.lisa.symbolic.value.operator.binary;

import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.operator.BitwiseOperator;
import it.unive.lisa.type.NumericType;

/**
 * Given two expressions that both evaluate to numeric values, a
 * {@link BinaryExpression} using this operator computes a new number built with
 * the bits of the first argument's value shifted to the right by an amount
 * specified by the second argument's value. Excess bits on the right are
 * dropped, while new bits on the left are set to {@code 0}.<br>
 * <br>
 * First argument expression type: any {@link NumericType}<br>
 * Second argument expression type: any {@link NumericType}<br>
 * Computed expression type: {@link NumericType}
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class BitwiseUnsignedShiftRight extends NumericOperation implements BitwiseOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final BitwiseUnsignedShiftRight INSTANCE = new BitwiseUnsignedShiftRight();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected BitwiseUnsignedShiftRight() {
	}

	@Override
	public String toString() {
		return ">>>";
	}

}
