package it.unive.lisa.util.datastructures.graph;

import it.unive.lisa.outputs.serializableGraph.SerializableGraph;
import it.unive.lisa.outputs.serializableGraph.SerializableNodeDescription;
import it.unive.lisa.outputs.serializableGraph.SerializableValue;
import java.util.Collection;
import java.util.function.Function;

/**
 * Interface of a generic graph structure.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <G> the type of this graph
 * @param <N> the type of {@link Node}s in this graph
 * @param <E> the type of {@link Edge}s in this graph
 */
public interface Graph<G extends Graph<G, N, E>, N extends Node<G, N, E>, E extends Edge<G, N, E>> {

	/**
	 * Yields the nodes of this graph that are entrypoints, that is, roots of
	 * the graph. This usually contains the first node of this graph, but might
	 * also contain other ones.
	 * 
	 * @return the entrypoints of this graph
	 */
	Collection<N> getEntrypoints();

	/**
	 * Yields the set of nodes of this graph.
	 * 
	 * @return the collection of nodes
	 */
	Collection<N> getNodes();

	/**
	 * Yields the set of edges of this graph.
	 * 
	 * @return the collection of edges
	 */
	Collection<E> getEdges();

	/**
	 * Adds the given node to the set of nodes. This is equivalent to invoking
	 * {@link #addNode(Node, boolean)} with {@code false} as second parameter.
	 * 
	 * @param node the node to add
	 */
	void addNode(N node);

	/**
	 * Adds the given node to the set of nodes, optionally marking this as
	 * entrypoint (that is, root).
	 * 
	 * @param node       the node to add
	 * @param entrypoint if {@code true} causes the given node to be considered
	 *                       as an entrypoint.
	 */
	void addNode(N node, boolean entrypoint);

	/**
	 * Adds an edge to this graph.
	 * 
	 * @param edge the edge to add
	 * 
	 * @throws UnsupportedOperationException if the source or the destination of
	 *                                           the given edge are not part of
	 *                                           this graph
	 */
	void addEdge(E edge);

	/**
	 * Yields the total number of nodes of this graph.
	 * 
	 * @return the number of nodes
	 */
	int getNodesCount();

	/**
	 * Yields the total number of edges of this graph.
	 * 
	 * @return the number of edges
	 */
	int getEdgesCount();

	/**
	 * Yields {@code true} if the given node is contained in this graph.
	 * 
	 * @param node the node to check
	 * 
	 * @return {@code true} if the node is in this graph
	 */
	boolean containsNode(N node);

	/**
	 * Yields {@code true} if the given edge is contained in this graph.
	 * 
	 * @param edge the edge to check
	 * 
	 * @return {@code true} if the edge is in this graph
	 */
	boolean containsEdge(E edge);

	/**
	 * Yields the edge connecting the two given nodes, if any. Yields
	 * {@code null} if such edge does not exist, or if one of the two nodes is
	 * not inside this graph. If more than one edge connects the two nodes, this
	 * method returns one of them arbitrarily (but consistently: successive
	 * calls with the same parameters will always return the same edge). To
	 * retrieve all such edges, use {@link #getEdgesConnecting(Node, Node)}.
	 * 
	 * @param source      the source node
	 * @param destination the destination node
	 * 
	 * @return the edge connecting {@code source} to {@code destination}, or
	 *             {@code null}
	 */
	E getEdgeConnecting(N source, N destination);

	/**
	 * Yields all edges connecting the two given nodes, if any. Yields an empty
	 * collection if no edge exists, or if one of the two nodes is not inside
	 * this graph.
	 * 
	 * @param source      the source node
	 * @param destination the destination node
	 * 
	 * @return the edges connecting {@code source} to {@code destination}
	 */
	Collection<E> getEdgesConnecting(N source, N destination);

	/**
	 * Yields the ingoing edges to the given node.
	 * 
	 * @param node the node
	 * 
	 * @return the collection of ingoing edges
	 */
	Collection<E> getIngoingEdges(N node);

	/**
	 * Yields the outgoing edges from the given node.
	 * 
	 * @param node the node
	 * 
	 * @return the collection of outgoing edges
	 */
	Collection<E> getOutgoingEdges(N node);

	/**
	 * Yields the collection of the nodes that are followers of the given one,
	 * that is, all nodes such that there exist an edge in this control flow
	 * graph going from the given node to such node. Yields {@code null} if the
	 * node is not in this graph.
	 * 
	 * @param node the node
	 * 
	 * @return the collection of followers
	 */
	Collection<N> followersOf(N node);

	/**
	 * Yields the collection of the nodes that are predecessors of the given
	 * vertex, that is, all nodes such that there exist an edge in this control
	 * flow graph going from such node to the given one. Yields {@code null} if
	 * the node is not in this graph.
	 * 
	 * @param node the node
	 * 
	 * @return the collection of predecessors
	 */
	Collection<N> predecessorsOf(N node);

	/**
	 * Yields an instance of {@link SerializableGraph} built from this one. The
	 * default implementation of this method is equivalent to invoking
	 * {@link #toSerializableGraph(Function)} with {@code null} as argument.
	 * 
	 * @return a {@link SerializableGraph} instance
	 */
	public default SerializableGraph toSerializableGraph() {
		return toSerializableGraph(null);
	}

	/**
	 * Yields an instance of {@link SerializableGraph} built from this one. If
	 * {@code descriptionGenerator} is not {@code null},
	 * {@link SerializableNodeDescription} for each node will be generated using
	 * it.
	 * 
	 * @param descriptionGenerator the function to be used for generating node
	 *                                 descriptions, can be {@code null}
	 * 
	 * @return a {@link SerializableGraph} instance
	 */
	SerializableGraph toSerializableGraph(Function<N, SerializableValue> descriptionGenerator);

	/**
	 * Checks if this graph is effectively equal to the given one, that is, if
	 * they have the same structure while potentially being different instances.
	 * 
	 * @param graph the other graph
	 * 
	 * @return {@code true} if this graph and the given one are effectively
	 *             equals
	 */
	boolean isEqualTo(G graph);

	/**
	 * Accepts the given {@link GraphVisitor}. This method first invokes
	 * {@link GraphVisitor#visit(Object, Graph)} on this graph, and then
	 * proceeds by first invoking
	 * {@link GraphVisitor#visit(Object, Graph, Node)} on all the nodes in the
	 * order they are returned by {@link #getNodes()}, and later invoking
	 * {@link GraphVisitor#visit(Object, Graph, Edge)} on all the edges in the
	 * order they are returned by {@link #getEdges()}. The visiting stops at the
	 * first of such calls that return {@code false}.
	 * 
	 * @param <V>     the type of auxiliary tool that {@code visitor} can use
	 * @param visitor the visitor that is visiting the {@link Graph} containing
	 *                    this graph
	 * @param tool    the auxiliary tool that {@code visitor} can use
	 */
	@SuppressWarnings("unchecked")
	public default <V> void accept(GraphVisitor<G, N, E, V> visitor, V tool) {
		if (!visitor.visit(tool, (G) this))
			return;

		for (N node : getNodes())
			if (!node.accept(visitor, tool))
				return;

		for (E edge : getEdges())
			if (!edge.accept(visitor, tool))
				return;
	}
}
