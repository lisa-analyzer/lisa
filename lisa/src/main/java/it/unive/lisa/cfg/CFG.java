package it.unive.lisa.cfg;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.CFGWithAnalysisResults;
import it.unive.lisa.analysis.FunctionalLattice;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.callgraph.CallGraph;
import it.unive.lisa.cfg.edge.Edge;
import it.unive.lisa.cfg.edge.SequentialEdge;
import it.unive.lisa.cfg.statement.Expression;
import it.unive.lisa.cfg.statement.NoOp;
import it.unive.lisa.cfg.statement.Ret;
import it.unive.lisa.cfg.statement.Return;
import it.unive.lisa.cfg.statement.Statement;
import it.unive.lisa.outputs.DotCFG;
import it.unive.lisa.outputs.DotGraph;
import it.unive.lisa.util.datastructures.graph.AdjacencyMatrix;
import it.unive.lisa.util.datastructures.graph.FixpointException;
import it.unive.lisa.util.datastructures.graph.FixpointGraph;
import it.unive.lisa.util.workset.FIFOWorkingSet;
import it.unive.lisa.util.workset.WorkingSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A control flow graph, that has {@link Statement}s as nodes and {@link Edge}s
 * as edges.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class CFG extends FixpointGraph<Statement, Edge> {

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
		super();
		this.descriptor = descriptor;
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
	public CFG(CFGDescriptor descriptor, Collection<Statement> entrypoints,
			AdjacencyMatrix<Statement, Edge> adjacencyMatrix) {
		super(entrypoints, adjacencyMatrix);
		this.descriptor = descriptor;
	}

	/**
	 * Clones the given control flow graph.
	 * 
	 * @param other the original cfg
	 */
	protected CFG(CFG other) {
		super(other.entrypoints, other.adjacencyMatrix);
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((descriptor == null) ? 0 : descriptor.hashCode());
		return result;
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
	 *                                           non-sequential edge.
	 */
	public void simplify() {
		super.simplify(NoOp.class);
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
	 * @param <A>        the type of {@link AbstractState} contained into the
	 *                       analysis state
	 * @param <H>        the type of {@link HeapDomain} contained into the
	 *                       computed abstract state
	 * @param <V>        the type of {@link ValueDomain} contained into the
	 *                       computed abstract state
	 * @param entryState the entry states to apply to each {@link Statement}
	 *                       returned by {@link #getEntrypoints()}
	 * @param cg         the callgraph that can be queried when a call towards
	 *                       an other cfg is encountered
	 * @param semantics  the {@link SemanticFunction} that will be used for
	 *                       computing the abstract post-state of statements
	 * 
	 * @return a {@link CFGWithAnalysisResults} instance that is equivalent to
	 *             this control flow graph, and that stores for each
	 *             {@link Statement} the result of the fixpoint computation
	 * 
	 * @throws FixpointException if an error occurs during the semantic
	 *                               computation of a statement, or if some
	 *                               unknown/invalid statement ends up in the
	 *                               working set
	 */
	public final <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> CFGWithAnalysisResults<A, H, V> fixpoint(
					AnalysisState<A, H, V> entryState, CallGraph cg, SemanticFunction<A, H, V> semantics)
					throws FixpointException {
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
	 * @param <A>        the type of {@link AbstractState} contained into the
	 *                       analysis state
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
	 *                       {@link Lattice#lub(Lattice)}
	 * @param semantics  the {@link SemanticFunction} that will be used for
	 *                       computing the abstract post-state of statements
	 * 
	 * @return a {@link CFGWithAnalysisResults} instance that is equivalent to
	 *             this control flow graph, and that stores for each
	 *             {@link Statement} the result of the fixpoint computation
	 * 
	 * @throws FixpointException if an error occurs during the semantic
	 *                               computation of a statement, or if some
	 *                               unknown/invalid statement ends up in the
	 *                               working set
	 */
	public final <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> CFGWithAnalysisResults<A, H, V> fixpoint(
					AnalysisState<A, H, V> entryState, CallGraph cg, int widenAfter,
					SemanticFunction<A, H, V> semantics)
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
	 * @param <A>        the type of {@link AbstractState} contained into the
	 *                       analysis state
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
	 * @param semantics  the {@link SemanticFunction} that will be used for
	 *                       computing the abstract post-state of statements
	 * 
	 * @return a {@link CFGWithAnalysisResults} instance that is equivalent to
	 *             this control flow graph, and that stores for each
	 *             {@link Statement} the result of the fixpoint computation
	 * 
	 * @throws FixpointException if an error occurs during the semantic
	 *                               computation of a statement, or if some
	 *                               unknown/invalid statement ends up in the
	 *                               working set
	 */
	public final <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> CFGWithAnalysisResults<A, H, V> fixpoint(
					AnalysisState<A, H, V> entryState, CallGraph cg, WorkingSet<Statement> ws,
					SemanticFunction<A, H, V> semantics)
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
	 * @param <A>        the type of {@link AbstractState} contained into the
	 *                       analysis state
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
	 *                       {@link Lattice#lub(Lattice)}
	 * @param semantics  the {@link SemanticFunction} that will be used for
	 *                       computing the abstract post-state of statements
	 * 
	 * @return a {@link CFGWithAnalysisResults} instance that is equivalent to
	 *             this control flow graph, and that stores for each
	 *             {@link Statement} the result of the fixpoint computation
	 * 
	 * @throws FixpointException if an error occurs during the semantic
	 *                               computation of a statement, or if some
	 *                               unknown/invalid statement ends up in the
	 *                               working set
	 */
	public final <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> CFGWithAnalysisResults<A, H, V> fixpoint(
					AnalysisState<A, H, V> entryState, CallGraph cg, WorkingSet<Statement> ws, int widenAfter,
					SemanticFunction<A, H, V> semantics)
					throws FixpointException {
		Map<Statement, AnalysisState<A, H, V>> start = new HashMap<>();
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
	 * @param <A>         the type of {@link AbstractState} contained into the
	 *                        analysis state
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
	 * @param semantics   the {@link SemanticFunction} that will be used for
	 *                        computing the abstract post-state of statements
	 * 
	 * @return a {@link CFGWithAnalysisResults} instance that is equivalent to
	 *             this control flow graph, and that stores for each
	 *             {@link Statement} the result of the fixpoint computation
	 * 
	 * @throws FixpointException if an error occurs during the semantic
	 *                               computation of a statement, or if some
	 *                               unknown/invalid statement ends up in the
	 *                               working set
	 */
	public final <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> CFGWithAnalysisResults<A, H, V> fixpoint(
					Collection<Statement> entrypoints, AnalysisState<A, H, V> entryState, CallGraph cg,
					SemanticFunction<A, H, V> semantics) throws FixpointException {
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
	 * @param <A>         the type of {@link AbstractState} contained into the
	 *                        analysis state
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
	 *                        {@link Lattice#lub(Lattice)}
	 * @param semantics   the {@link SemanticFunction} that will be used for
	 *                        computing the abstract post-state of statements
	 * 
	 * @return a {@link CFGWithAnalysisResults} instance that is equivalent to
	 *             this control flow graph, and that stores for each
	 *             {@link Statement} the result of the fixpoint computation
	 * 
	 * @throws FixpointException if an error occurs during the semantic
	 *                               computation of a statement, or if some
	 *                               unknown/invalid statement ends up in the
	 *                               working set
	 */
	public final <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> CFGWithAnalysisResults<A, H, V> fixpoint(
					Collection<Statement> entrypoints, AnalysisState<A, H, V> entryState, CallGraph cg, int widenAfter,
					SemanticFunction<A, H, V> semantics)
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
	 * @param <A>         the type of {@link AbstractState} contained into the
	 *                        analysis state
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
	 * @param semantics   the {@link SemanticFunction} that will be used for
	 *                        computing the abstract post-state of statements
	 * 
	 * @return a {@link CFGWithAnalysisResults} instance that is equivalent to
	 *             this control flow graph, and that stores for each
	 *             {@link Statement} the result of the fixpoint computation
	 * 
	 * @throws FixpointException if an error occurs during the semantic
	 *                               computation of a statement, or if some
	 *                               unknown/invalid statement ends up in the
	 *                               working set
	 */
	public final <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> CFGWithAnalysisResults<A, H, V> fixpoint(
					Collection<Statement> entrypoints, AnalysisState<A, H, V> entryState, CallGraph cg,
					WorkingSet<Statement> ws,
					SemanticFunction<A, H, V> semantics)
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
	 * @param <A>         the type of {@link AbstractState} contained into the
	 *                        analysis state
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
	 *                        {@link Lattice#lub(Lattice)}
	 * @param semantics   the {@link SemanticFunction} that will be used for
	 *                        computing the abstract post-state of statements
	 * 
	 * @return a {@link CFGWithAnalysisResults} instance that is equivalent to
	 *             this control flow graph, and that stores for each
	 *             {@link Statement} the result of the fixpoint computation
	 * 
	 * @throws FixpointException if an error occurs during the semantic
	 *                               computation of a statement, or if some
	 *                               unknown/invalid statement ends up in the
	 *                               working set
	 */
	public final <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> CFGWithAnalysisResults<A, H, V> fixpoint(
					Collection<Statement> entrypoints, AnalysisState<A, H, V> entryState, CallGraph cg,
					WorkingSet<Statement> ws,
					int widenAfter, SemanticFunction<A, H, V> semantics) throws FixpointException {
		Map<Statement, AnalysisState<A, H, V>> start = new HashMap<>();
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
	 * @param <A>            the type of {@link AbstractState} contained into
	 *                           the analysis state
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
	 * @param semantics      the {@link SemanticFunction} that will be used for
	 *                           computing the abstract post-state of statements
	 * 
	 * @return a {@link CFGWithAnalysisResults} instance that is equivalent to
	 *             this control flow graph, and that stores for each
	 *             {@link Statement} the result of the fixpoint computation
	 * 
	 * @throws FixpointException if an error occurs during the semantic
	 *                               computation of a statement, or if some
	 *                               unknown/invalid statement ends up in the
	 *                               working set
	 */
	public final <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> CFGWithAnalysisResults<A, H, V> fixpoint(
					Map<Statement, AnalysisState<A, H, V>> startingPoints, CallGraph cg,
					SemanticFunction<A, H, V> semantics)
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
	 * @param <A>            the type of {@link AbstractState} contained into
	 *                           the analysis state
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
	 *                           {@link Lattice#lub(Lattice)}
	 * @param semantics      the {@link SemanticFunction} that will be used for
	 *                           computing the abstract post-state of statements
	 * 
	 * @return a {@link CFGWithAnalysisResults} instance that is equivalent to
	 *             this control flow graph, and that stores for each
	 *             {@link Expression} the result of the fixpoint computation
	 * 
	 * @throws FixpointException if an error occurs during the semantic
	 *                               computation of an statement, or if some
	 *                               unknown/invalid statement ends up in the
	 *                               working set
	 */
	public final <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> CFGWithAnalysisResults<A, H, V> fixpoint(
					Map<Statement, AnalysisState<A, H, V>> startingPoints, CallGraph cg, int widenAfter,
					SemanticFunction<A, H, V> semantics) throws FixpointException {
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
	 * @param <A>            the type of {@link AbstractState} contained into
	 *                           the analysis state
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
	 * @param semantics      the {@link SemanticFunction} that will be used for
	 *                           computing the abstract post-state of statements
	 * 
	 * @return a {@link CFGWithAnalysisResults} instance that is equivalent to
	 *             this control flow graph, and that stores for each
	 *             {@link Expression} the result of the fixpoint computation
	 * 
	 * @throws FixpointException if an error occurs during the semantic
	 *                               computation of an statement, or if some
	 *                               unknown/invalid statement ends up in the
	 *                               working set
	 */
	public final <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> CFGWithAnalysisResults<A, H, V> fixpoint(
					Map<Statement, AnalysisState<A, H, V>> startingPoints, CallGraph cg, WorkingSet<Statement> ws,
					SemanticFunction<A, H, V> semantics)
					throws FixpointException {
		return fixpoint(startingPoints, cg, ws, DEFAULT_WIDENING_THRESHOLD, semantics);
	}

	/**
	 * A functional interface that can be used for compute the semantics of
	 * {@link Statement}s, producing {@link AnalysisState}s.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 * 
	 * @param <A> the type of {@link AbstractState} contained into the analysis
	 *                state
	 * @param <H> the concrete type of {@link HeapDomain} embedded in the
	 *                analysis states
	 * @param <V> the concrete type of {@link ValueDomain} embedded in the
	 *                analysis states
	 */
	public interface SemanticFunction<A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> extends
			it.unive.lisa.util.datastructures.graph.FixpointGraph.SemanticFunction<Statement, A, H, V,
					StatementStore<A, H, V>> {

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
	 * @param <A>            the type of {@link AbstractState} contained into
	 *                           the analysis state
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
	 *                           {@link Lattice#lub(Lattice)}
	 * @param semantics      the {@link SemanticFunction} that will be used for
	 *                           computing the abstract post-state of statements
	 * 
	 * @return a {@link CFGWithAnalysisResults} instance that is equivalent to
	 *             this control flow graph, and that stores for each
	 *             {@link Statement} the result of the fixpoint computation
	 * 
	 * @throws FixpointException if an error occurs during the semantic
	 *                               computation of a statement, or if some
	 *                               unknown/invalid statement ends up in the
	 *                               working set
	 */
	public <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> CFGWithAnalysisResults<A, H, V> fixpoint(
					Map<Statement, AnalysisState<A, H, V>> startingPoints, CallGraph cg, WorkingSet<Statement> ws,
					int widenAfter,
					SemanticFunction<A, H, V> semantics)
					throws FixpointException {
		return new CFGWithAnalysisResults<>(this, super.fixpoint(startingPoints, cg, ws, widenAfter, semantics));
	}

	@Override
	protected <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> FunctionalLattice<?, Statement, AnalysisState<A, H, V>> mkInternalStore(
					AnalysisState<A, H, V> entrystate) {
		return new StatementStore<>(entrystate);
	}

	@Override
	protected DotGraph<Statement, Edge> toDot(Function<Statement, String> labelGenerator) {
		return DotCFG.fromCFG(this, labelGenerator);
	}
}
