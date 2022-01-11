package it.unive.lisa.symbolic.value.operator.binary;

import it.unive.lisa.caches.Caches;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.operator.ComparisonOperator;
import it.unive.lisa.type.BooleanType;
import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.common.BoolType;
import it.unive.lisa.util.collections.externalSet.ExternalSet;

/**
 * Given two expressions that both evaluate to numeric values, a
 * {@link BinaryExpression} using this operator checks if the value of the first
 * argument is less or equal than the value of the right-hand side.<br>
 * <br>
 * First argument expression type: {@link NumericType}<br>
 * Second argument expression type: {@link NumericType}<br>
 * Computed expression type: {@link BooleanType}
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class ComparisonLe implements ComparisonOperator, BinaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final ComparisonLe INSTANCE = new ComparisonLe();

	private ComparisonLe() {
	}

	@Override
	public String toString() {
		return "<=";
	}

	@Override
	public ComparisonOperator opposite() {
		return ComparisonGt.INSTANCE;
	}

	@Override
	public ExternalSet<Type> typeInference(ExternalSet<Type> left, ExternalSet<Type> right) {
		if (left.noneMatch(Type::isNumericType) || right.noneMatch(Type::isNumericType))
			return Caches.types().mkEmptySet();
		ExternalSet<Type> set = NumericType.commonNumericalType(left, right);
		if (set.isEmpty())
			return Caches.types().mkEmptySet();
		return Caches.types().mkSingletonSet(BoolType.INSTANCE);
	}
}
