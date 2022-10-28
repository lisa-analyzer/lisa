package it.unive.lisa.symbolic.value.operator.binary;

import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.type.NumericType;

/**
 * Given two expressions that both evaluate to numeric values, a
 * {@link BinaryExpression} using this operator computes the arithmetic
 * subtraction of those values. This operation does never
 * overflows/underflows.<br>
 * <br>
 * First argument expression type: {@link NumericType}<br>
 * Second argument expression type: {@link NumericType}<br>
 * Computed expression type: {@link NumericType}
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class NumericNonOverflowingSub extends NumericOperation implements SubtractionOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final NumericNonOverflowingSub INSTANCE = new NumericNonOverflowingSub();

	private NumericNonOverflowingSub() {
	}

	@Override
	public String toString() {
		return "-";
	}
}
