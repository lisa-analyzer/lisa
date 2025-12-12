package it.unive.lisa.util.datastructures.graph.algorithms;

import static java.lang.String.format;

import it.unive.lisa.util.collections.workset.WorkingSet;
import it.unive.lisa.util.datastructures.graph.Edge;
import it.unive.lisa.util.datastructures.graph.Graph;
import it.unive.lisa.util.datastructures.graph.Node;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A forward fixpoint algorithm for a {@link Graph}, parametric to the
 * {@link FixpointImplementation} that one wants to use to compute the results.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <G> the type of the source {@link Graph}
 * @param <N> the type of the {@link Node}s in the source graph
 * @param <E> the type of the {@link Edge}s in the source graph
 * @param <T> the type of data computed by the fixpoint
 */
public class ForwardFixpoint<
		G extends Graph<G, N, E>,
		N extends Node<G, N, E>,
		E extends Edge<G, N, E>,
		T>
		implements
		Fixpoint<G, N, E, T> {

	/**
	 * The graph to target.
	 */
	protected final G graph;

	/**
	 * Whether or not all nodes should be processed at least once.
	 */
	protected final boolean forceFullEvaluation;

	/**
	 * Builds a fixpoint for the given {@link Graph}.
	 * 
	 * @param graph               the source graph
	 * @param forceFullEvaluation whether or not the fixpoint should evaluate
	 *                                all nodes independently of the fixpoint
	 *                                implementation
	 */
	public ForwardFixpoint(
			G graph,
			boolean forceFullEvaluation) {
		this.graph = graph;
		this.forceFullEvaluation = forceFullEvaluation;
	}

	@Override
	public Fixpoint<G, N, E, T> mk(
			G graph,
			boolean forceFullEvaluation) {
		return new ForwardFixpoint<>(graph, forceFullEvaluation);
	}

	@Override
	public Map<N, T> fixpoint(
			Map<N, T> startingPoints,
			WorkingSet<N> ws,
			FixpointImplementation<N, E, T> implementation)
			throws FixpointException {
		return fixpoint(startingPoints, ws, implementation, null);
	}

	@Override
	public Map<N, T> fixpoint(
			Map<N, T> startingPoints,
			WorkingSet<N> ws,
			FixpointImplementation<N, E, T> implementation,
			Map<N, T> initialResult)
			throws FixpointException {
		Map<N, T> result = initialResult == null ? new HashMap<>(graph.getNodesCount()) : new HashMap<>(initialResult);
		startingPoints.keySet().forEach(ws::push);

		Set<N> toProcess = null;
		if (forceFullEvaluation)
			toProcess = new HashSet<>(graph.getNodes());

		T newApprox;
		while (!ws.isEmpty()) {
			N current = ws.pop();

			if (current == null)
				throw new FixpointException("null node encountered during fixpoint in '" + graph + "'");
			if (!graph.containsNode(current))
				throw new FixpointException("'" + current + "' is not part of '" + graph + "'");

			T entrystate = getEntryState(graph, current, startingPoints.get(current), implementation, result);
			if (entrystate == null)
				throw new FixpointException("'" + current + "' does not have an entry state");

			try {
				newApprox = implementation.semantics(current, entrystate);
			} catch (Exception e) {
				throw new FixpointException(format(ERROR, "computing semantics", current, graph), e);
			}

			T oldApprox = result.get(current);
			T postApprox = newApprox;
			if (oldApprox != null)
				try {
					postApprox = implementation.join(current, newApprox, oldApprox);
				} catch (Exception e) {
					throw new FixpointException(format(ERROR, "joining states", current, graph), e);
				}
			try {
				// we go on if we were asked to analyze all nodes at least once
				if ((forceFullEvaluation && toProcess.remove(current))
						// or if this is the first time we analyze this node
						|| oldApprox == null
						// or if we got a result that should not be considered
						// equal
						|| !implementation.leq(current, postApprox, oldApprox)) {
					result.put(current, postApprox);
					for (N instr : graph.followersOf(current))
						ws.push(instr);
				}
			} catch (Exception e) {
				throw new FixpointException(format(ERROR, "updating result", current, graph), e);
			}
		}

		return result;
	}

}
