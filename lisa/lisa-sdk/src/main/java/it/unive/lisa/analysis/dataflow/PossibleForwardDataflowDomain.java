package it.unive.lisa.analysis.dataflow;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.SetLattice;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.collections4.SetUtils;

/**
 * A {@link DataflowDomain} for <b>forward</b> and <b>possible</b> dataflow
 * analysis. Being possible means that this domain is an instance of
 * {@link SetLattice}, i.e., is a set whose join operation is the set union.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <E> the type of {@link DataflowElement} contained in this domain
 */
public class PossibleForwardDataflowDomain<E extends DataflowElement<PossibleForwardDataflowDomain<E>, E>> extends
		DataflowDomain<PossibleForwardDataflowDomain<E>, E> {

	/**
	 * Builds an empty domain.
	 * 
	 * @param domain a singleton instance to be used during semantic operations
	 *                   to perform <i>kill</i> and <i>gen</i> operations
	 */
	public PossibleForwardDataflowDomain(E domain) {
		super(domain, new HashSet<>(), true, false);
	}

	private PossibleForwardDataflowDomain(E domain, Set<E> elements, boolean isTop, boolean isBottom) {
		super(domain, elements, isTop, isBottom);
	}

	@Override
	public PossibleForwardDataflowDomain<E> mk(E domain, Set<E> elements, boolean isTop, boolean isBottom) {
		return new PossibleForwardDataflowDomain<>(domain, elements, isTop, isBottom);
	}

	@Override
	public PossibleForwardDataflowDomain<E> lubAux(PossibleForwardDataflowDomain<E> other) throws SemanticException {
		Set<E> union = SetUtils.union(this.getDataflowElements(), other.getDataflowElements());
		return new PossibleForwardDataflowDomain<>(domain, union, false, false);
	}

	@Override
	public boolean lessOrEqualAux(PossibleForwardDataflowDomain<E> other) throws SemanticException {
		return other.getDataflowElements().containsAll(this.getDataflowElements());
	}

	@Override
	public PossibleForwardDataflowDomain<E> glbAux(PossibleForwardDataflowDomain<E> other) throws SemanticException {
		Set<E> intersection = SetUtils.intersection(this.getDataflowElements(), other.getDataflowElements());
		return new PossibleForwardDataflowDomain<>(domain, intersection, false, false);
	}
}
