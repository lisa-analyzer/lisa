package it.unive.lisa.outputs;

import it.unive.lisa.outputs.serializableGraph.SerializableArray;
import it.unive.lisa.outputs.serializableGraph.SerializableEdge;
import it.unive.lisa.outputs.serializableGraph.SerializableNode;
import it.unive.lisa.outputs.serializableGraph.SerializableObject;
import it.unive.lisa.outputs.serializableGraph.SerializableString;
import it.unive.lisa.outputs.serializableGraph.SerializableValue;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Element;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.stream.file.FileSinkDOT;
import org.graphstream.stream.file.FileSourceDOT;

/**
 * A graph that can be dumped into Dot format.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class DotGraph extends GraphStreamWrapper {

	/**
	 * The black color.
	 */
	protected static final String COLOR_BLACK = "black";

	/**
	 * The gray color.
	 */
	protected static final String COLOR_GRAY = "gray";

	/**
	 * The red color.
	 */
	protected static final String COLOR_RED = "red";

	/**
	 * The blue color.
	 */
	protected static final String COLOR_BLUE = "blue";

	/**
	 * The style attribute name.
	 */
	protected static final String STYLE = "style";

	/**
	 * The color attribute name.
	 */
	protected static final String COLOR = "color";

	/**
	 * The shape attribute name.
	 */
	protected static final String SHAPE = "shape";

	/**
	 * The label attribute name.
	 */
	protected static final String LABEL = "label";

	/**
	 * The name of the extra attribute identifying exit nodes.
	 */
	protected static final String EXIT_NODE_EXTRA_ATTR = "peripheries";

	/**
	 * The default shape of a node.
	 */
	protected static final String NODE_SHAPE = "rect";

	/**
	 * The value of the extra attribute identifying exit nodes.
	 */
	protected static final String EXIT_NODE_EXTRA_VALUE = "2";

	/**
	 * The color of a special node (entry or exit).
	 */
	protected static final String SPECIAL_NODE_COLOR = COLOR_BLACK;

	/**
	 * The color of a normal node.
	 */
	protected static final String NORMAL_NODE_COLOR = COLOR_GRAY;

	/**
	 * The style of conditional edges.
	 */
	protected static final String CONDITIONAL_EDGE_STYLE = "dashed";

	private final org.graphstream.graph.Graph legend;

	private final String title;

	/**
	 * Builds a graph.
	 * 
	 * @param title the title of the graph, if any
	 */
	public DotGraph(String title) {
		super();
		this.legend = new Legend().graph;
		this.title = title;
	}

	private static String dotEscape(String extraLabel) {
		String escapeHtml4 = StringEscapeUtils.escapeHtml4(extraLabel);
		String replace = escapeHtml4.replace("\n", "<BR/>");
		replace = replace.replace("\\", "\\\\");
		return replace;
	}

	/**
	 * Adds a node to the graph. The label of {@code node} will be composed by
	 * joining {@code node.toString()} ( {@link Object#toString()}) with
	 * {@code labelGenerator.apply(node)} ({@link Function#apply(Object)})
	 * through a new line.
	 * 
	 * @param node  the source node
	 * @param entry whether or not this edge is an entrypoint of the graph
	 * @param exit  whether or not this edge is an exitpoint of the graph
	 * @param label the additional label that can be added to each node's text
	 *                  (can be {@code null})
	 */
	public void addNode(SerializableNode node, boolean entry, boolean exit, SerializableValue label) {
		Node n = graph.addNode(nodeName(node.getId()));

		n.setAttribute(SHAPE, NODE_SHAPE);
		if (entry || exit)
			n.setAttribute(COLOR, SPECIAL_NODE_COLOR);
		else
			n.setAttribute(COLOR, NORMAL_NODE_COLOR);

		if (exit)
			n.setAttribute(EXIT_NODE_EXTRA_ATTR, EXIT_NODE_EXTRA_VALUE);

		String l = dotEscape(node.getText());
		String extra = "";
		if (label != null)
			extra = "<BR/><BR/>" + dotEscape(format(label));
		n.setAttribute(LABEL, "<" + l + extra + ">");
	}

	private static String format(SerializableValue value) {
		if (value instanceof SerializableString) {
			return value.toString();
		} else if (value instanceof SerializableArray) {
			SerializableArray array = (SerializableArray) value;
			if (array.getElements().stream().allMatch(SerializableString.class::isInstance))
				return "[" + StringUtils.join(array.getElements(), ", ") + "]";
			else {
				StringBuilder builder = new StringBuilder();
				boolean first = true;
				for (int i = 0; i < array.getElements().size(); i++) {
					SerializableValue array_element = array.getElements().get(i);
					if (!first)
						builder.append(",\n");
					first = false;
					builder.append(format(array_element));
				}
				return builder.toString();
			}
		} else if (value instanceof SerializableObject) {
			SerializableObject object = (SerializableObject) value;
			StringBuilder builder = new StringBuilder("{ ");
			boolean first = true;
			for (Entry<String, SerializableValue> field : object.getFields().entrySet()) {
				SerializableValue fieldValue = field.getValue();
				if (!first) {
					if (builder.toString().endsWith("\n"))
						builder.delete(builder.length() - 1, builder.length());
					builder.append(",\n");
				}
				first = false;
				builder.append(field.getKey()).append(": ").append(format(fieldValue));
			}
			return builder.append(" }\n").toString();
		} else
			throw new IllegalArgumentException("Unknown value type: " + value.getClass().getName());
	}

	/**
	 * Adds an edge to the graph.
	 * 
	 * @param edge the source edge
	 */
	public void addEdge(SerializableEdge edge) {
		long id = edge.getSourceId();
		long id1 = edge.getDestId();

		Edge e = graph.addEdge(edgeName(id, id1), nodeName(id), nodeName(id1), true);

		switch (edge.getKind()) {
		case "TrueEdge":
			e.setAttribute(STYLE, CONDITIONAL_EDGE_STYLE);
			e.setAttribute(COLOR, COLOR_BLUE);
			break;
		case "FalseEdge":
			e.setAttribute(STYLE, CONDITIONAL_EDGE_STYLE);
			e.setAttribute(COLOR, COLOR_RED);
			break;
		case "SequentialEdge":
		default:
			e.setAttribute(COLOR, COLOR_BLACK);
			break;
		}
	}

	@Override
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
	 * @param reader the reader to use for reading the graph
	 * 
	 * @return the {@link DotGraph} that has been read
	 * 
	 * @throws IOException if an I/O error occurs while reading
	 */
	public static DotGraph readDot(Reader reader) throws IOException {
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
				writer.append("\n");
			}
			content = writer.toString();
		}
		FileSourceDOT source = new FileSourceDOT();
		DotGraph graph = new DotGraph(null) {
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

	private static final class Legend {
		private final org.graphstream.graph.Graph graph;

		private Legend() {
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
}
