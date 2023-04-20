package it.unive.lisa.util.datastructures.graph.algorithms;

import it.unive.lisa.util.collections.workset.FIFOWorkingSet;
import it.unive.lisa.util.collections.workset.WorkingSet;
import it.unive.lisa.util.datastructures.graph.Edge;
import it.unive.lisa.util.datastructures.graph.Graph;
import it.unive.lisa.util.datastructures.graph.Node;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * An algorithms that evaluates the dominators of each node in a graph. A node
 * {@code d} dominates a node {@code n} if every path from an entry node to
 * {@code n} must go through {@code d}. By definition, every node dominates
 * itself.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <G> the type of the target {@link Graph}s
 * @param <N> the type of {@link Node}s in the target graphs
 * @param <E> the type of {@link Edge}s in the target graphs
 * 
 * @see <a href=
 *          "https://en.wikipedia.org/wiki/Dominator_(graph_theory)">Dominators
 *          (graph theory)</a>
 */
public class Dominators<G extends Graph<G, N, E>, N extends Node<G, N, E>, E extends Edge<G, N, E>> {

	private final Map<N, Set<N>> dominators;

	/**
	 * Builds the dominators. To run the algorithm, use {@link #build(Graph)}.
	 */
	public Dominators() {
		dominators = new IdentityHashMap<>();
	}

	/**
	 * Yields the last computed dominators through {@link #build(Graph)}. The
	 * returned value is a map going from each node of the given graph to the
	 * set of nodes that dominates it.
	 * 
	 * @return a map containing, for each node of the graph passed as argument
	 *             to the last call to {@link #build(Graph)}, the set of nodes
	 *             that dominates it
	 */
	public Map<N, Set<N>> getDominators() {
		return dominators;
	}

	/**
	 * Builds the dominators for the given graph. The returned value, that can
	 * also be accessed later through {@link #getDominators()}, is a map going
	 * from each node of the given graph to the set of nodes that dominates it.
	 * 
	 * @param graph the graph whose dominators are to be computed
	 * 
	 * @return a map containing, for each node of the graph, the set of nodes
	 *             that dominates it
	 */
	public Map<N, Set<N>> build(G graph) {
		dominators.clear();
		Collection<N> entries = graph.getEntrypoints();
		WorkingSet<N> ws = FIFOWorkingSet.mk();
		entries.forEach(ws::push);
		while (!ws.isEmpty()) {
			N current = ws.pop();

			Set<N> res;
			if (entries.contains(current))
				res = new HashSet<>();
			else
				res = intersect(graph.predecessorsOf(current));
			res.add(current);

			if (!res.equals(dominators.get(current))) {
				dominators.put(current, res);
				graph.followersOf(current).forEach(ws::push);
			}
		}

		return dominators;
	}

	private Set<N> intersect(Collection<N> nodes) {
		if (nodes == null || nodes.isEmpty())
			return Collections.emptySet();

		Set<N> res = null;
		for (N node : nodes) {
			Set<N> doms = dominators.get(node);
			// might not have been processed yet
			if (doms != null)
				if (res == null)
					res = new HashSet<>(doms);
				else
					res.retainAll(doms);
			if (res != null && res.isEmpty())
				// nothing more to do
				break;
		}

		return res == null ? new HashSet<>() : res;
	}
}
