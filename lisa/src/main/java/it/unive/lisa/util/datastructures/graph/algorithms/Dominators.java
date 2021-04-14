package it.unive.lisa.util.datastructures.graph.algorithms;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import it.unive.lisa.util.datastructures.graph.Edge;
import it.unive.lisa.util.datastructures.graph.Graph;
import it.unive.lisa.util.datastructures.graph.Node;
import it.unive.lisa.util.workset.FIFOWorkingSet;
import it.unive.lisa.util.workset.WorkingSet;

public class Dominators<G extends Graph<G, N, E>, N extends Node<N, E, G>, E extends Edge<N, E, G>> {

	private final Map<N, Set<N>> dominators;

	public Dominators() {
		dominators = new IdentityHashMap<>();
	}

	public Map<N, Set<N>> getDominators() {
		return dominators;
	}

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
			if (doms != null) // might not have been processed yet
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
