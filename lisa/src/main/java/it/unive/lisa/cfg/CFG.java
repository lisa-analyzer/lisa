package it.unive.lisa.cfg;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.text.StringEscapeUtils;

import it.unive.lisa.cfg.statement.Statement;

/**
 * A control flow graph, that has {@link Statement}s as nodes and {@link Edge}s
 * as edges.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class CFG {

	/**
	 * Singleton for the empty collection
	 */
	private static final Collection<Edge> EMPTY_LIST = new ArrayList<>();

	/**
	 * The adjacency matrix of this graph, mapping statements to the collection of
	 * edges attached to it.
	 */
	private final Map<Statement, Collection<Edge>> adjacencyMatrix;

	/**
	 * The first statement of this control flow graph.
	 */
	private Statement first;

	/**
	 * The descriptor of this control flow graph.
	 */
	private final CFGDescriptor descriptor;

	/**
	 * Builds the control flow graph.
	 * 
	 * @param descriptor the descriptor of this cfg
	 */
	public CFG(CFGDescriptor descriptor) {
		this.adjacencyMatrix = new HashMap<>();
		this.descriptor = descriptor;
	}

	/**
	 * Clones the given control flow graph.
	 * 
	 * @param other the original cfg
	 */
	protected CFG(CFG other) {
		adjacencyMatrix = new HashMap<>(other.adjacencyMatrix);
		first = other.first;
		descriptor = other.descriptor;
	}

	/**
	 * Yields the name of this control flow graph.
	 * 
	 * @return the name
	 */
	public final CFGDescriptor getDescriptor() {
		return descriptor;
	}

	/**
	 * Yields the root of this control flow graph.
	 * 
	 * @return the root
	 */
	public final Statement getFirstStatement() {
		return first;
	}

	/**
	 * Yields the set of nodes of this control flow graph.
	 * 
	 * @return the collection of nodes
	 */
	public final Collection<Statement> getNodes() {
		return adjacencyMatrix.keySet();
	}

	/**
	 * Yields the set of edges of this control flow graph.
	 * 
	 * @return the collection of edges
	 */
	public final Collection<Edge> getEdges() {
		return adjacencyMatrix.values().stream().flatMap(c -> c.stream()).collect(Collectors.toList());
	}

	/**
	 * Adds the given node to the set of nodes, optionally setting that as root.
	 * This is equivalent to invoking {@link #addNode(Statement, boolean)} with
	 * {@code false} as second parameter.
	 * 
	 * @param node the node to add
	 */
	public final void addNode(Statement node) {
		addNode(node, false);
	}

	/**
	 * Adds the given node to the set of nodes, optionally setting that as first
	 * instruction for this cfg.
	 * 
	 * @param node the node to add
	 * @param root if {@code true} causes the given node to be set as first
	 *             instruction
	 */
	public final void addNode(Statement node, boolean root) {
		adjacencyMatrix.put(node, new LinkedList<>());
		if (root)
			this.first = node;
	}

	/**
	 * Adds an edge to this control flow graph.
	 * 
	 * @param edge the edge to add
	 * @throws UnsupportedOperationException if the source or the destination of the
	 *                                       given edge is not part of this cfg
	 */
	protected void addEdge(Edge edge) {
		if (!adjacencyMatrix.containsKey(edge.getSource()))
			throw new UnsupportedOperationException("The source node is not in the graph");

		if (!adjacencyMatrix.containsKey(edge.getDestination()))
			throw new UnsupportedOperationException("The destination node is not in the graph");

		adjacencyMatrix.get(first).add(edge);
	}

	/**
	 * Yields the total number of nodes of this control flow graph.
	 * 
	 * @return the number of nodes
	 */
	public final int getNodesCount() {
		return getNodes().size();
	}

	/**
	 * Yields the total number of edges of this control flow graph.
	 * 
	 * @return the number of edges
	 */
	public final int getEdgesCount() {
		return getEdges().size();
	}

	/**
	 * Yields the collection of the nodes that are followers of the given one, that
	 * is, all nodes such that there exist an edge in this control flow graph going
	 * from the given node to such node.
	 * 
	 * @param node the node
	 * @return the collection of followers
	 */
	public final Collection<Statement> followersOf(Statement node) {
		return adjacencyMatrix.getOrDefault(node, EMPTY_LIST).stream().map(e -> e.getDestination())
				.collect(Collectors.toList());
	}

	/**
	 * Yields the collection of the nodes that are predecessors of the given vertex,
	 * that is, all nodes such that there exist an edge in this control flow graph
	 * going from such node to the given one.
	 * 
	 * @param node the node
	 * @return the collection of predecessors
	 */
	public final Collection<Statement> predecessorsOf(Statement node) {
		return adjacencyMatrix.values().stream().flatMap(c -> c.stream())
				.filter(edge -> edge.getDestination().equals(node)).map(edge -> edge.getSource()).distinct()
				.collect(Collectors.toList());
	}

	/**
	 * Dumps the content of this control flow graph in the given writer, formatted
	 * as a dot file.
	 * 
	 * @param writer the writer where the content will be written
	 * @param name   the name of the dot diagraph
	 * @throws IOException if an exception happens while writing something to the
	 *                     given writer
	 */
	public void dump(Writer writer, String name) throws IOException {
		dump(writer, name, st -> "");
	}

	/**
	 * Dumps the content of this control flow graph in the given writer, formatted
	 * as a dot file. The content of each vertex will be enriched by invoking
	 * labelGenerator on the vertex itself, to obtain an extra description to be
	 * concatenated with the standard call to the vertex's {@link #toString()}
	 * 
	 * @param writer         the writer where the content will be written
	 * @param name           the name of the dot diagraph
	 * @param labelGenerator the function used to generate extra labels
	 * @throws IOException if an exception happens while writing something to the
	 *                     given writer
	 */
	public void dump(Writer writer, String name, Function<Statement, String> labelGenerator) throws IOException {
		writer.write("digraph " + name + " {\n");

		Map<Statement, Integer> codes = new IdentityHashMap<>();
		int code = 0;

		for (Map.Entry<Statement, Collection<Edge>> entry : adjacencyMatrix.entrySet()) {
			Statement current = entry.getKey();

			if (!codes.containsKey(current))
				codes.put(current, code++);

			int id = codes.get(current);
			String extraLabel = labelGenerator.apply(current);
			if (!extraLabel.isEmpty())
				extraLabel = "<BR/>" + dotEscape(extraLabel);

			writer.write("node" + id + " [");
			writer.write(provideVertexShapeIfNeeded(current));
			writer.write("label = <" + dotEscape(current.toString()) + extraLabel + ">];\n");

			for (Edge edge : entry.getValue()) {
				Statement follower = edge.getDestination();
				if (!codes.containsKey(follower))
					codes.put(follower, code++);

				int id1 = codes.get(follower);
				String label = provideEdgeLabelIfNeeded(edge);
				if (!label.isEmpty())
					writer.write("node" + id + " -> node" + id1 + " [label=\"" + label + "\"]\n");
				else
					writer.write("node" + id + " -> node" + id1 + "\n");
			}
		}

		writer.write("}");
	}

	private String dotEscape(String extraLabel) {
		String escapeHtml4 = StringEscapeUtils.escapeHtml4(extraLabel);
		String replace = escapeHtml4.replaceAll("\\n", "<BR/>");
		return replace.replace("\\", "\\\\");
	}

	private String provideVertexShapeIfNeeded(Statement vertex) {
		String shape = "shape = rect,";
		if (predecessorsOf(vertex).isEmpty() || followersOf(vertex).isEmpty())
			shape += "peripheries=2,";

		return shape;
	}

	private String provideEdgeLabelIfNeeded(Edge edge) {
		if (edge instanceof TrueEdge)
			return "true";
		else if (edge instanceof FalseEdge)
			return "false";

		return "";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((descriptor == null) ? 0 : descriptor.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		// equality relies on identity
		if (this == obj)
			return true;
		return true;
	}

	@Override
	public String toString() {
		return descriptor.toString();
	}
}
