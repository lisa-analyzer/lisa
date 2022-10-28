package it.unive.lisa.symbolic.value.operator.binary;

import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.operator.ComparisonOperator;
import it.unive.lisa.type.BooleanType;
import it.unive.lisa.type.NumericType;

/**
 * Given two expressions that both evaluate to numeric values, a
 * {@link BinaryExpression} using this operator checks if the value of the first
 * argument is greater or equal than the value of the right-hand side.<br>
 * <br>
 * First argument expression type: {@link NumericType}<br>
 * Second argument expression type: {@link NumericType}<br>
 * Computed expression type: {@link BooleanType}
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class ComparisonGe extends NumericComparison {

	/**
	 * The singleton instance of this class.
	 */
	public static final ComparisonGe INSTANCE = new ComparisonGe();

	private ComparisonGe() {
	}

	@Override
	public String toString() {
		return ">=";
	}

	@Override
	public ComparisonOperator opposite() {
		return ComparisonLt.INSTANCE;
	}
}
