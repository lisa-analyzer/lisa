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

/**
 * A backward fixpoint algorithm for a {@link Graph}, parametric to the
 * {@link Fixpoint.FixpointImplementation} that one wants to use to compute the
 * results.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <G> the type of the source {@link Graph}
 * @param <N> the type of the {@link Node}s in the source graph
 * @param <E> the type of the {@link Edge}s in the source graph
 * @param <T> the type of data computed by the fixpoint
 */
public class BackwardFixpoint<G extends Graph<G, N, E>, N extends Node<G, N, E>, E extends Edge<G, N, E>, T> {

	/**
	 * Common format for error messages.
	 */
	protected static final String ERROR = "Exception while %s of '%s' in '%s'";

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
	public BackwardFixpoint(
			G graph,
			boolean forceFullEvaluation) {
		this.graph = graph;
		this.forceFullEvaluation = forceFullEvaluation;
	}

	/**
	 * Runs the fixpoint. Invoking this method effectively recomputes the
	 * result: no caching on previous runs is executed. It starts with empty
	 * result.
	 * 
	 * @param startingPoints a map containing all the nodes to start the
	 *                           fixpoint at, each mapped to its entry state.
	 * @param ws             the instance of {@link WorkingSet} to use for the
	 *                           fixpoint
	 * @param implementation the {@link Fixpoint.FixpointImplementation} to use
	 *                           for running the fixpoint
	 * 
	 * @return a mapping from each (reachable) node of the source graph to the
	 *             fixpoint result computed at that node
	 * 
	 * @throws FixpointException if something goes wrong during the fixpoint
	 *                               execution
	 */
	public Map<N, T> fixpoint(
			Map<N, T> startingPoints,
			WorkingSet<N> ws,
			Fixpoint.FixpointImplementation<N, E, T> implementation)
			throws FixpointException {
		return fixpoint(startingPoints, ws, implementation, null);
	}

	/**
	 * Runs the fixpoint. Invoking this method effectively recomputes the
	 * result: no caching on previous runs is executed.
	 * 
	 * @param startingPoints a map containing all the nodes to start the
	 *                           fixpoint at, each mapped to its entry state.
	 * @param ws             the instance of {@link WorkingSet} to use for the
	 *                           fixpoint
	 * @param implementation the {@link Fixpoint.FixpointImplementation} to use
	 *                           for running the fixpoint
	 * @param initialResult  the map of initial result to use for running the
	 *                           fixpoint
	 * 
	 * @return a mapping from each (reachable) node of the source graph to the
	 *             fixpoint result computed at that node
	 * 
	 * @throws FixpointException if something goes wrong during the fixpoint
	 *                               execution
	 */
	public Map<N, T> fixpoint(
			Map<N, T> startingPoints,
			WorkingSet<N> ws,
			Fixpoint.FixpointImplementation<N, E, T> implementation,
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

			T exitstate = getExitState(current, startingPoints.get(current), implementation, result);
			if (exitstate == null)
				throw new FixpointException("'" + current + "' does not have an entry state");

			try {
				newApprox = implementation.semantics(current, exitstate);
			} catch (Exception e) {
				throw new FixpointException(format(ERROR, "computing semantics", current, graph), e);
			}

			T oldApprox = result.get(current);
			if (oldApprox != null)
				try {
					newApprox = implementation.operation(current, newApprox, oldApprox);
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
						|| !implementation.equality(current, newApprox, oldApprox)) {
					result.put(current, newApprox);
					for (N instr : graph.predecessorsOf(current))
						ws.push(instr);
				}
			} catch (Exception e) {
				throw new FixpointException(format(ERROR, "updating result", current, graph), e);
			}
		}

		return result;
	}

	/**
	 * Yields the exit state for the given node.
	 * 
	 * @param node           the node under evaluation
	 * @param startstate     a predefined ending state that must be taken into
	 *                           account for the computation
	 * @param implementation the fixpoint implementation that knows how to
	 *                           combine different states
	 * @param result         the current approximations for each node
	 * 
	 * @return the computed state
	 * 
	 * @throws FixpointException if something goes wrong during the computation
	 */
	protected T getExitState(
			N node,
			T startstate,
			Fixpoint.FixpointImplementation<N, E, T> implementation,
			Map<N, T> result)
			throws FixpointException {
		Collection<N> follows = graph.followersOf(node);
		List<T> states = new ArrayList<>(follows.size());

		for (N follow : follows)
			if (result.containsKey(follow)) {
				// this might not have been computed yet
				E edge = graph.getEdgeConnecting(node, follow);
				try {
					states.add(implementation.traverse(edge, result.get(follow)));
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
					exitstate = implementation.union(node, exitstate, s);
		} catch (Exception e) {
			throw new FixpointException(format(ERROR, "creating entry state", node, graph), e);
		}

		return exitstate;
	}
}
