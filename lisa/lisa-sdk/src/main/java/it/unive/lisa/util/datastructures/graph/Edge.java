package it.unive.lisa.util.datastructures.graph;

/**
 * An edge of a {@link Graph}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <G> the type of {@link Graph} this edge can be used with
 * @param <N> the type of {@link Node}s connected to this edge
 * @param <E> the type of this edge
 */
public interface Edge<G extends Graph<G, N, E>, N extends Node<G, N, E>, E extends Edge<G, N, E>> {

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
	 * Accepts the given {@link GraphVisitor}. Implementors of this method are
	 * responsible for invoking {@link GraphVisitor#visit(Object, Graph, Edge)}
	 * on this edge. The visiting should stop if such call returns
	 * {@code false}.
	 * 
	 * @param <V>     the type of auxiliary tool that {@code visitor} can use
	 * @param visitor the visitor that is visiting the {@link Graph} containing
	 *                    this edge
	 * @param tool    the auxiliary tool that {@code visitor} can use
	 * 
	 * @return whether or not the visiting should stop when this call returns,
	 *             as decided by the visitor itself
	 */
	<V> boolean accept(
			GraphVisitor<G, N, E, V> visitor,
			V tool);
}
