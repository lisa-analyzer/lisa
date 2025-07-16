package it.unive.lisa.util.datastructures.graph.code;

import it.unive.lisa.outputs.serializableGraph.SerializableGraph;
import it.unive.lisa.outputs.serializableGraph.SerializableValue;
import it.unive.lisa.util.datastructures.graph.Graph;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;

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
public abstract class CodeGraph<G extends CodeGraph<G, N, E>,
		N extends CodeNode<G, N, E>,
		E extends CodeEdge<G, N, E>>
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
		throw new UnsupportedOperationException(
				getClass().getName() + " does not provide a serialization logic");
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

	/**
	 * Simplifies the adjacency matrix beneath this graph, removing all nodes
	 * that are instances of {@code <T>} and rewriting the edge set accordingly.
	 * This method will throw an {@link UnsupportedOperationException} if one of
	 * the nodes being simplified has an outgoing edge that is not simplifiable,
	 * according to {@link CodeEdge#isUnconditional()}.
	 *
	 * @param target        the class of the {@link CodeNode} that needs to be
	 *                          simplified
	 * @param removedEdges  the collections of edges that got removed during the
	 *                          simplification, filled by this method (the
	 *                          collection will be cleared before simplifying)
	 * @param replacedEdges the map of edges that got replaced during the
	 *                          simplification, filled by this method (the map
	 *                          will be cleared before simplifying); each entry
	 *                          refers to a single simplified edge, and is in
	 *                          the form
	 *                          {@code <<ingoing removed, outgoing removed>, added>}
	 * 
	 * @return the set of nodes that have been simplified
	 * 
	 * @throws UnsupportedOperationException if there exists at least one node
	 *                                           being simplified with an
	 *                                           outgoing non-simplifiable edge
	 */
	public Set<N> simplify(
			Class<? extends N> target,
			Collection<E> removedEdges,
			Map<Pair<E, E>, E> replacedEdges) {
		Set<N> targets = getNodes()
				.stream()
				.filter(k -> target.isAssignableFrom(k.getClass()))
				.collect(Collectors.toSet());
		targets.forEach(this::preSimplify);
		list.simplify(targets, entrypoints, removedEdges, replacedEdges);
		return targets;
	}

	/**
	 * Callback that is invoked on a node before simplifying it.
	 * 
	 * @param node the node about to be simplified
	 */
	public void preSimplify(
			N node) {
		// nothing to do, but subclasses might redefine
	}

}
