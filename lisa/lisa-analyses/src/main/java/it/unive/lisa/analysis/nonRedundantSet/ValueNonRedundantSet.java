package it.unive.lisa.analysis.nonRedundantSet;

import java.util.Set;

import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;

/**
 * This class expands the {@link NonRedundantPowerset} class fixing the type of elements in the set
 * to be {@link ValueDomain}, the {@link SymbolicExpression} processable to be {@link ValueExpression} and the manageable {@link Identifier} 
 * to be all the {@link Identifier}.
 *
 * @param <T> the concrete type of the elements in the sets
 */
public class ValueNonRedundantSet<T extends ValueDomain<T>> 
	extends NonRedundantPowerset<ValueNonRedundantSet<T>, T, ValueExpression, Identifier>  {

	/**
	 * Builds the value non redundant set.
	 *
	 * @param elements  the set of elements of the set
	 * @param isTop whether or not the element is the top element
	 * @param valueDomain an element representing the types of elements in the set
	 */
	public ValueNonRedundantSet(Set<T> elements, boolean isTop, T valueDomain) {
		super(elements, isTop, valueDomain);
	}

	@Override
	public ValueNonRedundantSet<T> mk(Set<T> set, boolean isTop, T valueDomain) {
		return new ValueNonRedundantSet<>(set, isTop, valueDomain);
	}

}
