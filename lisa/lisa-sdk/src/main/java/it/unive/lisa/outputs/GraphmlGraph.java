package it.unive.lisa.outputs;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.stream.file.FileSinkGraphML;

import it.unive.lisa.outputs.serializableGraph.SerializableArray;
import it.unive.lisa.outputs.serializableGraph.SerializableEdge;
import it.unive.lisa.outputs.serializableGraph.SerializableNode;
import it.unive.lisa.outputs.serializableGraph.SerializableObject;
import it.unive.lisa.outputs.serializableGraph.SerializableString;
import it.unive.lisa.outputs.serializableGraph.SerializableValue;

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
	private static final String NODE_TEXT = "NODE_TEXT";
	private static final String NODE_IS_EXIT = "NODE_IS_EXIT";
	private static final String NODE_IS_ENTRY = "NODE_IS_ENTRY";
	private static final String GRAPH_TITLE = "GRAPH_TITLE";
	private static final String NO = "no";
	private static final String YES = "yes";

	private final Map<String, AtomicInteger> subnodesCount = new HashMap<>();

	/**
	 * Builds a graph.
	 * 
	 * @param title the title of the graph, if any
	 */
	public GraphmlGraph(String title) {
		super();
		this.graph.setAttribute(GRAPH_TITLE, title);
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
			n.setAttribute(NODE_LABEL, labelgraph);
			n.setAttribute(NODE_KIND, KIND_DESCRIPTION);
		}
	}

	private void populate(String prefix, int depth, MultiGraph g, SerializableValue value) {
		if (value instanceof SerializableString) {
			Node node = g.addNode(prefix + ELEMENT_QUALIFIER);
			node.setAttribute(LABEL_TEXT, value.toString());
		} else if (value instanceof SerializableArray) {
			SerializableArray array = (SerializableArray) value;
			if (array.getElements().stream().allMatch(SerializableString.class::isInstance)) {
				Node node = g.addNode(prefix + ELEMENT_QUALIFIER);
				node.setAttribute(LABEL_TEXT, value.toString());
			} else
				for (int i = 0; i < array.getElements().size(); i++) {
					String graphname = prefix + QUALIFIER + depth + ARRAY_QUALIFIER + QUALIFIER + i;
					Node node = g.addNode(graphname);
					SerializableValue array_element = array.getElements().get(i);
					MultiGraph labelgraph = new MultiGraph(graphname);
					populate(graphname, depth + 1, labelgraph, array_element);
					node.setAttribute(LABEL_ARRAY, labelgraph);
					node.setAttribute(NODE_TEXT, "Element " + i);
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
			}
		} else
			throw new IllegalArgumentException("Unknown value type: " + value.getClass().getName());
	}

	public void markSubNode(SerializableNode node, SerializableNode inner) {
		Node sub = graph.removeNode(nodeName(inner.getId()));
		Node outer = graph.getNode(nodeName(node.getId()));

		MultiGraph innergraph = new MultiGraph(node.getId() + QUALIFIER + inner.getId());
		Node n = innergraph.addNode(sub.getId());
		sub.attributeKeys().forEach(k -> n.setAttribute(k, sub.getAttribute(k)));

		AtomicInteger counter = subnodesCount.computeIfAbsent(nodeName(node.getId()), k -> new AtomicInteger(0));
		int idx = counter.getAndIncrement();
		outer.setAttribute(NODE_SUBNODE_PREFIX + idx, innergraph);
		n.setAttribute(NODE_KIND, KIND_SUBNODE);
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
		FileSinkGraphML sink = new CustomGraphMLSink();
		sink.writeAll(graph, writer);
	}

	/**
	 * The default graphstream sink does not support hierarchical nodes.
	 */
	private static class CustomGraphMLSink extends FileSinkGraphML {

		private void print(String format, Object... args) throws IOException {
			if (args.length == 0)
				output.write(format);
			else
				output.write(String.format(format, args));
		}

		private static String escapeXmlString(String string) {
			return string.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
					.replace("'", "&apos;");
		}

		@Override
		protected void exportGraph(Graph g) {
			Map<String, Pair<String, String>> nodeAttributes = new TreeMap<>();
			Map<String, Pair<String, String>> edgeAttributes = new TreeMap<>();
			AtomicInteger counter = new AtomicInteger(0);
			collectKeys(g, nodeAttributes, edgeAttributes, counter);
			dumpKeys(nodeAttributes, edgeAttributes);
			dumpGraph(g, nodeAttributes, edgeAttributes);
		}

		protected void processAttribute(String key, Object value, Map<String, Pair<String, String>> attributes,
				Map<String, Pair<String, String>> nodeAttributes, Map<String, Pair<String, String>> edgeAttributes,
				AtomicInteger counter) {
			String type;

			if (value == null)
				throw new OutputDumpingException("Attribute with no value are not supported");

			if (value instanceof Graph) {
				collectKeys((Graph) value, nodeAttributes, edgeAttributes, counter);
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

			if (!attributes.containsKey(key)) {
				String id = String.format("attr%04X", counter.getAndIncrement());
				attributes.put(key, Pair.of(id, type));
			} else if (!attributes.get(key).getRight().equals(type))
				throw new OutputDumpingException("Attributes with the same name have different value type");
		}

		protected void collectKeys(Graph g, Map<String, Pair<String, String>> nodeAttributes,
				Map<String, Pair<String, String>> edgeAttributes, AtomicInteger counter) {
			g.nodes().forEach(n -> n.attributeKeys().forEach(
					k -> processAttribute(k, n.getAttribute(k), nodeAttributes, nodeAttributes, edgeAttributes,
							counter)));

			g.edges().forEach(n -> n.attributeKeys().forEach(
					k -> processAttribute(k, n.getAttribute(k), edgeAttributes, nodeAttributes, edgeAttributes,
							counter)));
		}

		protected void dumpKeys(Map<String, Pair<String, String>> nodeAttributes,
				Map<String, Pair<String, String>> edgeAttributes) {
			for (Entry<String, Pair<String, String>> entry : nodeAttributes.entrySet())
				try {
					print("\t<key id=\"%s\" for=\"node\" attr.name=\"%s\" attr.type=\"%s\"/>\n",
							entry.getValue().getLeft(),
							escapeXmlString(entry.getKey()), entry.getValue().getRight());
				} catch (Exception ex) {
					throw new OutputDumpingException("Exception while dumping graphml attribute key", ex);
				}

			for (Entry<String, Pair<String, String>> entry : edgeAttributes.entrySet())
				try {
					print("\t<key id=\"%s\" for=\"edge\" attr.name=\"%s\" attr.type=\"%s\"/>\n",
							entry.getValue().getLeft(),
							escapeXmlString(entry.getKey()), entry.getValue().getRight());
				} catch (Exception ex) {
					throw new OutputDumpingException("Exception while dumping graphml attribute key", ex);
				}
		}

		protected void dumpGraph(Graph g, Map<String, Pair<String, String>> nodeAttributes,
				Map<String, Pair<String, String>> edgeAttributes) {
			try {
				print("\t<graph id=\"%s\" edgedefault=\"directed\">\n", escapeXmlString(g.getId()));
			} catch (Exception e) {
				throw new OutputDumpingException("Exception while dumping graphml graph element", e);
			}

			g.nodes().forEach(n -> {
				try {
					print("\t\t<node id=\"%s\">\n", n.getId());

					n.attributeKeys().forEach(k -> {
						try {
							Object value = n.getAttribute(k);

							if (!(value instanceof Graph))
								print("\t\t\t<data key=\"%s\">%s</data>\n", nodeAttributes.get(k).getLeft(),
										escapeXmlString(value.toString()));
							else {
								Graph inner = (Graph) value;
								CustomGraphMLSink innersink = new CustomGraphMLSink();
								StringWriter innerwriter = new StringWriter();
								innersink.output = innerwriter;
								innersink.dumpGraph(inner, nodeAttributes, edgeAttributes);
								for (String line : innerwriter.toString().split("\n"))
									print("\t\t" + line + "\n");
							}
						} catch (IOException ex) {
							throw new OutputDumpingException("Exception while dumping graphml node attribute element",
									ex);
						}
					});

					print("\t\t</node>\n");
				} catch (Exception ex) {
					throw new OutputDumpingException("Exception while dumping graphml node element", ex);
				}
			});

			g.edges().forEach(e -> {
				try {
					print("\t\t<edge id=\"%s\" source=\"%s\" target=\"%s\" directed=\"%s\">\n", e.getId(),
							e.getSourceNode().getId(), e.getTargetNode().getId(), e.isDirected());

					e.attributeKeys().forEach(k -> {
						try {
							print("\t\t\t<data key=\"%s\">%s</data>\n", edgeAttributes.get(k).getLeft(),
									escapeXmlString(e.getAttribute(k).toString()));
						} catch (IOException ex) {
							throw new OutputDumpingException("Exception while dumping graphml edge attribute element",
									ex);
						}
					});

					print("\t\t</edge>\n");
				} catch (Exception ex) {
					throw new OutputDumpingException("Exception while dumping graphml edge element", ex);
				}
			});

			try {
				print("\t</graph>\n");
			} catch (Exception e) {
				throw new OutputDumpingException("Exception while dumping graphml graph element", e);
			}
		}
	}
}
