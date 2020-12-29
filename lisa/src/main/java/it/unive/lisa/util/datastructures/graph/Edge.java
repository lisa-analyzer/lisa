package it.unive.lisa.util.datastructures.graph;

/**
 * An edge of a {@link Graph}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <N> the type of nodes connected to this edge
 * @param <E> the type of this edge
 */
public interface Edge<N extends Node<N, E, G>, E extends Edge<N, E, G>, G extends Graph<G, N, E>> {

	/**
	 * Yields the node where this edge originates.
	 * 
	 * @return the source node
	 */
	N getSource();

	/**
	 * Yields the node where this edge ends.
	 * 
	 * @return the destination node
	 */
	N getDestination();

	/**
	 * Checks if this edge is effectively equal to the given one, that is, if
	 * they have the same structure while potentially being different instances.
	 * This translates into comparing source and destination nodes with
	 * {@link Node#isEqualTo(Node)} instead of using
	 * {@link Object#equals(Object)}.
	 * 
	 * @param other the other edge
	 * 
	 * @return {@code true} if this edge and the given one are effectively
	 *             equals
	 */
	boolean isEqualTo(E other);

	/**
	 * Yields {@code true} if and only if this edge could be simplified if one
	 * of the nodes connected to it is simplified (i.e., removed from the
	 * graph).
	 * 
	 * @return whether or not this edge can be simplified
	 */
	boolean canBeSimplified();

	/**
	 * Builds a new instance of this edge, connecting the given nodes.
	 * 
	 * @param source      the source node
	 * @param destination the destination node
	 * 
	 * @return a new instance of this edge, connecting the given nodes
	 */
	E newInstance(N source, N destination);

	<V extends VisitTool> boolean accept(GraphVisitor<G, N, E, V> visitor, V tool);
}
