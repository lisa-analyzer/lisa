package it.unive.lisa.cfg;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.CFGWithAnalysisResults;
import it.unive.lisa.analysis.ExpressionStore;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.callgraph.CallGraph;
import it.unive.lisa.cfg.edge.Edge;
import it.unive.lisa.cfg.edge.FalseEdge;
import it.unive.lisa.cfg.edge.SequentialEdge;
import it.unive.lisa.cfg.edge.TrueEdge;
import it.unive.lisa.cfg.statement.Expression;
import it.unive.lisa.cfg.statement.NoOp;
import it.unive.lisa.cfg.statement.Ret;
import it.unive.lisa.cfg.statement.Return;
import it.unive.lisa.cfg.statement.Statement;
import it.unive.lisa.util.collections.ExternalSet;
import it.unive.lisa.util.workset.FIFOWorkingSet;
import it.unive.lisa.util.workset.WorkingSet;

/**
 * A control flow graph, that has {@link Statement}s as nodes and {@link Edge}s
 * as edges.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class CFG {

	private static final Logger log = LogManager.getLogger(CFG.class);

	/**
	 * The default number of fixpoint iteration on a given statement after which
	 * calls ti {@link Lattice#lub(Lattice)} gets replaced with
	 * {@link Lattice#widening(Lattice)}.
	 */
	public static final int DEFAULT_WIDENING_THRESHOLD = 5;

	/**
	 * The adjacency matrix of this graph, mapping statements to the collection
	 * of edges attached to it.
	 */
	private final AdjacencyMatrix adjacencyMatrix;

	/**
	 * The statements of this control flow graph that are entrypoints, that is,
	 * that can be executed from other cfgs.
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
	 * Builds the control flow graph.
	 * 
	 * @param descriptor      the descriptor of this cfg
	 * @param entrypoints     the statements of this cfg that will be reachable
	 *                            from other cfgs
	 * @param adjacencyMatrix the matrix containing all the statements and the
	 *                            edges that will be part of this cfg
	 */
	public CFG(CFGDescriptor descriptor, Collection<Statement> entrypoints, AdjacencyMatrix adjacencyMatrix) {
		this.adjacencyMatrix = adjacencyMatrix;
		this.descriptor = descriptor;
		this.entrypoints = entrypoints;
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
	 * Yields the statements of this control flow graph that are entrypoints,
	 * that is, that can be executed from other cfgs. This usually contains the
	 * first statement of this cfg, but might also contain other ones.
	 * 
	 * @return the entrypoints of this cfg.
	 */
	public final Collection<Statement> getEntrypoints() {
		return entrypoints;
	}

	/**
	 * Yields the statements of this control flow graph that are normal
	 * exitpoints, that is, that normally ends the execution of this cfg,
	 * returning the control to the caller.
	 * 
	 * @return the exitpoints of this cfg.
	 */
	public final Collection<Statement> getNormalExitpoints() {
		return adjacencyMatrix.getNodes().stream().filter(st -> st instanceof Return || st instanceof Ret)
				.collect(Collectors.toList());
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
	 * statement of a cfg should always be marked as entrypoint. Besides,
	 * statements that might be reached through jumps from external cfgs should
	 * be marked as entrypoints as well.
	 * 
	 * @param node       the node to add
	 * @param entrypoint if {@code true} causes the given node to be considered
	 *                       as an entrypoint.
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
	 * 
	 * @throws UnsupportedOperationException if the source or the destination of
	 *                                           the given edge are not part of
	 *                                           this cfg
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
	 * {@code null} if such edge does not exist, or if one of the two statements
	 * is not inside this cfg.
	 * 
	 * @param source      the source statement
	 * @param destination the destination statement
	 * 
	 * @return the edge connecting {@code source} to {@code destination}, or
	 *             {@code null}
	 */
	public final Edge getEdgeConnecting(Statement source, Statement destination) {
		return adjacencyMatrix.getEdgeConnecting(source, destination);
	}

	/**
	 * Yields the collection of the nodes that are followers of the given one,
	 * that is, all nodes such that there exist an edge in this control flow
	 * graph going from the given node to such node. Yields {@code null} if the
	 * node is not in this cfg.
	 * 
	 * @param node the node
	 * 
	 * @return the collection of followers
	 */
	public final Collection<Statement> followersOf(Statement node) {
		return adjacencyMatrix.followersOf(node);
	}

	/**
	 * Yields the collection of the nodes that are predecessors of the given
	 * vertex, that is, all nodes such that there exist an edge in this control
	 * flow graph going from such node to the given one. Yields {@code null} if
	 * the node is not in this cfg.
	 * 
	 * @param node the node
	 * 
	 * @return the collection of predecessors
	 */
	public final Collection<Statement> predecessorsOf(Statement node) {
		return adjacencyMatrix.predecessorsOf(node);
	}

	/**
	 * Dumps the content of this control flow graph in the given writer,
	 * formatted as a dot file.
	 * 
	 * @param writer the writer where the content will be written
	 * @param name   the name of the dot diagraph
	 * 
	 * @throws IOException if an exception happens while writing something to
	 *                         the given writer
	 */
	public void dump(Writer writer, String name) throws IOException {
		dump(writer, name, st -> "");
	}

	/**
	 * Dumps the content of this control flow graph in the given writer,
	 * formatted as a dot file. The content of each vertex will be enriched by
	 * invoking labelGenerator on the vertex itself, to obtain an extra
	 * description to be concatenated with the standard call to the vertex's
	 * {@link #toString()}
	 * 
	 * @param writer         the writer where the content will be written
	 * @param name           the name of the dot diagraph
	 * @param labelGenerator the function used to generate extra labels
	 * 
	 * @throws IOException if an exception happens while writing something to
	 *                         the given writer
	 */
	public void dump(Writer writer, String name, Function<Statement, String> labelGenerator) throws IOException {
		writer.write("digraph " + cleanupForDiagraphTitle(name) + " {\n");
		writer.write("graph [ordering=\"out\"];\n");
		writer.write("node [shape=rect,color=gray];\n");

		Map<Statement, Integer> codes = new IdentityHashMap<>();
		int code = 0;

		// dump the entrypoints first for the layout
		for (Statement st : entrypoints) {
			StringBuilder label = new StringBuilder();
			code = dotNode(codes, st, code, labelGenerator, label);
			writer.write(label.toString());
		}

		// dump all other statements
		for (Statement st : adjacencyMatrix.getNodes())
			if (!entrypoints.contains(st)) {
				StringBuilder label = new StringBuilder();
				code = dotNode(codes, st, code, labelGenerator, label);
				writer.write(label.toString());
			}

		for (Map.Entry<Statement, Pair<ExternalSet<Edge>, ExternalSet<Edge>>> entry : adjacencyMatrix) {
			int id = codes.get(entry.getKey());
			for (Edge edge : entry.getValue().getRight()) {
				int id1 = codes.get(edge.getDestination());
				String label = provideEdgeLabelIfNeeded(edge);
				if (!label.isEmpty())
					writer.write("node" + id + " -> node" + id1 + " " + label + "\n");
				else
					writer.write("node" + id + " -> node" + id1 + "\n");
			}
		}

		appendLegend(writer);
		writer.write("}");
	}

	private static void appendLegend(Writer writer) throws IOException {
		writer.write("\nsubgraph cluster_01 {\n");
		writer.write("  label = \"Legend\";\n");
		writer.write("  style=dotted;\n");
		writer.write("  node [shape=plaintext];\n");
		writer.write("  values [label=<<table border=\"0\" cellpadding=\"2\" cellspacing=\"0\" cellborder=\"0\">\n");
		writer.write("    <tr><td align=\"left\">gray</td></tr>\n");
		writer.write("    <tr><td align=\"left\">black</td></tr>\n");
		writer.write("    <tr><td align=\"left\">black, double</td></tr>\n");
		writer.write("    <tr><td port=\"e1\">&nbsp;</td></tr>\n");
		writer.write("    <tr><td port=\"e2\">&nbsp;</td></tr>\n");
		writer.write("    <tr><td port=\"e3\">&nbsp;</td></tr>\n");
		writer.write("    </table>>];\n");
		writer.write("  keys [label=<<table border=\"0\" cellpadding=\"2\" cellspacing=\"0\" cellborder=\"0\">\n");
		writer.write("    <tr><td align=\"right\">node border</td></tr>\n");
		writer.write("    <tr><td align=\"right\">entrypoint border</td></tr>\n");
		writer.write("    <tr><td align=\"right\">exitpoint border</td></tr>\n");
		writer.write("    <tr><td align=\"right\" port=\"i1\">sequential edge&nbsp;</td></tr>\n");
		writer.write("    <tr><td align=\"right\" port=\"i2\">true edge&nbsp;</td></tr>\n");
		writer.write("    <tr><td align=\"right\" port=\"i3\">false edge&nbsp;</td></tr>\n");
		writer.write("    </table>>];\n");
		writer.write("  keys:i1:e -> values:e1:w [constraint=false]\n");
		writer.write("  keys:i2:e -> values:e2:w [style=dashed,color=red,constraint=false]\n");
		writer.write("  keys:i3:e -> values:e3:w [style=dashed,color=blue,constraint=false]\n");
		writer.write("}\n");
	}

	private int dotNode(Map<Statement, Integer> codes, Statement st, int nextCode,
			Function<Statement, String> labelGenerator, StringBuilder label) {
		if (!codes.containsKey(st))
			codes.put(st, nextCode++);

		int id = codes.get(st);
		String extraLabel = labelGenerator.apply(st);
		if (!extraLabel.isEmpty())
			extraLabel = "<BR/>" + dotEscape(extraLabel);

		label.append("node").append(id).append(" [").append(provideVertexShapeIfNeeded(st)).append("label = <")
				.append(dotEscape(st.toString())).append(extraLabel).append(">];\n");
		return nextCode;
	}

	private static String cleanupForDiagraphTitle(String name) {
		String result = name.replace(' ', '_');
		result = result.replace("(", "___");
		result = result.replace(")", "___");
		return result;
	}

	private static String dotEscape(String extraLabel) {
		String escapeHtml4 = StringEscapeUtils.escapeHtml4(extraLabel);
		String replace = escapeHtml4.replaceAll("\\n", "<BR/>");
		return replace.replace("\\", "\\\\");
	}

	private String provideVertexShapeIfNeeded(Statement vertex) {
		if (followersOf(vertex).isEmpty())
			return "peripheries=2,color=black,";
		if (entrypoints.contains(vertex))
			return "color=black,";
		return "";
	}

	private String provideEdgeLabelIfNeeded(Edge edge) {
		if (edge instanceof TrueEdge)
			return "[style=dashed,color=blue]";
		else if (edge instanceof FalseEdge)
			return "[style=dashed,color=red]";

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
	 * assumption that every cfg is unique. For checking if two cfgs are
	 * effectively equal (that is, they are different object with the same
	 * structure) use {@link #isEqualTo(CFG)}. <br>
	 * <br>
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj) {
		return this == obj;
	}

	/**
	 * Checks if this cfg is effectively equal to the given one, that is, if
	 * they have the same structure while potentially being different instances.
	 * 
	 * @param cfg the other cfg
	 * 
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
			// statements use reference equality, thus
			// entrypoint.equals(cfg.entrypoints)
			// won't
			// achieve content comparison. Need to do this manually.

			// the following keeps track of the unmatched statements in
			// cfg.entrypoints
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
				// we also have to match all of the entrypoints in
				// cfg.entrypoints
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
	 * Simplifies this cfg, removing all {@link NoOp}s and rewriting the edge
	 * set accordingly. This method will throw an
	 * {@link UnsupportedOperationException} if one of the {@link NoOp}s has an
	 * outgoing edge that is not a {@link SequentialEdge}, since such statement
	 * is expected to always be sequential.
	 * 
	 * @throws UnsupportedOperationException if there exists at least one
	 *                                           {@link NoOp} with an outgoing
	 *                                           non-sequential edge, or if one
	 *                                           of the ingoing edges to the
	 *                                           {@link NoOp} is not currently
	 *                                           supported.
	 */
	public void simplify() {
		adjacencyMatrix.simplify();
	}

	/**
	 * Computes a fixpoint over this control flow graph. This method returns a
	 * {@link CFGWithAnalysisResults} instance mapping each {@link Statement} to
	 * the {@link AnalysisState} computed by this method. The computation uses
	 * {@link Lattice#lub(Lattice)} to compose results obtained at different
	 * iterations, up to {@link #DEFAULT_WIDENING_THRESHOLD}
	 * {@code * predecessors_number} times, where {@code predecessors_number} is
	 * the number of expressions that are predecessors of the one being
	 * processed. After overcoming that threshold,
	 * {@link Lattice#widening(Lattice)} is used. The computation starts at the
	 * statements returned by {@link #getEntrypoints()}, using
	 * {@code entryState} as entry state for all of them. {@code cg} will be
	 * invoked to get the approximation of all invoked cfgs, while a fresh
	 * instance of {@link FIFOWorkingSet} is used as working set for the
	 * statements to process.
	 * 
	 * @param <H>        the type of {@link HeapDomain} contained into the
	 *                       computed abstract state
	 * @param <V>        the type of {@link ValueDomain} contained into the
	 *                       computed abstract state
	 * @param entryState the entry states to apply to each {@link Statement}
	 *                       returned by {@link #getEntrypoints()}
	 * @param cg         the callgraph that can be queried when a call towards
	 *                       an other cfg is encountered
	 * 
	 * @return a {@link CFGWithAnalysisResults} instance that os equivalent to
	 *             this control flow graph, and that stores for each
	 *             {@link Statement} the result of the fixpoint computation
	 * 
	 * @throws FixpointException if an error occurs during the semantic
	 *                               computation of a statement, or if some
	 *                               unknown/invalid statement ends up in the
	 *                               working set
	 */
	public final <H extends HeapDomain<H>, V extends ValueDomain<V>> CFGWithAnalysisResults<H, V> fixpoint(
			AnalysisState<H, V> entryState, CallGraph cg, SemanticFunction<H, V> semantics) throws FixpointException {
		return fixpoint(entryState, cg, FIFOWorkingSet.mk(), DEFAULT_WIDENING_THRESHOLD, semantics);
	}

	/**
	 * Computes a fixpoint over this control flow graph. This method returns a
	 * {@link CFGWithAnalysisResults} instance mapping each {@link Statement} to
	 * the {@link AnalysisState} computed by this method. The computation uses
	 * {@link Lattice#lub(Lattice)} to compose results obtained at different
	 * iterations, up to {@code widenAfter * predecessors_number} times, where
	 * {@code predecessors_number} is the number of expressions that are
	 * predecessors of the one being processed. After overcoming that threshold,
	 * {@link Lattice#widening(Lattice)} is used. The computation starts at the
	 * statements returned by {@link #getEntrypoints()}, using
	 * {@code entryState} as entry state for all of them. {@code cg} will be
	 * invoked to get the approximation of all invoked cfgs, while a fresh
	 * instance of {@link FIFOWorkingSet} is used as working set for the
	 * statements to process.
	 * 
	 * @param <H>        the type of {@link HeapDomain} contained into the
	 *                       computed abstract state
	 * @param <V>        the type of {@link ValueDomain} contained into the
	 *                       computed abstract state
	 * @param entryState the entry states to apply to each {@link Statement}
	 *                       returned by {@link #getEntrypoints()}
	 * @param cg         the callgraph that can be queried when a call towards
	 *                       an other cfg is encountered
	 * @param widenAfter the number of times after which the
	 *                       {@link Lattice#lub(Lattice)} invocation gets
	 *                       replaced by the {@link Lattice#widening(Lattice)}
	 *                       call. Use {@code 0} to <b>always</b> use
	 *                       {@link Lattice#lub(Lattice)}.
	 * 
	 * @return a {@link CFGWithAnalysisResults} instance that os equivalent to
	 *             this control flow graph, and that stores for each
	 *             {@link Statement} the result of the fixpoint computation
	 * 
	 * @throws FixpointException if an error occurs during the semantic
	 *                               computation of a statement, or if some
	 *                               unknown/invalid statement ends up in the
	 *                               working set
	 */
	public final <H extends HeapDomain<H>, V extends ValueDomain<V>> CFGWithAnalysisResults<H, V> fixpoint(
			AnalysisState<H, V> entryState, CallGraph cg, int widenAfter, SemanticFunction<H, V> semantics)
			throws FixpointException {
		return fixpoint(entryState, cg, FIFOWorkingSet.mk(), widenAfter, semantics);
	}

	/**
	 * Computes a fixpoint over this control flow graph. This method returns a
	 * {@link CFGWithAnalysisResults} instance mapping each {@link Statement} to
	 * the {@link AnalysisState} computed by this method. The computation uses
	 * {@link Lattice#lub(Lattice)} to compose results obtained at different
	 * iterations, up to {@link #DEFAULT_WIDENING_THRESHOLD}
	 * {@code * predecessors_number} times, where {@code predecessors_number} is
	 * the number of expressions that are predecessors of the one being
	 * processed. After overcoming that threshold,
	 * {@link Lattice#widening(Lattice)} is used. The computation starts at the
	 * statements returned by {@link #getEntrypoints()}, using
	 * {@code entryState} as entry state for all of them. {@code cg} will be
	 * invoked to get the approximation of all invoked cfgs, while {@code ws} is
	 * used as working set for the statements to process.
	 * 
	 * @param <H>        the type of {@link HeapDomain} contained into the
	 *                       computed abstract state
	 * @param <V>        the type of {@link ValueDomain} contained into the
	 *                       computed abstract state
	 * @param entryState the entry states to apply to each {@link Statement}
	 *                       returned by {@link #getEntrypoints()}
	 * @param cg         the callgraph that can be queried when a call towards
	 *                       an other cfg is encountered
	 * @param ws         the {@link WorkingSet} instance to use for this
	 *                       computation
	 * 
	 * @return a {@link CFGWithAnalysisResults} instance that os equivalent to
	 *             this control flow graph, and that stores for each
	 *             {@link Statement} the result of the fixpoint computation
	 * 
	 * @throws FixpointException if an error occurs during the semantic
	 *                               computation of a statement, or if some
	 *                               unknown/invalid statement ends up in the
	 *                               working set
	 */
	public final <H extends HeapDomain<H>, V extends ValueDomain<V>> CFGWithAnalysisResults<H, V> fixpoint(
			AnalysisState<H, V> entryState, CallGraph cg, WorkingSet<Statement> ws, SemanticFunction<H, V> semantics)
			throws FixpointException {
		return fixpoint(entryState, cg, ws, DEFAULT_WIDENING_THRESHOLD, semantics);
	}

	/**
	 * Computes a fixpoint over this control flow graph. This method returns a
	 * {@link CFGWithAnalysisResults} instance mapping each {@link Statement} to
	 * the {@link AnalysisState} computed by this method. The computation uses
	 * {@link Lattice#lub(Lattice)} to compose results obtained at different
	 * iterations, up to {@code widenAfter * predecessors_number} times, where
	 * {@code predecessors_number} is the number of expressions that are
	 * predecessors of the one being processed. After overcoming that threshold,
	 * {@link Lattice#widening(Lattice)} is used. The computation starts at the
	 * statements returned by {@link #getEntrypoints()}, using
	 * {@code entryState} as entry state for all of them. {@code cg} will be
	 * invoked to get the approximation of all invoked cfgs, while {@code ws} is
	 * used as working set for the statements to process.
	 * 
	 * @param <H>        the type of {@link HeapDomain} contained into the
	 *                       computed abstract state
	 * @param <V>        the type of {@link ValueDomain} contained into the
	 *                       computed abstract state
	 * @param entryState the entry states to apply to each {@link Statement}
	 *                       returned by {@link #getEntrypoints()}
	 * @param cg         the callgraph that can be queried when a call towards
	 *                       an other cfg is encountered
	 * @param ws         the {@link WorkingSet} instance to use for this
	 *                       computation
	 * @param widenAfter the number of times after which the
	 *                       {@link Lattice#lub(Lattice)} invocation gets
	 *                       replaced by the {@link Lattice#widening(Lattice)}
	 *                       call. Use {@code 0} to <b>always</b> use
	 *                       {@link Lattice#lub(Lattice)}.
	 * 
	 * @return a {@link CFGWithAnalysisResults} instance that os equivalent to
	 *             this control flow graph, and that stores for each
	 *             {@link Statement} the result of the fixpoint computation
	 * 
	 * @throws FixpointException if an error occurs during the semantic
	 *                               computation of a statement, or if some
	 *                               unknown/invalid statement ends up in the
	 *                               working set
	 */
	public final <H extends HeapDomain<H>, V extends ValueDomain<V>> CFGWithAnalysisResults<H, V> fixpoint(
			AnalysisState<H, V> entryState, CallGraph cg, WorkingSet<Statement> ws, int widenAfter,
			SemanticFunction<H, V> semantics)
			throws FixpointException {
		Map<Statement, AnalysisState<H, V>> start = new HashMap<>();
		entrypoints.forEach(e -> start.put(e, entryState));
		return fixpoint(start, cg, ws, widenAfter, semantics);
	}

	/**
	 * Computes a fixpoint over this control flow graph. This method returns a
	 * {@link CFGWithAnalysisResults} instance mapping each {@link Statement} to
	 * the {@link AnalysisState} computed by this method. The computation uses
	 * {@link Lattice#lub(Lattice)} to compose results obtained at different
	 * iterations, up to {@link #DEFAULT_WIDENING_THRESHOLD}
	 * {@code * predecessors_number} times, where {@code predecessors_number} is
	 * the number of expressions that are predecessors of the one being
	 * processed. After overcoming that threshold,
	 * {@link Lattice#widening(Lattice)} is used. The computation starts at the
	 * statements in {@code entrypoints}, using {@code entryState} as entry
	 * state for all of them. {@code cg} will be invoked to get the
	 * approximation of all invoked cfgs, while a fresh instance of
	 * {@link FIFOWorkingSet} is used as working set for the statements to
	 * process.
	 * 
	 * @param <H>         the type of {@link HeapDomain} contained into the
	 *                        computed abstract state
	 * @param <V>         the type of {@link ValueDomain} contained into the
	 *                        computed abstract state
	 * @param entrypoints the collection of {@link Statement}s that to use as a
	 *                        starting point of the computation (that must be
	 *                        nodes of this cfg)
	 * @param entryState  the entry states to apply to each {@link Statement} in
	 *                        {@code entrypoints}
	 * @param cg          the callgraph that can be queried when a call towards
	 *                        an other cfg is encountered
	 * 
	 * @return a {@link CFGWithAnalysisResults} instance that os equivalent to
	 *             this control flow graph, and that stores for each
	 *             {@link Statement} the result of the fixpoint computation
	 * 
	 * @throws FixpointException if an error occurs during the semantic
	 *                               computation of a statement, or if some
	 *                               unknown/invalid statement ends up in the
	 *                               working set
	 */
	public final <H extends HeapDomain<H>, V extends ValueDomain<V>> CFGWithAnalysisResults<H, V> fixpoint(
			Collection<Statement> entrypoints, AnalysisState<H, V> entryState, CallGraph cg,
			SemanticFunction<H, V> semantics) throws FixpointException {
		return fixpoint(entrypoints, entryState, cg, FIFOWorkingSet.mk(), DEFAULT_WIDENING_THRESHOLD, semantics);
	}

	/**
	 * Computes a fixpoint over this control flow graph. This method returns a
	 * {@link CFGWithAnalysisResults} instance mapping each {@link Statement} to
	 * the {@link AnalysisState} computed by this method. The computation uses
	 * {@link Lattice#lub(Lattice)} to compose results obtained at different
	 * iterations, up to {@code widenAfter * predecessors_number} times, where
	 * {@code predecessors_number} is the number of expressions that are
	 * predecessors of the one being processed. After overcoming that threshold,
	 * {@link Lattice#widening(Lattice)} is used. The computation starts at the
	 * statements in {@code entrypoints}, using {@code entryState} as entry
	 * state for all of them. {@code cg} will be invoked to get the
	 * approximation of all invoked cfgs, while a fresh instance of
	 * {@link FIFOWorkingSet} is used as working set for the statements to
	 * process.
	 * 
	 * @param <H>         the type of {@link HeapDomain} contained into the
	 *                        computed abstract state
	 * @param <V>         the type of {@link ValueDomain} contained into the
	 *                        computed abstract state
	 * @param entrypoints the collection of {@link Statement}s that to use as a
	 *                        starting point of the computation (that must be
	 *                        nodes of this cfg)
	 * @param entryState  the entry states to apply to each {@link Statement} in
	 *                        {@code entrypoints}
	 * @param cg          the callgraph that can be queried when a call towards
	 *                        an other cfg is encountered
	 * @param widenAfter  the number of times after which the
	 *                        {@link Lattice#lub(Lattice)} invocation gets
	 *                        replaced by the {@link Lattice#widening(Lattice)}
	 *                        call. Use {@code 0} to <b>always</b> use
	 *                        {@link Lattice#lub(Lattice)}.
	 * 
	 * @return a {@link CFGWithAnalysisResults} instance that os equivalent to
	 *             this control flow graph, and that stores for each
	 *             {@link Statement} the result of the fixpoint computation
	 * 
	 * @throws FixpointException if an error occurs during the semantic
	 *                               computation of a statement, or if some
	 *                               unknown/invalid statement ends up in the
	 *                               working set
	 */
	public final <H extends HeapDomain<H>, V extends ValueDomain<V>> CFGWithAnalysisResults<H, V> fixpoint(
			Collection<Statement> entrypoints, AnalysisState<H, V> entryState, CallGraph cg, int widenAfter,
			SemanticFunction<H, V> semantics)
			throws FixpointException {
		return fixpoint(entrypoints, entryState, cg, FIFOWorkingSet.mk(), widenAfter, semantics);
	}

	/**
	 * Computes a fixpoint over this control flow graph. This method returns a
	 * {@link CFGWithAnalysisResults} instance mapping each {@link Statement} to
	 * the {@link AnalysisState} computed by this method. The computation uses
	 * {@link Lattice#lub(Lattice)} to compose results obtained at different
	 * iterations, up to {@link #DEFAULT_WIDENING_THRESHOLD}
	 * {@code * predecessors_number} times, where {@code predecessors_number} is
	 * the number of expressions that are predecessors of the one being
	 * processed. After overcoming that threshold,
	 * {@link Lattice#widening(Lattice)} is used. The computation starts at the
	 * statements in {@code entrypoints}, using {@code entryState} as entry
	 * state for all of them. {@code cg} will be invoked to get the
	 * approximation of all invoked cfgs, while {@code ws} is used as working
	 * set for the statements to process.
	 * 
	 * @param <H>         the type of {@link HeapDomain} contained into the
	 *                        computed abstract state
	 * @param <V>         the type of {@link ValueDomain} contained into the
	 *                        computed abstract state
	 * @param entrypoints the collection of {@link Statement}s that to use as a
	 *                        starting point of the computation (that must be
	 *                        nodes of this cfg)
	 * @param entryState  the entry states to apply to each {@link Statement} in
	 *                        {@code entrypoints}
	 * @param cg          the callgraph that can be queried when a call towards
	 *                        an other cfg is encountered
	 * @param ws          the {@link WorkingSet} instance to use for this
	 *                        computation
	 * 
	 * @return a {@link CFGWithAnalysisResults} instance that os equivalent to
	 *             this control flow graph, and that stores for each
	 *             {@link Statement} the result of the fixpoint computation
	 * 
	 * @throws FixpointException if an error occurs during the semantic
	 *                               computation of a statement, or if some
	 *                               unknown/invalid statement ends up in the
	 *                               working set
	 */
	public final <H extends HeapDomain<H>, V extends ValueDomain<V>> CFGWithAnalysisResults<H, V> fixpoint(
			Collection<Statement> entrypoints, AnalysisState<H, V> entryState, CallGraph cg, WorkingSet<Statement> ws,
			SemanticFunction<H, V> semantics)
			throws FixpointException {
		return fixpoint(entrypoints, entryState, cg, ws, DEFAULT_WIDENING_THRESHOLD, semantics);
	}

	/**
	 * Computes a fixpoint over this control flow graph. This method returns a
	 * {@link CFGWithAnalysisResults} instance mapping each {@link Statement} to
	 * the {@link AnalysisState} computed by this method. The computation uses
	 * {@link Lattice#lub(Lattice)} to compose results obtained at different
	 * iterations, up to {@code widenAfter * predecessors_number} times, where
	 * {@code predecessors_number} is the number of expressions that are
	 * predecessors of the one being processed. After overcoming that threshold,
	 * {@link Lattice#widening(Lattice)} is used. The computation starts at the
	 * statements in {@code entrypoints}, using {@code entryState} as entry
	 * state for all of them. {@code cg} will be invoked to get the
	 * approximation of all invoked cfgs, while {@code ws} is used as working
	 * set for the statements to process.
	 * 
	 * @param <H>         the type of {@link HeapDomain} contained into the
	 *                        computed abstract state
	 * @param <V>         the type of {@link ValueDomain} contained into the
	 *                        computed abstract state
	 * @param entrypoints the collection of {@link Statement}s that to use as a
	 *                        starting point of the computation (that must be
	 *                        nodes of this cfg)
	 * @param entryState  the entry states to apply to each {@link Statement} in
	 *                        {@code entrypoints}
	 * @param cg          the callgraph that can be queried when a call towards
	 *                        an other cfg is encountered
	 * @param ws          the {@link WorkingSet} instance to use for this
	 *                        computation
	 * @param widenAfter  the number of times after which the
	 *                        {@link Lattice#lub(Lattice)} invocation gets
	 *                        replaced by the {@link Lattice#widening(Lattice)}
	 *                        call. Use {@code 0} to <b>always</b> use
	 *                        {@link Lattice#lub(Lattice)}.
	 * 
	 * @return a {@link CFGWithAnalysisResults} instance that os equivalent to
	 *             this control flow graph, and that stores for each
	 *             {@link Statement} the result of the fixpoint computation
	 * 
	 * @throws FixpointException if an error occurs during the semantic
	 *                               computation of a statement, or if some
	 *                               unknown/invalid statement ends up in the
	 *                               working set
	 */
	public final <H extends HeapDomain<H>, V extends ValueDomain<V>> CFGWithAnalysisResults<H, V> fixpoint(
			Collection<Statement> entrypoints, AnalysisState<H, V> entryState, CallGraph cg, WorkingSet<Statement> ws,
			int widenAfter, SemanticFunction<H, V> semantics) throws FixpointException {
		Map<Statement, AnalysisState<H, V>> start = new HashMap<>();
		entrypoints.forEach(e -> start.put(e, entryState));
		return fixpoint(start, cg, ws, widenAfter, semantics);
	}

	/**
	 * Computes a fixpoint over this control flow graph. This method returns a
	 * {@link CFGWithAnalysisResults} instance mapping each {@link Statement} to
	 * the {@link AnalysisState} computed by this method. The computation uses
	 * {@link Lattice#lub(Lattice)} to compose results obtained at different
	 * iterations, up to {@link #DEFAULT_WIDENING_THRESHOLD}
	 * {@code * predecessors_number} times, where {@code predecessors_number} is
	 * the number of expressions that are predecessors of the one being
	 * processed. After overcoming that threshold,
	 * {@link Lattice#widening(Lattice)} is used. The computation starts at the
	 * statements in {@code startingPoints}, using as its entry state their
	 * respective value. {@code cg} will be invoked to get the approximation of
	 * all invoked cfgs, while a fresh instance of {@link FIFOWorkingSet} is
	 * used as working set for the statements to process.
	 * 
	 * @param <H>            the type of {@link HeapDomain} contained into the
	 *                           computed abstract state
	 * @param <V>            the type of {@link ValueDomain} contained into the
	 *                           computed abstract state
	 * @param startingPoints a map between {@link Statement}s that to use as a
	 *                           starting point of the computation (that must be
	 *                           nodes of this cfg) and the entry states to
	 *                           apply on it
	 * @param cg             the callgraph that can be queried when a call
	 *                           towards an other cfg is encountered
	 * 
	 * @return a {@link CFGWithAnalysisResults} instance that os equivalent to
	 *             this control flow graph, and that stores for each
	 *             {@link Statement} the result of the fixpoint computation
	 * 
	 * @throws FixpointException if an error occurs during the semantic
	 *                               computation of a statement, or if some
	 *                               unknown/invalid statement ends up in the
	 *                               working set
	 */
	public final <H extends HeapDomain<H>, V extends ValueDomain<V>> CFGWithAnalysisResults<H, V> fixpoint(
			Map<Statement, AnalysisState<H, V>> startingPoints, CallGraph cg, SemanticFunction<H, V> semantics)
			throws FixpointException {
		return fixpoint(startingPoints, cg, FIFOWorkingSet.mk(), DEFAULT_WIDENING_THRESHOLD, semantics);
	}

	/**
	 * Computes a fixpoint over this control flow graph. This method returns a
	 * {@link CFGWithAnalysisResults} instance mapping each {@link Statement} to
	 * the {@link AnalysisState} computed by this method. The computation uses
	 * {@link Lattice#lub(Lattice)} to compose results obtained at different
	 * iterations, up to {@code widenAfter * predecessors_number} times, where
	 * {@code predecessors_number} is the number of expressions that are
	 * predecessors of the one being processed. After overcoming that threshold,
	 * {@link Lattice#widening(Lattice)} is used. The computation starts at the
	 * statements in {@code startingPoints}, using as its entry state their
	 * respective value. {@code cg} will be invoked to get the approximation of
	 * all invoked cfgs, while a fresh instance of {@link FIFOWorkingSet} is
	 * used as working set for the statements to process.
	 * 
	 * @param <H>            the type of {@link HeapDomain} contained into the
	 *                           computed abstract state
	 * @param <V>            the type of {@link ValueDomain} contained into the
	 *                           computed abstract state
	 * @param startingPoints a map between {@link Expression}s that to use as a
	 *                           starting point of the computation (that must be
	 *                           nodes of this cfg) and the entry states to
	 *                           apply on it
	 * @param cg             the callgraph that can be queried when a call
	 *                           towards an other cfg is encountered
	 * @param widenAfter     the number of times after which the
	 *                           {@link Lattice#lub(Lattice)} invocation gets
	 *                           replaced by the
	 *                           {@link Lattice#widening(Lattice)} call. Use
	 *                           {@code 0} to <b>always</b> use
	 *                           {@link Lattice#lub(Lattice)}.
	 * 
	 * @return a {@link CFGWithAnalysisResults} instance that os equivalent to
	 *             this control flow graph, and that stores for each
	 *             {@link Expression} the result of the fixpoint computation
	 * 
	 * @throws FixpointException if an error occurs during the semantic
	 *                               computation of an statement, or if some
	 *                               unknown/invalid statement ends up in the
	 *                               working set
	 */
	public final <H extends HeapDomain<H>, V extends ValueDomain<V>> CFGWithAnalysisResults<H, V> fixpoint(
			Map<Statement, AnalysisState<H, V>> startingPoints, CallGraph cg, int widenAfter,
			SemanticFunction<H, V> semantics) throws FixpointException {
		return fixpoint(startingPoints, cg, FIFOWorkingSet.mk(), widenAfter, semantics);
	}

	/**
	 * Computes a fixpoint over this control flow graph. This method returns a
	 * {@link CFGWithAnalysisResults} instance mapping each {@link Statement} to
	 * the {@link AnalysisState} computed by this method. The computation uses
	 * {@link Lattice#lub(Lattice)} to compose results obtained at different
	 * iterations, up to {@link #DEFAULT_WIDENING_THRESHOLD}
	 * {@code * predecessors_number} times, where {@code predecessors_number} is
	 * the number of expressions that are predecessors of the one being
	 * processed. After overcoming that threshold,
	 * {@link Lattice#widening(Lattice)} is used. The computation starts at the
	 * statements in {@code startingPoints}, using as its entry state their
	 * respective value. {@code cg} will be invoked to get the approximation of
	 * all invoked cfgs, while {@code ws} is used as working set for the
	 * statements to process.
	 * 
	 * @param <H>            the type of {@link HeapDomain} contained into the
	 *                           computed abstract state
	 * @param <V>            the type of {@link ValueDomain} contained into the
	 *                           computed abstract state
	 * @param startingPoints a map between {@link Expression}s that to use as a
	 *                           starting point of the computation (that must be
	 *                           nodes of this cfg) and the entry states to
	 *                           apply on it
	 * @param cg             the callgraph that can be queried when a call
	 *                           towards an other cfg is encountered
	 * @param ws             the {@link WorkingSet} instance to use for this
	 *                           computation
	 * 
	 * @return a {@link CFGWithAnalysisResults} instance that os equivalent to
	 *             this control flow graph, and that stores for each
	 *             {@link Expression} the result of the fixpoint computation
	 * 
	 * @throws FixpointException if an error occurs during the semantic
	 *                               computation of an statement, or if some
	 *                               unknown/invalid statement ends up in the
	 *                               working set
	 */
	public final <H extends HeapDomain<H>, V extends ValueDomain<V>> CFGWithAnalysisResults<H, V> fixpoint(
			Map<Statement, AnalysisState<H, V>> startingPoints, CallGraph cg, WorkingSet<Statement> ws,
			SemanticFunction<H, V> semantics)
			throws FixpointException {
		return fixpoint(startingPoints, cg, ws, DEFAULT_WIDENING_THRESHOLD, semantics);
	}

	@FunctionalInterface
	public interface SemanticFunction<H extends HeapDomain<H>, V extends ValueDomain<V>> {
		AnalysisState<H, V> compute(Statement st, AnalysisState<H, V> entryState, CallGraph callGraph,
				ExpressionStore<AnalysisState<H, V>> expressions) throws SemanticException;
	}

	/**
	 * Computes a fixpoint over this control flow graph. This method returns a
	 * {@link CFGWithAnalysisResults} instance mapping each {@link Statement} to
	 * the {@link AnalysisState} computed by this method. The computation uses
	 * {@link Lattice#lub(Lattice)} to compose results obtained at different
	 * iterations, up to {@code widenAfter * predecessors_number} times, where
	 * {@code predecessors_number} is the number of expressions that are
	 * predecessors of the one being processed. After overcoming that threshold,
	 * {@link Lattice#widening(Lattice)} is used. The computation starts at the
	 * statements in {@code startingPoints}, using as its entry state their
	 * respective value. {@code cg} will be invoked to get the approximation of
	 * all invoked cfgs, while {@code ws} is used as working set for the
	 * statements to process.
	 * 
	 * @param <H>            the type of {@link HeapDomain} contained into the
	 *                           computed abstract state
	 * @param <V>            the type of {@link ValueDomain} contained into the
	 *                           computed abstract state
	 * @param startingPoints a map between {@link Statement}s that to use as a
	 *                           starting point of the computation (that must be
	 *                           nodes of this cfg) and the entry states to
	 *                           apply on it
	 * @param cg             the callgraph that can be queried when a call
	 *                           towards an other cfg is encountered
	 * @param ws             the {@link WorkingSet} instance to use for this
	 *                           computation
	 * @param widenAfter     the number of times after which the
	 *                           {@link Lattice#lub(Lattice)} invocation gets
	 *                           replaced by the
	 *                           {@link Lattice#widening(Lattice)} call. Use
	 *                           {@code 0} to <b>always</b> use
	 *                           {@link Lattice#lub(Lattice)}.
	 * 
	 * @return a {@link CFGWithAnalysisResults} instance that os equivalent to
	 *             this control flow graph, and that stores for each
	 *             {@link Statement} the result of the fixpoint computation
	 * 
	 * @throws FixpointException if an error occurs during the semantic
	 *                               computation of a statement, or if some
	 *                               unknown/invalid statement ends up in the
	 *                               working set
	 */
	public final <H extends HeapDomain<H>, V extends ValueDomain<V>> CFGWithAnalysisResults<H, V> fixpoint(
			Map<Statement, AnalysisState<H, V>> startingPoints, CallGraph cg, WorkingSet<Statement> ws, int widenAfter,
			SemanticFunction<H, V> semantics)
			throws FixpointException {
		int size = adjacencyMatrix.getNodes().size();
		Map<Statement, AtomicInteger> lubs = new HashMap<>(size);
		Map<Statement, Pair<AnalysisState<H, V>, ExpressionStore<AnalysisState<H, V>>>> result = new HashMap<>(size);
		startingPoints.keySet().forEach(ws::push);

		AnalysisState<H, V> oldApprox = null, newApprox;
		ExpressionStore<AnalysisState<H, V>> oldExprs = null, newExprs;
		try {
			while (!ws.isEmpty()) {
				Statement current = ws.pop();

				if (current == null)
					throw new FixpointException(
							"Unknown instruction encountered during fixpoint execution in '" + descriptor + "'");
				if (!adjacencyMatrix.getNodes().contains(current))
					throw new FixpointException("'" + current
							+ "' is not part of this control flow graph, and cannot be analyzed in this fixpoint computation");

				AnalysisState<H, V> entrystate;
				try {
					entrystate = getEntryState(current, startingPoints, result);
				} catch (SemanticException e) {
					throw new FixpointException(
							"Exception while computing the entry state for '" + current + "' in " + descriptor, e);
				}

				if (entrystate == null)
					throw new FixpointException(current + " does not have an entry state");

				if (result.containsKey(current)) {
					oldApprox = result.get(current).getLeft();
					oldExprs = result.get(current).getRight();
				} else {
					oldApprox = null;
					oldExprs = null;
				}

				try {
					newExprs = new ExpressionStore<>(entrystate);
					newApprox = semantics.compute(current, entrystate, cg, newExprs);
				} catch (SemanticException e) {
					log.error("Evaluation of the semantics of '" + current + "' in " + descriptor
							+ " led to an exception: " + e);
					throw new FixpointException("Semantic exception during fixpoint computation", e);
				}

				if (oldApprox != null && oldExprs != null)
					try {
						if (widenAfter == 0) {
							newApprox = newApprox.lub(oldApprox);
							newExprs = newExprs.lub(oldExprs);
						} else {
							// we multiply by the number of predecessors since
							// if we have more than one
							// the threshold will be reached faster
							int lub = lubs
									.computeIfAbsent(current,
											e -> new AtomicInteger(widenAfter * predecessorsOf(e).size()))
									.getAndDecrement();
							if (lub > 0) {
								newApprox = newApprox.lub(oldApprox);
								newExprs = newExprs.lub(oldExprs);
							} else {
								newApprox = newApprox.widening(oldApprox);
								newExprs = newExprs.widening(oldExprs);
							}
						}
					} catch (SemanticException e) {
						throw new FixpointException(
								"Exception while updating the analysis results of '" + current + "' in " + descriptor,
								e);
					}

				if ((oldApprox == null && oldExprs == null) || !newApprox.lessOrEqual(oldApprox)
						|| !newExprs.lessOrEqual(oldExprs)) {
					result.put(current, Pair.of(newApprox, newExprs));
					for (Statement instr : followersOf(current))
						ws.push(instr);
				}
			}

			HashMap<Statement, AnalysisState<H, V>> finalResults = new HashMap<>(result.size());
			for (Entry<Statement, Pair<AnalysisState<H, V>, ExpressionStore<AnalysisState<H, V>>>> e : result
					.entrySet()) {
				finalResults.put(e.getKey(), e.getValue().getLeft());
				for (Entry<Expression, AnalysisState<H, V>> ee : e.getValue().getRight())
					finalResults.put(ee.getKey(), ee.getValue());
			}

			return new CFGWithAnalysisResults<>(this, finalResults);
		} catch (Exception e) {
			log.fatal("Unexpected exception during fixpoint computation of '" + descriptor + "': " + e);
			throw new FixpointException("Unexpected exception during fixpoint computation", e);
		}
	}

	private <H extends HeapDomain<H>, V extends ValueDomain<V>> AnalysisState<H, V> getEntryState(Statement current,
			Map<Statement, AnalysisState<H, V>> startingPoints,
			Map<Statement, Pair<AnalysisState<H, V>, ExpressionStore<AnalysisState<H, V>>>> result)
			throws SemanticException {
		AnalysisState<H, V> entrystate = startingPoints.get(current);
		Collection<Statement> preds = predecessorsOf(current);
		List<AnalysisState<H, V>> states = new ArrayList<>(preds.size());

		for (Statement pred : preds)
			if (result.containsKey(pred)) {
				// this might not have been computed yet
				Edge edge = adjacencyMatrix.getEdgeConnecting(pred, current);
				states.add(edge.traverse(result.get(edge.getSource()).getLeft()));
			}

		for (AnalysisState<H, V> s : states)
			if (entrystate == null)
				entrystate = s;
			else
				entrystate = entrystate.lub(s);

		return entrystate;
	}
}
