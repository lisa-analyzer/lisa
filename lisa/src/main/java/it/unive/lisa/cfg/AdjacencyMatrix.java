package it.unive.lisa.cfg;

import it.unive.lisa.cfg.edge.Edge;
import it.unive.lisa.cfg.edge.FalseEdge;
import it.unive.lisa.cfg.edge.SequentialEdge;
import it.unive.lisa.cfg.edge.TrueEdge;
import it.unive.lisa.cfg.statement.NoOp;
import it.unive.lisa.cfg.statement.Statement;
import it.unive.lisa.util.collections.ExternalSet;
import it.unive.lisa.util.collections.ExternalSetCache;
import it.unive.lisa.util.workset.FIFOWorkingSet;
import it.unive.lisa.util.workset.WorkingSet;
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
 * An adjacency matrix for a graph that has {@link Statement}s as nodes and
 * {@link Edge}s as edges. It is represented as a map between a statement and a
 * {@link Pair} of set of edges, where the {@link Pair#getLeft()} yields the set
 * of ingoing edges and {@link Pair#getRight()} yields the set of outgoing
 * edges. Set of edges are represented as {@link ExternalSet}s derived from a
 * matrix-unique {@link ExternalSetCache}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class AdjacencyMatrix implements Iterable<Map.Entry<Statement, Pair<ExternalSet<Edge>, ExternalSet<Edge>>>> {

	/**
	 * The factory where edges are stored.
	 */
	private final ExternalSetCache<Edge> edgeFactory;

	/**
	 * The matrix. The left set in the mapped value is the set of ingoing edges,
	 * while the right one is the set of outgoing edges.
	 */
	private final Map<Statement, Pair<ExternalSet<Edge>, ExternalSet<Edge>>> matrix;

	/**
	 * The next available offset to be assigned to the next statement
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
	 * {@link ExternalSetCache}, shallow-copying the {@link Statement}s and
	 * deep-copying the values.
	 * 
	 * @param other the matrix to copy
	 */
	public AdjacencyMatrix(AdjacencyMatrix other) {
		edgeFactory = other.edgeFactory;
		matrix = new ConcurrentHashMap<>();
		for (Map.Entry<Statement, Pair<ExternalSet<Edge>, ExternalSet<Edge>>> entry : other.matrix.entrySet())
			matrix.put(entry.getKey(), Pair.of(entry.getValue().getLeft().copy(), entry.getValue().getRight().copy()));
		nextOffset = other.nextOffset;
	}

	/**
	 * Adds the given node to the set of nodes.
	 * 
	 * @param node the node to add
	 */
	public void addNode(Statement node) {
		matrix.put(node, Pair.of(edgeFactory.mkEmptySet(), edgeFactory.mkEmptySet()));
		nextOffset = node.setOffset(nextOffset) + 1;
	}

	/**
	 * Yields the collection of nodes of this matrix.
	 * 
	 * @return the collection of nodes
	 */
	public final Collection<Statement> getNodes() {
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
	public void addEdge(Edge e) {
		if (!matrix.containsKey(e.getSource()))
			throw new UnsupportedOperationException("The source node is not in the graph");

		if (!matrix.containsKey(e.getDestination()))
			throw new UnsupportedOperationException("The destination node is not in the graph");

		matrix.get(e.getSource()).getRight().add(e);
		matrix.get(e.getDestination()).getLeft().add(e);
	}

	/**
	 * Yields the edge connecting the two given statements, if any. Yields
	 * {@code null} if such edge does not exist, or if one of the two statements
	 * is not inside this matrix.
	 * 
	 * @param source      the source statement
	 * @param destination the destination statement
	 * 
	 * @return the edge connecting {@code source} to {@code destination}, or
	 *             {@code null}
	 */
	public final Edge getEdgeConnecting(Statement source, Statement destination) {
		if (!matrix.containsKey(source))
			return null;

		for (Edge e : matrix.get(source).getRight())
			if (e.getDestination().equals(destination))
				return e;

		return null;
	}

	/**
	 * Yields the set of edges of this matrix.
	 * 
	 * @return the collection of edges
	 */
	public final Collection<Edge> getEdges() {
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
	 * @return the collection of followers
	 */
	public final Collection<Statement> followersOf(Statement node) {
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
	 * @return the collection of predecessors
	 */
	public final Collection<Statement> predecessorsOf(Statement node) {
		if (!matrix.containsKey(node))
			return null;

		return matrix.get(node).getLeft().collect().stream().map(e -> e.getSource()).collect(Collectors.toSet());
	}

	/**
	 * Simplifies this matrix, removing all {@link NoOp}s and rewriting the edge
	 * set accordingly. This method will throw an
	 * {@link UnsupportedOperationException} if one of the {@link NoOp}s has an
	 * outgoing edge that is not a {@link SequentialEdge}, since such statement
	 * is expected to always be sequential.
	 * 
	 * @throws UnsupportedOperationException if there exists at least one
	 *                                           {@link NoOp} with an outgoing
	 *                                           non-sequential edge, or if one
	 *                                           of the ingoing edges to the
	 *                                           {@link NoOp} is not currently
	 *                                           supported.
	 */
	public synchronized void simplify() {
		Set<Statement> noops = matrix.keySet().stream().filter(k -> k instanceof NoOp).collect(Collectors.toSet());
		for (Statement noop : noops) {
			for (Edge ingoing : matrix.get(noop).getLeft())
				for (Edge outgoing : matrix.get(noop).getRight()) {
					if (!(outgoing instanceof SequentialEdge))
						throw new UnsupportedOperationException(
								"Cannot remove no-op with non-sequential outgoing edge");

					// replicate the edge from ingoing.source to outgoing.dest
					Edge _new = null;
					if (ingoing instanceof SequentialEdge)
						_new = new SequentialEdge(ingoing.getSource(), outgoing.getDestination());
					else if (ingoing instanceof TrueEdge)
						_new = new TrueEdge(ingoing.getSource(), outgoing.getDestination());
					else if (ingoing instanceof FalseEdge)
						_new = new FalseEdge(ingoing.getSource(), outgoing.getDestination());
					else
						throw new UnsupportedOperationException("Unknown edge type: " + ingoing.getClass().getName());

					matrix.get(ingoing.getSource()).getRight().remove(ingoing);
					matrix.get(ingoing.getSource()).getRight().add(_new);
					matrix.get(outgoing.getDestination()).getLeft().remove(outgoing);
					matrix.get(outgoing.getDestination()).getLeft().add(_new);
				}
			matrix.remove(noop);
		}
	}

	@Override
	public Iterator<Entry<Statement, Pair<ExternalSet<Edge>, ExternalSet<Edge>>>> iterator() {
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
		AdjacencyMatrix other = (AdjacencyMatrix) obj;
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
	public boolean isEqualTo(AdjacencyMatrix other) {
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

	private static boolean areEqual(Map<Statement, Pair<ExternalSet<Edge>, ExternalSet<Edge>>> first,
			Map<Statement, Pair<ExternalSet<Edge>, ExternalSet<Edge>>> second) {
		// the following keeps track of the unmatched statements in second
		Collection<Statement> copy = new HashSet<>(second.keySet());
		boolean found;
		for (Map.Entry<Statement, Pair<ExternalSet<Edge>, ExternalSet<Edge>>> entry : first.entrySet()) {
			found = false;
			for (Map.Entry<Statement, Pair<ExternalSet<Edge>, ExternalSet<Edge>>> entry2 : second.entrySet())
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
			// we also have to match all of the entrypoints in cfg.entrypoints
			return false;

		return true;
	}

	private static boolean areEqual(ExternalSet<Edge> first, ExternalSet<Edge> second) {
		// the following keeps track of the unmatched statements in second
		Collection<Edge> copy = second.collect();
		boolean found;
		for (Edge e : first) {
			found = false;
			for (Edge ee : second)
				if (copy.contains(ee) && e.isEqualTo(ee)) {
					copy.remove(ee);
					found = true;
					break;
				}
			if (!found)
				return false;
		}

		if (!copy.isEmpty())
			// we also have to match all of the entrypoints in cfg.entrypoints
			return false;

		return true;
	}

	@Override
	public String toString() {
		StringBuilder res = new StringBuilder();
		for (Map.Entry<Statement, Pair<ExternalSet<Edge>, ExternalSet<Edge>>> entry : this) {
			res.append("\"").append(entry.getKey()).append("\" -> [\n\tingoing: ");
			res.append(StringUtils.join(entry.getValue().getLeft(), ", "));
			res.append("\n\toutgoing: ");
			res.append(StringUtils.join(entry.getValue().getRight(), ", "));
			res.append("\n]\n");
		}
		return res.toString();
	}

	/**
	 * Merges this adjacency matrix with the given one. The algorithm used for
	 * merging {@code code} into {@code this} treats {@code code} as a graph
	 * starting at {@code root}. The algorithm traverses the graph adding all
	 * nodes and edges to this matrix. <b>No edge between any node already
	 * existing into {@code this} matrix and {@code root} is added.</b>
	 * 
	 * @param root the statement where the exploration of {@code code} should
	 *                 start
	 * @param code the matrix to merge
	 */
	public final void mergeWith(Statement root, AdjacencyMatrix code) {
		WorkingSet<Statement> ws = FIFOWorkingSet.mk();
		ws.push(root);

		do {
			Statement current = ws.pop();
			if (!matrix.containsKey(current))
				addNode(current);
			for (Edge edge : code.matrix.get(current).getRight()) {
				if (matrix.containsKey(edge.getDestination()))
					continue;

				addNode(edge.getDestination());
				ws.push(edge.getDestination());
				if (edge instanceof SequentialEdge)
					addEdge(new SequentialEdge(current, edge.getDestination()));
				else if (edge instanceof TrueEdge)
					addEdge(new TrueEdge(current, edge.getDestination()));
				else if (edge instanceof FalseEdge)
					addEdge(new FalseEdge(current, edge.getDestination()));
				else
					throw new UnsupportedOperationException("Unsupported edge type " + edge.getClass().getName());
			}
		} while (!ws.isEmpty()); // this will remove unreachable code
	}
}
