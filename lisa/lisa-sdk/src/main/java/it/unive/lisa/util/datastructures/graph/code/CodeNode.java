package it.unive.lisa.util.datastructures.graph.code;

import it.unive.lisa.util.datastructures.graph.Node;

/**
 * A {@link Node} that can be used with a {@link CodeGraph}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <G> the type of the {@link CodeGraph}s this list can be used in
 * @param <N> the type of the {@link CodeNode}s in this list
 * @param <E> the type of the {@link CodeEdge}s in this list
 */
public interface CodeNode<G extends CodeGraph<G, N, E>, N extends CodeNode<G, N, E>, E extends CodeEdge<G, N, E>>
		extends
		Node<G, N, E>,
		Comparable<N> {
}
