package it.unive.lisa.outputs;

import static guru.nidi.graphviz.model.Factory.mutNode;

import guru.nidi.graphviz.attribute.Color;
import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.attribute.Shape;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.Factory;
import guru.nidi.graphviz.model.Link;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;
import it.unive.lisa.outputs.serializableGraph.SerializableArray;
import it.unive.lisa.outputs.serializableGraph.SerializableEdge;
import it.unive.lisa.outputs.serializableGraph.SerializableNode;
import it.unive.lisa.outputs.serializableGraph.SerializableObject;
import it.unive.lisa.outputs.serializableGraph.SerializableString;
import it.unive.lisa.outputs.serializableGraph.SerializableValue;
import java.io.IOException;
import java.io.Writer;
import java.util.Map.Entry;
import java.util.function.Function;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

/**
 * A graph that can be dumped into Dot format.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class DotGraph
		extends
		VisualGraph {

	/**
	 * The wrapped graph.
	 */
	final MutableGraph graph;

	private final String title;

	/**
	 * Builds a graph.
	 * 
	 * @param title the title of the graph, if any
	 */
	public DotGraph(
			String title) {
		this.graph = Factory.mutGraph(title).setDirected(true);
		this.title = title;
	}

	private static MutableGraph buildLegend() {
		MutableGraph legend = Factory.mutGraph("legend")
				.graphAttrs()
				.add(Label.html("Legend"))
				.graphAttrs()
				.add("style", "dotted")
				.setCluster(true);

		StringBuilder builder = new StringBuilder();
		String row = "<tr><td align=\"right\">%s&nbsp;</td><td align=\"left\"><font color=\"%s\">%s</font>, %s</td></tr>";
		builder.append("<table border=\"0\" cellpadding=\"2\" cellspacing=\"0\" cellborder=\"0\">");
		builder.append(String.format(row, "node border", "gray", "gray", "single"));
		builder.append(String.format(row, "entrypoint border", "black", "black", "dashed"));
		builder.append(String.format(row, "exitpoint border", "black", "black", "solid"));
		builder.append(String.format(row, "sequential edge", "black", "black", "solid"));
		builder.append(String.format(row, "true edge", "blue", "blue", "dashed"));
		builder.append(String.format(row, "false edge", "red", "red", "dashed"));
		builder.append(String.format(row, "error edge", "orange", "orange", "dashed"));
		builder.append("</table>");

		MutableNode n = Factory.mutNode("legend").setName("legend").add(Label.html(builder.toString())).add(Shape.NONE);

		legend.add(n);

		return legend;
	}

	/**
	 * Yields the title of the graph.
	 * 
	 * @return the title
	 */
	public String getTitle() {
		return title;
	};

	private static String dotEscape(
			String extraLabel) {
		String escapeHtml4 = StringEscapeUtils.escapeHtml4(extraLabel);
		String replace = escapeHtml4.replace("\n", "<br/>");
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
	public void addNode(
			SerializableNode node,
			boolean entry,
			boolean exit,
			SerializableValue label) {
		String l = dotEscape(node.getText());
		String extra = "";
		if (label != null)
			extra = "<br/><br/>" + dotEscape(format(label));

		String nodeName = nodeName(node.getId());
		MutableNode n = Factory.mutNode(nodeName)
				.setName(nodeName)
				.add(Label.html(l + extra))
				.add(Shape.RECT)
				// we keep trace of what was the original id of the node
				.add("id", nodeName);

		if (!entry && !exit)
			n = n.add(Color.GRAY);

		if (entry)
			n = n.add(Style.DASHED);

		graph.add(n);
	}

	private static String format(
			SerializableValue value) {
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
	public void addEdge(
			SerializableEdge edge) {
		long id = edge.getSourceId();
		long id1 = edge.getDestId();

		MutableNode src = mutNode(nodeName(id));
		MutableNode dest = mutNode(nodeName(id1));

		Link link = src.linkTo(dest);

		switch (edge.getKind()) {
		case "TrueEdge":
			link = link.with(Style.DASHED);
			link = link.with(Color.BLUE);
			break;
		case "FalseEdge":
			link = link.with(Style.DASHED);
			link = link.with(Color.RED);
			break;
		case "ErrorEdge":
			link = link.with(Label.of(edge.getLabel()));
			link = link.with(Style.DASHED);
			link = link.with(Color.ORANGE);
			break;
		case "BeginFinallyEdge":
			link = link.with(Label.of(edge.getLabel()));
			link = link.with(Style.DASHED);
			link = link.with(Color.GREEN);
			break;
		case "EndFinallyEdge":
			link = link.with(Label.of(edge.getLabel()));
			link = link.with(Style.DASHED);
			link = link.with(Color.PURPLE);
			break;
		case "SequentialEdge":
		default:
			// black is the default
			// link = link.with(Color.BLACK);
			break;
		}

		src.links().add(link);

		// need to re-add the node to have it updated
		graph.add(src);
	}

	@Override
	public void dump(
			Writer writer)
			throws IOException {
		MutableGraph copy = graph.copy();
		copy.graphAttrs().add(Label.of(title)).graphAttrs().add("labelloc", "t");
		copy.add(buildLegend());
		String exportedGraph = Graphviz.fromGraph(copy).render(Format.DOT).toString();
		writer.write(exportedGraph);
	}

	/**
	 * Dumps this graph through the given {@link Writer}, excluding the legend.
	 * 
	 * @param writer the writer to use for dumping the graph
	 * 
	 * @throws IOException if an I/O error occurs while writing
	 */
	public void dumpStripped(
			Writer writer)
			throws IOException {
		String exportedGraph = Graphviz.fromGraph(graph).render(Format.DOT).toString();
		writer.write(exportedGraph);
	}

}
