package it.unive.lisa.symbolic.value.operator.binary;

import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.operator.ModuleOperator;
import it.unive.lisa.type.NumericType;

/**
 * Given two expressions that both evaluate to numeric values, a
 * {@link BinaryExpression} using this operator computes the arithmetic
 * remainder of those values. This operation does never
 * overflows/underflows.<br>
 * <br>
 * First argument expression type: {@link NumericType}<br>
 * Second argument expression type: {@link NumericType}<br>
 * Computed expression type: {@link NumericType}
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class NumericNonOverflowingMod extends NumericOperation implements ModuleOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final NumericNonOverflowingMod INSTANCE = new NumericNonOverflowingMod();

	private NumericNonOverflowingMod() {
	}

	@Override
	public String toString() {
		return "%";
	}
}