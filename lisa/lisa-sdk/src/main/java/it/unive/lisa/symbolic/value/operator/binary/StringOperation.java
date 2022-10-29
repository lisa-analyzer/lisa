package it.unive.lisa.symbolic.value.operator.binary;

import it.unive.lisa.symbolic.value.operator.StringOperator;
import it.unive.lisa.type.StringType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Collections;
import java.util.Set;

/**
 * A common implementation for classes implementing {@link BinaryOperator} and
 * {@link StringOperator}, providing a
 * {@link #typeInference(TypeSystem, Set, Set)} implementation that returns an
 * empty set if no {@link StringType} can be found in one of the arguments, and
 * a singleton set containing {@link #resultType(TypeSystem)} otherwise.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class StringOperation implements StringOperator, BinaryOperator {

	@Override
	public Set<Type> typeInference(TypeSystem types, Set<Type> left, Set<Type> right) {
		if (left.stream().noneMatch(Type::isStringType) || right.stream().noneMatch(Type::isStringType))
			return Collections.emptySet();
		return Collections.singleton(resultType(types));
	}

	/**
	 * Yields the {@link Type} of this operation's result.
	 * 
	 * @param types the type system knowing about the types of the
	 *                  currentprogram
	 * 
	 * @return the type
	 */
	protected abstract Type resultType(TypeSystem types);
}