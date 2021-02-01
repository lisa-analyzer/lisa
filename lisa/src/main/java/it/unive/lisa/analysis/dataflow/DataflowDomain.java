package it.unive.lisa.analysis.dataflow;

import java.util.Collection;

import it.unive.lisa.analysis.ValueDomain;

public interface DataflowDomain<D extends DataflowDomain<D, E>, E extends DataflowElement<D, E>> extends ValueDomain<D> {

	Collection<E> getDataflowElements();
}
