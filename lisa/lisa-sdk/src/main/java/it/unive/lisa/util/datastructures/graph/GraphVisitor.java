package it.unive.lisa.util.datastructures.graph;

/**
 * A visitor of a {@link Graph}. Instances of this interface provide callbacks
 * to invoke when visiting the various components of a graph.<br>
 * <br>
 * The visiting starts by visiting the graph itself through
 * {@link #visit(Object, Graph)}, and then by visiting {@link Node}s and
 * {@link Edge}s independently, in no particular order, through
 * {@link #visit(Object, Graph, Node)} and {@link #visit(Object, Graph, Edge)}
 * respectively.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <G> the type of {@link Graph} that this visitor visits
 * @param <N> the type of {@link Node}s in the visited graph
 * @param <E> the type of {@link Edge}s in the visited graph
 * @param <V> the type of auxiliary tool that the callbacks provided by this
 *                visitor can use while visiting the graph
 */
public interface GraphVisitor<G extends Graph<G, N, E>, N extends Node<G, N, E>, E extends Edge<G, N, E>, V> {

	/**
	 * Visits a graph, inspecting its structure and signature, but
	 * <b>without</b> accessing its contents. The default implementation does
	 * nothing and returns {@code true}.
	 * 
	 * @param tool  the auxiliary tool that this visitor can use
	 * @param graph the graph being visited
	 * 
	 * @return whether or not the visiting should continue when this call
	 *             returns. If this method returns {@code false}, the visiting
	 *             will be interrupted
	 */
	default boolean visit(
			V tool,
			G graph) {
		return true;
	}

	/**
	 * Visits a node of the given graph. The default implementation does nothing
	 * and returns {@code true}.
	 * 
	 * @param tool  the auxiliary tool that this visitor can use
	 * @param graph the graph where the visited node belongs
	 * @param node  the node being visited
	 * 
	 * @return whether or not the visiting should continue when this call
	 *             returns. If this method returns {@code false}, the visiting
	 *             will be interrupted
	 */
	default boolean visit(
			V tool,
			G graph,
			N node) {
		return true;
	}

	/**
	 * Visits an edge of the given graph. The default implementation does
	 * nothing and returns {@code true}.
	 * 
	 * @param tool  the auxiliary tool that this visitor can use
	 * @param graph the graph where the visited edge belongs
	 * @param edge  the edge being visited
	 * 
	 * @return whether or not the visiting should continue when this call
	 *             returns. If this method returns {@code false}, the visiting
	 *             will be interrupted
	 */
	default boolean visit(
			V tool,
			G graph,
			E edge) {
		return true;
	}

	/**
	 * Yields whether or not, when visiting a compound node through
	 * {@link #visit(Object, Graph, Node)}, its sub-nodes should be visited
	 * before the node itself.
	 * 
	 * @return {@code true} if that condition holds
	 */
	default boolean visitSubNodesFirst() {
		return true;
	}
}
