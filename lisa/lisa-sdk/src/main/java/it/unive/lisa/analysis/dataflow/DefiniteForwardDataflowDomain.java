package it.unive.lisa.analysis.dataflow;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.InverseSetLattice;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.collections4.SetUtils;

/**
 * A {@link DataflowDomain} for <b>forward</b> and <b>definite</b> dataflow
 * analysis. Being definite means that this domain is an instance of
 * {@link InverseSetLattice}, i.e., is a set whose join operation is the set
 * intersection.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <E> the type of {@link DataflowElement} contained in this domain
 */
public class DefiniteForwardDataflowDomain<E extends DataflowElement<DefiniteForwardDataflowDomain<E>, E>>
		extends DataflowDomain<DefiniteForwardDataflowDomain<E>, E> {

	/**
	 * Builds an empty domain.
	 * 
	 * @param domain a singleton instance to be used during semantic operations
	 *                   to perform <i>kill</i> and <i>gen</i> operations
	 */
	public DefiniteForwardDataflowDomain(E domain) {
		this(domain, new HashSet<>(), true, false);
	}

	private DefiniteForwardDataflowDomain(E domain, Set<E> elements, boolean isTop, boolean isBottom) {
		super(domain, elements, isTop, isBottom);
	}

	@Override
	public DefiniteForwardDataflowDomain<E> mk(E domain, Set<E> elements, boolean isTop, boolean isBottom) {
		return new DefiniteForwardDataflowDomain<>(domain, elements, isTop, isBottom);
	}

	@Override
	public DefiniteForwardDataflowDomain<E> lubAux(DefiniteForwardDataflowDomain<E> other) throws SemanticException {
		Set<E> intersection = SetUtils.intersection(this.getDataflowElements(), other.getDataflowElements());
		return new DefiniteForwardDataflowDomain<>(domain, intersection, false, false);
	}

	@Override
	public boolean lessOrEqualAux(DefiniteForwardDataflowDomain<E> other) throws SemanticException {
		return this.getDataflowElements().containsAll(other.getDataflowElements());
	}

	@Override
	public DefiniteForwardDataflowDomain<E> glbAux(DefiniteForwardDataflowDomain<E> other) throws SemanticException {
		Set<E> intersection = SetUtils.union(this.getDataflowElements(), other.getDataflowElements());
		return new DefiniteForwardDataflowDomain<>(domain, intersection, false, false);
	}
}
