package it.unive.lisa.util.datastructures.graph;

import it.unive.lisa.program.ProgramValidationException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;

/**
 * An adjacency matrix for a graph that has {@link Node}s as nodes and
 * {@link Edge}s as edges. It is represented as a map between a node and a
 * {@link NodeEdges}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <G> the type of the {@link BaseGraph}s this matrix can be used in
 * @param <N> the type of the {@link Node}s in this matrix
 * @param <E> the type of the {@link Edge}s in this matrix
 */
public class AdjacencyMatrix<G extends BaseGraph<G, N, E>, N extends Node<G, N, E>, E extends Edge<G, N, E>>
		implements
		Iterable<Entry<N, AdjacencyMatrix.NodeEdges<G, N, E>>> {

	/**
	 * The matrix. The left set in the mapped value is the set of ingoing edges,
	 * while the right one is the set of outgoing edges.
	 */
	private final Map<N, NodeEdges<G, N, E>> matrix;

	/**
	 * Builds a new matrix.
	 */
	public AdjacencyMatrix() {
		matrix = new HashMap<>();
	}

	/**
	 * Copies the given matrix by shallow-copying the {@link Node}s and
	 * deep-copying the values.
	 * 
	 * @param other the matrix to copy
	 */
	public AdjacencyMatrix(
			AdjacencyMatrix<G, N, E> other) {
		matrix = new HashMap<>();
		for (Map.Entry<N, NodeEdges<G, N, E>> entry : other.matrix.entrySet())
			matrix.put(entry.getKey(), new NodeEdges<>(entry.getValue()));
	}

	/**
	 * Adds the given node to the set of nodes. Note that, if the given node is
	 * already present in the matrix, all existing edges are kept.
	 * 
	 * @param node the node to add
	 */
	public void addNode(
			N node) {
		matrix.putIfAbsent(node, new NodeEdges<>());
	}

	/**
	 * Removes the given node from the matrix, together with all its connected
	 * edges.
	 * 
	 * @param node the node to remove
	 */
	public void removeNode(
			N node) {
		if (!containsNode(node))
			return;

		NodeEdges<G, N, E> edges = matrix.get(node);
		Set<E> union = new HashSet<>(edges.ingoing);
		union.addAll(edges.outgoing);
		union.forEach(this::removeEdge);
		matrix.remove(node);
	}

	/**
	 * Yields the collection of nodes of this matrix.
	 * 
	 * @return the collection of nodes
	 */
	public Collection<N> getNodes() {
		return matrix.keySet();
	}

	/**
	 * Adds an edge to this matrix.
	 * 
	 * @param e the edge to add
	 * 
	 * @throws UnsupportedOperationException if the source or the destination of
	 *                                           the given edge are not part of
	 *                                           this matrix
	 */
	public void addEdge(
			E e) {
		if (!matrix.containsKey(e.getSource()))
			throw new UnsupportedOperationException("The source node is not in the graph");

		if (!matrix.containsKey(e.getDestination()))
			throw new UnsupportedOperationException("The destination node is not in the graph");

		matrix.get(e.getSource()).outgoing.add(e);
		matrix.get(e.getDestination()).ingoing.add(e);
	}

	/**
	 * Removes the given edge from the matrix.
	 * 
	 * @param e the edge to remove
	 */
	public void removeEdge(
			E e) {
		if (!matrix.containsKey(e.getSource()) || !matrix.containsKey(e.getDestination()))
			return;

		matrix.get(e.getSource()).outgoing.remove(e);
		matrix.get(e.getDestination()).ingoing.remove(e);
	}

	/**
	 * Yields the edge connecting the two given nodes, if any. Yields
	 * {@code null} if such edge does not exist, or if one of the two node is
	 * not inside this matrix.
	 * 
	 * @param source      the source node
	 * @param destination the destination node
	 * 
	 * @return the edge connecting {@code source} to {@code destination}, or
	 *             {@code null}
	 */
	public E getEdgeConnecting(
			N source,
			N destination) {
		if (!matrix.containsKey(source))
			return null;

		for (E e : matrix.get(source).outgoing)
			if (e.getDestination().equals(destination))
				return e;

		return null;
	}

	/**
	 * Yields all edges connecting the two given nodes, if any. Yields an empty
	 * collection if no edge exists, or if one of the two nodes is not inside
	 * this matrix.
	 * 
	 * @param source      the source node
	 * @param destination the destination node
	 * 
	 * @return the edges connecting {@code source} to {@code destination}
	 */
	public Collection<E> getEdgesConnecting(
			N source,
			N destination) {
		if (!matrix.containsKey(source))
			return Collections.emptyList();

		Set<E> edges = new HashSet<>();
		for (E e : matrix.get(source).outgoing)
			if (e.getDestination().equals(destination))
				edges.add(e);

		return edges.isEmpty() ? Collections.emptyList() : edges;
	}

	/**
	 * Yields the ingoing edges to the given node.
	 * 
	 * @param node the node
	 * 
	 * @return the collection of ingoing edges
	 */
	public Collection<E> getIngoingEdges(
			N node) {
		return matrix.get(node).ingoing;
	}

	/**
	 * Yields the outgoing edges from the given node.
	 * 
	 * @param node the node
	 * 
	 * @return the collection of outgoing edges
	 */
	public Collection<E> getOutgoingEdges(
			N node) {
		return matrix.get(node).outgoing;
	}

	/**
	 * Yields the set of edges of this matrix.
	 * 
	 * @return the collection of edges
	 */
	public Collection<E> getEdges() {
		return matrix.values()
			.stream()
			.flatMap(c -> Stream.concat(c.ingoing.stream(), c.outgoing.stream()))
			.distinct()
			.collect(Collectors.toSet());
	}

	/**
	 * Yields the collection of the nodes that are followers of the given one,
	 * that is, all nodes such that there exist an edge in this matrix going
	 * from the given node to such node.
	 * 
	 * @param node the node
	 * 
	 * @return the collection of followers
	 * 
	 * @throws IllegalArgumentException if the node is not in the graph
	 */
	public Collection<N> followersOf(
			N node) {
		if (!matrix.containsKey(node))
			throw new IllegalArgumentException("'" + node + "' is not in the graph");

		return matrix.get(node).outgoing.stream().map(Edge::getDestination).collect(Collectors.toSet());
	}

	/**
	 * Yields the collection of the nodes that are predecessors of the given
	 * vertex, that is, all nodes such that there exist an edge in this matrix
	 * going from such node to the given one.
	 * 
	 * @param node the node
	 * 
	 * @return the collection of predecessors
	 * 
	 * @throws IllegalArgumentException if the node is not in the graph
	 */
	public Collection<N> predecessorsOf(
			N node) {
		if (!matrix.containsKey(node))
			throw new IllegalArgumentException("'" + node + "' is not in the graph");

		return matrix.get(node).ingoing.stream().map(Edge::getSource).collect(Collectors.toSet());
	}

	/**
	 * Yields {@code true} if the given node is contained in this matrix.
	 * 
	 * @param node the node to check
	 * 
	 * @return {@code true} if the node is in this matrix
	 */
	public boolean containsNode(
			N node) {
		return matrix.containsKey(node);
	}

	/**
	 * Yields {@code true} if the given edge is contained in this matrix.
	 * 
	 * @param edge the edge to check
	 * 
	 * @return {@code true} if the edge is in this matrix
	 */
	public boolean containsEdge(
			E edge) {
		for (NodeEdges<G, N, E> edges : matrix.values())
			for (E e : edges.outgoing)
				if (e == edge || e.equals(edge))
					return true;

		return false;
	}

	@Override
	public Iterator<Entry<N, NodeEdges<G, N, E>>> iterator() {
		return matrix.entrySet().iterator();
	}

	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((matrix == null) ? 0 : matrix.hashCode());
		return result;
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AdjacencyMatrix<?, ?, ?> other = (AdjacencyMatrix<?, ?, ?>) obj;
		if (matrix == null) {
			if (other.matrix != null)
				return false;
		} else if (!matrix.equals(other.matrix))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder res = new StringBuilder();
		for (Entry<N, NodeEdges<G, N, E>> entry : this) {
			res.append(entry.getKey()).append(" -> [\n\tingoing: ");
			res.append(StringUtils.join(entry.getValue().ingoing, ", "));
			res.append("\n\toutgoing: ");
			res.append(StringUtils.join(entry.getValue().outgoing, ", "));
			res.append("\n]\n");
		}
		return res.toString();
	}

	/**
	 * Yields the entry nodes of this matrix, that is, the nodes that have no
	 * predecessors.
	 * 
	 * @return the entries nodes
	 */
	public Collection<N> getEntries() {
		return matrix.entrySet()
			.stream()
			.filter(e -> e.getValue().ingoing.isEmpty())
			.map(Entry::getKey)
			.collect(Collectors.toSet());
	}

	/**
	 * Yields the exit nodes of this matrix, that is, the nodes that have no
	 * followers.
	 * 
	 * @return the exit nodes
	 */
	public Collection<N> getExits() {
		return matrix.entrySet()
			.stream()
			.filter(e -> e.getValue().outgoing.isEmpty())
			.map(Entry::getKey)
			.collect(Collectors.toSet());
	}

	/**
	 * Yields the minimum distance, in terms of number of edges to traverse,
	 * between the given nodes. If one of the nodes is not inside this matrix,
	 * or if no path can be found, this method returns {@code -1}. If the
	 * distance is greater than {@link Integer#MAX_VALUE},
	 * {@link Integer#MAX_VALUE} is returned.
	 * 
	 * @param from the starting node
	 * @param to   the destination node
	 * 
	 * @return the minimum distance, in terms of number of edges to traverse,
	 *             between the given nodes
	 */
	public int distance(
			N from,
			N to) {
		if (!containsNode(from) || !containsNode(to))
			return -1;

		Map<N, Integer> distances = new IdentityHashMap<>(matrix.size());

		Queue<N> queue = new LinkedList<>();
		distances.put(from, 0);

		queue.add(from);
		while (!queue.isEmpty()) {
			N x = queue.peek();
			queue.poll();

			for (N follower : followersOf(x)) {
				if (distances.containsKey(follower))
					continue;

				distances.put(follower, distances.get(x) + 1);
				queue.add(follower);
			}
		}

		return distances.getOrDefault(to, -1);
	}

	/**
	 * Merges this matrix with the given one, by adding all nodes and all edges
	 * contained in {@code other}.
	 * 
	 * @param other the matrix to merge into this one
	 */
	public void mergeWith(
			AdjacencyMatrix<G, N, E> other) {
		for (N node : other.getNodes())
			addNode(node);

		for (E edge : other.getEdges())
			addEdge(edge);
	}

	/**
	 * Validates this matrix, ensuring that the it is well formed. This method
	 * checks that:
	 * <ul>
	 * <li>each {@link Edge} is connected to {@link Node}s contained in the
	 * matrix</li>
	 * <li>all {@link Node}s with no ingoing edges are marked as entrypoints of
	 * the cfg (i.e. no deadcode)</li>
	 * </ul>
	 * 
	 * @param entrypoints the collection of {@link Node}s that are considered as
	 *                        entrypoints of the graph built over this adjacency
	 *                        matrix
	 * 
	 * @throws ProgramValidationException if one of the aforementioned checks
	 *                                        fail
	 */
	public void validate(
			Collection<N> entrypoints)
			throws ProgramValidationException {
		Collection<N> nodes = getNodes();

		// all edges should be connected to statements inside the matrix
		for (Entry<N, NodeEdges<G, N, E>> st : matrix.entrySet()) {
			for (E in : st.getValue().ingoing)
				validateEdge(nodes, in);

			for (E out : st.getValue().outgoing)
				validateEdge(nodes, out);

			// no deadcode
			if (st.getValue().ingoing.isEmpty() && !entrypoints.contains(st.getKey()))
				throw new ProgramValidationException(
					"Unreachable node that is not marked as entrypoint: " + st.getKey());
		}
	}

	private void validateEdge(
			Collection<N> nodes,
			E edge)
			throws ProgramValidationException {
		if (!nodes.contains(edge.getSource()))
			throw new ProgramValidationException(
				"Invalid edge: '" + edge + "' originates in a node that is not part of the graph");
		else if (!nodes.contains(edge.getDestination()))
			throw new ProgramValidationException(
				"Invalid edge: '" + edge + "' reaches a node that is not part of the graph");
	}

	/**
	 * Utility class for representing the edges tied to a node, split into two
	 * sets: ingoing and outgoing.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 * 
	 * @param <G> the type of the {@link BaseGraph}s the containing matrix can
	 *                be used in
	 * @param <N> the type of the {@link Node}s in the containing matrix
	 * @param <E> the type of the {@link Edge}s in the containing matrix
	 */
	public static class NodeEdges<G extends BaseGraph<G, N, E>, N extends Node<G, N, E>, E extends Edge<G, N, E>> {

		private final Set<E> ingoing;

		private final Set<E> outgoing;

		private NodeEdges() {
			ingoing = new HashSet<>();
			outgoing = new HashSet<>();
		}

		private NodeEdges(
				NodeEdges<G, N, E> other) {
			ingoing = new HashSet<>(other.ingoing);
			outgoing = new HashSet<>(other.outgoing);
		}

		/**
		 * Yields the ingoing edges.
		 * 
		 * @return the set of ingoing edges
		 */
		public Set<E> getIngoing() {
			return ingoing;
		}

		/**
		 * Yields the outgoing edges.
		 * 
		 * @return the set of outgoing edges
		 */
		public Set<E> getOutgoing() {
			return outgoing;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((ingoing == null) ? 0 : ingoing.hashCode());
			result = prime * result + ((outgoing == null) ? 0 : outgoing.hashCode());
			return result;
		}

		@Override
		public boolean equals(
				Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			NodeEdges<?, ?, ?> other = (NodeEdges<?, ?, ?>) obj;
			if (ingoing == null) {
				if (other.ingoing != null)
					return false;
			} else if (!ingoing.equals(other.ingoing))
				return false;
			if (outgoing == null) {
				if (other.outgoing != null)
					return false;
			} else if (!outgoing.equals(other.outgoing))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "ins: " + ingoing + ", outs: " + outgoing;
		}

	}

	/**
	 * Empties this matrix, removing all the nodes and the edges in it.
	 */
	public void clear() {
		matrix.clear();
	}

}
