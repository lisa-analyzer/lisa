package it.unive.lisa.outputs;

import java.io.IOException;
import java.io.Writer;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.MultiGraph;

/**
 * A graph instance that decorates a graphstream {@link Graph}, offering
 * custom/improved dumping.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class GraphStreamWrapper {

	/**
	 * The wrapped graph.
	 */
	public final Graph graph;

	/**
	 * Builds the wrapper.
	 */
	protected GraphStreamWrapper() {
		this.graph = new MultiGraph("graph");
	}

	@Override
	public String toString() {
		return graph.toString();
	}

	/**
	 * Given a code, builds the name of a node that can be added to, removed
	 * from or retrieved from the wrapped graph.
	 * 
	 * @param code the code
	 * 
	 * @return the name of the node
	 */
	protected static String nodeName(long code) {
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
	protected static String edgeName(long src, long dest) {
		return "edge-" + src + "-" + dest;
	}

	/**
	 * Dumps this graph through the given {@link Writer}.
	 * 
	 * @param writer the writer to use for dumping the graph
	 * 
	 * @throws IOException if an I/O error occurs while writing
	 */
	public abstract void dump(Writer writer) throws IOException;
}
