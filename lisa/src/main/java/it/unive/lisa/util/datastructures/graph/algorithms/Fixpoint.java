package it.unive.lisa.util.datastructures.graph.algorithms;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.unive.lisa.util.datastructures.graph.Edge;
import it.unive.lisa.util.datastructures.graph.Graph;
import it.unive.lisa.util.datastructures.graph.Node;
import it.unive.lisa.util.workset.WorkingSet;

public class Fixpoint<G extends Graph<G, N, E>, N extends Node<N, E, G>, E extends Edge<N, E, G>, T> {

	private static final String ERROR = "Exception while %s of '%s' in '%s'";

	private final Graph<G, N, E> graph;
	private final Map<N, T> result;

	public Fixpoint(Graph<G, N, E> graph) {
		this.graph = graph;
		result = new HashMap<>(graph.getNodesCount());
	}

	@FunctionalInterface
	public interface SemanticFunction<E, T> {
		T compute(E element, T entrystate) throws Exception;
	}

	@FunctionalInterface
	public interface SemanticBiFunction<E, T> {
		T compute(E element, T first, T second) throws Exception;
	}

	@FunctionalInterface
	public interface SemanticBiPredicate<E, T> {
		boolean test(E element, T first, T second) throws Exception;
	}

	public Map<N, T> fixpoint(Map<N, T> startingPoints, WorkingSet<N> ws, SemanticFunction<N, T> nodeSemantics,
			SemanticFunction<E, T> edgeSemantics, SemanticBiFunction<N, T> join, SemanticBiPredicate<N, T> equality)
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

			T entrystate = getEntryState(current, startingPoints.get(current), edgeSemantics, join);
			if (entrystate == null)
				throw new FixpointException("'" + current + "' does not have an entry state");

			T oldApprox = result.get(current);

			try {
				newApprox = nodeSemantics.compute(current, entrystate);
			} catch (Exception e) {
				throw new FixpointException(format(ERROR, "computing semantics", current, graph), e);
			}

			if (oldApprox != null)
				try {
					newApprox = join.compute(current, newApprox, oldApprox);
				} catch (Exception e) {
					throw new FixpointException(format(ERROR, "joining states", current, graph), e);
				}

			try {
				if (oldApprox == null || !equality.test(current, newApprox, oldApprox)) {
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

	private T getEntryState(N current, T startstate, SemanticFunction<E, T> edgeSemantics,
			SemanticBiFunction<N, T> join)
			throws FixpointException {
		T entrystate = startstate;
		Collection<N> preds = graph.predecessorsOf(current);
		List<T> states = new ArrayList<>(preds.size());

		for (N pred : preds)
			if (result.containsKey(pred)) {
				// this might not have been computed yet
				E edge = graph.getEdgeConnecting(pred, current);
				try {
					states.add(edgeSemantics.compute(edge, result.get(pred)));
				} catch (Exception e) {
					throw new FixpointException(format(ERROR, "computing edge semantics", edge, graph), e);
				}
			}

		try {
			for (T s : states)
				if (entrystate == null)
					entrystate = s;
				else
					entrystate = join.compute(current, entrystate, s);
		} catch (Exception e) {
			throw new FixpointException(format(ERROR, "creating entry state", current, graph), e);
		}

		return entrystate;
	}
}
