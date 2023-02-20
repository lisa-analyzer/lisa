package it.unive.lisa.util.datastructures.graph.code;

import it.unive.lisa.program.ProgramValidationException;
import it.unive.lisa.util.collections.CollectionUtilities.SortedSetCollector;
import it.unive.lisa.util.datastructures.graph.Edge;
import it.unive.lisa.util.datastructures.graph.Node;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A list of nodes of a {@link CodeGraph}, together with the edges connecting
 * them.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <G> the type of the {@link CodeGraph}s this list can be used in
 * @param <N> the type of the {@link CodeNode}s in this list
 * @param <E> the type of the {@link CodeEdge}s in this list
 */
public class NodeList<G extends CodeGraph<G, N, E>, N extends CodeNode<G, N, E>, E extends CodeEdge<G, N, E>>
		implements Iterable<N> {

	private static final String EDGE_SIMPLIFY_ERROR = "Cannot simplify an edge with class ";

	/**
	 * The list of nodes.
	 */
	private final List<N> nodes;

	/**
	 * The list of indexes of the nodes that are cutoff points for sequential
	 * execution, meaning that its follower in {@link #nodes} is not a follower
	 * in the code.
	 */
	private final Set<Integer> cutoff;

	/**
	 * Mapping from each node to all the edges that cannot be represented as
	 * sequential exection through {@link #nodes}.
	 */
	private final SortedMap<N, NodeEdges<G, N, E>> extraEdges;

	/**
	 * A singleton to be used for creating sequential edges.
	 */
	private final E sequentialSingleton;

	/**
	 * The next available offset to be assigned to the next node
	 */
	private int nextOffset;

	/**
	 * If true, this list will compute offsets of its nodes. This can be turned
	 * off for lists that serve as views of other lists, thus not representing
	 * the whole code of a graph.
	 */
	private final boolean computeOffsets;

	/**
	 * Builds a new list. Offsets of nodes added to this list will be set
	 * automatically.
	 * 
	 * @param sequentialSingleton an instance of an edge of this list that can
	 *                                be used to invoke
	 *                                {@link CodeEdge#newInstance(CodeNode, CodeNode)}
	 *                                to obtain instances of sequential edges
	 */
	public NodeList(E sequentialSingleton) {
		this(sequentialSingleton, true);
	}

	/**
	 * Builds a new list.
	 * 
	 * @param sequentialSingleton an instance of an edge of this list that can
	 *                                be used to invoke
	 *                                {@link CodeEdge#newInstance(CodeNode, CodeNode)}
	 *                                to obtain instances of sequential edges
	 * @param computeOffsets      whether or not offsets should be set to nodes
	 *                                when added to this list
	 */
	public NodeList(E sequentialSingleton, boolean computeOffsets) {
		this.sequentialSingleton = sequentialSingleton;
		nodes = new LinkedList<>();
		cutoff = new HashSet<>();
		extraEdges = new TreeMap<>();
		nextOffset = 0;
		this.computeOffsets = computeOffsets;
	}

	/**
	 * Copies the given list.
	 * 
	 * @param other the list to copy
	 */
	public NodeList(NodeList<G, N, E> other) {
		sequentialSingleton = other.sequentialSingleton;
		nodes = new LinkedList<>(other.nodes);
		cutoff = new HashSet<>(other.cutoff);
		extraEdges = new TreeMap<>();
		for (Entry<N, NodeEdges<G, N, E>> entry : other.extraEdges.entrySet())
			extraEdges.put(entry.getKey(), new NodeEdges<>(entry.getValue()));
		nextOffset = other.nextOffset;
		computeOffsets = other.computeOffsets;
	}

	/**
	 * Adds the given node to the list of nodes. A cutoff point is introduced
	 * between this node and its predecessor in the list, meaning that
	 * {@code node} will not be a follower of its predecessor in the list. Note
	 * that, if the given node is already present in the list, all existing
	 * edges are kept. <br>
	 * <br>
	 * Also note that adding a node to a node list sets its offset.
	 * 
	 * @param node the node to add
	 */
	public void addNode(N node) {
		if (containsNode(node))
			// already in the graph
			return;

		int size = nodes.size();
		if (size != 0)
			cutoff.add(size - 1);
		nodes.add(node);
		if (computeOffsets)
			nextOffset = node.setOffset(nextOffset) + 1;
	}

	/**
	 * Removes the given node from the list, together with all its connected
	 * edges. Note that to prevent its predecessor and its follower in the list
	 * to become sequentially connected, a cutoff will be introduced if not
	 * already present.
	 * 
	 * @param node the node to remove
	 */
	public void removeNode(N node) {
		if (!containsNode(node))
			return;

		int target = nodes.indexOf(node);
		NodeEdges<G, N, E> edges = extraEdges.get(node);
		if (edges != null) {
			Set<E> union = new HashSet<>(edges.ingoing);
			union.addAll(edges.outgoing);
			union.forEach(this::removeEdge);
			cutoff.remove(target);
		}

		nodes.remove(node);
		// need to shift all successive cutoff back by one
		List<Integer> interesting = cutoff.stream().filter(i -> i >= target).sorted().collect(Collectors.toList());
		cutoff.removeAll(interesting);
		interesting.forEach(i -> cutoff.add(i - 1));

		if (target != 0)
			if (target != nodes.size()) { // we don't remove 1 since we want to
											// compare wrt the 'old' position
				N pred = nodes.get(target - 1);
				N succ = nodes.get(target); // before was at target+1
				NodeEdges<G, N, E> predEdges = extraEdges.get(pred);
				if (predEdges != null) {
					E seq = sequentialSingleton.newInstance(pred, succ);
					if (predEdges.outgoing.contains(seq)) {
						// sequential edge can be encoded in the list
						removeEdge(seq);
						cutoff.remove(target - 1);
					} else
						cutoff.add(target - 1);
				} else
					cutoff.add(target - 1);
			}

		recomputeOffsets();
	}

	private void recomputeOffsets() {
		if (!computeOffsets)
			return;
		int of = 0;
		for (N node : nodes)
			of = node.setOffset(of) + 1;
		nextOffset = of;
	}

	/**
	 * Yields the collection of nodes of this list.
	 * 
	 * @return the collection of nodes
	 */
	public final Collection<N> getNodes() {
		return new TreeSet<>(nodes);
	}

	/**
	 * Adds an edge to this list.
	 * 
	 * @param e the edge to add
	 * 
	 * @throws UnsupportedOperationException if the source or the destination of
	 *                                           the given edge are not part of
	 *                                           this list
	 */
	public void addEdge(E e) {
		int src = nodes.indexOf(e.getSource());
		if (src == -1)
			throw new UnsupportedOperationException("The source node is not in the graph");

		int dest = nodes.indexOf(e.getDestination());
		if (dest == -1)
			throw new UnsupportedOperationException("The destination node is not in the graph");

		if (e.isUnconditional() && src == dest - 1)
			// just remove the cutoff
			cutoff.remove(src);
		else {
			extraEdges.computeIfAbsent(e.getSource(), n -> new NodeEdges<>()).outgoing.add(e);
			extraEdges.computeIfAbsent(e.getDestination(), n -> new NodeEdges<>()).ingoing.add(e);
		}
	}

	/**
	 * Removes the given edge from the list.
	 * 
	 * @param e the edge to remove
	 */
	public void removeEdge(E e) {
		int src = nodes.indexOf(e.getSource());
		int dest = nodes.indexOf(e.getDestination());
		if (src == -1 || dest == -1)
			return;

		if (e.isUnconditional() && src == dest - 1)
			// just add the cutoff
			cutoff.add(src);

		// the edge might still be inside the extraEdges
		// if this method has been invoked by removeNode
		NodeEdges<G, N, E> edges = extraEdges.get(e.getSource());
		if (edges != null) {
			edges.outgoing.remove(e);
			if (edges.ingoing.isEmpty() && edges.outgoing.isEmpty())
				extraEdges.remove(e.getSource());
		}
		edges = extraEdges.get(e.getDestination());
		if (edges != null) {
			edges.ingoing.remove(e);
			if (edges.ingoing.isEmpty() && edges.outgoing.isEmpty())
				extraEdges.remove(e.getDestination());
		}
	}

	/**
	 * Yields the edge connecting the two given nodes, if any. Yields
	 * {@code null} if such edge does not exist, or if one of the two node is
	 * not inside this list.
	 * 
	 * @param source      the source node
	 * @param destination the destination node
	 * 
	 * @return the edge connecting {@code source} to {@code destination}, or
	 *             {@code null}
	 */
	public final E getEdgeConnecting(N source, N destination) {
		int src = nodes.indexOf(source);
		int dest = nodes.indexOf(destination);
		if (src == -1 || dest == -1)
			return null;

		if (src == dest - 1 && !cutoff.contains(src))
			return sequentialSingleton.newInstance(source, destination);

		NodeEdges<G, N, E> edges = extraEdges.get(source);
		if (edges == null)
			return null;

		for (E e : edges.outgoing)
			if (e.getDestination().equals(destination))
				return e;
		return null;
	}

	/**
	 * Yields all edges connecting the two given nodes, if any. Yields an empty
	 * collection if no edge exists, or if one of the two nodes is not inside
	 * this list.
	 * 
	 * @param source      the source node
	 * @param destination the destination node
	 * 
	 * @return the edges connecting {@code source} to {@code destination}
	 */
	public Collection<E> getEdgesConnecting(N source, N destination) {
		int src = nodes.indexOf(source);
		int dest = nodes.indexOf(destination);
		if (src == -1 || dest == -1)
			return Collections.emptySet();

		SortedSet<E> result = new TreeSet<>();
		if (src == dest - 1 && !cutoff.contains(src))
			result.add(sequentialSingleton.newInstance(source, destination));

		NodeEdges<G, N, E> edges = extraEdges.get(source);
		if (edges != null)
			for (E e : edges.outgoing)
				if (e.getDestination().equals(destination))
					result.add(e);

		return result.isEmpty() ? Collections.emptySet() : result;
	}

	/**
	 * Yields the ingoing edges to the given node.
	 * 
	 * @param node the node
	 * 
	 * @return the collection of ingoing edges
	 */
	public final Collection<E> getIngoingEdges(N node) {
		int src = nodes.indexOf(node);
		if (src == -1)
			return Collections.emptySet();

		SortedSet<E> result = new TreeSet<>();
		if (src != 0 && !cutoff.contains(src - 1))
			result.add(sequentialSingleton.newInstance(nodes.get(src - 1), node));

		NodeEdges<G, N, E> edges = extraEdges.get(node);
		if (edges != null)
			result.addAll(edges.ingoing);

		return result.isEmpty() ? Collections.emptySet() : result;
	}

	/**
	 * Yields the outgoing edges from the given node.
	 * 
	 * @param node the node
	 * 
	 * @return the collection of outgoing edges
	 */
	public final Collection<E> getOutgoingEdges(N node) {
		int src = nodes.indexOf(node);
		if (src == -1)
			return Collections.emptySet();

		SortedSet<E> result = new TreeSet<>();
		if (src != nodes.size() - 1 && !cutoff.contains(src))
			result.add(sequentialSingleton.newInstance(node, nodes.get(src + 1)));

		NodeEdges<G, N, E> edges = extraEdges.get(node);
		if (edges != null)
			result.addAll(edges.outgoing);

		return result.isEmpty() ? Collections.emptySet() : result;
	}

	/**
	 * Yields the set of edges of this list.
	 * 
	 * @return the collection of edges
	 */
	public final Collection<E> getEdges() {
		SortedSet<E> result = extraEdges.values().stream()
				.flatMap(c -> Stream.concat(c.ingoing.stream(), c.outgoing.stream()))
				.distinct()
				.collect(new SortedSetCollector<>());
		for (int i = 0; i < nodes.size() - 1; i++)
			if (!cutoff.contains(i))
				result.add(sequentialSingleton.newInstance(nodes.get(i), nodes.get(i + 1)));
		return result;
	}

	/**
	 * Yields the collection of the nodes that are followers of the given one,
	 * that is, all nodes such that there exist an edge in this list going from
	 * the given node to such node.
	 * 
	 * @param node the node
	 * 
	 * @return the collection of followers
	 * 
	 * @throws IllegalArgumentException if the node is not in the graph
	 */
	public final Collection<N> followersOf(N node) {
		int src = nodes.indexOf(node);
		if (src == -1)
			throw new IllegalArgumentException("'" + node + "' is not in the graph");

		SortedSet<N> result = new TreeSet<>();
		if (src != nodes.size() - 1 && !cutoff.contains(src))
			result.add(nodes.get(src + 1));

		NodeEdges<G, N, E> edges = extraEdges.get(node);
		if (edges != null)
			result.addAll(edges.outgoing.stream().map(Edge::getDestination).collect(Collectors.toSet()));

		return result.isEmpty() ? Collections.emptySet() : result;
	}

	/**
	 * Yields the collection of the nodes that are predecessors of the given
	 * vertex, that is, all nodes such that there exist an edge in this list
	 * going from such node to the given one.
	 * 
	 * @param node the node
	 * 
	 * @return the collection of predecessors
	 * 
	 * @throws IllegalArgumentException if the node is not in the graph
	 */
	public final Collection<N> predecessorsOf(N node) {
		int src = nodes.indexOf(node);
		if (src == -1)
			throw new IllegalArgumentException("'" + node + "' is not in the graph");

		SortedSet<N> result = new TreeSet<>();
		if (src != 0 && !cutoff.contains(src - 1))
			result.add(nodes.get(src - 1));

		NodeEdges<G, N, E> edges = extraEdges.get(node);
		if (edges != null)
			result.addAll(edges.ingoing.stream().map(Edge::getSource).collect(Collectors.toSet()));

		return result.isEmpty() ? Collections.emptySet() : result;
	}

	/**
	 * Simplifies this list, removing all the given nodes and rewriting the edge
	 * set accordingly. This method will throw an
	 * {@link UnsupportedOperationException} if one of the nodes being
	 * simplified has an outgoing edge that is not unconditional, according to
	 * {@link CodeEdge#isUnconditional()}.
	 * 
	 * @param targets       the set of the {@link Node}s that needs to be
	 *                          simplified
	 * @param entrypoints   the collection of {@link Node}s that are considered
	 *                          as entrypoints of the graph built over this
	 *                          adjacency list
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
	public void simplify(Iterable<N> targets, Collection<N> entrypoints, Collection<E> removedEdges,
			Map<Pair<E, E>, E> replacedEdges) {
		removedEdges.clear();
		replacedEdges.clear();

		for (N t : targets) {
			boolean entry = entrypoints.contains(t);
			Collection<E> ingoing = getIngoingEdges(t);
			Collection<E> outgoing = getOutgoingEdges(t);

			if (ingoing.isEmpty() && !outgoing.isEmpty())
				// this is a entry node
				for (E out : outgoing) {
					if (!out.isUnconditional())
						throw new UnsupportedOperationException(
								EDGE_SIMPLIFY_ERROR + out.getClass().getSimpleName());

					// remove the edge
					removedEdges.add(out);
					if (entry)
						entrypoints.add(out.getDestination());
				}
			else if (!ingoing.isEmpty() && outgoing.isEmpty())
				// this is an exit node
				for (E in : ingoing) {
					if (!in.isUnconditional())
						throw new UnsupportedOperationException(
								EDGE_SIMPLIFY_ERROR + in.getClass().getSimpleName());

					// remove the edge
					removedEdges.add(in);
				}
			else
				// normal intermediate edge
				for (E in : ingoing)
					for (E out : outgoing) {
						if (!out.isUnconditional())
							throw new UnsupportedOperationException(
									EDGE_SIMPLIFY_ERROR + out.getClass().getSimpleName());

						// replicate the edge from ingoing.source to
						// outgoing.dest
						E _new = in.newInstance(in.getSource(), out.getDestination());
						replacedEdges.put(Pair.of(in, out), _new);
						addEdge(_new);
					}

			// remove the simplified node
			if (entry)
				entrypoints.remove(t);

			removeNode(t);
		}

		if (!removedEdges.isEmpty() || !replacedEdges.isEmpty())
			recomputeOffsets();
	}

	/**
	 * Yields {@code true} if the given node is contained in this list.
	 * 
	 * @param node the node to check
	 * 
	 * @return {@code true} if the node is in this list
	 */
	public boolean containsNode(N node) {
		return nodes.contains(node);
	}

	/**
	 * Yields {@code true} if the given edge is contained in this list.
	 * 
	 * @param edge the edge to check
	 * 
	 * @return {@code true} if the edge is in this list
	 */
	public boolean containsEdge(E edge) {
		int src = nodes.indexOf(edge.getSource());
		int dest = nodes.indexOf(edge.getDestination());
		if (src == -1 || dest == -1)
			return false;

		if (src == dest - 1 && !cutoff.contains(src)
				&& edge.isUnconditional()
				&& sequentialSingleton.newInstance(edge.getSource(), edge.getDestination()).equals(edge))
			return true;

		NodeEdges<G, N, E> edges = extraEdges.get(edge.getSource());
		if (edges == null)
			return false;

		return edges.outgoing.contains(edge);
	}

	@Override
	public Iterator<N> iterator() {
		return getNodes().iterator();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((cutoff == null) ? 0 : cutoff.hashCode());
		result = prime * result + ((extraEdges == null) ? 0 : extraEdges.hashCode());
		result = prime * result + ((nodes == null) ? 0 : nodes.hashCode());
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
		NodeList<?, ?, ?> other = (NodeList<?, ?, ?>) obj;
		if (cutoff == null) {
			if (other.cutoff != null)
				return false;
		} else if (!cutoff.equals(other.cutoff))
			return false;
		if (extraEdges == null) {
			if (other.extraEdges != null)
				return false;
		} else if (!extraEdges.equals(other.extraEdges))
			return false;
		if (nodes == null) {
			if (other.nodes != null)
				return false;
		} else if (!nodes.equals(other.nodes))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder res = new StringBuilder();
		for (int i = 0; i < nodes.size(); i++) {

			N node = nodes.get(i);
			res.append(node.getOffset()).append(": ").append(node);

			NodeEdges<G, N, E> edges = extraEdges.get(node);
			if (edges != null) {
				String ins = null, outs = null;
				if (!edges.ingoing.isEmpty())
					ins = StringUtils.join(edges.ingoing, ", ");
				if (!edges.outgoing.isEmpty())
					outs = StringUtils.join(edges.outgoing, ", ");
				if (ins != null || outs != null) {
					res.append(" [");
					if (ins != null)
						res.append("in: ").append(ins);
					if (outs != null) {
						if (ins != null)
							res.append(", ");
						res.append("out: ").append(outs);
					}

					res.append("]");
				}
			}

			res.append("\n");
			if (cutoff.contains(i))
				res.append("-----\n");
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
		if (!containsNode(root))
			return;

		Set<N> add = new HashSet<>(), remove = new HashSet<>(), check = new HashSet<>();

		if (containsNode(root))
			add.add(root);
		else {
			for (N n : nodes)
				if (n.equals(root))
					add.add(n);
		}

		do {
			// add the ones that were computed at last iteration
			remove.addAll(add);

			// find new successors
			check.clear();
			for (N node : add)
				for (N next : followersOf(node))
					check.add(next);

			// compute the ones that need to be added
			add.clear();
			check.stream().filter(n -> !remove.contains(n)).forEach(add::add);
		} while (!add.isEmpty());

		// we do not care about the output values
		simplify(remove, Collections.emptyList(), new LinkedList<>(), new HashMap<>());
	}

	/**
	 * Yields the entry nodes of this list, that is, the nodes that have no
	 * predecessors.
	 * 
	 * @return the entries nodes
	 */
	public Collection<N> getEntries() {
		return nodes.stream().filter(nodes -> predecessorsOf(nodes).isEmpty()).collect(new SortedSetCollector<>());
	}

	/**
	 * Yields the exit nodes of this list, that is, the nodes that have no
	 * followers.
	 * 
	 * @return the exit nodes
	 */
	public Collection<N> getExits() {
		return nodes.stream().filter(nodes -> followersOf(nodes).isEmpty()).collect(new SortedSetCollector<>());
	}

	/**
	 * Yields the minimum distance, in terms of number of edges to traverse,
	 * between the given nodes. If one of the nodes is not inside this list, or
	 * if no path can be found, this method returns {@code -1}. If the distance
	 * is greater than {@link Integer#MAX_VALUE}, {@link Integer#MAX_VALUE} is
	 * returned.
	 * 
	 * @param from the starting node
	 * @param to   the destination node
	 * 
	 * @return the minimum distance, in terms of number of edges to traverse,
	 *             between the given nodes
	 */
	public int distance(N from, N to) {
		if (!containsNode(from) || !containsNode(to))
			return -1;

		Map<N, Integer> distances = new IdentityHashMap<>(nodes.size());

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
	 * Merges this list with the given one, by adding all nodes and all edges
	 * contained in {@code other}.
	 * 
	 * @param other the list to merge into this one
	 */
	public void mergeWith(NodeList<G, N, E> other) {
		for (N node : other.getNodes())
			addNode(node);

		for (E edge : other.getEdges())
			addEdge(edge);
	}

	/**
	 * Validates this list, ensuring that the it is well formed. This method
	 * checks that:
	 * <ul>
	 * <li>each {@link Edge} is connected to {@link Node}s contained in the
	 * list</li>
	 * <li>all {@link Node}s with no ingoing edges are marked as entrypoints of
	 * the cfg (i.e. no deadcode)</li>
	 * </ul>
	 * 
	 * @param entrypoints the collection of {@link Node}s that are considered as
	 *                        entrypoints of the graph built over this adjacency
	 *                        list
	 * 
	 * @throws ProgramValidationException if one of the aforementioned checks
	 *                                        fail
	 */
	public void validate(Collection<N> entrypoints) throws ProgramValidationException {
		// all edges should be connected to statements inside the list
		for (N node : nodes) {
			NodeEdges<G, N, E> edges = extraEdges.get(node);
			if (edges == null)
				continue;

			for (E in : edges.ingoing)
				validateEdge(nodes, in);

			for (E out : edges.outgoing)
				validateEdge(nodes, out);

			// no deadcode
			int idx = nodes.indexOf(node);
			if (edges.ingoing.isEmpty()
					&& (idx == 0 || cutoff.contains(idx - 1))
					&& !entrypoints.contains(node))
				throw new ProgramValidationException(
						"Unreachable node that is not marked as entrypoint: " + node);
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

	/**
	 * Utility class for representing the edges tied to a node, split into two
	 * sets: ingoing and outgoing.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 * 
	 * @param <G> the type of the {@link CodeGraph}s the containing list can be
	 *                used in
	 * @param <N> the type of the {@link CodeNode}s in the containing list
	 * @param <E> the type of the {@link CodeEdge}s in the containing list
	 */
	public static class NodeEdges<G extends CodeGraph<G, N, E>,
			N extends CodeNode<G, N, E>,
			E extends CodeEdge<G, N, E>> {
		private final SortedSet<E> ingoing;
		private final SortedSet<E> outgoing;

		private NodeEdges() {
			ingoing = new TreeSet<>();
			outgoing = new TreeSet<>();
		}

		private NodeEdges(NodeEdges<G, N, E> other) {
			ingoing = new TreeSet<>(other.ingoing);
			outgoing = new TreeSet<>(other.outgoing);
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
		public boolean equals(Object obj) {
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
}