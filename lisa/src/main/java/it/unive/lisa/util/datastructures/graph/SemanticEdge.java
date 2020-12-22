package it.unive.lisa.util.datastructures.graph;

import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.ValueDomain;

/**
 * An extension of {@link Edge} that can modify abstract information when being
 * traversed.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <N> the type of nodes connected to this edge
 * @param <E> the type of this edge
 */
public interface SemanticEdge<N extends Node<N>, E extends SemanticEdge<N, E>> extends Edge<N, E> {

	/**
	 * Traverses this edge, optionally modifying the given {@code sourceState}
	 * by applying semantic assumptions.
	 * 
	 * @param <H>         the concrete {@link HeapDomain} instance
	 * @param <V>         the concrete {@link ValueDomain} instance
	 * @param sourceState the {@link AnalysisState} computed at the source of
	 *                        this edge
	 * 
	 * @return the {@link AnalysisState} after traversing this edge
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	<H extends HeapDomain<H>, V extends ValueDomain<V>> AnalysisState<H, V> traverse(AnalysisState<H, V> sourceState)
			throws SemanticException;
}
