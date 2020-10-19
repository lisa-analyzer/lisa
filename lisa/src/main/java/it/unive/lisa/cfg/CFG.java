package it.unive.lisa.cfg;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.StringEscapeUtils;

import it.unive.lisa.cfg.edge.Edge;
import it.unive.lisa.cfg.edge.FalseEdge;
import it.unive.lisa.cfg.edge.SequentialEdge;
import it.unive.lisa.cfg.edge.TrueEdge;
import it.unive.lisa.cfg.statement.NoOp;
import it.unive.lisa.cfg.statement.Statement;
import it.unive.lisa.util.collections.ExternalSet;

/**
 * A control flow graph, that has {@link Statement}s as nodes and {@link Edge}s
 * as edges.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class CFG {

	/**
	 * The adjacency matrix of this graph, mapping statements to the collection of
	 * edges attached to it.
	 */
	private final AdjacencyMatrix adjacencyMatrix;

	/**
	 * The statements of this control flow graph that are entrypoints, that is, that
	 * can be executed from other cfgs.
	 */
	private Collection<Statement> entrypoints;

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
		this.adjacencyMatrix = new AdjacencyMatrix();
		this.descriptor = descriptor;
		this.entrypoints = new HashSet<>();
	}

	/**
	 * Clones the given control flow graph.
	 * 
	 * @param other the original cfg
	 */
	protected CFG(CFG other) {
		this.adjacencyMatrix = new AdjacencyMatrix(other.adjacencyMatrix);
		this.entrypoints = new ArrayList<>(other.entrypoints);
		this.descriptor = other.descriptor;
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
	 * Yields the statements of this control flow graph that are entrypoints, that
	 * is, that can be executed from other cfgs. This usually contains the first
	 * statement of this cfg, but might also contain other ones.
	 * 
	 * @return the entrypoints of this cfg.
	 */
	public final Collection<Statement> getEntrypoints() {
		return entrypoints;
	}

	/**
	 * Yields the set of nodes of this control flow graph.
	 * 
	 * @return the collection of nodes
	 */
	public final Collection<Statement> getNodes() {
		return adjacencyMatrix.getNodes();
	}

	/**
	 * Yields the set of edges of this control flow graph.
	 * 
	 * @return the collection of edges
	 */
	public final Collection<Edge> getEdges() {
		return adjacencyMatrix.getEdges();
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
	 * Adds the given node to the set of nodes, optionally marking this as
	 * entrypoint (that is, reachable executable from other cfgs). The first
	 * statement of a cfg should always be marked as entrypoint. Besides, statements
	 * that might be reached through jumps from external cfgs should be marked as
	 * entrypoints as well.
	 * 
	 * @param node       the node to add
	 * @param entrypoint if {@code true} causes the given node to be considered as
	 *                   an entrypoint.
	 */
	public final void addNode(Statement node, boolean entrypoint) {
		adjacencyMatrix.addNode(node);
		if (entrypoint)
			this.entrypoints.add(node);
	}

	/**
	 * Adds an edge to this control flow graph.
	 * 
	 * @param edge the edge to add
	 * @throws UnsupportedOperationException if the source or the destination of the
	 *                                       given edge are not part of this cfg
	 */
	public void addEdge(Edge edge) {
		adjacencyMatrix.addEdge(edge);
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
	 * Yields the edge connecting the two given statements, if any. Yields
	 * {@code null} if such edge does not exist, or if one of the two statements is
	 * not inside this cfg.
	 * 
	 * @param source      the source statement
	 * @param destination the destination statement
	 * @return the edge connecting {@code source} to {@code destination}, or
	 *         {@code null}
	 */
	public final Edge getEdgeConnecting(Statement source, Statement destination) {
		return adjacencyMatrix.getEdgeConnecting(source, destination);
	}

	/**
	 * Yields the collection of the nodes that are followers of the given one, that
	 * is, all nodes such that there exist an edge in this control flow graph going
	 * from the given node to such node. Yields {@code null} if the node is not in
	 * this cfg.
	 * 
	 * @param node the node
	 * @return the collection of followers
	 */
	public final Collection<Statement> followersOf(Statement node) {
		return adjacencyMatrix.followersOf(node);
	}

	/**
	 * Yields the collection of the nodes that are predecessors of the given vertex,
	 * that is, all nodes such that there exist an edge in this control flow graph
	 * going from such node to the given one. Yields {@code null} if the node is not
	 * in this cfg.
	 * 
	 * @param node the node
	 * @return the collection of predecessors
	 */
	public final Collection<Statement> predecessorsOf(Statement node) {
		return adjacencyMatrix.predecessorsOf(node);
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

		for (Map.Entry<Statement, Pair<ExternalSet<Edge>, ExternalSet<Edge>>> entry : adjacencyMatrix) {
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

			for (Edge edge : entry.getValue().getRight()) {
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
		result = prime * result + ((entrypoints == null) ? 0 : entrypoints.hashCode());
		return result;
	}

	/**
	 * CFG instances use reference equality for equality checks, under the
	 * assumption that every cfg is unique. For checking if two cfgs are effectively
	 * equal (that is, they are different object with the same structure) use
	 * {@link #isEqualTo(CFG)}. <br>
	 * <br>
	 * 
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj) {
		return this == obj;
	}

	/**
	 * Checks if this cfg is effectively equal to the given one, that is, if they
	 * have the same structure while potentially being different instances.
	 * 
	 * @param cfg the other cfg
	 * @return {@code true} if this cfg and the given one are effectively equals
	 */
	public boolean isEqualTo(CFG cfg) {
		if (this == cfg)
			return true;
		if (cfg == null)
			return false;
		if (getClass() != cfg.getClass())
			return false;
		if (descriptor == null) {
			if (cfg.descriptor != null)
				return false;
		} else if (!descriptor.equals(cfg.descriptor))
			return false;
		if (entrypoints == null) {
			if (cfg.entrypoints != null)
				return false;
		} else if (entrypoints.size() != cfg.entrypoints.size())
			return false;
		else {
			// statements use reference equality, thus entrypoint.equals(cfg.entrypoints)
			// won't
			// achieve content comparison. Need to do this manually.

			// the following keeps track of the unmatched statements in cfg.entrypoints
			Collection<Statement> copy = new HashSet<>(cfg.entrypoints);
			boolean found;
			for (Statement s : entrypoints) {
				found = false;
				for (Statement ss : cfg.entrypoints)
					if (copy.contains(ss) && s.isEqualTo(ss)) {
						copy.remove(ss);
						found = true;
						break;
					}
				if (!found)
					return false;
			}

			if (!copy.isEmpty())
				// we also have to match all of the entrypoints in cfg.entrypoints
				return false;
		}
		if (adjacencyMatrix == null) {
			if (cfg.adjacencyMatrix != null)
				return false;
		} else if (!adjacencyMatrix.isEqualTo(cfg.adjacencyMatrix))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return descriptor.toString();
	}

	/**
	 * Simplifies this cfg, removing all {@link NoOp}s and rewriting the edge set
	 * accordingly. This method will throw an {@link UnsupportedOperationException}
	 * if one of the {@link NoOp}s has an outgoing edge that is not a
	 * {@link SequentialEdge}, since such statement is expected to always be
	 * sequential.
	 * 
	 * @throws UnsupportedOperationException if there exists at least one
	 *                                       {@link NoOp} with an outgoing
	 *                                       non-sequential edge, or if one of the
	 *                                       ingoing edges to the {@link NoOp} is
	 *                                       not currently supported.
	 */
	public void simplify() {
		adjacencyMatrix.simplify();
	}
}
