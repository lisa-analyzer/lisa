package it.unive.lisa.analysis.nonrelational.type;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.Environment;
import it.unive.lisa.analysis.type.TypeLattice;
import it.unive.lisa.symbolic.value.Identifier;
import java.util.Map;

/**
 * An {@link Environment} that is also a {@link TypeLattice}, tracking types of
 * variables and heap locations.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <L> the type of the lattice used to represent the types stored in this
 *                environment
 */
public class TypeEnvironment<
		L extends TypeValue<L>> extends Environment<L, TypeEnvironment<L>> implements TypeLattice<TypeEnvironment<L>> {

	/**
	 * Builds an empty environment.
	 * 
	 * @param domain a singleton instance to be used during semantic operations
	 *                   to retrieve top and bottom values
	 */
	public TypeEnvironment(
			L domain) {
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
	public TypeEnvironment(
			L domain,
			Map<Identifier, L> function) {
		super(domain, function);
	}

	@Override
	public TypeEnvironment<L> top() {
		return isTop() ? this : new TypeEnvironment<>(lattice.top(), null);
	}

	@Override
	public TypeEnvironment<L> bottom() {
		return isBottom() ? this : new TypeEnvironment<>(lattice.bottom(), null);
	}

	@Override
	public TypeEnvironment<L> mk(
			L lattice,
			Map<Identifier, L> function) {
		return new TypeEnvironment<>(lattice, function);
	}

	@Override
	public TypeEnvironment<L> store(
			Identifier target,
			Identifier source)
			throws SemanticException {
		if (isTop() || isBottom() || function == null || !function.containsKey(source))
			return this;
		return putState(target, getState(source));
	}

}
