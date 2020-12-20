package it.unive.lisa.outputs;

import it.unive.lisa.util.datastructures.graph.Edge;
import it.unive.lisa.util.datastructures.graph.Graph;
import it.unive.lisa.util.datastructures.graph.Node;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import org.apache.commons.text.StringEscapeUtils;
import org.graphstream.graph.Element;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.stream.file.FileSinkDOT;
import org.graphstream.stream.file.FileSourceDOT;

/**
 * An auxiliary graph built from a {@link Graph} that can be dumped in dot
 * format, together with a legend. Instances of this class can be read from a
 * file through {@link #readDot(Reader)}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class DotGraph<N extends Node<N>, E extends Edge<N, E>> {

	protected static final String NODE_SHAPE = "rect";

	protected static final String SHAPE = "shape";

	protected static final String EXIT_NODE_EXTRA_VALUE = "2";

	protected static final String EXIT_NODE_EXTRA_ATTR = "peripheries";

	protected static final String COLOR_BLACK = "black";

	protected static final String COLOR_GRAY = "gray";

	protected static final String COLOR_RED = "red";

	protected static final String COLOR_BLUE = "blue";

	protected static final String SPECIAL_NODE_COLOR = COLOR_BLACK;

	protected static final String NORMAL_NODE_COLOR = COLOR_GRAY;

	protected static final String CONDITIONAL_EDGE_STYLE = "dashed";

	protected static final String STYLE = "style";

	protected static final String COLOR = "color";

	protected static final String LABEL_ATTR = "label";

	private static String dotEscape(String extraLabel) {
		String escapeHtml4 = StringEscapeUtils.escapeHtml4(extraLabel);
		String replace = escapeHtml4.replaceAll("\\n", "<BR/>");
		return replace.replace("\\", "\\\\");
	}

	private final org.graphstream.graph.Graph graph, legend;

	private final Map<N, Long> codes = new IdentityHashMap<>();

	private long nextCode = 0;

	protected DotGraph(org.graphstream.graph.Graph legend) {
		this.graph = new MultiGraph("graph");
		this.legend = legend;
	}

	protected void addNode(N node, boolean entry, boolean exit, Function<N, String> labelGenerator) {
		org.graphstream.graph.Node n = graph.addNode(nodeName(codes.computeIfAbsent(node, nn -> nextCode++)));

		n.setAttribute(SHAPE, NODE_SHAPE);
		if (entry || exit)
			n.setAttribute(COLOR, SPECIAL_NODE_COLOR);
		else
			n.setAttribute(COLOR, NORMAL_NODE_COLOR);

		if (exit)
			n.setAttribute(EXIT_NODE_EXTRA_ATTR, EXIT_NODE_EXTRA_VALUE);

		String label = dotEscape(node.toString());
		String extraLabel = labelGenerator.apply(node);
		if (!extraLabel.isEmpty())
			extraLabel = "<BR/>" + dotEscape(extraLabel);
		n.setAttribute(LABEL_ATTR, "<" + label + extraLabel + ">");
	}

	private String nodeName(long id) {
		return "node" + id;
	}

	protected void addEdge(E edge, String color, String style) {
		long id = codes.computeIfAbsent(edge.getSource(), n -> nextCode++);
		long id1 = codes.computeIfAbsent(edge.getDestination(), n -> nextCode++);

		org.graphstream.graph.Edge e = graph.addEdge("edge-" + id + "-" + id1, nodeName(id), nodeName(id1), true);

		if (style != null)
			e.setAttribute(STYLE, style);

		if (color != null)
			e.setAttribute(COLOR, color);
	}

	/**
	 * Dumps this graph through the given {@link Writer}. A legend will also be
	 * added to the output, to improve its readability.
	 * 
	 * @param writer the writer to use for dumping the graph
	 * 
	 * @throws IOException if an I/O error occurs while writing
	 */
	public void dumpDot(Writer writer) throws IOException {
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((graph == null) ? 0 : graph.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DotGraph<?, ?> other = (DotGraph<?, ?>) obj;
		if (graph == null) {
			if (other.graph != null)
				return false;
		} else if (!sameGraphs(graph, other.graph))
			// there is no equals method implemented in graphstream
			return false;
		return true;
	}

	private static boolean sameGraphs(org.graphstream.graph.Graph first, org.graphstream.graph.Graph second) {
		if (first.getNodeCount() != second.getNodeCount())
			return false;

		if (first.getEdgeCount() != second.getEdgeCount())
			return false;

		Map<org.graphstream.graph.Node, String> fMapping = new IdentityHashMap<>(), sMapping = new IdentityHashMap<>();
		// we use lists to allow duplicates: different nodes might have the same
		// label
		List<String> fNodes = new ArrayList<>(), sNodes = new ArrayList<>();
		List<String> fEntries = new ArrayList<>(), sEntries = new ArrayList<>();
		List<String> fExits = new ArrayList<>(), sExits = new ArrayList<>();
		parseNodes(first, fMapping, fNodes, fEntries, fExits);
		parseNodes(second, sMapping, sNodes, sEntries, sExits);

		if (!fEntries.equals(sEntries) || !fExits.equals(sExits) || !fNodes.equals(sNodes))
			// we start comparing from the smaller set for fast-failing
			return false;

		List<String> fEdges = new ArrayList<>(), sEdges = new ArrayList<>();
		parseEdges(first, fMapping, fNodes, fEdges);
		parseEdges(second, sMapping, sNodes, sEdges);

		if (!fEdges.equals(sEdges))
			return false;

		return true;
	}

	private static void parseEdges(org.graphstream.graph.Graph g, Map<org.graphstream.graph.Node, String> mapping,
			List<String> nodes, List<String> edges) {
		g.edges().forEach(e -> {
			// TODO the indexOf will return the index of the first node with
			// that label, so edges targeting or originating different nodes
			// might be collapsed on that one. This is a problem for graphs with
			// nodes having the same label
			int source = nodes.indexOf(mapping.get(e.getSourceNode()));
			int dest = nodes.indexOf(mapping.get(e.getTargetNode()));

			String middle = "";
			if (e.hasAttribute(STYLE))
				middle += "_" + e.getAttribute(STYLE) + "_";
			if (e.hasAttribute(COLOR))
				middle += "_" + e.getAttribute(COLOR) + "_";
			edges.add(source + middle + dest);
		});
		Collections.sort(edges);
	}

	private static void parseNodes(org.graphstream.graph.Graph g, Map<org.graphstream.graph.Node, String> mapping,
			List<String> nodes, List<String> entries,
			List<String> exits) {
		g.nodes().forEach(n -> {
			String label = n.getAttribute(LABEL_ATTR, String.class);
			mapping.put(n, label);
			nodes.add(label);

			if (n.hasArray(COLOR) && n.getAttribute(COLOR).equals(SPECIAL_NODE_COLOR))
				if (n.hasAttribute(EXIT_NODE_EXTRA_ATTR)
						&& n.getAttribute(EXIT_NODE_EXTRA_ATTR).equals(EXIT_NODE_EXTRA_VALUE))
					exits.add(label);
				else
					entries.add(label);
		});

		Collections.sort(nodes);
		Collections.sort(entries);
		Collections.sort(exits);
	}

	@Override
	public String toString() {
		return graph.toString();
	}

	/**
	 * Reads a graph through the given {@link Reader}. Any legend (i.e.,
	 * subgraph) will be stripped from the input.
	 * 
	 * @param <N>    the type of nodes in the graph
	 * @param <E>    the type of edges in the graph
	 * @param reader the reader to use for reading the graph
	 * 
	 * @return the {@link DotGraph} that has been read
	 * 
	 * @throws IOException if an I/O error occurs while reading
	 */
	public static <N extends Node<N>, E extends Edge<N, E>> DotGraph<N, E> readDot(Reader reader) throws IOException {
		// we have to re-add the quotes wrapping the labels, otherwise the
		// parser will break
		String content;
		String sentinel = LABEL_ATTR + "=<";
		String replacement = LABEL_ATTR + "=\"<";
		String ending = ">];";
		String endingReplacement = ">\"];";
		try (BufferedReader br = new BufferedReader(reader); StringWriter writer = new StringWriter();) {
			String line;
			while ((line = br.readLine()) != null) {
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
				writer.append("\n");
			}
			content = writer.toString();
		}
		FileSourceDOT source = new FileSourceDOT();
		DotGraph<N, E> graph = new DotGraph<>(null) {
		};
		source.addSink(graph.graph);
		try (StringReader sr = new StringReader(content)) {
			source.readAll(sr);
		}
		return graph;
	}

	public static class CFGLegend {
		public final org.graphstream.graph.Graph graph;

		public CFGLegend() {
			graph = new MultiGraph("legend");
			org.graphstream.graph.Node l = graph.addNode("legend");
			StringBuilder builder = new StringBuilder();
			builder.append("<");
			builder.append("<table border=\"0\" cellpadding=\"2\" cellspacing=\"0\" cellborder=\"0\">");
			builder.append("<tr><td align=\"right\">node border&nbsp;</td><td align=\"left\"><font color=\"");
			builder.append(NORMAL_NODE_COLOR);
			builder.append("\">");
			builder.append(NORMAL_NODE_COLOR);
			builder.append("</font>, single</td></tr>");
			builder.append("<tr><td align=\"right\">entrypoint border&nbsp;</td><td align=\"left\"><font color=\"");
			builder.append(SPECIAL_NODE_COLOR);
			builder.append("\">");
			builder.append(SPECIAL_NODE_COLOR);
			builder.append("</font>, single</td></tr>");
			builder.append("<tr><td align=\"right\">exitpoint border&nbsp;</td><td align=\"left\"><font color=\"");
			builder.append(SPECIAL_NODE_COLOR);
			builder.append("\">");
			builder.append(SPECIAL_NODE_COLOR);
			builder.append("</font>, double</td></tr>");
			builder.append("<tr><td align=\"right\">sequential edge&nbsp;</td><td align=\"left\"><font color=\"");
			builder.append(COLOR_BLACK);
			builder.append("\">");
			builder.append(COLOR_BLACK);
			builder.append("</font>, solid</td></tr>");
			builder.append("<tr><td align=\"right\">true edge&nbsp;</td><td align=\"left\"><font color=\"");
			builder.append(COLOR_BLUE);
			builder.append("\">");
			builder.append(COLOR_BLUE);
			builder.append("</font>, ");
			builder.append(CONDITIONAL_EDGE_STYLE);
			builder.append("</td></tr>");
			builder.append("<tr><td align=\"right\">false edge&nbsp;</td><td align=\"left\"><font color=\"");
			builder.append(COLOR_RED);
			builder.append("\">");
			builder.append(COLOR_RED);
			builder.append("</font>, ");
			builder.append(CONDITIONAL_EDGE_STYLE);
			builder.append("</td></tr>");
			builder.append("</table>");
			builder.append(">");
			l.setAttribute("label", builder.toString());
		}
	}

	private static class CustomDotSink extends FileSinkDOT {

		@Override
		protected String outputAttribute(String key, Object value, boolean first) {
			boolean quote = true;

			if (value instanceof Number || key.equals(LABEL_ATTR))
				// labels that we output are always in html format
				// so no need to quote them
				quote = false;

			return String.format("%s%s=%s%s%s", first ? "" : ",", key, quote ? "\"" : "", value, quote ? "\"" : "");
		}

		@Override
		protected String outputAttributes(Element e) {
			if (e.getAttributeCount() == 0)
				return "";

			Map<String, String> attrs = new HashMap<>();
			e.attributeKeys().forEach(key -> attrs.put(key, outputAttribute(key, e.getAttribute(key), true)));

			StringBuilder buffer = new StringBuilder("[");
			for (Entry<String, String> entry : attrs.entrySet())
				if (!entry.getKey().equals(LABEL_ATTR))
					buffer.append(entry.getValue()).append(",");

			if (attrs.containsKey(LABEL_ATTR))
				buffer.append(attrs.get(LABEL_ATTR));

			String result = buffer.toString();
			if (result.endsWith(","))
				result = result.substring(0, result.length() - 1);

			return result + "]";
		}
	}

	private static class LegendClusterSink extends CustomDotSink {
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
