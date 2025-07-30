package it.unive.lisa.util.datastructures.graph.code;

import it.unive.lisa.program.cfg.protection.CatchBlock;
import it.unive.lisa.program.cfg.protection.ProtectionBlock;
import it.unive.lisa.util.datastructures.graph.Edge;

/**
 * An {@link Edge} that can be used with a {@link CodeGraph}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <G> the type of the {@link CodeGraph}s this list can be used in
 * @param <N> the type of the {@link CodeNode}s in this list
 * @param <E> the type of the {@link CodeEdge}s in this list
 */
public interface CodeEdge<G extends CodeGraph<G, N, E>,
		N extends CodeNode<G, N, E>,
		E extends CodeEdge<G, N, E>>
		extends
		Edge<G, N, E>,
		Comparable<E> {

	/**
	 * Yields {@code true} if and only if this edge represent unconditional
	 * execution flow between its bounds. This means that (i) it could be
	 * simplified if one of the nodes connected to it is simplified (i.e.,
	 * removed from the graph), and (ii) the graph containing it can avoid
	 * storing the edge if its bounds are subsequent.
	 * 
	 * @return whether or not this edge can be simplified
	 */
	boolean isUnconditional();

	/**
	 * Yields {@code true} if and only if this edge is used to handle errors.
	 * This includes edges leaving statements of a {@link ProtectionBlock}'s body
	 * and going into one of the {@link CatchBlock}s, or ones connecting it to a finally
	 * block.
	 * 
	 * @return whether or not this edge is used for error handling
	 */
	boolean isErrorHandling();

	/**
	 * Builds a new instance of this edge, connecting the given nodes.
	 * 
	 * @param source      the source node
	 * @param destination the destination node
	 * 
	 * @return a new instance of this edge, connecting the given nodes
	 */
	E newInstance(
			N source,
			N destination);

}
