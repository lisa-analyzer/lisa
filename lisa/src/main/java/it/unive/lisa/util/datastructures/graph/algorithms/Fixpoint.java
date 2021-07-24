package it.unive.lisa.util.datastructures.graph.algorithms;

import static java.lang.String.format;

import it.unive.lisa.util.collections.workset.WorkingSet;
import it.unive.lisa.util.datastructures.graph.Edge;
import it.unive.lisa.util.datastructures.graph.Graph;
import it.unive.lisa.util.datastructures.graph.Node;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Fixpoint<G extends Graph<G, N, E>, N extends Node<N, E, G>, E extends Edge<N, E, G>, T> {

	private static final String ERROR = "Exception while %s of '%s' in '%s'";

	private final Graph<G, N, E> graph;
	private final Map<N, T> result;

	public Fixpoint(Graph<G, N, E> graph) {
		this.graph = graph;
		result = new HashMap<>(graph.getNodesCount());
	}

	public interface FixpointImplementation<N, E, T> {
		T semantics(N node, T entrystate) throws Exception;

		T traverse(E edge, T entrystate) throws Exception;

		T union(N node, T left, T right) throws Exception;

		T join(N node, T approx, T old) throws Exception;

		boolean equality(N node, T approx, T old) throws Exception;
	}

	public Map<N, T> fixpoint(Map<N, T> startingPoints, WorkingSet<N> ws,
			FixpointImplementation<N, E, T> implementation)
			throws FixpointException {
		result.clear();
		startingPoints.keySet().forEach(ws::push);

		T newApprox;
		while (!ws.isEmpty()) {
			N current = ws.pop();

			if (current == null)
				throw new FixpointException("null node encountered during fixpoint in '" + graph + "'");
			if (!graph.getAdjacencyMatrix().containsNode(current))
				throw new FixpointException("'" + current + "' is not part of '" + graph + "'");

			T entrystate = getEntryState(current, startingPoints.get(current), implementation);
			if (entrystate == null)
				throw new FixpointException("'" + current + "' does not have an entry state");

			try {
				newApprox = implementation.semantics(current, entrystate);
			} catch (Exception e) {
				throw new FixpointException(format(ERROR, "computing semantics", current, graph), e);
			}

			T oldApprox = result.get(current);
			if (oldApprox != null)
				try {
					newApprox = implementation.join(current, newApprox, oldApprox);
				} catch (Exception e) {
					throw new FixpointException(format(ERROR, "joining states", current, graph), e);
				}

			try {
				if (oldApprox == null || !implementation.equality(current, newApprox, oldApprox)) {
					result.put(current, newApprox);
					for (N instr : graph.followersOf(current))
						ws.push(instr);
				}
			} catch (Exception e) {
				throw new FixpointException(format(ERROR, "updating result", current, graph), e);
			}
		}

		return result;
	}

	private T getEntryState(N current, T startstate, FixpointImplementation<N, E, T> implementation)
			throws FixpointException {
		Collection<N> preds = graph.predecessorsOf(current);
		List<T> states = new ArrayList<>(preds.size());

		for (N pred : preds)
			if (result.containsKey(pred)) {
				// this might not have been computed yet
				E edge = graph.getEdgeConnecting(pred, current);
				try {
					states.add(implementation.traverse(edge, result.get(pred)));
				} catch (Exception e) {
					throw new FixpointException(format(ERROR, "computing edge semantics", edge, graph), e);
				}
			}

		T entrystate = startstate;
		try {
			for (T s : states)
				if (entrystate == null)
					entrystate = s;
				else
					entrystate = implementation.union(current, entrystate, s);
		} catch (Exception e) {
			throw new FixpointException(format(ERROR, "creating entry state", current, graph), e);
		}

		return entrystate;
	}
}
