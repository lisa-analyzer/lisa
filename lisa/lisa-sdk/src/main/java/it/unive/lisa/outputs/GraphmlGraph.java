package it.unive.lisa.outputs;

import it.unive.lisa.outputs.serializableGraph.SerializableArray;
import it.unive.lisa.outputs.serializableGraph.SerializableEdge;
import it.unive.lisa.outputs.serializableGraph.SerializableNode;
import it.unive.lisa.outputs.serializableGraph.SerializableObject;
import it.unive.lisa.outputs.serializableGraph.SerializableString;
import it.unive.lisa.outputs.serializableGraph.SerializableValue;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.stream.file.FileSinkGraphML;

/**
 * A graph that can be dumped into compound GraphML format.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class GraphmlGraph extends GraphStreamWrapper {

	private static final String QUALIFIER = "::";
	private static final String LABEL_QUALIFIER = QUALIFIER + "LABEL";
	private static final String ARRAY_QUALIFIER = QUALIFIER + "ARRAY";
	private static final String ELEMENT_QUALIFIER = QUALIFIER + "ELEMENT";
	private static final String NODE_SUBNODE_PREFIX = "NODE_SUBNODE_";
	private static final String LABEL_FIELD_PREFIX = "LABEL_FIELD_";
	private static final String LABEL_ARRAY = "LABEL_ARRAY";
	private static final String LABEL_TEXT = "LABEL_TEXT";
	private static final String KIND_DESCRIPTION = "DESCRIPTION";
	private static final String KIND_SUBNODE = "SUBNODE";
	private static final String EDGE_KIND = "EDGE_KIND";
	private static final String NODE_KIND = "NODE_KIND";
	private static final String NODE_LABEL = "NODE_LABEL";
	private static final String NODE_CONTENT = "NODE_CONTENT";
	private static final String NODE_TEXT = "NODE_TEXT";
	private static final String NODE_IS_EXIT = "NODE_IS_EXIT";
	private static final String NODE_IS_ENTRY = "NODE_IS_ENTRY";
	private static final String GRAPH_TITLE = "GRAPH_TITLE";
	private static final String NO = "no";
	private static final String YES = "yes";

	private final Map<String, AtomicInteger> subnodesCount = new HashMap<>();

	private final String title;

	/**
	 * Builds a graph.
	 * 
	 * @param title the title of the graph, if any
	 */
	public GraphmlGraph(String title) {
		super();
		this.graph.setAttribute(GRAPH_TITLE, title);
		this.title = title;
	}

	/**
	 * Yields the title of the graph.
	 * 
	 * @return the title
	 */
	public String getTitle() {
		return title;
	};

	/**
	 * Adds a node to the graph. The label of {@code node} will be composed by
	 * joining {@code node.toString()} ( {@link Object#toString()}) with
	 * {@code labelGenerator.apply(node)} ({@link Function#apply(Object)})
	 * through a new line.
	 * 
	 * @param node  the source node
	 * @param entry whether or not this edge is an entrypoint of the graph
	 * @param exit  whether or not this edge is an exitpoint of the graph
	 * @param label the additional label that can be added to each node as a
	 *                  subnode
	 */
	public void addNode(SerializableNode node, boolean entry, boolean exit, SerializableValue label) {
		Node n = graph.addNode(nodeName(node.getId()));

		if (entry)
			n.setAttribute(NODE_IS_ENTRY, YES);
		else
			n.setAttribute(NODE_IS_ENTRY, NO);
		if (exit)
			n.setAttribute(NODE_IS_EXIT, YES);
		else
			n.setAttribute(NODE_IS_EXIT, NO);

		n.setAttribute(NODE_TEXT, node.getText());
		if (label != null) {
			String graphname = node.getId() + LABEL_QUALIFIER;
			MultiGraph labelgraph = new MultiGraph(graphname);
			populate(graphname, 0, labelgraph, label);

			MultiGraph wrappergraph = new MultiGraph(graphname + "_WRAPPER");
			Node wrapper = wrappergraph.addNode(graphname + "_WRAPPERNODE");
			wrapper.setAttribute(NODE_KIND, KIND_DESCRIPTION);
			wrapper.setAttribute(NODE_CONTENT, labelgraph);
			n.setAttribute(NODE_LABEL, wrappergraph);
		}
	}

	private void populate(String prefix, int depth, MultiGraph g, SerializableValue value) {
		if (value instanceof SerializableString) {
			Node node = g.addNode(prefix + ELEMENT_QUALIFIER);
			node.setAttribute(LABEL_TEXT, value.toString());
			value.getProperties().forEach((k, v) -> node.setAttribute(k, v));
		} else if (value instanceof SerializableArray) {
			SerializableArray array = (SerializableArray) value;
			if (array.getElements().stream().allMatch(SerializableString.class::isInstance)) {
				Node node = g.addNode(prefix + ELEMENT_QUALIFIER);
				node.setAttribute(LABEL_TEXT, value.toString());
				value.getProperties().forEach((k, v) -> node.setAttribute(k, v));
			} else
				for (int i = 0; i < array.getElements().size(); i++) {
					String graphname = prefix + QUALIFIER + depth + ARRAY_QUALIFIER + QUALIFIER + i;
					Node node = g.addNode(graphname);
					SerializableValue array_element = array.getElements().get(i);
					MultiGraph labelgraph = new MultiGraph(graphname);
					populate(graphname, depth + 1, labelgraph, array_element);
					node.setAttribute(LABEL_ARRAY, labelgraph);
					node.setAttribute(NODE_TEXT, "Element " + i);
					array_element.getProperties().forEach((k, v) -> node.setAttribute(k, v));
				}
		} else if (value instanceof SerializableObject) {
			SerializableObject object = (SerializableObject) value;
			for (Entry<String, SerializableValue> field : object.getFields().entrySet()) {
				String graphname = prefix + QUALIFIER + depth + QUALIFIER + field.getKey();
				Node node = g.addNode(graphname);
				MultiGraph labelgraph = new MultiGraph(graphname);
				populate(graphname, depth + 1, labelgraph, field.getValue());
				node.setAttribute(LABEL_FIELD_PREFIX + field.getKey(), labelgraph);
				node.setAttribute(NODE_TEXT, field.getKey());
				field.getValue().getProperties().forEach((k, v) -> node.setAttribute(k, v));
			}
		} else
			throw new IllegalArgumentException("Unknown value type: " + value.getClass().getName());
	}

	/**
	 * Takes note that {@code inner} is a subnode of {@code node} instead of a
	 * proper node of the graph. This removes {@code inner} from the graph, and
	 * adds a new subgraph to {@code node} containing {@code inner}.
	 * 
	 * @param node  the parent node
	 * @param inner the subnode
	 */
	public void markSubNode(SerializableNode node, SerializableNode inner) {
		Node sub = graph.removeNode(nodeName(inner.getId()));
		Node outer = graph.getNode(nodeName(node.getId()));

		String graphname = node.getId() + QUALIFIER + inner.getId();
		MultiGraph innergraph = new MultiGraph(graphname);
		Node n = innergraph.addNode(sub.getId());
		sub.attributeKeys().forEach(k -> n.setAttribute(k, sub.getAttribute(k)));

		MultiGraph wrappergraph = new MultiGraph(graphname + "_WRAPPER");
		Node wrapper = wrappergraph.addNode(graphname + "_WRAPPERNODE");
		wrapper.setAttribute(NODE_KIND, KIND_SUBNODE);
		wrapper.setAttribute(NODE_CONTENT, innergraph);

		AtomicInteger counter = subnodesCount.computeIfAbsent(nodeName(node.getId()), k -> new AtomicInteger(0));
		int idx = counter.getAndIncrement();
		outer.setAttribute(NODE_SUBNODE_PREFIX + idx, wrappergraph);
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
		e.setAttribute(EDGE_KIND, edge.getKind());
	}

	@Override
	public void dump(Writer writer) throws IOException {
		dump(writer, true);
	}

	/**
	 * This method provides the actual implementation for {@link #dump(Writer)},
	 * optionally avoiding the xml formatting (i.e. new lines and indentations).
	 * 
	 * @param writer the writer to use for dumping the graph
	 * @param format whether or not the output should be formatted
	 * 
	 * @throws IOException if an I/O error occurs while writing
	 */
	public void dump(Writer writer, boolean format) throws IOException {
		FileSinkGraphML sink = new CustomGraphMLSink(format);
		sink.writeAll(graph, writer);
	}

	/**
	 * The default graphstream sink does not support hierarchical nodes.
	 */
	private static class CustomGraphMLSink extends FileSinkGraphML {

		private final boolean format;

		private CustomGraphMLSink(boolean format) {
			this.format = format;
		}

		private void print(int indent, String format, Object... args) throws IOException {
			String out = "";
			if (this.format)
				out += "\t".repeat(indent);
			if (args.length == 0)
				out += format;
			else
				out += String.format(format, args);
			if (this.format)
				out += "\n";
			output.write(out);
		}

		@Override
		protected void outputEndOfFile() throws IOException {
			print(0, "</graphml>");
		}

		@Override
		protected void outputHeader() throws IOException {
			print(0, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			print(0, "<graphml xmlns=\"http://graphml.graphdrawing.org/xmlns\"");
			print(1, " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
			print(1, " xsi:schemaLocation=\"http://graphml.graphdrawing.org/xmlns");
			print(1, "   http://graphml.graphdrawing.org/xmlns/1.0/graphml.xsd\">");
		}

		private static String escapeXmlString(String string) {
			return string.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
					.replace("'", "&apos;");
		}

		@Override
		protected void exportGraph(Graph g) {
			Map<String, String> nodeAttributes = new TreeMap<>();
			Map<String, String> edgeAttributes = new TreeMap<>();
			collectKeys(g, nodeAttributes, edgeAttributes);
			dumpKeys(nodeAttributes, edgeAttributes);
			dumpGraph(g, nodeAttributes, edgeAttributes);
		}

		protected void processAttribute(String key, Object value, Map<String, String> attributes,
				Map<String, String> nodeAttributes, Map<String, String> edgeAttributes) {
			String type;

			if (value == null)
				throw new OutputDumpingException("Attribute with no value are not supported");

			if (value instanceof Graph) {
				collectKeys((Graph) value, nodeAttributes, edgeAttributes);
				return;
			}

			if (value instanceof Boolean)
				type = "boolean";
			else if (value instanceof Long)
				type = "long";
			else if (value instanceof Integer)
				type = "int";
			else if (value instanceof Double)
				type = "double";
			else if (value instanceof Float)
				type = "float";
			else
				type = "string";

			if (!attributes.containsKey(key))
				attributes.put(key, type);
			else if (!attributes.get(key).equals(type))
				throw new OutputDumpingException("Attributes with the same name have different value type");
		}

		protected void collectKeys(Graph g, Map<String, String> nodeAttributes, Map<String, String> edgeAttributes) {
			g.nodes().forEach(n -> n.attributeKeys().forEach(
					k -> processAttribute(k, n.getAttribute(k), nodeAttributes, nodeAttributes, edgeAttributes)));

			g.edges().forEach(e -> e.attributeKeys().forEach(
					k -> processAttribute(k, e.getAttribute(k), edgeAttributes, nodeAttributes, edgeAttributes)));
		}

		protected void dumpKeys(Map<String, String> nodeAttributes,
				Map<String, String> edgeAttributes) {
			for (Entry<String, String> entry : nodeAttributes.entrySet())
				try {
					print(1, "<key id=\"%s\" for=\"node\" attr.name=\"%s\" attr.type=\"%s\"/>",
							entry.getKey(),
							entry.getKey(),
							entry.getValue());
				} catch (Exception ex) {
					throw new OutputDumpingException("Exception while dumping graphml attribute key", ex);
				}

			for (Entry<String, String> entry : edgeAttributes.entrySet())
				try {
					print(1, "<key id=\"%s\" for=\"edge\" attr.name=\"%s\" attr.type=\"%s\"/>",
							entry.getKey(),
							entry.getKey(),
							entry.getValue());
				} catch (Exception ex) {
					throw new OutputDumpingException("Exception while dumping graphml attribute key", ex);
				}
		}

		protected void dumpGraph(Graph g, Map<String, String> nodeAttributes,
				Map<String, String> edgeAttributes) {
			try {
				print(1, "<graph id=\"%s\" edgedefault=\"directed\">", escapeXmlString(g.getId()));
			} catch (Exception e) {
				throw new OutputDumpingException("Exception while dumping graphml graph element", e);
			}

			g.nodes().forEach(n -> {
				try {
					print(2, "<node id=\"%s\">", n.getId());

					n.attributeKeys().forEach(k -> {
						try {
							Object value = n.getAttribute(k);

							if (!(value instanceof Graph))
								print(3, "<data key=\"%s\">%s</data>", k, escapeXmlString(value.toString()));
							else {
								Graph inner = (Graph) value;
								CustomGraphMLSink innersink = new CustomGraphMLSink(this.format);
								StringWriter innerwriter = new StringWriter();
								innersink.output = innerwriter;
								innersink.dumpGraph(inner, nodeAttributes, edgeAttributes);
								if (this.format)
									for (String line : innerwriter.toString().split("\n"))
										print(2, line);
								else
									print(0, innerwriter.toString());
							}
						} catch (IOException ex) {
							throw new OutputDumpingException("Exception while dumping graphml node attribute element",
									ex);
						}
					});

					print(2, "</node>");
				} catch (Exception ex) {
					throw new OutputDumpingException("Exception while dumping graphml node element", ex);
				}
			});

			g.edges().forEach(e -> {
				try {
					print(2, "<edge id=\"%s\" source=\"%s\" target=\"%s\" directed=\"%s\">", e.getId(),
							e.getSourceNode().getId(), e.getTargetNode().getId(), e.isDirected());

					e.attributeKeys().forEach(k -> {
						try {
							print(3, "<data key=\"%s\">%s</data>", k,
									escapeXmlString(e.getAttribute(k).toString()));
						} catch (IOException ex) {
							throw new OutputDumpingException("Exception while dumping graphml edge attribute element",
									ex);
						}
					});

					print(2, "</edge>");
				} catch (Exception ex) {
					throw new OutputDumpingException("Exception while dumping graphml edge element", ex);
				}
			});

			try {
				print(1, "</graph>");
			} catch (Exception e) {
				throw new OutputDumpingException("Exception while dumping graphml graph element", e);
			}
		}
	}
}
