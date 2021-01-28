package it.unive.lisa.util.datastructures.graph;

import it.unive.lisa.analysis.AbstractState;
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
 * @param <N> the type of {@link Node}s connected to this edge
 * @param <E> the type of this edge
 * @param <G> the type of {@link Graph} this edge can be used with
 */
public interface SemanticEdge<N extends Node<N, E, G>, E extends SemanticEdge<N, E, G>, G extends Graph<G, N, E>>
		extends Edge<N, E, G> {

	/**
	 * Traverses this edge, optionally modifying the given {@code sourceState}
	 * by applying semantic assumptions.
	 * 
	 * @param <A>         the concrete {@link AbstractState} instance
	 * @param <H>         the concrete {@link HeapDomain} instance
	 * @param <V>         the concrete {@link ValueDomain} instance
	 * @param sourceState the {@link AnalysisState} computed at the source of
	 *                        this edge
	 * 
	 * @return the {@link AnalysisState} after traversing this edge
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	<A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> AnalysisState<A, H, V> traverse(
					AnalysisState<A, H, V> sourceState)
					throws SemanticException;
}
