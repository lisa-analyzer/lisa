package it.unive.lisa.symbolic.value.operator.binary;

import it.unive.lisa.symbolic.value.operator.ComparisonOperator;
import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Collections;
import java.util.Set;

/**
 * A common implementation for classes implementing {@link BinaryOperator} and
 * {@link ComparisonOperator}, providing a
 * {@link #typeInference(TypeSystem, Set, Set)} implementation that returns an
 * empty set if no {@link NumericType} can be found in one of the arguments or
 * if {@link NumericType#commonNumericalType(Set, Set)} returns an empty set,
 * and a singleton set containing {@link TypeSystem#getBooleanType()} otherwise.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class NumericComparison implements ComparisonOperator, BinaryOperator {

	@Override
	public Set<Type> typeInference(TypeSystem types, Set<Type> left, Set<Type> right) {
		if (left.stream().noneMatch(Type::isNumericType) || right.stream().noneMatch(Type::isNumericType))
			return Collections.emptySet();
		Set<Type> set = NumericType.commonNumericalType(left, right);
		if (set.isEmpty())
			return Collections.emptySet();
		return Collections.singleton(types.getBooleanType());
	}
}
