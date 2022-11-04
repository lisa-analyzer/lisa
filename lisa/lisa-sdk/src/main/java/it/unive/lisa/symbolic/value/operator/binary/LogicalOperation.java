package it.unive.lisa.symbolic.value.operator.binary;

import it.unive.lisa.symbolic.value.operator.LogicalOperator;
import it.unive.lisa.type.BooleanType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Collections;
import java.util.Set;

/**
 * A common implementation for classes implementing {@link BinaryOperator} and
 * {@link LogicalOperator}, providing a
 * {@link #typeInference(TypeSystem, Set, Set)} implementation that returns an
 * empty set if no {@link BooleanType} can be found in one of the arguments, and
 * a singleton set containing {@link TypeSystem#getBooleanType()} otherwise.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class LogicalOperation implements LogicalOperator, BinaryOperator {

	@Override
	public Set<Type> typeInference(TypeSystem types, Set<Type> left, Set<Type> right) {
		if (left.stream().noneMatch(Type::isBooleanType) || right.stream().noneMatch(Type::isBooleanType))
			return Collections.emptySet();
		return Collections.singleton(types.getBooleanType());
	}
}
