package it.unive.lisa.util.datastructures.graph.algorithms;

import static java.lang.String.format;

import it.unive.lisa.util.collections.workset.WorkingSet;
import it.unive.lisa.util.datastructures.graph.Edge;
import it.unive.lisa.util.datastructures.graph.Graph;
import it.unive.lisa.util.datastructures.graph.Node;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A backward fixpoint algorithm for a {@link Graph}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <G> the type of the source {@link Graph}
 * @param <N> the type of the {@link Node}s in the source graph
 * @param <E> the type of the {@link Edge}s in the source graph
 * @param <T> the type of data computed by the fixpoint
 */
public abstract class BackwardFixpoint<
		G extends Graph<G, N, E>,
		N extends Node<G, N, E>,
		E extends Edge<G, N, E>,
		T>
		implements
		Fixpoint<G, N, E, T>,
		FixpointImplementation<G, N, E, T> {

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
	protected BackwardFixpoint(
			G graph,
			boolean forceFullEvaluation) {
		this.graph = graph;
		this.forceFullEvaluation = forceFullEvaluation;
	}

	@Override
	public Map<N, T> fixpoint(
			Map<N, T> startingPoints,
			WorkingSet<N> ws)
			throws FixpointException {
		return fixpoint(startingPoints, ws, null);
	}

	@Override
	public Map<N, T> fixpoint(
			Map<N, T> startingPoints,
			WorkingSet<N> ws,
			Map<N, T> initialResult)
			throws FixpointException {
		Map<N, T> result = initialResult == null ? new HashMap<>(graph.getNodesCount()) : new HashMap<>(initialResult);
		startingPoints.keySet().forEach(ws::push);

		Set<N> toProcess = null;
		if (forceFullEvaluation)
			toProcess = new HashSet<>(graph.getNodes());

		while (!ws.isEmpty()) {
			N current = ws.pop();

			if (current == null)
				throw new FixpointException("null node encountered during fixpoint in '" + graph + "'");
			if (!graph.containsNode(current))
				throw new FixpointException("'" + current + "' is not part of '" + graph + "'");

			T exitstate = getExitState(graph, current, startingPoints.get(current), result);
			if (exitstate == null)
				throw new FixpointException("'" + current + "' does not have an entry state");

			T newApprox;
			N last;
			try {
				Pair<T, N> r = semantics(current, exitstate, result);
				newApprox = r.getLeft();
				last = r.getRight();
			} catch (Exception e) {
				throw new FixpointException(format(ERROR, "computing semantics", current, graph), e);
			}

			T oldApprox = result.get(last);
			T postApprox = newApprox;
			if (oldApprox != null)
				try {
					postApprox = join(last, newApprox, oldApprox);
				} catch (Exception e) {
					throw new FixpointException(format(ERROR, "joining states", last, graph), e);
				}
			try {
				// we go on if we were asked to analyze all nodes at least once
				if ((forceFullEvaluation && toProcess.remove(current))
						// or if this is the first time we analyze this node
						|| oldApprox == null
						// or if we got a result that should not be considered
						// equal
						|| !leq(last, postApprox, oldApprox)) {
					result.put(last, postApprox);
					for (N instr : graph.predecessorsOf(last))
						ws.push(instr);
				}
			} catch (Exception e) {
				throw new FixpointException(format(ERROR, "updating result", last, graph), e);
			}
		}

		cleanup(result);

		return result;
	}

	/**
	 * Yields the exit state for the given node.
	 * 
	 * @param graph      the graph containing the node
	 * @param node       the node under evaluation
	 * @param startstate a predefined ending state that must be taken into
	 *                       account for the computation
	 * @param result     the current approximations for each node
	 * 
	 * @return the computed state
	 * 
	 * @throws FixpointException if something goes wrong during the computation
	 */
	protected T getExitState(
			G graph,
			N node,
			T startstate,
			Map<N, T> result)
			throws FixpointException {
		Collection<N> follows = graph.followersOf(node);
		List<T> states = new ArrayList<>(follows.size());

		for (N follow : follows)
			if (result.containsKey(follow)) {
				// this might not have been computed yet
				E edge = graph.getEdgeConnecting(node, follow);
				try {
					states.add(traverse(edge, result.get(follow)));
				} catch (Exception e) {
					throw new FixpointException(format(ERROR, "computing edge semantics", edge, graph), e);
				}
			}

		T exitstate = startstate;
		try {
			for (T s : states)
				if (exitstate == null)
					exitstate = s;
				else
					exitstate = union(node, exitstate, s);
		} catch (Exception e) {
			throw new FixpointException(format(ERROR, "creating entry state", node, graph), e);
		}

		return exitstate;
	}

	/**
	 * Cleans up the result of the fixpoint computation before returning it.
	 * 
	 * @param result the computed result
	 */
	protected void cleanup(
			Map<N, T> result) {
		// by default, do nothing
	}
}
