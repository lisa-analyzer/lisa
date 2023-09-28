package it.unive.lisa.util.datastructures.graph.algorithms;

import it.unive.lisa.util.collections.workset.LIFOWorkingSet;
import it.unive.lisa.util.collections.workset.WorkingSet;
import it.unive.lisa.util.datastructures.graph.Edge;
import it.unive.lisa.util.datastructures.graph.Graph;
import it.unive.lisa.util.datastructures.graph.Node;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * An algorithms that evaluates the strongly connected components a graph. A
 * strongly connected component is a set of nodes of the graph that is strongly
 * connected, that is, where there exist a path in both directions between any
 * two nodes of the set. This is implemented Tarjan's algorithm, computing
 * maximal non-trivial SCCs (i.e., containing at least one edge and such that no
 * other node could be added without breaking strong connectivity).
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <G> the type of the target {@link Graph}s
 * @param <N> the type of {@link Node}s in the target graphs
 * @param <E> the type of {@link Edge}s in the target graphs
 * 
 * @see <a href=
 *          "https://en.wikipedia.org/wiki/Strongly_connected_component">Strongly
 *          connected component</a>
 * @see <a href=
 *          "https://en.wikipedia.org/wiki/Tarjan%27s_strongly_connected_components_algorithm">Tarjan
 *          strongly connected components algorithm</a>
 */
public class SCCs<G extends Graph<G, N, E>, N extends Node<G, N, E>, E extends Edge<G, N, E>> {

	private final Collection<Collection<N>> sccs;

	/**
	 * Builds the SCCs. To run the algorithm, use {@link #build(Graph)}.
	 */
	public SCCs() {
		sccs = new HashSet<>();
	}

	/**
	 * Yields the last computed strongly connected components through
	 * {@link #build(Graph)}. Note that, depending on the method used for
	 * computing them, the returned value might contain <b>all</b> components,
	 * including trivial single-node ones.
	 * 
	 * @return a set containing all the strongly connected components
	 */
	public Collection<Collection<N>> getSCCs() {
		return sccs;
	}

	/**
	 * Builds the strongly connected components for the given graph. The
	 * returned value can also be accessed later through {@link #getSCCs()}.
	 * Note that the returned value does not contain trivial single-node
	 * components. This also holds for the value returned by {@link #getSCCs()}.
	 * 
	 * @param graph the graph whose sccs are to be computed
	 * 
	 * @return the set of non trivial sccs
	 */
	public Collection<Collection<N>> buildNonTrivial(
			G graph) {
		build(graph);
		Collection<Collection<N>> toRemove = new HashSet<>();
		for (Collection<N> scc : sccs) {
			N first = scc.iterator().next();
			if (scc.size() == 1 && !graph.followersOf(first).contains(first))
				// only one node with no self loops
				toRemove.add(scc);
		}
		sccs.removeAll(toRemove);
		return sccs;
	}

	/**
	 * Builds the strongly connected components for the given graph. The
	 * returned value can also be accessed later through {@link #getSCCs()}.
	 * Note that the returned value contains <b>all</b> components, including
	 * trivial single-node ones. This also holds for the value returned by
	 * {@link #getSCCs()}.
	 * 
	 * @param graph the graph whose sccs are to be computed
	 * 
	 * @return the set of all sccs
	 */
	public Collection<Collection<N>> build(
			G graph) {
		sccs.clear();

		int index = 0;
		Map<N, Integer> indexes = new HashMap<>();
		Map<N, Integer> lowlinks = new HashMap<>();
		WorkingSet<N> ws = LIFOWorkingSet.mk();
		for (N n : graph.getNodes())
			if (!indexes.containsKey(n))
				index = strongconnect(graph, indexes, lowlinks, index, ws, n);

		return sccs;
	}

	private int strongconnect(
			G graph,
			Map<N, Integer> indexes,
			Map<N, Integer> lowlinks,
			int index,
			WorkingSet<N> ws,
			N v) {
		indexes.put(v, index);
		lowlinks.put(v, index);
		index++;
		ws.push(v);

		for (N w : graph.followersOf(v))
			if (!indexes.containsKey(w)) {
				index = strongconnect(graph, indexes, lowlinks, index, ws, w);
				lowlinks.put(v, Math.min(lowlinks.get(v), lowlinks.get(w)));
			} else if (ws.getContents().contains(w))
				lowlinks.put(v, Math.min(lowlinks.get(v), indexes.get(w)));

		if (lowlinks.get(v) == indexes.get(v)) {
			Set<N> scc = new HashSet<>();
			N w = null;
			do {
				w = ws.pop();
				scc.add(w);
			} while (w != v);
			sccs.add(scc);
		}

		return index;
	}
}
