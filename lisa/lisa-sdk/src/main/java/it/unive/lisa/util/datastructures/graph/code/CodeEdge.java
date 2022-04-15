package it.unive.lisa.util.datastructures.graph.code;

import it.unive.lisa.util.datastructures.graph.Edge;

public interface CodeEdge<G extends CodeGraph<G, N, E>, N extends CodeNode<G, N, E>, E extends CodeEdge<G, N, E>>
		extends Edge<G, N, E>, Comparable<E> {

	/**
	 * Yields {@code true} if and only if this edge represent unconditional
	 * execution flow between its bounds. This means that (i) it could be
	 * simplified if one of the nodes connected to it is simplified (i.e.,
	 * removed from the graph), and (ii) the graph containing it can avoid
	 * storing the edge if its bounds are subsequent.
	 * 
	 * @return whether or not this edge can be simplified
	 */
	boolean isUnconditional();

	/**
	 * Builds a new instance of this edge, connecting the given nodes.
	 * 
	 * @param source      the source node
	 * @param destination the destination node
	 * 
	 * @return a new instance of this edge, connecting the given nodes
	 */
	E newInstance(N source, N destination);
}
