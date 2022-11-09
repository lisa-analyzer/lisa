package it.unive.lisa.analysis.value;

import it.unive.lisa.type.Type;
import java.util.Set;

/**
 * An domain that is able to determine the runtime types of an expression given
 * the runtime types of its operands.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <T> the concrete type of the {@link TypeDomain}
 */
public interface TypeDomain<T extends TypeDomain<T>> extends ValueDomain<T> {

	/**
	 * Yields the runtime types that this analysis inferred for the last
	 * computed expression.
	 * 
	 * @return the runtime types
	 */
	Set<Type> getInferredRuntimeTypes();

	/**
	 * Yields the dynamic type that this analysis inferred for the last computed
	 * expression. The dynamic type is the least common supertype of all its
	 * runtime types.
	 * 
	 * @return the dynamic type
	 */
	Type getInferredDynamicType();
}
