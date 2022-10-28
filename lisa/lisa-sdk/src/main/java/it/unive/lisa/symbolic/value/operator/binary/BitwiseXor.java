package it.unive.lisa.symbolic.value.operator.binary;

import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.operator.BitwiseOperator;
import it.unive.lisa.type.NumericType;

/**
 * Given two expressions that both evaluate to numeric values, a
 * {@link BinaryExpression} using this operator computes the XOR operation
 * (i.e., setting each bit to {@code 1} only the corresponding bits of the
 * operands are different) on the arguments.<br>
 * <br>
 * First argument expression type: any {@link NumericType}<br>
 * Second argument expression type: any {@link NumericType}<br>
 * Computed expression type: {@link NumericType}
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class BitwiseXor extends NumericOperation implements BitwiseOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final BitwiseXor INSTANCE = new BitwiseXor();

	private BitwiseXor() {
	}

	@Override
	public String toString() {
		return "^";
	}
}
