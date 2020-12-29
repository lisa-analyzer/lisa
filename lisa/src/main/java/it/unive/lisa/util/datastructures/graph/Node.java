package it.unive.lisa.util.datastructures.graph;

/**
 * A node of a {@link Graph}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <N> the type of this node
 */
public interface Node<N extends Node<N, E, G>, E extends Edge<N, E, G>, G extends Graph<G, N, E>> {

	/**
	 * Sets the offset of this node to the given value, and then proceeds by
	 * setting the one of its nested nodes (if any) to subsequent values. The
	 * last offset used is returned.
	 * 
	 * @param offset the offset to set
	 * 
	 * @return the last offset used while setting the offsets of nested nodes
	 */
	int setOffset(int offset);

	/**
	 * Checks if this node is effectively equal to the given one, that is, if
	 * they have the same structure while potentially being different instances.
	 * 
	 * @param other the other node
	 * 
	 * @return {@code true} if this node and the given one are effectively
	 *             equals
	 */
	boolean isEqualTo(N other);

	<V extends VisitTool> boolean accept(GraphVisitor<G, N, E, V> visitor, V tool);
}
