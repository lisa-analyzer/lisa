package it.unive.lisa.analysis.nonrelational.value;

import it.unive.lisa.analysis.nonrelational.NonRelationalDomain;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.type.Type;
import java.util.Set;

/**
 * A non-relational type domain, that is able to compute the types of a
 * {@link ValueExpression} by knowing the types of all program variables.
 * Instances of this class can be wrapped inside an {@link TypeEnvironment} to
 * represent runtime types of individual {@link Identifier}s.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <T> the concrete type of the domain
 */
public interface NonRelationalTypeDomain<T extends NonRelationalTypeDomain<T>>
		extends NonRelationalDomain<T, ValueExpression, TypeEnvironment<T>> {

	/**
	 * Yields the set containing the types held by this instance.
	 * 
	 * @return the set of types inside this instance
	 */
	Set<Type> getRuntimeTypes();
}
