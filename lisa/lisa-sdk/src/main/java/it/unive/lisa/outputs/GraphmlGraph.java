package it.unive.lisa.outputs;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.stream.file.FileSinkGraphML;

import it.unive.lisa.outputs.serializableGraph.SerializableEdge;
import it.unive.lisa.outputs.serializableGraph.SerializableNode;

public class GraphmlGraph extends GraphStreamWrapper {

	private final Map<String, AtomicInteger> subnodesCount = new HashMap<>();

	/**
	 * Builds a graph.
	 * 
	 * @param title the title of the graph, if any
	 */
	public GraphmlGraph(String title) {
		super();
		this.graph.setAttribute("GRAPH_TITLE", title);
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
	public void addNode(SerializableNode node, boolean entry, boolean exit, String label) {
		Node n = graph.addNode(nodeName(node.getId()));

		if (entry)
			n.setAttribute("NODE_IS_ENTRY", "yes");
		else
			n.setAttribute("NODE_IS_ENTRY", "no");
		if (exit)
			n.setAttribute("NODE_IS_EXIT", "yes");
		else
			n.setAttribute("NODE_IS_EXIT", "no");

		n.setAttribute("NODE_TEXT", node.getText());
		if (!label.isEmpty())
			n.setAttribute("NODE_DESCRIPTION", label);
	}

	public void markSubNode(SerializableNode node, SerializableNode inner) {
		Node sub = graph.removeNode(nodeName(inner.getId()));
		Node outer = graph.getNode(nodeName(node.getId()));

		MultiGraph innergraph = new MultiGraph(node.getId() + "::" + inner.getId());
		Node n = innergraph.addNode(sub.getId());
		sub.attributeKeys().forEach(k -> n.setAttribute(k, sub.getAttribute(k)));

		AtomicInteger counter = subnodesCount.computeIfAbsent(nodeName(node.getId()), k -> new AtomicInteger(0));
		int idx = counter.getAndIncrement();
		outer.setAttribute("NODE_SUBNODE_" + idx, innergraph);
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
		e.setAttribute("EDGE_KIND", edge.getKind());
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
			final Consumer<Exception> onException = Exception::printStackTrace;

			AtomicInteger attribute = new AtomicInteger(0);
			HashMap<String, String> nodeAttributes = new HashMap<>();
			HashMap<String, String> edgeAttributes = new HashMap<>();

			g.nodes().forEach(n -> {
				n.attributeKeys().forEach(k -> {
					if (!nodeAttributes.containsKey(k)) {
						Object value = n.getAttribute(k);
						String type;

						if (value == null)
							return;

						if (value instanceof Graph)
							// separate handling
							return;

						String id = String.format("attr%04X", attribute.getAndIncrement());

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

						nodeAttributes.put(k, id);

						try {
							print("\t<key id=\"%s\" for=\"node\" attr.name=\"%s\" attr.type=\"%s\"/>\n", id,
									escapeXmlString(k), type);
						} catch (Exception ex) {
							onException.accept(ex);
						}
					}
				});
			});

			g.edges().forEach(n -> {
				n.attributeKeys().forEach(k -> {
					if (!edgeAttributes.containsKey(k)) {
						Object value = n.getAttribute(k);
						String type;

						if (value == null)
							return;

						String id = String.format("attr%04X", attribute.getAndIncrement());

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

						edgeAttributes.put(k, id);

						try {
							print("\t<key id=\"%s\" for=\"edge\" attr.name=\"%s\" attr.type=\"%s\"/>\n", id,
									escapeXmlString(k), type);
						} catch (Exception ex) {
							onException.accept(ex);
						}
					}
				});
			});

			try {
				print("\t<graph id=\"%s\" edgedefault=\"directed\">\n", escapeXmlString(g.getId()));
			} catch (Exception e) {
				onException.accept(e);
			}

			g.nodes().forEach(n -> {
				try {
					print("\t\t<node id=\"%s\">\n", n.getId());

					n.attributeKeys().forEach(k -> {
						try {
							Object value = n.getAttribute(k);

							if (!(value instanceof Graph))
								print("\t\t\t<data key=\"%s\">%s</data>\n", nodeAttributes.get(k),
										escapeXmlString(value.toString()));
							else {
								Graph inner = (Graph) value;
								CustomGraphMLSink innersink = new CustomGraphMLSink();
								StringWriter innerwriter = new StringWriter();
								innersink.output = innerwriter;
								innersink.exportGraph(inner);
								for (String line : innerwriter.toString().split("\n"))
									print("\t\t\t" + line + "\n");
							}
						} catch (IOException e) {
							onException.accept(e);
						}
					});

					print("\t\t</node>\n");
				} catch (Exception ex) {
					onException.accept(ex);
				}
			});

			g.edges().forEach(e -> {
				try {
					print("\t\t<edge id=\"%s\" source=\"%s\" target=\"%s\" directed=\"%s\">\n", e.getId(),
							e.getSourceNode().getId(), e.getTargetNode().getId(), e.isDirected());

					e.attributeKeys().forEach(k -> {
						try {
							print("\t\t\t<data key=\"%s\">%s</data>\n", edgeAttributes.get(k),
									escapeXmlString(e.getAttribute(k).toString()));
						} catch (IOException e1) {
							onException.accept(e1);
						}
					});

					print("\t\t</edge>\n");
				} catch (Exception ex) {
					onException.accept(ex);
				}
			});

			try {
				print("\t</graph>\n");
			} catch (Exception e) {
				onException.accept(e);
			}
		}
	}
}
