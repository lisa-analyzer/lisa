package it.unive.lisa.analysis;

import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import java.util.ArrayList;
import java.util.Collection;

/**
 * An abstract domain of the analysis. Instances of this class must process
 * information about the whole program memory: values, types, and memory
 * structure should be modeled and taken into account by this domain.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <L> the type of {@link AbstractLattice} that this domain works with
 */
public interface AbstractDomain<
		L extends AbstractLattice<L>>
		extends
		SemanticDomain<L, L, SymbolicExpression, Identifier> {

	/**
	 * Builds a {@link SemanticOracle} that can be used either by sub-domains or
	 * by the analysis itself to query the current state of the analysis.
	 *
	 * @param state   the current state of the analysis
	 * @param context the interprocedural context where the oracle will be used,
	 *                    that will be exposed through
	 *                    {@link SemanticOracle#getContext()}
	 * 
	 * @return a {@link SemanticOracle} instance
	 */
	SemanticOracle makeOracle(
			L state,
			ScopeId<?> context);

	/**
	 * Yields all the instances of a specific domain, of class {@code domain},
	 * contained inside this domain, also recursively querying inner domains
	 * (enabling retrieval through combinations of domains, or through domains
	 * wrapping other domains). This is the counterpart, for domains, of
	 * {@link Lattice#getAllLatticeInstances(Class)}, and lets code that needs a
	 * specific domain (e.g., the value domain of a
	 * {@link SimpleAbstractDomain}) retrieve it without depending on how the
	 * domain of the analysis is built.<br>
	 * <br>
	 * The default implementation of this method returns a singleton collection
	 * containing {@code this} if it is an instance of {@code domain}, and an
	 * empty collection otherwise.
	 * 
	 * @param <T>    the type of domain to retrieve
	 * @param domain the class of the domain instances to retrieve
	 * 
	 * @return the instances of that domain
	 */
	default <T> Collection<T> getAllDomainInstances(
			Class<T> domain) {
		Collection<T> result = new ArrayList<>();
		if (domain.isInstance(this))
			result.add(domain.cast(this));
		return result;
	}

	/**
	 * Yields the first instance of a specific domain, of class {@code domain},
	 * contained inside this domain, as returned by
	 * {@link #getAllDomainInstances(Class)}.
	 * 
	 * @param <T>    the type of domain to retrieve
	 * @param domain the class of the domain instance to retrieve
	 * 
	 * @return the instance of that domain, or {@code null} if there is none
	 */
	default <T> T getDomainInstance(
			Class<T> domain) {
		Collection<T> all = getAllDomainInstances(domain);
		return all.isEmpty() ? null : all.iterator().next();
	}

}
