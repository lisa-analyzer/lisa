package it.unive.lisa.util.datastructures.graph;

import it.unive.lisa.program.ProgramValidationException;
import it.unive.lisa.util.collections.CollectionUtilities;
import it.unive.lisa.util.collections.externalSet.ExternalSet;
import it.unive.lisa.util.collections.externalSet.ExternalSetCache;
import it.unive.lisa.util.workset.LIFOWorkingSet;
import it.unive.lisa.util.workset.VisitOnceWorkingSet;
import it.unive.lisa.util.workset.WorkingSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
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
	 * Builds a new matrix, using the given edge factory.
	 * 
	 * @param edgeFactory the factory that will be used to back this matrix
	 */
	public AdjacencyMatrix(ExternalSetCache<E> edgeFactory) {
		this.edgeFactory = edgeFactory;
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
	 * Yields the edge factory backing this matrix.
	 * 
	 * @return the edge factory
	 */
	public ExternalSetCache<E> getEdgeFactory() {
		return edgeFactory;
	}

	/**
	 * Adds the given node to the set of nodes. Note that, if the given node is
	 * already present in the matrix, all existing edges are kept.
	 * 
	 * @param node the node to add
	 */
	public void addNode(N node) {
		if (matrix.putIfAbsent(node, Pair.of(edgeFactory.mkEmptySet(), edgeFactory.mkEmptySet())) == null)
			nextOffset = node.setOffset(nextOffset) + 1;
	}

	/**
	 * Removes the given node from the matrix, together with all its connected
	 * edges.
	 * 
	 * @param node the node to remove
	 */
	public void removeNode(N node) {
		if (!containsNode(node, false))
			return;

		Pair<ExternalSet<E>, ExternalSet<E>> edges = matrix.get(node);
		edges.getLeft().forEach(this::removeEdge);
		edges.getRight().forEach(this::removeEdge);
		matrix.remove(node);
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
	 * Removes the given edge from the matrix.
	 * 
	 * @param e the edge to remove
	 */
	public void removeEdge(E e) {
		if (!matrix.containsKey(e.getSource()) || !matrix.containsKey(e.getDestination()))
			return;

		matrix.get(e.getSource()).getRight().remove(e);
		matrix.get(e.getDestination()).getLeft().remove(e);
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
	 * Yields the ingoing edges to the given node.
	 * 
	 * @param node the node
	 * 
	 * @return the collection of ingoing edges
	 */
	public final Collection<E> getIngoingEdges(N node) {
		return matrix.get(node).getLeft();
	}

	/**
	 * Yields the outgoing edges from the given node.
	 * 
	 * @param node the node
	 * 
	 * @return the collection of outgoing edges
	 */
	public final Collection<E> getOutgoingEdges(N node) {
		return matrix.get(node).getRight();
	}

	/**
	 * Yields the set of edges of this matrix.
	 * 
	 * @return the collection of edges
	 */
	public final Collection<E> getEdges() {
		return matrix.values().stream()
				.flatMap(c -> Stream.concat(c.getLeft().collect().stream(), c.getRight().collect().stream()))
				.distinct()
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
	 * @param targets       the set of the {@link Node}s that needs to be
	 *                          simplified
	 * @param entrypoints   the collection of {@link Node}s that are considered
	 *                          as entrypoints of the graph built over this
	 *                          adjacency matrix
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
	 * @throws UnsupportedOperationException if there exists at least one node
	 *                                           being simplified with an
	 *                                           outgoing non-simplifiable edge
	 */
	public synchronized void simplify(Set<N> targets, Collection<N> entrypoints, Collection<E> removedEdges,
			Map<Pair<E, E>, E> replacedEdges) {
		removedEdges.clear();
		replacedEdges.clear();

		for (N t : targets) {
			ExternalSet<E> ingoing = matrix.get(t).getLeft();
			ExternalSet<E> outgoing = matrix.get(t).getRight();
			boolean entry = entrypoints.contains(t);

			if (ingoing.isEmpty() && !outgoing.isEmpty())
				// this is a entry node
				for (E out : outgoing) {
					if (!out.canBeSimplified())
						throw new UnsupportedOperationException(
								"Cannot simplify an edge with class " + out.getClass().getSimpleName());

					// remove the edge
					removedEdges.add(out);
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
					removedEdges.add(in);
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
						replacedEdges.put(Pair.of(in, out), _new);

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

	/**
	 * Yields {@code true} if the given node is contained in this matrix.
	 * 
	 * @param node         the node to check
	 * @param weakEquality if {@code true}, {@link Node#isEqualTo(Node)} is used
	 *                         to test for equality while searching for the node
	 *                         instead of using {@link Object#equals(Object)}.
	 * 
	 * @return {@code true} if the node is in this matrix
	 */
	public boolean containsNode(N node, boolean weakEquality) {
		if (!weakEquality)
			return matrix.containsKey(node);

		for (N n : matrix.keySet())
			if (n == node || n.equals(node) || n.isEqualTo(node))
				return true;

		return false;
	}

	/**
	 * Yields {@code true} if the given edge is contained in this matrix.
	 * 
	 * @param edge         the edge to check
	 * @param weakEquality if {@code true}, {@link Edge#isEqualTo(Edge)} is used
	 *                         to test for equality while searching for the edge
	 *                         instead of using {@link Object#equals(Object)}.
	 * 
	 * @return {@code true} if the edge is in this matrix
	 */
	public boolean containsEdge(E edge, boolean weakEquality) {
		for (Pair<ExternalSet<E>, ExternalSet<E>> pair : matrix.values())
			for (E e : pair.getRight())
				if (e == edge || e.equals(edge) || (weakEquality && e.isEqualTo(edge)))
					return true;

		return false;
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
		return CollectionUtilities.equals(first.entrySet(), second.entrySet(),
				(e1, e2) -> e1.getKey().isEqualTo(e2.getKey())
						&& areEqual(e1.getValue().getLeft(), e2.getValue().getLeft())
						&& areEqual(e1.getValue().getRight(), e2.getValue().getRight()));
	}

	private boolean areEqual(ExternalSet<E> first, ExternalSet<E> second) {
		return CollectionUtilities.equals(first, second, (e, ee) -> e.isEqualTo(ee));
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

	/**
	 * Removes every node and edge that is reachable from the given
	 * {@code root}.
	 * 
	 * @param root the node to use as root for the removal
	 */
	public void removeFrom(N root) {
		if (!containsNode(root, true))
			return;

		Set<N> add = new HashSet<>(), remove = new HashSet<>(), check = new HashSet<>();

		if (containsNode(root, false))
			add.add(root);
		else {
			for (N n : matrix.keySet())
				if (n.isEqualTo(root))
					add.add(n);
		}

		do {
			// add the ones that were computed at last iteration
			remove.addAll(add);

			// find new successors
			check.clear();
			for (N node : add)
				matrix.get(node).getRight().stream().map(e -> e.getDestination()).forEach(check::add);

			// compute the ones that need to be added
			add.clear();
			check.stream().filter(n -> !remove.contains(n)).forEach(add::add);
		} while (!add.isEmpty());

		// we do not care about the output values
		simplify(remove, Collections.emptyList(), new LinkedList<>(), new HashMap<>());
	}

	/**
	 * Yields the entry nodes of this matrix, that is, the nodes that have no
	 * predecessors.
	 * 
	 * @return the entries nodes
	 */
	public Collection<N> getEntries() {
		return matrix.entrySet().stream().filter(e -> e.getValue().getLeft().isEmpty()).map(Entry::getKey)
				.collect(Collectors.toList());
	}

	/**
	 * Yields the exit nodes of this matrix, that is, the nodes that have no
	 * followers.
	 * 
	 * @return the exit nodes
	 */
	public Collection<N> getExits() {
		return matrix.entrySet().stream().filter(e -> e.getValue().getRight().isEmpty()).map(Entry::getKey)
				.collect(Collectors.toList());
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
	public int distance(N from, N to) {
		if (!containsNode(from, false) || !containsNode(to, false))
			return -1;

		int distance = -1;
		WorkingSet<Pair<N, Integer>> ws = VisitOnceWorkingSet.mk(LIFOWorkingSet.mk());
		ws.push(Pair.of(from, 0));
		while (!ws.isEmpty()) {
			Pair<N, Integer> current = ws.pop();
			if (current.getLeft() != to)
				for (N dest : followersOf(current.getLeft()))
					if (current.getRight() == Integer.MAX_VALUE)
						ws.push(Pair.of(dest, Integer.MAX_VALUE));
					else
						ws.push(Pair.of(dest, current.getRight() + 1));
			else if (distance == -1 || current.getRight() < distance)
				distance = current.getRight();
		}

		return distance;
	}

	/**
	 * Merges this matrix with the given one, by adding all nodes and all edges
	 * contained in {@code other}.
	 * 
	 * @param other the matrix to merge into this one
	 */
	public void mergeWith(AdjacencyMatrix<N, E, G> other) {
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
	public void validate(Collection<N> entrypoints) throws ProgramValidationException {
		Collection<N> nodes = getNodes();

		// all edges should be connected to statements inside the matrix
		for (Entry<N, Pair<ExternalSet<E>, ExternalSet<E>>> st : matrix.entrySet()) {
			for (E in : st.getValue().getLeft())
				validateEdge(nodes, in);

			for (E out : st.getValue().getRight())
				validateEdge(nodes, out);

			// no deadcode
			if (st.getValue().getLeft().isEmpty() && !entrypoints.contains(st.getKey()))
				throw new ProgramValidationException(
						"Unreachable node that is not marked as entrypoint: " + st.getKey());
		}
	}

	private void validateEdge(Collection<N> nodes, E edge) throws ProgramValidationException {
		if (!nodes.contains(edge.getSource()))
			throw new ProgramValidationException("Invalid edge: '" + edge
					+ "' originates in a node that is not part of the graph");
		else if (!nodes.contains(edge.getDestination()))
			throw new ProgramValidationException("Invalid edge: '" + edge
					+ "' reaches a node that is not part of the graph");
	}
}
