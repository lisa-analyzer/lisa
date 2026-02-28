package it.unive.lisa.util.datastructures.graph.algorithms;

import it.unive.lisa.util.collections.workset.WorkingSet;
import it.unive.lisa.util.datastructures.graph.Edge;
import it.unive.lisa.util.datastructures.graph.Graph;
import it.unive.lisa.util.datastructures.graph.Node;
import java.util.Map;

/**
 * A common interface for fixpoint algorithms for a {@link Graph}.
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
	 * Runs the fixpoint. Invoking this method effectively recomputes the
	 * result: no caching on previous runs is executed. It starts with empty
	 * result.
	 * 
	 * @param startingPoints a map containing all the nodes to start the
	 *                           fixpoint at, each mapped to its entry state.
	 * @param ws             the instance of {@link WorkingSet} to use for the
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
			WorkingSet<N> ws)
			throws FixpointException;

	/**
	 * Runs the fixpoint. Invoking this method effectively recomputes the
	 * result: no caching on previous runs is executed.
	 * 
	 * @param startingPoints a map containing all the nodes to start the
	 *                           fixpoint at, each mapped to its entry state.
	 * @param ws             the instance of {@link WorkingSet} to use for the
	 *                           fixpoint
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
			Map<N, T> initialResult)
			throws FixpointException;

}
