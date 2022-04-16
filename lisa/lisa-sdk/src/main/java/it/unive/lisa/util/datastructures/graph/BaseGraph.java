package it.unive.lisa.util.datastructures.graph;

import it.unive.lisa.outputs.serializableGraph.SerializableGraph;
import it.unive.lisa.outputs.serializableGraph.SerializableValue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.function.Function;

/**
 * A generic {@link Graph}, backed by an {@link AdjacencyMatrix}.<br>
 * <br>
 * Note that this class does not define {@link #equals(Object)} nor
 * {@link #hashCode()}, since we leave the decision to be unique instances to
 * implementers.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <G> the type of this graph
 * @param <N> the type of {@link Node}s in this graph
 * @param <E> the type of {@link Edge}s in this graph
 */
public abstract class BaseGraph<G extends BaseGraph<G, N, E>, N extends Node<G, N, E>, E extends Edge<G, N, E>>
		implements Graph<G, N, E> {

	/**
	 * The adjacency matrix of this graph, mapping nodes to the collection of
	 * edges attached to it.
	 */
	protected final AdjacencyMatrix<G, N, E> adjacencyMatrix;

	/**
	 * The nodes of this graph that are entrypoints, that is, that can be
	 * executed from other graphs.
	 */
	protected final Collection<N> entrypoints;

	/**
	 * Builds the graph.
	 */
	protected BaseGraph() {
		this.adjacencyMatrix = new AdjacencyMatrix<>();
		this.entrypoints = new HashSet<>();
	}

	/**
	 * Builds the graph.
	 * 
	 * @param entrypoints     the nodes of this graph that will be reachable
	 *                            from other graphs
	 * @param adjacencyMatrix the matrix containing all the nodes and the edges
	 *                            that will be part of this graph
	 */
	protected BaseGraph(Collection<N> entrypoints, AdjacencyMatrix<G, N, E> adjacencyMatrix) {
		this.adjacencyMatrix = adjacencyMatrix;
		this.entrypoints = entrypoints;
	}

	/**
	 * Clones the given graph.
	 * 
	 * @param other the original graph
	 */
	protected BaseGraph(G other) {
		this.adjacencyMatrix = new AdjacencyMatrix<>(other.adjacencyMatrix);
		this.entrypoints = new ArrayList<>(other.entrypoints);
	}

	/**
	 * Yields the adjacency matrix backing this graph.
	 * 
	 * @return the matrix
	 */
	public AdjacencyMatrix<G, N, E> getAdjacencyMatrix() {
		return adjacencyMatrix;
	}

	@Override
	public Collection<N> getEntrypoints() {
		return entrypoints;
	}

	@Override
	public Collection<N> getNodes() {
		return adjacencyMatrix.getNodes();
	}

	@Override
	public Collection<E> getEdges() {
		return adjacencyMatrix.getEdges();
	}

	@Override
	public void addNode(N node) {
		addNode(node, false);
	}

	@Override
	public void addNode(N node, boolean entrypoint) {
		adjacencyMatrix.addNode(node);
		if (entrypoint)
			this.entrypoints.add(node);
	}

	@Override
	public void addEdge(E edge) {
		adjacencyMatrix.addEdge(edge);
	}

	@Override
	public int getNodesCount() {
		return getNodes().size();
	}

	@Override
	public int getEdgesCount() {
		return getEdges().size();
	}

	@Override
	public boolean containsNode(N node) {
		return adjacencyMatrix.containsNode(node);
	}

	@Override
	public boolean containsEdge(E edge) {
		return adjacencyMatrix.containsEdge(edge);
	}

	@Override
	public E getEdgeConnecting(N source, N destination) {
		return adjacencyMatrix.getEdgeConnecting(source, destination);
	}

	@Override
	public Collection<E> getEdgesConnecting(N source, N destination) {
		return adjacencyMatrix.getEdgesConnecting(source, destination);
	}

	@Override
	public Collection<E> getIngoingEdges(N node) {
		return adjacencyMatrix.getIngoingEdges(node);
	}

	@Override
	public Collection<E> getOutgoingEdges(N node) {
		return adjacencyMatrix.getOutgoingEdges(node);
	}

	@Override
	public Collection<N> followersOf(N node) {
		return adjacencyMatrix.followersOf(node);
	}

	@Override
	public Collection<N> predecessorsOf(N node) {
		return adjacencyMatrix.predecessorsOf(node);
	}

	@Override
	public SerializableGraph toSerializableGraph(Function<N, SerializableValue> descriptionGenerator) {
		throw new UnsupportedOperationException(getClass().getName() + " does not provide a serialization logic");
	}

	@Override
	public boolean isEqualTo(G graph) {
		if (this == graph)
			return true;
		if (graph == null)
			return false;
		if (getClass() != graph.getClass())
			return false;
		if (entrypoints == null) {
			if (graph.entrypoints != null)
				return false;
		} else if (!entrypoints.equals(graph.entrypoints))
			return false;
		if (adjacencyMatrix == null) {
			if (graph.adjacencyMatrix != null)
				return false;
		} else if (!adjacencyMatrix.equals(graph.adjacencyMatrix))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return adjacencyMatrix.toString();
	}
}
