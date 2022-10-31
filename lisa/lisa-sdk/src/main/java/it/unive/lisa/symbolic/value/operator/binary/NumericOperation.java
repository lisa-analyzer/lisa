package it.unive.lisa.symbolic.value.operator.binary;

import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Collections;
import java.util.Set;

/**
 * A common implementation for classes implementing {@link BinaryOperator},
 * providing a {@link #typeInference(TypeSystem, Set, Set)} implementation that
 * returns an empty set if no {@link NumericType}, and the result of
 * {@link NumericType#commonNumericalType(Set, Set)} otherwise.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class NumericOperation implements BinaryOperator {

	@Override
	public Set<Type> typeInference(TypeSystem types, Set<Type> left, Set<Type> right) {
		if (left.stream().noneMatch(Type::isNumericType) || right.stream().noneMatch(Type::isNumericType))
			return Collections.emptySet();
		Set<Type> set = NumericType.commonNumericalType(left, right);
		if (set.isEmpty())
			return Collections.emptySet();
		return set;
	}
}
