package it.unive.lisa.symbolic.value.operator.binary;

import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.operator.BitwiseOperator;
import it.unive.lisa.type.NumericType;

/**
 * Given two expressions that both evaluate to numeric values, a
 * {@link BinaryExpression} using this operator computes a new number built with
 * the bits of the first argument's value shifted to the right by an amount
 * specified by the second argument's value. Excess bits on the right are
 * dropped, while new bits on the left preserve the sign of the original first
 * argument's value: if it was negative, bits are set to {@code 1}, otherwise
 * they are set to {@code 0}.<br>
 * <br>
 * First argument expression type: any {@link NumericType}<br>
 * Second argument expression type: any {@link NumericType}<br>
 * Computed expression type: {@link NumericType}
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class BitwiseShiftRight extends NumericOperation implements BitwiseOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final BitwiseShiftRight INSTANCE = new BitwiseShiftRight();

	private BitwiseShiftRight() {
	}

	@Override
	public String toString() {
		return ">>";
	}
}
