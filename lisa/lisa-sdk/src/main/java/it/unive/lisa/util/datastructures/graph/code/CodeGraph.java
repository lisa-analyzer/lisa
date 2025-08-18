package it.unive.lisa.util.datastructures.graph.code;

import it.unive.lisa.outputs.serializableGraph.SerializableGraph;
import it.unive.lisa.outputs.serializableGraph.SerializableValue;
import it.unive.lisa.util.datastructures.graph.Graph;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.function.BiFunction;

/**
 * A {@link Graph} that contains a list of nodes, backed by a {@link NodeList}.
 * The characteristic of this graph is that a nodes are mostly sequential, and
 * can be almost perfectly stored as a list. The {@link NodeList} backing this
 * graph also supports custom edges.<br>
 * <br>
 * Note that this class does not define {@link #equals(Object)} nor
 * {@link #hashCode()}, since we leave the decision to be unique instances to
 * implementers.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <G> the type of this graph
 * @param <N> the type of {@link CodeNode}s in this graph
 * @param <E> the type of {@link CodeEdge}s in this graph
 */
public abstract class CodeGraph<G extends CodeGraph<G, N, E>, N extends CodeNode<G, N, E>, E extends CodeEdge<G, N, E>>
		implements
		Graph<G, N, E> {

	/**
	 * The node list of this graph.
	 */
	protected final NodeList<G, N, E> list;

	/**
	 * The nodes of this graph that are entrypoints, that is, that can be
	 * executed from other graphs.
	 */
	protected final Collection<N> entrypoints;

	/**
	 * Builds the graph.
	 * 
	 * @param sequentialSingleton an instance of an edge of this list that can
	 *                                be used to invoke
	 *                                {@link CodeEdge#newInstance(CodeNode, CodeNode)}
	 *                                to obtain instances of sequential edges
	 */
	protected CodeGraph(
			E sequentialSingleton) {
		this.list = new NodeList<>(sequentialSingleton);
		this.entrypoints = new HashSet<>();
	}

	/**
	 * Builds the graph.
	 * 
	 * @param entrypoints the nodes of this graph that will be reachable from
	 *                        other graphs
	 * @param nodes       the list of nodes contained in this graph
	 */
	protected CodeGraph(
			Collection<N> entrypoints,
			NodeList<G, N, E> nodes) {
		this.list = nodes;
		this.entrypoints = entrypoints;
	}

	/**
	 * Clones the given graph.
	 * 
	 * @param other the original graph
	 */
	protected CodeGraph(
			G other) {
		this.list = new NodeList<>(other.list);
		this.entrypoints = new ArrayList<>(other.entrypoints);
	}

	/**
	 * Yields the node list backing this graph.
	 * 
	 * @return the list
	 */
	public NodeList<G, N, E> getNodeList() {
		return list;
	}

	@Override
	public Collection<N> getEntrypoints() {
		return entrypoints;
	}

	@Override
	public Collection<N> getNodes() {
		return list.getNodes();
	}

	@Override
	public Collection<E> getEdges() {
		return list.getEdges();
	}

	@Override
	public void addNode(
			N node) {
		addNode(node, false);
	}

	@Override
	public void addNode(
			N node,
			boolean entrypoint) {
		list.addNode(node);
		if (entrypoint)
			this.entrypoints.add(node);
	}

	@Override
	public void addEdge(
			E edge) {
		list.addEdge(edge);
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
	public boolean containsNode(
			N node) {
		return list.containsNode(node);
	}

	@Override
	public boolean containsEdge(
			E edge) {
		return list.containsEdge(edge);
	}

	@Override
	public E getEdgeConnecting(
			N source,
			N destination) {
		return list.getEdgeConnecting(source, destination);
	}

	@Override
	public Collection<E> getEdgesConnecting(
			N source,
			N destination) {
		return list.getEdgesConnecting(source, destination);
	}

	@Override
	public Collection<E> getIngoingEdges(
			N node) {
		return list.getIngoingEdges(node);
	}

	@Override
	public Collection<E> getOutgoingEdges(
			N node) {
		return list.getOutgoingEdges(node);
	}

	@Override
	public Collection<N> followersOf(
			N node) {
		return list.followersOf(node);
	}

	@Override
	public Collection<N> predecessorsOf(
			N node) {
		return list.predecessorsOf(node);
	}

	@Override
	public SerializableGraph toSerializableGraph(
			BiFunction<G, N, SerializableValue> descriptionGenerator) {
		throw new UnsupportedOperationException(getClass().getName() + " does not provide a serialization logic");
	}

	@Override
	public boolean isEqualTo(
			G graph) {
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
		if (list == null) {
			if (graph.list != null)
				return false;
		} else if (!list.equals(graph.list))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return list.toString();
	}

}
