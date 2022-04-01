package it.unive.lisa.outputs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import org.apache.commons.text.StringEscapeUtils;
import org.graphstream.graph.Element;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.stream.file.FileSinkDOT;
import org.graphstream.stream.file.FileSourceDOT;

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
public abstract class DotGraph<N extends Node<N, E, G>, E extends Edge<N, E, G>, G extends Graph<G, N, E>> extends FileGraph<N, E, G> {

	protected final org.graphstream.graph.Graph graph, legend;

	private final String title;

	private final Map<N, Long> codes = new IdentityHashMap<>();

	private long nextCode;

	/**
	 * Builds a graph.
	 * 
	 * @param title  the title of the graph, if any
	 * @param legend the legend to append to the graph, if any
	 */
	protected DotGraph(String title, org.graphstream.graph.Graph legend) {
		super(title, legend);
		this.graph = new MultiGraph("graph");
		this.legend = legend;
		this.title = title;
	}

	private static String escape(String extraLabel) {
		String escapeHtml4 = StringEscapeUtils.escapeHtml4(extraLabel);
		String replace = escapeHtml4.replace("\n", "<BR/>");
		return replace.replace("\\", "\\\\");
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
		n.setAttribute(LABEL,  "<" + label + escape(extraLabel) + ">");
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
		FileSinkDOT sink = new CustomDotSink() {
			@Override
			protected void outputEndOfFile() throws IOException {
				if (DotGraph.this.legend != null) {
					LegendClusterSink legend = new LegendClusterSink();
					legend.setDirected(true);
					StringWriter sw = new StringWriter();
					legend.writeAll(DotGraph.this.legend, sw);
					out.printf("%s%n", sw.toString());
				}
				super.outputEndOfFile();
			}
		};
		sink.setDirected(true);
		sink.writeAll(graph, writer);
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
	 * @return the {@link DotGraph} that has been read
	 * 
	 * @throws IOException if an I/O error occurs while reading
	 */
	public static <N extends Node<N, E, G>,
			E extends Edge<N, E, G>,
			G extends Graph<G, N, E>> DotGraph<N, E, G> read(
					Reader reader) throws IOException {
		// we have to re-add the quotes wrapping the labels, otherwise the
		// parser will break
		String content;
		String sentinel = LABEL + "=<";
		String replacement = LABEL + "=\"<";
		String ending = ">];";
		String endingReplacement = ">\"];";
		try (BufferedReader br = new BufferedReader(reader); StringWriter writer = new StringWriter()) {
			String line;
			while ((line = br.readLine()) != null) {
				if (line.trim().startsWith(LABEL))
					// skip graph title
					continue;

				int i = line.indexOf(sentinel);
				if (i != -1) {
					writer.append(line.substring(0, i));
					writer.append(replacement);
					// we can do the following since we know the label will
					// always be last
					writer.append(line.substring(i + sentinel.length(), line.length() - ending.length()));
					writer.append(endingReplacement);
				} else if (line.startsWith("subgraph")) {
					// we skip the legend
					writer.append("}");
					break;
				} else
					writer.append(line);
				writer.append("");
			}
			content = writer.toString();
		}
		FileSourceDOT source = new FileSourceDOT();
		DotGraph<N, E, G> graph = new DotGraph<>(null, null) {
		};
		source.addSink(graph.graph);
		try (StringReader sr = new StringReader(content)) {
			source.readAll(sr);
		} catch (Exception e) {
			System.out.println();
		}
		return graph;
	}

	private class CustomDotSink extends FileSinkDOT {

		@Override
		protected void outputHeader() throws IOException {
			out = (PrintWriter) output;
			out.printf("%s {%n", "digraph");

			if (title != null) {
				out.printf("\tlabelloc=\"t\";%n");
				out.printf("\tlabel=\"" + title + "\";%n");
			}
		}

		@Override
		protected String outputAttribute(String key, Object value, boolean first) {
			boolean quote = true;

			if (value instanceof Number || key.equals(LABEL))
				// labels that we output are always in html format
				// so no need to quote them
				quote = false;

			Object quoting = quote ? "\"" : "";
			return String.format("%s%s=%s%s%s", first ? "" : ",", key, quoting, value, quoting);
		}

		@Override
		protected String outputAttributes(Element e) {
			if (e.getAttributeCount() == 0)
				return "";

			Map<String, String> attrs = new HashMap<>();
			e.attributeKeys().forEach(key -> attrs.put(key, outputAttribute(key, e.getAttribute(key), true)));

			StringBuilder buffer = new StringBuilder("[");
			for (Entry<String, String> entry : attrs.entrySet())
				if (!entry.getKey().equals(LABEL))
					buffer.append(entry.getValue()).append(",");

			if (attrs.containsKey(LABEL))
				buffer.append(attrs.get(LABEL));

			String result = buffer.toString();
			if (result.endsWith(","))
				result = result.substring(0, result.length() - 1);

			return result + "]";
		}
	}

	private class LegendClusterSink extends CustomDotSink {
		@Override
		protected void outputHeader() throws IOException {
			out = (PrintWriter) output;
			out.printf("%s {%n", "subgraph cluster_legend");
			out.printf("\tlabel=\"Legend\";%n");
			out.printf("\tstyle=dotted;%n");
			out.printf("\tnode [shape=plaintext];%n");
		}
	}
}
