package it.unive.lisa.symbolic.value.operator.binary;

import it.unive.lisa.caches.Caches;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.operator.BitwiseOperator;
import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.collections.externalSet.ExternalSet;

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
public class BitwiseUnsignedShiftRight implements BitwiseOperator, BinaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final BitwiseUnsignedShiftRight INSTANCE = new BitwiseUnsignedShiftRight();

	private BitwiseUnsignedShiftRight() {
	}

	@Override
	public String toString() {
		return ">>>";
	}

	@Override
	public ExternalSet<Type> typeInference(ExternalSet<Type> left, ExternalSet<Type> right) {
		if (left.noneMatch(Type::isNumericType) || right.noneMatch(Type::isNumericType))
			return Caches.types().mkEmptySet();
		ExternalSet<Type> set = NumericType.commonNumericalType(left, right);
		if (set.isEmpty())
			return Caches.types().mkEmptySet();
		return set;
	}
}
