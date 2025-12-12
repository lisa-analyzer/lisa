package it.unive.lisa.util.datastructures.graph.algorithms;

import it.unive.lisa.util.datastructures.graph.Edge;
import it.unive.lisa.util.datastructures.graph.Graph;
import it.unive.lisa.util.datastructures.graph.Node;

/**
 * A fixpoint implementation for a {@link Graph}, defining the semantics of
 * nodes and edges, as well as the lattice operations to be used during the
 * fixpoint computation.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <G> the type of {@link Graph} this implementation is for
 * @param <N> the type of {@link Node} contained into the graph
 * @param <E> the type of {@link Edge} contained into the graph
 * @param <T> the type of the abstract state computed during the fixpoint
 *                computation
 */
public interface FixpointImplementation<
		G extends Graph<G, N, E>,
		N extends Node<G, N, E>,
		E extends Edge<G, N, E>,
		T> {

	/**
	 * Given a node and its entry state, computes its exit state relying on its
	 * semantics.<br>
	 * <br>
	 * This callback is invoked after the overall entry state for a node has
	 * been computed through {@link #union(Object, Object, Object)} of the exit
	 * states of its predecessors.
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
	 * Given an edge and a state, computes a new state by modifying the given
	 * one assuming that the edge gets traversed.<br>
	 * <br>
	 * This callback is invoked while computing the overall entry state for a
	 * node to filter a state according to the logic of the edge (an edge might
	 * be always traversed, or only if a condition holds).
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
	 * This callback is invoked for the computation of the overall entry state
	 * for a node to merge the exit states of its predecessors, after traversing
	 * the edges connecting them through {@link #traverse(Object, Object)}.
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
	 * This callback is invoked after the exit state of a node has been computed
	 * through {@link #semantics(Object, Object)}, to join it with results from
	 * older fixpoint iterations.
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
	 * Given a node and two states, yields whether or not the most recent one
	 * has to be considered <i>less than or equal</i> to the older one in terms
	 * of fixpoint iterations. This means that if this method returns
	 * {@code true}, the result for the given node won't be updated and the
	 * successors of such node won't be added back to the list of nodes to
	 * process.<br>
	 * <br>
	 * This callback is invoked after the exit state of a node has been computed
	 * through {@link #semantics(Object, Object)} and joined with the older one
	 * through {@link #join(Object, Object, Object)}.
	 * 
	 * @param node   the node where the computation takes place
	 * @param approx the most recent state
	 * @param old    the older state
	 * 
	 * @return {@code true} if there is no need to update the previous result,
	 *             {@code false} otherwise
	 * 
	 * @throws Exception if something goes wrong during the computation
	 */
	boolean leq(
			N node,
			T approx,
			T old)
			throws Exception;

}
