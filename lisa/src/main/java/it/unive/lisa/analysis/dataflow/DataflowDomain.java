package it.unive.lisa.analysis.dataflow;

import it.unive.lisa.analysis.ValueDomain;
import java.util.Collection;

/**
 * A dataflow domain that collects instances of {@link DataflowElement}. A
 * dataflow domain is a value domain that is represented as a set of elements,
 * that can be retrieved through {@link #getDataflowElements()}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <D> the concrete type of {@link DataflowDomain}
 * @param <E> the concrete type of {@link DataflowElement} contained in this
 *                domain
 */
public interface DataflowDomain<D extends DataflowDomain<D, E>, E extends DataflowElement<D, E>>
		extends ValueDomain<D> {

	/**
	 * Yields all the {@link DataflowElement}s contained in this domain.
	 * 
	 * @return the elements
	 */
	Collection<E> getDataflowElements();
}
