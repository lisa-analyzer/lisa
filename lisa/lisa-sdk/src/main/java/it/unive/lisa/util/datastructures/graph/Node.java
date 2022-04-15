package it.unive.lisa.util.datastructures.graph;

/**
 * A node of a {@link Graph}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <G> the type of {@link Graph} this node can be used with
 * @param <N> the type of this node
 * @param <E> the type of {@link Edge} that this node can be connected to
 */
public interface Node<G extends Graph<G, N, E>, N extends Node<G, N, E>, E extends Edge<G, N, E>> {

	/**
	 * Accepts the given {@link GraphVisitor}. Implementors of this method are
	 * responsible for invoking {@link GraphVisitor#visit(Object, Graph, Node)}
	 * on this node after {@link #accept(GraphVisitor, Object)} has been invoked
	 * on all nested nodes, if any. The visiting should stop at the first of
	 * such calls that return {@code false}.
	 * 
	 * @param <V>     the type of auxiliary tool that {@code visitor} can use
	 * @param visitor the visitor that is visiting the {@link Graph} containing
	 *                    this node
	 * @param tool    the auxiliary tool that {@code visitor} can use
	 * 
	 * @return whether or not the visiting should stop when this call returns,
	 *             as decided by the visitor itself
	 */
	<V> boolean accept(GraphVisitor<G, N, E, V> visitor, V tool);
}
