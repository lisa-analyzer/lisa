package it.unive.lisa.outputs;

import java.io.IOException;
import java.io.Writer;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.MultiGraph;

public abstract class GraphStreamWrapper {

	public final Graph graph;

	protected GraphStreamWrapper() {
		this.graph = new MultiGraph("graph");
	}

	@Override
	public String toString() {
		return graph.toString();
	}

	protected static String nodeName(long id) {
		return "node" + id;
	}

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
