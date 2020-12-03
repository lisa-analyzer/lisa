package it.unive.lisa.outputs;

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
import org.graphstream.graph.Edge;
import org.graphstream.graph.Element;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.stream.file.FileSinkDOT;
import org.graphstream.stream.file.FileSourceDOT;

import it.unive.lisa.cfg.CFG;
import it.unive.lisa.cfg.edge.FalseEdge;
import it.unive.lisa.cfg.edge.TrueEdge;
import it.unive.lisa.cfg.statement.Ret;
import it.unive.lisa.cfg.statement.Return;
import it.unive.lisa.cfg.statement.Statement;
import it.unive.lisa.cfg.statement.Throw;

public class DotGraph {

	private static final String NODE_SHAPE = "rect";

	private static final String SHAPE = "shape";

	private static final String SPECIAL_NODE_COLOR = "black";

	private static final String NORMAL_NODE_COLOR = "gray";

	private static final String EXIT_NODE_EXTRA_VALUE = "2";

	private static final String EXIT_NODE_EXTRA_ATTR = "peripheries";

	private static final String SEQUENTIAL_EDGE_COLOR = "black";

	private static final String FALSE_EDGE_COLOR = "red";

	private static final String TRUE_EDGE_COLOR = "blue";

	private static final String CONDITIONAL_EDGE_STYLE = "dashed";

	private static final String STYLE = "style";

	private static final String COLOR = "color";

	private static final String LABEL_ATTR = "label";

	private static String dotEscape(String extraLabel) {
		String escapeHtml4 = StringEscapeUtils.escapeHtml4(extraLabel);
		String replace = escapeHtml4.replaceAll("\\n", "<BR/>");
		return replace.replace("\\", "\\\\");
	}

	private static String cleanupForDiagraphTitle(String name) {
		String result = name.replace(' ', '_');
		result = result.replace("(", "___");
		result = result.replace(")", "___");
		return result;
	}

	private final Graph graph;

	private final Map<Statement, Long> codes = new IdentityHashMap<>();

	private long nextCode = 0;

	public DotGraph(String name) {
		this.graph = new MultiGraph(cleanupForDiagraphTitle(name));
		// graph.setAttribute("ordering", "out");
	}

	public void addNode(Statement st, boolean entry, boolean exit, Function<Statement, String> labelGenerator) {
		Node n = graph.addNode(nodeName(nextCode));
		codes.put(st, nextCode++);
		n.setAttribute(SHAPE, NODE_SHAPE);
		if (entry || exit)
			n.setAttribute(COLOR, SPECIAL_NODE_COLOR);
		else
			n.setAttribute(COLOR, NORMAL_NODE_COLOR);

		if (exit)
			n.setAttribute(EXIT_NODE_EXTRA_ATTR, EXIT_NODE_EXTRA_VALUE);

		String label = dotEscape(st.toString());
		String extraLabel = labelGenerator.apply(st);
		if (!extraLabel.isEmpty())
			extraLabel = "<BR/>" + dotEscape(extraLabel);
		n.setAttribute(LABEL_ATTR, "<" + label + extraLabel + ">");
	}

	private String nodeName(long id) {
		return "node" + id;
	}

	public void addEdge(it.unive.lisa.cfg.edge.Edge edge) {
		long id = codes.get(edge.getSource());
		long id1 = codes.get(edge.getDestination());

		Edge e = graph.addEdge("edge-" + id + "-" + id1, nodeName(id), nodeName(id1), true);

		if (edge instanceof TrueEdge) {
			e.setAttribute(STYLE, CONDITIONAL_EDGE_STYLE);
			e.setAttribute(COLOR, TRUE_EDGE_COLOR);
		} else if (edge instanceof FalseEdge) {
			e.setAttribute(STYLE, CONDITIONAL_EDGE_STYLE);
			e.setAttribute(COLOR, FALSE_EDGE_COLOR);
		} else
			e.setAttribute(COLOR, SEQUENTIAL_EDGE_COLOR);
	}

	public void dumpDot(Writer writer) throws IOException {
		FileSinkDOT sink = new CustomDotSink() {
			@Override
			protected void outputEndOfFile() throws IOException {
				LegendClusterSink legend = new LegendClusterSink();
				legend.setDirected(true);
				StringWriter sw = new StringWriter();
				legend.writeAll(new Legend().graph, sw);
				out.printf("%s%n", sw.toString());
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
		DotGraph other = (DotGraph) obj;
		if (graph == null) {
			if (other.graph != null)
				return false;
		} else if (!sameGraphs(graph, other.graph))
			// there is no equals method implemented in graphstream
			return false;
		return true;
	}

	private static boolean sameGraphs(Graph first, Graph second) {
		if (first.getNodeCount() != second.getNodeCount())
			return false;

		if (first.getEdgeCount() != second.getEdgeCount())
			return false;

		Map<Node, String> fMapping = new IdentityHashMap<>(), sMapping = new IdentityHashMap<>();
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

	private static void parseEdges(Graph g, Map<Node, String> mapping, List<String> nodes, List<String> edges) {
		g.edges().forEach(e -> {
			// TODO the indexOf will return the index of the first node with that label,
			// so edges targeting or originating different nodes might be collapsed on
			// that one. This is a problem for graphs with nodes having the same label
			int source = nodes.indexOf(mapping.get(e.getSourceNode()));
			int dest = nodes.indexOf(mapping.get(e.getTargetNode()));
			
			if (e.hasAttribute(STYLE) && e.getAttribute(STYLE).equals(CONDITIONAL_EDGE_STYLE))
				if (e.hasAttribute(COLOR))
					if (e.getAttribute(COLOR).equals(TRUE_EDGE_COLOR))
						edges.add(source + "T" + dest);
					else if (e.getAttribute(COLOR).equals(FALSE_EDGE_COLOR))
						edges.add(source + "F" + dest);
					else 
						edges.add(source + "S" + dest);
				else 
					edges.add(source + "S" + dest);
			else 
				edges.add(source + "S" + dest);
		});
		Collections.sort(edges);
	}

	private static void parseNodes(Graph g, Map<Node, String> mapping, List<String> nodes, List<String> entries,
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

	public static DotGraph fromCFG(CFG cfg, Function<Statement, String> labelGenerator) {
		DotGraph graph = new DotGraph(cfg.getDescriptor().getFullName());

		for (Statement st : cfg.getEntrypoints())
			graph.addNode(st, true, st instanceof Return || st instanceof Ret || st instanceof Throw, labelGenerator);

		for (Statement st : cfg.getNodes())
			if (!cfg.getEntrypoints().contains(st))
				graph.addNode(st, false, st instanceof Return || st instanceof Ret || st instanceof Throw,
						labelGenerator);

		for (Statement source : cfg.getNodes())
			for (Statement dest : cfg.followersOf(source))
				graph.addEdge(cfg.getEdgeConnecting(source, dest));

		return graph;
	}

	public static DotGraph readDot(Reader reader) throws IOException {
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
		DotGraph graph = new DotGraph("read");
		source.addSink(graph.graph);
		try (StringReader sr = new StringReader(content)) {
			source.readAll(sr);
		}
		return graph;
	}

	private static class Legend {
		private final Graph graph;

		private Legend() {
			graph = new MultiGraph("legend");
			Node l = graph.addNode("legend");
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
			builder.append(SEQUENTIAL_EDGE_COLOR);
			builder.append("\">");
			builder.append(SEQUENTIAL_EDGE_COLOR);
			builder.append("</font>, solid</td></tr>");
			builder.append("<tr><td align=\"right\">true edge&nbsp;</td><td align=\"left\"><font color=\"");
			builder.append(TRUE_EDGE_COLOR);
			builder.append("\">");
			builder.append(TRUE_EDGE_COLOR);
			builder.append("</font>, ");
			builder.append(CONDITIONAL_EDGE_STYLE);
			builder.append("</td></tr>");
			builder.append("<tr><td align=\"right\">false edge&nbsp;</td><td align=\"left\"><font color=\"");
			builder.append(FALSE_EDGE_COLOR);
			builder.append("\">");
			builder.append(FALSE_EDGE_COLOR);
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
