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

/**
 * A fixpoint algorithm for a {@link Graph}, parametric to the
 * {@link FixpointImplementation} that one wants to use to compute the results.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <G> the type of the source {@link Graph}
 * @param <N> the type of the {@link Node}s in the source graph
 * @param <E> the type of the {@link Edge}s in the source graph
 * @param <T> the type of data computed by the fixpoint
 */
public class Fixpoint<G extends Graph<G, N, E>, N extends Node<G, N, E>, E extends Edge<G, N, E>, T> {

	private static final String ERROR = "Exception while %s of '%s' in '%s'";

	private final Graph<G, N, E> graph;

	private final Map<N, T> result;

	private boolean ascendingPhase;

	/**
	 * Builds a fixpoint for the given {@link Graph}.
	 * 
	 * @param graph the source graph
	 */
	public Fixpoint(Graph<G, N, E> graph) {
		this.graph = graph;
		result = new HashMap<>(graph.getNodesCount());
		this.ascendingPhase = true;
	}

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
		T semantics(N node, T entrystate) throws Exception;

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
		T traverse(E edge, T entrystate) throws Exception;

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
		T union(N node, T left, T right) throws Exception;

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
		T join(N node, T approx, T old) throws Exception;

		/**
		 * Given a node and two states, meets the states (i.e. greatest lower
		 * bound <i>or</i> narrowing) together.<br>
		 * <br>
		 * This callback is invoked after the exit state of a node has been
		 * computed through {@link #semantics(Object, Object)}, to meet it with
		 * results from older fixpoint iterations.
		 * 
		 * @param node   the node where the computation takes place
		 * @param approx the most recent state
		 * @param old    the older state
		 * 
		 * @return the met state
		 * 
		 * @throws Exception if something goes wrong during the computation
		 */
		default T meet(N node, T approx, T old) throws Exception {
			return approx;
		}

		/**
		 * Given a node and two states, yields whether or not the most recent
		 * one has to be considered <i>equal</i> to the older one in terms of
		 * fixpoint iterations. This means that if this method returns
		 * {@code true}, than the result for the given node won't be updated and
		 * the successors of such node won't be added back to the list of nodes
		 * to process.<br>
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
		boolean equality(N node, T approx, T old) throws Exception;

		boolean doDescendingPhase();
	}

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
	 * 
	 * @return a mapping from each (reachable) node of the source graph to the
	 *             fixpoint result computed at that node
	 * 
	 * @throws FixpointException if something goes wrong during the fixpoint
	 *                               execution
	 */
	public Map<N, T> fixpoint(Map<N, T> startingPoints, WorkingSet<N> ws,
			FixpointImplementation<N, E, T> implementation)
			throws FixpointException {

		if (this.ascendingPhase) {
			result.clear();
			startingPoints.keySet().forEach(ws::push);
		}

		T newApprox;
		while (!ws.isEmpty()) {
			N current = ws.pop();

			if (current == null)
				throw new FixpointException("null node encountered during fixpoint in '" + graph + "'");
			if (!graph.containsNode(current))
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
				if (this.ascendingPhase) {
					try {
						newApprox = implementation.join(current, newApprox, oldApprox);
					} catch (Exception e) {
						throw new FixpointException(format(ERROR, "joining states", current, graph), e);
					}
				} else {
					try {
						newApprox = implementation.meet(current, newApprox, oldApprox);
					} catch (Exception e) {
						throw new FixpointException(format(ERROR, "meeting states", current, graph), e);
					}
				}
			try {
				if (oldApprox == null ||
						(ascendingPhase && !implementation.equality(current, newApprox, oldApprox)) ||
						(!ascendingPhase && !implementation.equality(current, oldApprox, newApprox))) {
					result.put(current, newApprox);
					for (N instr : graph.followersOf(current))
						ws.push(instr);
				}
			} catch (Exception e) {
				throw new FixpointException(format(ERROR, "updating result", current, graph), e);
			}
		}

		if (!ascendingPhase || !implementation.doDescendingPhase())
			return result;

		this.ascendingPhase = false;

		graph.getNodes().forEach(ws::push);

		return this.fixpoint(startingPoints, ws, implementation);
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
