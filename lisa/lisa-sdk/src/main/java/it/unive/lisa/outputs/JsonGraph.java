package it.unive.lisa.outputs;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;

import org.graphstream.graph.implementations.MultiGraph;

import it.unive.lisa.util.datastructures.graph.Edge;
import it.unive.lisa.util.datastructures.graph.Graph;
import it.unive.lisa.util.datastructures.graph.Node;

/**
 * An auxiliary graph built from a {@link Graph} that can be dumped in dot
 * format, together with a legend. Instances of this class can be read from a
 * file through {@link #read(Reader)}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <N> the type of the {@link Node}s in the original graph
 * @param <E> the type of the {@link Edge}s in the original graph
 * @param <G> the type of the original {@link Graph}s
 */
public abstract class JsonGraph<N extends Node<N, E, G>, E extends Edge<N, E, G>, G extends Graph<G, N, E>>
		extends FileGraph<N, E, G> {

	protected final org.graphstream.graph.Graph graph, legend;

	private final Map<N, Long> codes = new IdentityHashMap<>();

	private long nextCode;

	/**
	 * Builds a graph.
	 *
	 * @param title  the title of the graph, if any
	 * @param legend the legend to append to the graph, if any
	 */
	protected JsonGraph(String title, org.graphstream.graph.Graph legend) {
		super(title, legend);
		this.graph = new MultiGraph("graph");
		this.legend = legend;
	}

	/**
	 * Adds a node to the graph. The label of {@code node} will be composed by
	 * joining {@code node.toString()} ( {@link Object#toString()}) with
	 * {@code labelGenerator.apply(node)} ({@link Function#apply(Object)})
	 * through a new line.
	 * 
	 * @param node           the source node
	 * @param entry          whether or not this edge is an entrypoint of the
	 *                           graph
	 * @param exit           whether or not this edge is an exitpoint of the
	 *                           graph
	 * @param labelGenerator the function that is used to enrich nodes labels
	 */
	protected void addNode(N node, boolean entry, boolean exit, Function<N, String> labelGenerator) {
		org.graphstream.graph.Node n = graph.addNode(nodeName(codes.computeIfAbsent(node, nn -> nextCode++)));

		n.setAttribute(SHAPE, NODE_SHAPE);
		if (entry || exit)
			n.setAttribute(COLOR, SPECIAL_NODE_COLOR);
		else
			n.setAttribute(COLOR, NORMAL_NODE_COLOR);

		if (exit)
			n.setAttribute(EXIT_NODE_EXTRA_ATTR, EXIT_NODE_EXTRA_VALUE);

		String label = node.toString().replace('"', '\'');
		String extraLabel = labelGenerator.apply(node);
		if (!extraLabel.isEmpty())
			extraLabel = extraLabel + "";
		n.setAttribute(LABEL, String.format("{\"instruction\" : \"%s\", \"data\" : %s", label, extraLabel));
	}

	protected void addEdge(E edge, String color, String style) {
		super.addEdge(edge, color, style, graph, codes);
	}

	/**
	 * Dumps this graph through the given {@link Writer}. A legend will also be
	 * added to the output, to improve its readability.
	 * 
	 * @param writer the writer to use for dumping the graph
	 * 
	 * @throws IOException if an I/O error occurs while writing
	 */
	public void dump(Writer writer) throws IOException {
		FileSinkJSON sink = new FileSinkJSON();
		sink.writeAll(graph, writer);
	}

	public String toString() {
		FileSinkJSON sink = new FileSinkJSON();
		return sink.toString(graph);
	}

	/**
	 * Reads a graph through the given {@link Reader}. Any legend (i.e.,
	 * subgraph) will be stripped from the input.
	 * 
	 * @param <N>    the type of {@link Node}s in the graph
	 * @param <E>    the type of {@link Edge}s in the graph
	 * @param <G>    the type of the {@link Graph}
	 * @param reader the reader to use for reading the graph
	 * 
	 * @return the {@link JsonGraph} that has been read
	 * 
	 * @throws IOException if an I/O error occurs while reading
	 */
	public static <N extends Node<N, E, G>,
			E extends Edge<N, E, G>,
			G extends Graph<G, N, E>> JsonGraph<N, E, G> read(
					Reader reader) throws IOException {

		return null;
	}

}
