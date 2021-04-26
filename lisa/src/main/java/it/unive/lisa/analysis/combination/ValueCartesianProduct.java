package it.unive.lisa.analysis.combination;

import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;

/**
 * A Cartesian product between two non-communicating {@link ValueDomain}s (i.e.,
 * no exchange of information between the abstract domains) assigning
 * {@link Identifier}s and handling {@link ValueExpression}s.
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 *
 * @param <T1> the concrete instance of the left-hand side value abstract domain
 *                 of the Cartesian product
 * @param <T2> the concrete instance of the right-hand side value abstract
 *                 domain of the Cartesian product
 */
public class ValueCartesianProduct<T1 extends ValueDomain<T1>, T2 extends ValueDomain<T2>>
		extends CartesianProduct<ValueCartesianProduct<T1, T2>, T1, T2, ValueExpression, Identifier>
		implements ValueDomain<ValueCartesianProduct<T1, T2>> {

	/**
	 * Builds the value Cartesian product.
	 * 
	 * @param left  the left-hand side of the value Cartesian product
	 * @param right the right-hand side of the value Cartesian product
	 */
	public ValueCartesianProduct(T1 left, T2 right) {
		super(left, right);
	}
	
	@Override
	protected ValueCartesianProduct<T1, T2> mk(T1 left, T2 right) {
		return new ValueCartesianProduct<>(left, right);
	}
}
