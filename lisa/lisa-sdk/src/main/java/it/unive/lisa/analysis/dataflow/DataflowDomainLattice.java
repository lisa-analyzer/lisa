package it.unive.lisa.analysis.dataflow;

import it.unive.lisa.analysis.value.ValueLattice;
import java.util.Set;

/**
 * A {@link ValueLattice} that is used to represent a dataflow domain. A
 * dataflow domain lattice is a set that contains instances of
 * {@link DataflowElement}, which can be retrieved through
 * {@link #getDataflowElements()}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <L> the concrete type of {@link DataflowDomainLattice}
 * @param <E> the concrete type of {@link DataflowElement} contained in this
 *                lattice
 */
public interface DataflowDomainLattice<L extends DataflowDomainLattice<L, E>,
		E extends DataflowElement<E>> extends ValueLattice<L> {

	/**
	 * Yields the {@link DataflowElement}s contained in this domain instance.
	 * 
	 * @return the elements
	 */
	Set<E> getDataflowElements();

	/**
	 * Updates this domain instance by removing the elements in {@code killed}
	 * and adding the elements in {@code generated}.
	 * 
	 * @param killed    the elements to remove
	 * @param generated the elements to add
	 * 
	 * @return the updated domain instance
	 */
	L update(
			Set<E> killed,
			Set<E> generated);

}
