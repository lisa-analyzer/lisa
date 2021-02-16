package it.unive.lisa.util.datastructures.graph;

import it.unive.lisa.util.collections.ExternalSet;
import it.unive.lisa.util.collections.ExternalSetCache;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

/**
 * An adjacency matrix for a graph that has {@link Node}s as nodes and
 * {@link Edge}s as edges. It is represented as a map between a node and a
 * {@link Pair} of set of edges, where the {@link Pair#getLeft()} yields the set
 * of ingoing edges and {@link Pair#getRight()} yields the set of outgoing
 * edges. Set of edges are represented as {@link ExternalSet}s derived from a
 * matrix-unique {@link ExternalSetCache}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <N> the type of the {@link Node}s in this matrix
 * @param <E> the type of the {@link Edge}s in this matrix
 * @param <G> the type of the {@link Graph}s this matrix can be used in
 */
public class AdjacencyMatrix<N extends Node<N, E, G>, E extends Edge<N, E, G>, G extends Graph<G, N, E>>
		implements Iterable<Map.Entry<N, Pair<ExternalSet<E>, ExternalSet<E>>>> {

	/**
	 * The factory where edges are stored.
	 */
	private final ExternalSetCache<E> edgeFactory;

	/**
	 * The matrix. The left set in the mapped value is the set of ingoing edges,
	 * while the right one is the set of outgoing edges.
	 */
	private final Map<N, Pair<ExternalSet<E>, ExternalSet<E>>> matrix;

	/**
	 * The next available offset to be assigned to the next node
	 */
	private int nextOffset;

	/**
	 * Builds a new matrix.
	 */
	public AdjacencyMatrix() {
		edgeFactory = new ExternalSetCache<>();
		matrix = new ConcurrentHashMap<>();
		nextOffset = 0;
	}

	/**
	 * Copies the given matrix by keeping the same edge
	 * {@link ExternalSetCache}, shallow-copying the {@link Node}s and
	 * deep-copying the values.
	 * 
	 * @param other the matrix to copy
	 */
	public AdjacencyMatrix(AdjacencyMatrix<N, E, G> other) {
		edgeFactory = other.edgeFactory;
		matrix = new ConcurrentHashMap<>();
		for (Map.Entry<N, Pair<ExternalSet<E>, ExternalSet<E>>> entry : other.matrix.entrySet())
			matrix.put(entry.getKey(), Pair.of(entry.getValue().getLeft().copy(), entry.getValue().getRight().copy()));
		nextOffset = other.nextOffset;
	}

	/**
	 * Adds the given node to the set of nodes.
	 * 
	 * @param node the node to add
	 */
	public void addNode(N node) {
		matrix.put(node, Pair.of(edgeFactory.mkEmptySet(), edgeFactory.mkEmptySet()));
		nextOffset = node.setOffset(nextOffset) + 1;
	}

	/**
	 * Yields the collection of nodes of this matrix.
	 * 
	 * @return the collection of nodes
	 */
	public final Collection<N> getNodes() {
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
	public void addEdge(E e) {
		if (!matrix.containsKey(e.getSource()))
			throw new UnsupportedOperationException("The source node is not in the graph");

		if (!matrix.containsKey(e.getDestination()))
			throw new UnsupportedOperationException("The destination node is not in the graph");

		matrix.get(e.getSource()).getRight().add(e);
		matrix.get(e.getDestination()).getLeft().add(e);
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
	public final E getEdgeConnecting(N source, N destination) {
		if (!matrix.containsKey(source))
			return null;

		for (E e : matrix.get(source).getRight())
			if (e.getDestination().equals(destination))
				return e;

		return null;
	}

	/**
	 * Yields the set of edges of this matrix.
	 * 
	 * @return the collection of edges
	 */
	public final Collection<E> getEdges() {
		return matrix.values().stream()
				.flatMap(c -> Stream.concat(c.getLeft().collect().stream(), c.getRight().collect().stream()))
				.collect(Collectors.toSet());
	}

	/**
	 * Yields the collection of the nodes that are followers of the given one,
	 * that is, all nodes such that there exist an edge in this matrix going
	 * from the given node to such node. Yields {@code null} if the node is not
	 * in this matrix.
	 * 
	 * @param node the node
	 * 
	 * @return the collection of followers, or {@code null}
	 */
	public final Collection<N> followersOf(N node) {
		if (!matrix.containsKey(node))
			return null;

		return matrix.get(node).getRight().collect().stream().map(e -> e.getDestination()).collect(Collectors.toSet());
	}

	/**
	 * Yields the collection of the nodes that are predecessors of the given
	 * vertex, that is, all nodes such that there exist an edge in this matrix
	 * going from such node to the given one. Yields {@code null} if the node is
	 * not in this matrix.
	 * 
	 * @param node the node
	 * 
	 * @return the collection of predecessors, or {@code null}
	 */
	public final Collection<N> predecessorsOf(N node) {
		if (!matrix.containsKey(node))
			return null;

		return matrix.get(node).getLeft().collect().stream().map(e -> e.getSource()).collect(Collectors.toSet());
	}

	/**
	 * Simplifies this matrix, removing all the given nodes and rewriting the
	 * edge set accordingly. This method will throw an
	 * {@link UnsupportedOperationException} if one of the nodes being
	 * simplified has an outgoing edge that is not simplifiable, according to
	 * {@link Edge#canBeSimplified()}.
	 * 
	 * @param targets     the set of the {@link Node}s that needs to be
	 *                        simplified
	 * @param entrypoints the collection of {@link Node}s that are considered as
	 *                        entrypoints of the graph built over this adjacency
	 *                        matrix
	 * 
	 * @throws UnsupportedOperationException if there exists at least one node
	 *                                           being simplified with an
	 *                                           outgoing non-simplifiable edge
	 */
	public synchronized void simplify(Set<N> targets, Collection<N> entrypoints) {
		for (N t : targets) {
			ExternalSet<E> ingoing = matrix.get(t).getLeft();
			ExternalSet<E> outgoing = matrix.get(t).getRight();
			boolean entry = entrypoints.contains(t);

			if (ingoing.isEmpty() && !outgoing.isEmpty())
				// this is a source node
				for (E out : outgoing) {
					if (!out.canBeSimplified())
						throw new UnsupportedOperationException(
								"Cannot simplify an edge with class " + out.getClass().getSimpleName());

					// remove the edge
					matrix.get(out.getDestination()).getLeft().remove(out);
					if (entry)
						entrypoints.add(out.getDestination());
				}
			else if (!ingoing.isEmpty() && outgoing.isEmpty())
				// this is an exit node
				for (E in : ingoing) {
					if (!in.canBeSimplified())
						throw new UnsupportedOperationException(
								"Cannot simplify an edge with class " + in.getClass().getSimpleName());

					// remove the edge
					matrix.get(in.getSource()).getRight().remove(in);
				}
			else
				// normal intermediate edge
				for (E in : ingoing)
					for (E out : outgoing) {
						if (!out.canBeSimplified())
							throw new UnsupportedOperationException(
									"Cannot simplify an edge with class " + out.getClass().getSimpleName());

						// replicate the edge from ingoing.source to
						// outgoing.dest
						E _new = in.newInstance(in.getSource(), out.getDestination());

						// swap the ingoing edge
						matrix.get(in.getSource()).getRight().remove(in);
						matrix.get(in.getSource()).getRight().add(_new);

						// swap the outgoing edge
						matrix.get(out.getDestination()).getLeft().remove(out);
						matrix.get(out.getDestination()).getLeft().add(_new);
					}

			// remove the simplified node
			if (entry)
				entrypoints.remove(t);
			matrix.remove(t);
		}
	}

	@Override
	public Iterator<Entry<N, Pair<ExternalSet<E>, ExternalSet<E>>>> iterator() {
		return matrix.entrySet().iterator();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((matrix == null) ? 0 : matrix.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
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

	/**
	 * Checks if this matrix is effectively equal to the given one, that is, if
	 * they have the same structure while potentially being different instances.
	 * 
	 * @param other the other matrix
	 * 
	 * @return {@code true} if this matrix and the given one are effectively
	 *             equals
	 */
	public boolean isEqualTo(AdjacencyMatrix<N, E, G> other) {
		if (this == other)
			return true;
		if (other == null)
			return false;
		if (matrix == null) {
			if (other.matrix != null)
				return false;
		} else if (!areEqual(matrix, other.matrix))
			return false;
		return true;
	}

	private boolean areEqual(Map<N, Pair<ExternalSet<E>, ExternalSet<E>>> first,
			Map<N, Pair<ExternalSet<E>, ExternalSet<E>>> second) {
		// the following keeps track of the unmatched nodes in second
		Collection<N> copy = new HashSet<>(second.keySet());
		boolean found;
		for (Map.Entry<N, Pair<ExternalSet<E>, ExternalSet<E>>> entry : first.entrySet()) {
			found = false;
			for (Map.Entry<N, Pair<ExternalSet<E>, ExternalSet<E>>> entry2 : second.entrySet())
				if (copy.contains(entry2.getKey()) && entry.getKey().isEqualTo(entry2.getKey())
						&& areEqual(entry.getValue().getLeft(), entry2.getValue().getLeft())
						&& areEqual(entry.getValue().getRight(), entry2.getValue().getRight())) {
					copy.remove(entry2.getKey());
					found = true;
					break;
				}
			if (!found)
				return false;
		}

		if (!copy.isEmpty())
			return false;

		return true;
	}

	private boolean areEqual(ExternalSet<E> first, ExternalSet<E> second) {
		// the following keeps track of the unmatched edges in second
		Collection<E> copy = second.collect();
		boolean found;
		for (E e : first) {
			found = false;
			for (E ee : second)
				if (copy.contains(ee) && e.isEqualTo(ee)) {
					copy.remove(ee);
					found = true;
					break;
				}
			if (!found)
				return false;
		}

		if (!copy.isEmpty())
			return false;

		return true;
	}

	@Override
	public String toString() {
		StringBuilder res = new StringBuilder();
		for (Map.Entry<N, Pair<ExternalSet<E>, ExternalSet<E>>> entry : this) {
			res.append("\"").append(entry.getKey()).append("\" -> [\n\tingoing: ");
			res.append(StringUtils.join(entry.getValue().getLeft(), ", "));
			res.append("\n\toutgoing: ");
			res.append(StringUtils.join(entry.getValue().getRight(), ", "));
			res.append("\n]\n");
		}
		return res.toString();
	}
}
