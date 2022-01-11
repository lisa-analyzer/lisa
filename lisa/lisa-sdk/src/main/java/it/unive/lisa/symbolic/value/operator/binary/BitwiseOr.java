package it.unive.lisa.symbolic.value.operator.binary;

import it.unive.lisa.caches.Caches;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.operator.BitwiseOperator;
import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.collections.externalSet.ExternalSet;

/**
 * Given two expressions that both evaluate to numeric values, a
 * {@link BinaryExpression} using this operator computes the OR operation (i.e.,
 * setting each bit to {@code 1} only if at least one of the corresponding bits
 * of the operands are {@code 1}) on the arguments.<br>
 * <br>
 * First argument expression type: any {@link NumericType}<br>
 * Second argument expression type: any {@link NumericType}<br>
 * Computed expression type: {@link NumericType}
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class BitwiseOr implements BitwiseOperator, BinaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final BitwiseOr INSTANCE = new BitwiseOr();

	private BitwiseOr() {
	}

	@Override
	public String toString() {
		return "|";
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
