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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.CFGWithAnalysisResults;
import it.unive.lisa.analysis.CallGraph;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.cfg.edge.Edge;
import it.unive.lisa.cfg.edge.FalseEdge;
import it.unive.lisa.cfg.edge.SequentialEdge;
import it.unive.lisa.cfg.edge.TrueEdge;
import it.unive.lisa.cfg.statement.Expression;
import it.unive.lisa.cfg.statement.NoOp;
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

	/**
	 * Computes a fixpoint over this control flow graph. This method returns a
	 * {@link CFGWithAnalysisResults} instance mapping each {@link Statement} to the
	 * {@link AnalysisState} computed by this method. The computation uses
	 * {@link Lattice#lub(Lattice)} to compose results obtained at different
	 * iterations, up to {@link #DEFAULT_WIDENING_THRESHOLD}
	 * {@code * predecessors_number} times, where {@code predecessors_number} is the
	 * number of expressions that are predecessors of the one being processed. After
	 * overcoming that threshold, {@link Lattice#widening(Lattice)} is used. The
	 * computation starts at the statements returned by {@link #getEntrypoints()},
	 * using {@code entryState} as entry state for all of them. {@code cg} will be
	 * invoked to get the approximation of all invoked cfgs, while a fresh instance
	 * of {@link FIFOWorkingSet} is used as working set for the statements to
	 * process.
	 * 
	 * @param <H>        the type of {@link HeapDomain} contained into the computed
	 *                   abstract state
	 * @param <V>        the type of {@link ValueDomain} contained into the computed
	 *                   abstract state
	 * @param entryState the entry states to apply to each {@link Statement}
	 *                   returned by {@link #getEntrypoints()}
	 * @param cg         the callgraph that can be queried when a call towards an
	 *                   other cfg is encountered
	 * @return a {@link CFGWithAnalysisResults} instance that os equivalent to this
	 *         control flow graph, and that stores for each {@link Statement} the
	 *         result of the fixpoint computation
	 * @throws FixpointException if an error occurs during the semantic computation
	 *                           of a statement, or if some unknown/invalid
	 *                           statement ends up in the working set
	 */
	public final <H extends HeapDomain<H>, V extends ValueDomain<V>> CFGWithAnalysisResults<H, V> fixpoint(
			AnalysisState<H, V> entryState, CallGraph cg) {
		return fixpoint(entryState, cg, FIFOWorkingSet.mk(), DEFAULT_WIDENING_THRESHOLD);
	}

	/**
	 * Computes a fixpoint over this control flow graph. This method returns a
	 * {@link CFGWithAnalysisResults} instance mapping each {@link Statement} to the
	 * {@link AnalysisState} computed by this method. The computation uses
	 * {@link Lattice#lub(Lattice)} to compose results obtained at different
	 * iterations, up to {@code widenAfter * predecessors_number} times, where
	 * {@code predecessors_number} is the number of expressions that are
	 * predecessors of the one being processed. After overcoming that threshold,
	 * {@link Lattice#widening(Lattice)} is used. The computation starts at the
	 * statements returned by {@link #getEntrypoints()}, using {@code entryState} as
	 * entry state for all of them. {@code cg} will be invoked to get the
	 * approximation of all invoked cfgs, while a fresh instance of
	 * {@link FIFOWorkingSet} is used as working set for the statements to process.
	 * 
	 * @param <H>        the type of {@link HeapDomain} contained into the computed
	 *                   abstract state
	 * @param <V>        the type of {@link ValueDomain} contained into the computed
	 *                   abstract state
	 * @param entryState the entry states to apply to each {@link Statement}
	 *                   returned by {@link #getEntrypoints()}
	 * @param cg         the callgraph that can be queried when a call towards an
	 *                   other cfg is encountered
	 * @param widenAfter the number of times after which the
	 *                   {@link Lattice#lub(Lattice)} invocation gets replaced by
	 *                   the {@link Lattice#widening(Lattice)} call. Use {@code 0}
	 *                   to <b>always</b> use {@link Lattice#lub(Lattice)}.
	 * @return a {@link CFGWithAnalysisResults} instance that os equivalent to this
	 *         control flow graph, and that stores for each {@link Statement} the
	 *         result of the fixpoint computation
	 * @throws FixpointException if an error occurs during the semantic computation
	 *                           of a statement, or if some unknown/invalid
	 *                           statement ends up in the working set
	 */
	public final <H extends HeapDomain<H>, V extends ValueDomain<V>> CFGWithAnalysisResults<H, V> fixpoint(
			AnalysisState<H, V> entryState, CallGraph cg, int widenAfter) {
		return fixpoint(entryState, cg, FIFOWorkingSet.mk(), widenAfter);
	}

	/**
	 * Computes a fixpoint over this control flow graph. This method returns a
	 * {@link CFGWithAnalysisResults} instance mapping each {@link Statement} to the
	 * {@link AnalysisState} computed by this method. The computation uses
	 * {@link Lattice#lub(Lattice)} to compose results obtained at different
	 * iterations, up to {@link #DEFAULT_WIDENING_THRESHOLD}
	 * {@code * predecessors_number} times, where {@code predecessors_number} is the
	 * number of expressions that are predecessors of the one being processed. After
	 * overcoming that threshold, {@link Lattice#widening(Lattice)} is used. The
	 * computation starts at the statements returned by {@link #getEntrypoints()},
	 * using {@code entryState} as entry state for all of them. {@code cg} will be
	 * invoked to get the approximation of all invoked cfgs, while {@code ws} is
	 * used as working set for the statements to process.
	 * 
	 * @param <H>        the type of {@link HeapDomain} contained into the computed
	 *                   abstract state
	 * @param <V>        the type of {@link ValueDomain} contained into the computed
	 *                   abstract state
	 * @param entryState the entry states to apply to each {@link Statement}
	 *                   returned by {@link #getEntrypoints()}
	 * @param cg         the callgraph that can be queried when a call towards an
	 *                   other cfg is encountered
	 * @param ws         the {@link WorkingSet} instance to use for this computation
	 * @return a {@link CFGWithAnalysisResults} instance that os equivalent to this
	 *         control flow graph, and that stores for each {@link Statement} the
	 *         result of the fixpoint computation
	 * @throws FixpointException if an error occurs during the semantic computation
	 *                           of a statement, or if some unknown/invalid
	 *                           statement ends up in the working set
	 */
	public final <H extends HeapDomain<H>, V extends ValueDomain<V>> CFGWithAnalysisResults<H, V> fixpoint(
			AnalysisState<H, V> entryState, CallGraph cg, WorkingSet<Statement> ws) {
		return fixpoint(entryState, cg, ws, DEFAULT_WIDENING_THRESHOLD);
	}

	/**
	 * Computes a fixpoint over this control flow graph. This method returns a
	 * {@link CFGWithAnalysisResults} instance mapping each {@link Statement} to the
	 * {@link AnalysisState} computed by this method. The computation uses
	 * {@link Lattice#lub(Lattice)} to compose results obtained at different
	 * iterations, up to {@code widenAfter * predecessors_number} times, where
	 * {@code predecessors_number} is the number of expressions that are
	 * predecessors of the one being processed. After overcoming that threshold,
	 * {@link Lattice#widening(Lattice)} is used. The computation starts at the
	 * statements returned by {@link #getEntrypoints()}, using {@code entryState} as
	 * entry state for all of them. {@code cg} will be invoked to get the
	 * approximation of all invoked cfgs, while {@code ws} is used as working set
	 * for the statements to process.
	 * 
	 * @param <H>        the type of {@link HeapDomain} contained into the computed
	 *                   abstract state
	 * @param <V>        the type of {@link ValueDomain} contained into the computed
	 *                   abstract state
	 * @param entryState the entry states to apply to each {@link Statement}
	 *                   returned by {@link #getEntrypoints()}
	 * @param cg         the callgraph that can be queried when a call towards an
	 *                   other cfg is encountered
	 * @param ws         the {@link WorkingSet} instance to use for this computation
	 * @param widenAfter the number of times after which the
	 *                   {@link Lattice#lub(Lattice)} invocation gets replaced by
	 *                   the {@link Lattice#widening(Lattice)} call. Use {@code 0}
	 *                   to <b>always</b> use {@link Lattice#lub(Lattice)}.
	 * @return a {@link CFGWithAnalysisResults} instance that os equivalent to this
	 *         control flow graph, and that stores for each {@link Statement} the
	 *         result of the fixpoint computation
	 * @throws FixpointException if an error occurs during the semantic computation
	 *                           of a statement, or if some unknown/invalid
	 *                           statement ends up in the working set
	 */
	public final <H extends HeapDomain<H>, V extends ValueDomain<V>> CFGWithAnalysisResults<H, V> fixpoint(
			AnalysisState<H, V> entryState, CallGraph cg, WorkingSet<Statement> ws, int widenAfter) {
		Map<Statement, AnalysisState<H, V>> start = new HashMap<>();
		entrypoints.forEach(e -> start.put(e, entryState));
		return fixpoint(start, cg, ws, widenAfter);
	}

	/**
	 * Computes a fixpoint over this control flow graph. This method returns a
	 * {@link CFGWithAnalysisResults} instance mapping each {@link Statement} to the
	 * {@link AnalysisState} computed by this method. The computation uses
	 * {@link Lattice#lub(Lattice)} to compose results obtained at different
	 * iterations, up to {@link #DEFAULT_WIDENING_THRESHOLD}
	 * {@code * predecessors_number} times, where {@code predecessors_number} is the
	 * number of expressions that are predecessors of the one being processed. After
	 * overcoming that threshold, {@link Lattice#widening(Lattice)} is used. The
	 * computation starts at the statements in {@code entrypoints}, using
	 * {@code entryState} as entry state for all of them. {@code cg} will be invoked
	 * to get the approximation of all invoked cfgs, while a fresh instance of
	 * {@link FIFOWorkingSet} is used as working set for the statements to process.
	 * 
	 * @param <H>         the type of {@link HeapDomain} contained into the computed
	 *                    abstract state
	 * @param <V>         the type of {@link ValueDomain} contained into the
	 *                    computed abstract state
	 * @param entrypoints the collection of {@link Statement}s that to use as a
	 *                    starting point of the computation (that must be nodes of
	 *                    this cfg)
	 * @param entryState  the entry states to apply to each {@link Statement} in
	 *                    {@code entrypoints}
	 * @param cg          the callgraph that can be queried when a call towards an
	 *                    other cfg is encountered
	 * @return a {@link CFGWithAnalysisResults} instance that os equivalent to this
	 *         control flow graph, and that stores for each {@link Statement} the
	 *         result of the fixpoint computation
	 * @throws FixpointException if an error occurs during the semantic computation
	 *                           of a statement, or if some unknown/invalid
	 *                           statement ends up in the working set
	 */
	public final <H extends HeapDomain<H>, V extends ValueDomain<V>> CFGWithAnalysisResults<H, V> fixpoint(
			Collection<Statement> entrypoints, AnalysisState<H, V> entryState, CallGraph cg) {
		return fixpoint(entrypoints, entryState, cg, FIFOWorkingSet.mk(), DEFAULT_WIDENING_THRESHOLD);
	}

	/**
	 * Computes a fixpoint over this control flow graph. This method returns a
	 * {@link CFGWithAnalysisResults} instance mapping each {@link Statement} to the
	 * {@link AnalysisState} computed by this method. The computation uses
	 * {@link Lattice#lub(Lattice)} to compose results obtained at different
	 * iterations, up to {@code widenAfter * predecessors_number} times, where
	 * {@code predecessors_number} is the number of expressions that are
	 * predecessors of the one being processed. After overcoming that threshold,
	 * {@link Lattice#widening(Lattice)} is used. The computation starts at the
	 * statements in {@code entrypoints}, using {@code entryState} as entry state
	 * for all of them. {@code cg} will be invoked to get the approximation of all
	 * invoked cfgs, while a fresh instance of {@link FIFOWorkingSet} is used as
	 * working set for the statements to process.
	 * 
	 * @param <H>         the type of {@link HeapDomain} contained into the computed
	 *                    abstract state
	 * @param <V>         the type of {@link ValueDomain} contained into the
	 *                    computed abstract state
	 * @param entrypoints the collection of {@link Statement}s that to use as a
	 *                    starting point of the computation (that must be nodes of
	 *                    this cfg)
	 * @param entryState  the entry states to apply to each {@link Statement} in
	 *                    {@code entrypoints}
	 * @param cg          the callgraph that can be queried when a call towards an
	 *                    other cfg is encountered
	 * @param widenAfter  the number of times after which the
	 *                    {@link Lattice#lub(Lattice)} invocation gets replaced by
	 *                    the {@link Lattice#widening(Lattice)} call. Use {@code 0}
	 *                    to <b>always</b> use {@link Lattice#lub(Lattice)}.
	 * @return a {@link CFGWithAnalysisResults} instance that os equivalent to this
	 *         control flow graph, and that stores for each {@link Statement} the
	 *         result of the fixpoint computation
	 * @throws FixpointException if an error occurs during the semantic computation
	 *                           of a statement, or if some unknown/invalid
	 *                           statement ends up in the working set
	 */
	public final <H extends HeapDomain<H>, V extends ValueDomain<V>> CFGWithAnalysisResults<H, V> fixpoint(
			Collection<Statement> entrypoints, AnalysisState<H, V> entryState, CallGraph cg, int widenAfter) {
		return fixpoint(entrypoints, entryState, cg, FIFOWorkingSet.mk(), widenAfter);
	}

	/**
	 * Computes a fixpoint over this control flow graph. This method returns a
	 * {@link CFGWithAnalysisResults} instance mapping each {@link Statement} to the
	 * {@link AnalysisState} computed by this method. The computation uses
	 * {@link Lattice#lub(Lattice)} to compose results obtained at different
	 * iterations, up to {@link #DEFAULT_WIDENING_THRESHOLD}
	 * {@code * predecessors_number} times, where {@code predecessors_number} is the
	 * number of expressions that are predecessors of the one being processed. After
	 * overcoming that threshold, {@link Lattice#widening(Lattice)} is used. The
	 * computation starts at the statements in {@code entrypoints}, using
	 * {@code entryState} as entry state for all of them. {@code cg} will be invoked
	 * to get the approximation of all invoked cfgs, while {@code ws} is used as
	 * working set for the statements to process.
	 * 
	 * @param <H>         the type of {@link HeapDomain} contained into the computed
	 *                    abstract state
	 * @param <V>         the type of {@link ValueDomain} contained into the
	 *                    computed abstract state
	 * @param entrypoints the collection of {@link Statement}s that to use as a
	 *                    starting point of the computation (that must be nodes of
	 *                    this cfg)
	 * @param entryState  the entry states to apply to each {@link Statement} in
	 *                    {@code entrypoints}
	 * @param cg          the callgraph that can be queried when a call towards an
	 *                    other cfg is encountered
	 * @param ws          the {@link WorkingSet} instance to use for this
	 *                    computation
	 * @return a {@link CFGWithAnalysisResults} instance that os equivalent to this
	 *         control flow graph, and that stores for each {@link Statement} the
	 *         result of the fixpoint computation
	 * @throws FixpointException if an error occurs during the semantic computation
	 *                           of a statement, or if some unknown/invalid
	 *                           statement ends up in the working set
	 */
	public final <H extends HeapDomain<H>, V extends ValueDomain<V>> CFGWithAnalysisResults<H, V> fixpoint(
			Collection<Statement> entrypoints, AnalysisState<H, V> entryState, CallGraph cg, WorkingSet<Statement> ws) {
		return fixpoint(entrypoints, entryState, cg, ws, DEFAULT_WIDENING_THRESHOLD);
	}

	/**
	 * Computes a fixpoint over this control flow graph. This method returns a
	 * {@link CFGWithAnalysisResults} instance mapping each {@link Statement} to the
	 * {@link AnalysisState} computed by this method. The computation uses
	 * {@link Lattice#lub(Lattice)} to compose results obtained at different
	 * iterations, up to {@code widenAfter * predecessors_number} times, where
	 * {@code predecessors_number} is the number of expressions that are
	 * predecessors of the one being processed. After overcoming that threshold,
	 * {@link Lattice#widening(Lattice)} is used. The computation starts at the
	 * statements in {@code entrypoints}, using {@code entryState} as entry state
	 * for all of them. {@code cg} will be invoked to get the approximation of all
	 * invoked cfgs, while {@code ws} is used as working set for the statements to
	 * process.
	 * 
	 * @param <H>         the type of {@link HeapDomain} contained into the computed
	 *                    abstract state
	 * @param <V>         the type of {@link ValueDomain} contained into the
	 *                    computed abstract state
	 * @param entrypoints the collection of {@link Statement}s that to use as a
	 *                    starting point of the computation (that must be nodes of
	 *                    this cfg)
	 * @param entryState  the entry states to apply to each {@link Statement} in
	 *                    {@code entrypoints}
	 * @param cg          the callgraph that can be queried when a call towards an
	 *                    other cfg is encountered
	 * @param ws          the {@link WorkingSet} instance to use for this
	 *                    computation
	 * @param widenAfter  the number of times after which the
	 *                    {@link Lattice#lub(Lattice)} invocation gets replaced by
	 *                    the {@link Lattice#widening(Lattice)} call. Use {@code 0}
	 *                    to <b>always</b> use {@link Lattice#lub(Lattice)}.
	 * @return a {@link CFGWithAnalysisResults} instance that os equivalent to this
	 *         control flow graph, and that stores for each {@link Statement} the
	 *         result of the fixpoint computation
	 * @throws FixpointException if an error occurs during the semantic computation
	 *                           of a statement, or if some unknown/invalid
	 *                           statement ends up in the working set
	 */
	public final <H extends HeapDomain<H>, V extends ValueDomain<V>> CFGWithAnalysisResults<H, V> fixpoint(
			Collection<Statement> entrypoints, AnalysisState<H, V> entryState, CallGraph cg, WorkingSet<Statement> ws,
			int widenAfter) {
		Map<Statement, AnalysisState<H, V>> start = new HashMap<>();
		entrypoints.forEach(e -> start.put(e, entryState));
		return fixpoint(start, cg, ws, widenAfter);
	}

	/**
	 * Computes a fixpoint over this control flow graph. This method returns a
	 * {@link CFGWithAnalysisResults} instance mapping each {@link Statement} to the
	 * {@link AnalysisState} computed by this method. The computation uses
	 * {@link Lattice#lub(Lattice)} to compose results obtained at different
	 * iterations, up to {@link #DEFAULT_WIDENING_THRESHOLD}
	 * {@code * predecessors_number} times, where {@code predecessors_number} is the
	 * number of expressions that are predecessors of the one being processed. After
	 * overcoming that threshold, {@link Lattice#widening(Lattice)} is used. The
	 * computation starts at the statements in {@code startingPoints}, using as its
	 * entry state their respective value. {@code cg} will be invoked to get the
	 * approximation of all invoked cfgs, while a fresh instance of
	 * {@link FIFOWorkingSet} is used as working set for the statements to process.
	 * 
	 * @param <H>            the type of {@link HeapDomain} contained into the
	 *                       computed abstract state
	 * @param <V>            the type of {@link ValueDomain} contained into the
	 *                       computed abstract state
	 * @param startingPoints a map between {@link Statement}s that to use as a
	 *                       starting point of the computation (that must be nodes
	 *                       of this cfg) and the entry states to apply on it
	 * @param cg             the callgraph that can be queried when a call towards
	 *                       an other cfg is encountered
	 * @return a {@link CFGWithAnalysisResults} instance that os equivalent to this
	 *         control flow graph, and that stores for each {@link Statement} the
	 *         result of the fixpoint computation
	 * @throws FixpointException if an error occurs during the semantic computation
	 *                           of a statement, or if some unknown/invalid
	 *                           statement ends up in the working set
	 */
	public final <H extends HeapDomain<H>, V extends ValueDomain<V>> CFGWithAnalysisResults<H, V> computeFixpoint(
			Map<Statement, AnalysisState<H, V>> startingPoints, CallGraph cg) {
		return fixpoint(startingPoints, cg, FIFOWorkingSet.mk(), DEFAULT_WIDENING_THRESHOLD);
	}

	/**
	 * Computes a fixpoint over this control flow graph. This method returns a
	 * {@link CFGWithAnalysisResults} instance mapping each {@link Statement} to the
	 * {@link AnalysisState} computed by this method. The computation uses
	 * {@link Lattice#lub(Lattice)} to compose results obtained at different
	 * iterations, up to {@code widenAfter * predecessors_number} times, where
	 * {@code predecessors_number} is the number of expressions that are
	 * predecessors of the one being processed. After overcoming that threshold,
	 * {@link Lattice#widening(Lattice)} is used. The computation starts at the
	 * statements in {@code startingPoints}, using as its entry state their
	 * respective value. {@code cg} will be invoked to get the approximation of all
	 * invoked cfgs, while a fresh instance of {@link FIFOWorkingSet} is used as
	 * working set for the statements to process.
	 * 
	 * @param <H>            the type of {@link HeapDomain} contained into the
	 *                       computed abstract state
	 * @param <V>            the type of {@link ValueDomain} contained into the
	 *                       computed abstract state
	 * @param startingPoints a map between {@link Expression}s that to use as a
	 *                       starting point of the computation (that must be nodes
	 *                       of this cfg) and the entry states to apply on it
	 * @param cg             the callgraph that can be queried when a call towards
	 *                       an other cfg is encountered
	 * @param widenAfter     the number of times after which the
	 *                       {@link Lattice#lub(Lattice)} invocation gets replaced
	 *                       by the {@link Lattice#widening(Lattice)} call. Use
	 *                       {@code 0} to <b>always</b> use
	 *                       {@link Lattice#lub(Lattice)}.
	 * @return a {@link CFGWithAnalysisResults} instance that os equivalent to this
	 *         control flow graph, and that stores for each {@link Expression} the
	 *         result of the fixpoint computation
	 * @throws FixpointException if an error occurs during the semantic computation
	 *                           of an statement, or if some unknown/invalid
	 *                           statement ends up in the working set
	 */
	public final <H extends HeapDomain<H>, V extends ValueDomain<V>> CFGWithAnalysisResults<H, V> computeFixpoint(
			Map<Statement, AnalysisState<H, V>> startingPoints, CallGraph cg, int widenAfter) {
		return fixpoint(startingPoints, cg, FIFOWorkingSet.mk(), widenAfter);
	}

	/**
	 * Computes a fixpoint over this control flow graph. This method returns a
	 * {@link CFGWithAnalysisResults} instance mapping each {@link Statement} to the
	 * {@link AnalysisState} computed by this method. The computation uses
	 * {@link Lattice#lub(Lattice)} to compose results obtained at different
	 * iterations, up to {@link #DEFAULT_WIDENING_THRESHOLD}
	 * {@code * predecessors_number} times, where {@code predecessors_number} is the
	 * number of expressions that are predecessors of the one being processed. After
	 * overcoming that threshold, {@link Lattice#widening(Lattice)} is used. The
	 * computation starts at the statements in {@code startingPoints}, using as its
	 * entry state their respective value. {@code cg} will be invoked to get the
	 * approximation of all invoked cfgs, while {@code ws} is used as working set
	 * for the statements to process.
	 * 
	 * @param <H>            the type of {@link HeapDomain} contained into the
	 *                       computed abstract state
	 * @param <V>            the type of {@link ValueDomain} contained into the
	 *                       computed abstract state
	 * @param startingPoints a map between {@link Expression}s that to use as a
	 *                       starting point of the computation (that must be nodes
	 *                       of this cfg) and the entry states to apply on it
	 * @param cg             the callgraph that can be queried when a call towards
	 *                       an other cfg is encountered
	 * @param ws             the {@link WorkingSet} instance to use for this
	 *                       computation
	 * @return a {@link CFGWithAnalysisResults} instance that os equivalent to this
	 *         control flow graph, and that stores for each {@link Expression} the
	 *         result of the fixpoint computation
	 * @throws FixpointException if an error occurs during the semantic computation
	 *                           of an statement, or if some unknown/invalid
	 *                           statement ends up in the working set
	 */
	public final <H extends HeapDomain<H>, V extends ValueDomain<V>> CFGWithAnalysisResults<H, V> computeFixpoint(
			Map<Statement, AnalysisState<H, V>> startingPoints, CallGraph cg, WorkingSet<Statement> ws) {
		return fixpoint(startingPoints, cg, ws, DEFAULT_WIDENING_THRESHOLD);
	}

	/**
	 * Computes a fixpoint over this control flow graph. This method returns a
	 * {@link CFGWithAnalysisResults} instance mapping each {@link Statement} to the
	 * {@link AnalysisState} computed by this method. The computation uses
	 * {@link Lattice#lub(Lattice)} to compose results obtained at different
	 * iterations, up to {@code widenAfter * predecessors_number} times, where
	 * {@code predecessors_number} is the number of expressions that are
	 * predecessors of the one being processed. After overcoming that threshold,
	 * {@link Lattice#widening(Lattice)} is used. The computation starts at the
	 * statements in {@code startingPoints}, using as its entry state their
	 * respective value. {@code cg} will be invoked to get the approximation of all
	 * invoked cfgs, while {@code ws} is used as working set for the statements to
	 * process.
	 * 
	 * @param <H>            the type of {@link HeapDomain} contained into the
	 *                       computed abstract state
	 * @param <V>            the type of {@link ValueDomain} contained into the
	 *                       computed abstract state
	 * @param startingPoints a map between {@link Statement}s that to use as a
	 *                       starting point of the computation (that must be nodes
	 *                       of this cfg) and the entry states to apply on it
	 * @param cg             the callgraph that can be queried when a call towards
	 *                       an other cfg is encountered
	 * @param ws             the {@link WorkingSet} instance to use for this
	 *                       computation
	 * @param widenAfter     the number of times after which the
	 *                       {@link Lattice#lub(Lattice)} invocation gets replaced
	 *                       by the {@link Lattice#widening(Lattice)} call. Use
	 *                       {@code 0} to <b>always</b> use
	 *                       {@link Lattice#lub(Lattice)}.
	 * @return a {@link CFGWithAnalysisResults} instance that os equivalent to this
	 *         control flow graph, and that stores for each {@link Statement} the
	 *         result of the fixpoint computation
	 * @throws FixpointException if an error occurs during the semantic computation
	 *                           of a statement, or if some unknown/invalid
	 *                           statement ends up in the working set
	 */
	public final <H extends HeapDomain<H>, V extends ValueDomain<V>> CFGWithAnalysisResults<H, V> fixpoint(
			Map<Statement, AnalysisState<H, V>> startingPoints, CallGraph cg, WorkingSet<Statement> ws,
			int widenAfter) {
		int size = adjacencyMatrix.getNodes().size();
		Map<Statement, AtomicInteger> lubs = new HashMap<>(size);
		Map<Statement, AnalysisState<H, V>> result = new HashMap<>(size);
		startingPoints.keySet().forEach(ws::push);

		AnalysisState<H, V> previousApprox = null, newApprox;
		while (!ws.isEmpty()) {
			Statement current = ws.pop();

			if (current == null)
				throw new FixpointException("Unknown instruction encountered during fixpoint execution");
			if (!adjacencyMatrix.getNodes().contains(current))
				throw new FixpointException(current
						+ " is not part of this control flow graph, and cannot be analyzed in this fixpoint computation");

			AnalysisState<H, V> entrystate = startingPoints.get(current);
			List<AnalysisState<H, V>> states = predecessorsOf(current).stream()
					.map(pred -> adjacencyMatrix.getEdgeConnecting(pred, current))
					.map(edge -> edge.traverse(result.get(edge.getSource()))).collect(Collectors.toList());
			for (AnalysisState<H, V> s : states)
				if (entrystate == null)
					entrystate = s;
				else
					entrystate = entrystate.lub(s);

			if (entrystate == null)
				throw new FixpointException(current + " does not have an entry state");

			previousApprox = result.get(current);

			try {
				newApprox = current.semantics(entrystate, cg);
			} catch (Exception e) {
				log.error("Exception while evaluating semantics of '" + current + "' in " + descriptor + ": " + e);
				throw new FixpointException("Exception during fixpoint computation", e);
			}

			if (previousApprox != null)
				if (widenAfter == 0)
					newApprox = newApprox.lub(previousApprox);
				else {
					// we multiply by the number of predecessors since if we have more than one
					// the threshold will be reached faster
					int lub = lubs
							.computeIfAbsent(current, e -> new AtomicInteger(widenAfter * predecessorsOf(e).size()))
							.getAndDecrement();
					if (lub > 0)
						newApprox = newApprox.lub(previousApprox);
					else
						newApprox = newApprox.widening(previousApprox);
				}

			if (!newApprox.equals(previousApprox)) {
				// TODO how do we save intermediate results for each expression?
				result.put(current, newApprox);
				for (Statement instr : followersOf(current))
					ws.push(instr);
			}
		}

		return new CFGWithAnalysisResults<>(this, result);
	}
}
