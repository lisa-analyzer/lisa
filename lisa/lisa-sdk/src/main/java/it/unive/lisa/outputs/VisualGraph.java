package it.unive.lisa.outputs;

import java.io.IOException;
import java.io.Writer;

/**
 * A graph instance that provides visualization of the data inside it. This
 * class mainly provides a unique logic for generating nodes and edges names
 * through {@link #nodeName(long)} and {@link #edgeName(long, long)}, and the
 * callback for dumping the graph onto a custom {@link Writer}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class VisualGraph {

	/**
	 * Given a code, builds the name of a node that can be added to, removed
	 * from or retrieved from the wrapped graph.
	 * 
	 * @param code the code
	 * 
	 * @return the name of the node
	 */
	public static String nodeName(
			long code) {
		return "node" + code;
	}

	/**
	 * Given the codes for the source and destination edges, builds the name of
	 * an edge that can be added to, removed from or retrieved from the wrapped
	 * graph.
	 * 
	 * @param src  the code of the source node
	 * @param dest the code of the destination node
	 * 
	 * @return the name of the edge
	 */
	public static String edgeName(
			long src,
			long dest) {
		return "edge-" + src + "-" + dest;
	}

	/**
	 * Dumps this graph through the given {@link Writer}.
	 * 
	 * @param writer the writer to use for dumping the graph
	 * 
	 * @throws IOException if an I/O error occurs while writing
	 */
	public abstract void dump(
			Writer writer)
			throws IOException;
}
