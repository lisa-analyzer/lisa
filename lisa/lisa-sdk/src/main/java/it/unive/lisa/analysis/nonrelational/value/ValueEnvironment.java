package it.unive.lisa.analysis.nonrelational.value;

import it.unive.lisa.analysis.lattices.FunctionalLattice;
import it.unive.lisa.analysis.nonrelational.Environment;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import java.util.Map;

/**
 * An environment for a {@link NonRelationalValueDomain}, that maps
 * {@link Identifier}s to instances of such domain. This is a
 * {@link FunctionalLattice}, that is, it implements a function mapping keys
 * (identifiers) to values (instances of the domain), and lattice operations are
 * automatically lifted for individual elements of the environment if they are
 * mapped to the same key.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <T> the concrete instance of the {@link NonRelationalValueDomain}
 *                whose instances are mapped in this environment
 */
public class ValueEnvironment<T extends NonRelationalValueDomain<T>>
		extends
		Environment<ValueEnvironment<T>, ValueExpression, T>
		implements
		ValueDomain<ValueEnvironment<T>> {

	/**
	 * Builds an empty environment.
	 * 
	 * @param domain a singleton instance to be used during semantic operations
	 *                   to retrieve top and bottom values
	 */
	public ValueEnvironment(
			T domain) {
		super(domain);
	}

	/**
	 * Builds an environment containing the given mapping. If function is
	 * {@code null}, the new environment is the top environment if
	 * {@code lattice.isTop()} holds, and it is the bottom environment if
	 * {@code lattice.isBottom()} holds.
	 * 
	 * @param domain   a singleton instance to be used during semantic
	 *                     operations to retrieve top and bottom values
	 * @param function the function representing the mapping contained in the
	 *                     new environment; can be {@code null}
	 */
	public ValueEnvironment(
			T domain,
			Map<Identifier, T> function) {
		super(domain, function);
	}

	@Override
	public ValueEnvironment<T> mk(
			T lattice,
			Map<Identifier, T> function) {
		return new ValueEnvironment<>(lattice, function);
	}

	@Override
	public ValueEnvironment<T> top() {
		return isTop() ? this : new ValueEnvironment<>(lattice.top(), null);
	}

	@Override
	public ValueEnvironment<T> bottom() {
		return isBottom() ? this : new ValueEnvironment<>(lattice.bottom(), null);
	}
}
