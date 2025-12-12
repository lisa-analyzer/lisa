package it.unive.lisa.util.datastructures.graph.algorithms;

import static java.lang.String.format;

import it.unive.lisa.util.collections.workset.WorkingSet;
import it.unive.lisa.util.datastructures.graph.Edge;
import it.unive.lisa.util.datastructures.graph.Graph;
import it.unive.lisa.util.datastructures.graph.Node;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * An interface for fixpoint algorithms for a {@link Graph}, parametric to the
 * {@link FixpointImplementation} that one wants to use to compute the results.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <G> the type of the source {@link Graph}
 * @param <N> the type of the {@link Node}s in the source graph
 * @param <E> the type of the {@link Edge}s in the source graph
 * @param <T> the type of data computed by the fixpoint
 */
public interface Fixpoint<
		G extends Graph<G, N, E>,
		N extends Node<G, N, E>,
		E extends Edge<G, N, E>,
		T> {

	/**
	 * Common format for error messages.
	 */
	public static final String ERROR = "Exception while %s of '%s' in '%s'";

	/**
	 * Builds a fixpoint for the given {@link Graph}.
	 * 
	 * @param graph               the source graph
	 * @param forceFullEvaluation whether or not the fixpoint should evaluate
	 *                                all nodes independently of the fixpoint
	 *                                implementation
	 */
	Fixpoint<G, N, E, T> mk(
			G graph,
			boolean forceFullEvaluation);

	/**
	 * Concrete implementation of the general methods used by a fixpoint
	 * algorithm to perform.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 * 
	 * @param <N> the type of the {@link Node}s in the source graph
	 * @param <E> the type of the {@link Edge}s in the source graph
	 * @param <T> the type of data computed by the fixpoint
	 */
	public interface FixpointImplementation<N, E, T> {

		/**
		 * Given a node and its entry state, computes its exit state relying on
		 * its semantics.<br>
		 * <br>
		 * This callback is invoked after the overall entry state for a node has
		 * been computed through {@link #union(Object, Object, Object)} of the
		 * exit states of its predecessors.
		 * 
		 * @param node       the node where the computation takes place
		 * @param entrystate the computed state before the computation
		 * 
		 * @return the exit state
		 * 
		 * @throws Exception if something goes wrong during the computation
		 */
		T semantics(
				N node,
				T entrystate)
				throws Exception;

		/**
		 * Given an edge and a state, computes a new state by modifying the
		 * given one assuming that the edge gets traversed.<br>
		 * <br>
		 * This callback is invoked while computing the overall entry state for
		 * a node to filter a state according to the logic of the edge (an edge
		 * might be always traversed, or only if a condition holds).
		 * 
		 * @param edge       the edge where the computation takes place
		 * @param entrystate the state before traversing
		 * 
		 * @return the state after traversing
		 * 
		 * @throws Exception if something goes wrong during the computation
		 */
		T traverse(
				E edge,
				T entrystate)
				throws Exception;

		/**
		 * Given a node and two states, computes their union (i.e. least upper
		 * bound, <b>not</b> widening).<br>
		 * <br>
		 * This callback is invoked for the computation of the overall entry
		 * state for a node to merge the exit states of its predecessors, after
		 * traversing the edges connecting them through
		 * {@link #traverse(Object, Object)}.
		 * 
		 * @param node  the node where the computation takes place
		 * @param left  the first state
		 * @param right the second state
		 * 
		 * @return the union of the states
		 * 
		 * @throws Exception if something goes wrong during the computation
		 */
		T union(
				N node,
				T left,
				T right)
				throws Exception;

		/**
		 * Given a node and two states, joins the states (i.e. least upper bound
		 * <i>or</i> widening) together.<br>
		 * <br>
		 * This callback is invoked after the exit state of a node has been
		 * computed through {@link #semantics(Object, Object)}, to join it with
		 * results from older fixpoint iterations.
		 * 
		 * @param node   the node where the computation takes place
		 * @param approx the most recent state
		 * @param old    the older state
		 * 
		 * @return the joined state
		 * 
		 * @throws Exception if something goes wrong during the computation
		 */
		T join(
				N node,
				T approx,
				T old)
				throws Exception;

		/**
		 * Given a node and two states, yields whether or not the most recent
		 * one has to be considered <i>less than or equal</i> to the older one
		 * in terms of fixpoint iterations. This means that if this method
		 * returns {@code true}, the result for the given node won't be updated
		 * and the successors of such node won't be added back to the list of
		 * nodes to process.<br>
		 * <br>
		 * This callback is invoked after the exit state of a node has been
		 * computed through {@link #semantics(Object, Object)} and joined with
		 * the older one through {@link #join(Object, Object, Object)}.
		 * 
		 * @param node   the node where the computation takes place
		 * @param approx the most recent state
		 * @param old    the older state
		 * 
		 * @return {@code true} if there is no need to update the previous
		 *             result, {@code false} otherwise
		 * 
		 * @throws Exception if something goes wrong during the computation
		 */
		boolean leq(
				N node,
				T approx,
				T old)
				throws Exception;

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
	 * @param implementation the {@link FixpointImplementation} to use for
	 *                           running the fixpoint
	 * 
	 * @return a mapping from each (reachable) node of the source graph to the
	 *             fixpoint result computed at that node
	 * 
	 * @throws FixpointException if something goes wrong during the fixpoint
	 *                               execution
	 */
	Map<N, T> fixpoint(
			Map<N, T> startingPoints,
			WorkingSet<N> ws,
			FixpointImplementation<N, E, T> implementation)
			throws FixpointException;

	/**
	 * Runs the fixpoint. Invoking this method effectively recomputes the
	 * result: no caching on previous runs is executed.
	 * 
	 * @param startingPoints a map containing all the nodes to start the
	 *                           fixpoint at, each mapped to its entry state.
	 * @param ws             the instance of {@link WorkingSet} to use for the
	 *                           fixpoint
	 * @param implementation the {@link FixpointImplementation} to use for
	 *                           running the fixpoint
	 * @param initialResult  the map of initial result to use for running the
	 *                           fixpoint
	 * 
	 * @return a mapping from each (reachable) node of the source graph to the
	 *             fixpoint result computed at that node
	 * 
	 * @throws FixpointException if something goes wrong during the fixpoint
	 *                               execution
	 */
	Map<N, T> fixpoint(
			Map<N, T> startingPoints,
			WorkingSet<N> ws,
			FixpointImplementation<N, E, T> implementation,
			Map<N, T> initialResult)
			throws FixpointException;

	/**
	 * Yields the entry state for the given node.
	 * 
	 * @param graph          the graph containing the node
	 * @param node           the node under evaluation
	 * @param startstate     a predefined starting state that must be taken into
	 *                           account for the computation
	 * @param implementation the fixpoint implementation that knows how to
	 *                           combine different states
	 * @param result         the current approximations for each node
	 * 
	 * @return the computed state
	 * 
	 * @throws FixpointException if something goes wrong during the computation
	 */
	default T getEntryState(
			G graph,
			N node,
			T startstate,
			FixpointImplementation<N, E, T> implementation,
			Map<N, T> result)
			throws FixpointException {
		Collection<N> preds = graph.predecessorsOf(node);
		List<T> states = new ArrayList<>(preds.size());

		for (N pred : preds)
			if (result.containsKey(pred)) {
				// this might not have been computed yet
				E edge = graph.getEdgeConnecting(pred, node);
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
					entrystate = implementation.union(node, entrystate, s);
		} catch (Exception e) {
			throw new FixpointException(format(ERROR, "creating entry state", node, graph), e);
		}

		return entrystate;
	}

	/**
	 * Yields the exit state for the given node.
	 * 
	 * @param graph          the graph containing the node
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
	default T getExitState(
			G graph,
			N node,
			T startstate,
			ForwardFixpoint.FixpointImplementation<N, E, T> implementation,
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
