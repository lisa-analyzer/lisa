package it.unive.lisa.program.cfg;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.CFGWithAnalysisResults;
import it.unive.lisa.analysis.FunctionalLattice;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.callgraph.CallGraph;
import it.unive.lisa.outputs.DotCFG;
import it.unive.lisa.program.ProgramValidationException;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.edge.SequentialEdge;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.util.collections.ExternalSet;
import it.unive.lisa.util.datastructures.graph.AdjacencyMatrix;
import it.unive.lisa.util.datastructures.graph.FixpointException;
import it.unive.lisa.util.datastructures.graph.FixpointGraph;
import it.unive.lisa.util.workset.FIFOWorkingSet;
import it.unive.lisa.util.workset.WorkingSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A control flow graph, that has {@link Statement}s as nodes and {@link Edge}s
 * as edges.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class CFG extends FixpointGraph<CFG, Statement, Edge> implements CodeMember {

	private static final Logger log = LogManager.getLogger(CFG.class);

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
			AdjacencyMatrix<Statement, Edge, CFG> adjacencyMatrix) {
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
	 * returning the control to the caller without throwing an error (i.e., all
	 * such statements on which {@link Statement#stopsExecution()} holds but
	 * {@link Statement#throwsError()} does not).
	 * 
	 * @return the normal exitpoints of this cfg.
	 */
	public final Collection<Statement> getNormalExitpoints() {
		return adjacencyMatrix.getNodes().stream().filter(st -> st.stopsExecution() && !st.throwsError())
				.collect(Collectors.toList());
	}

	/**
	 * Yields the statements of this control flow graph that are normal
	 * exitpoints, that is, that normally ends the execution of this cfg,
	 * returning the control to the caller, or throwing an error (i.e., all such
	 * statements on which either {@link Statement#stopsExecution()} or
	 * {@link Statement#throwsError()} hold).
	 * 
	 * @return the exitpoints of this cfg.
	 */
	public final Collection<Statement> getAllExitpoints() {
		return adjacencyMatrix.getNodes().stream().filter(st -> st.stopsExecution() || st.throwsError())
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
					AnalysisState<A, H, V> entryState, CallGraph cg)
					throws FixpointException {
		return fixpoint(entryState, cg, FIFOWorkingSet.mk(), DEFAULT_WIDENING_THRESHOLD);
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
					AnalysisState<A, H, V> entryState, CallGraph cg, int widenAfter)
					throws FixpointException {
		return fixpoint(entryState, cg, FIFOWorkingSet.mk(), widenAfter);
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
					AnalysisState<A, H, V> entryState, CallGraph cg, WorkingSet<Statement> ws)
					throws FixpointException {
		return fixpoint(entryState, cg, ws, DEFAULT_WIDENING_THRESHOLD);
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
					AnalysisState<A, H, V> entryState, CallGraph cg, WorkingSet<Statement> ws, int widenAfter)
					throws FixpointException {
		Map<Statement, AnalysisState<A, H, V>> start = new HashMap<>();
		entrypoints.forEach(e -> start.put(e, entryState));
		return fixpoint(start, cg, ws, widenAfter);
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
					Collection<Statement> entrypoints, AnalysisState<A, H, V> entryState, CallGraph cg)
					throws FixpointException {
		return fixpoint(entrypoints, entryState, cg, FIFOWorkingSet.mk(), DEFAULT_WIDENING_THRESHOLD);
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
					Collection<Statement> entrypoints, AnalysisState<A, H, V> entryState, CallGraph cg, int widenAfter)
					throws FixpointException {
		return fixpoint(entrypoints, entryState, cg, FIFOWorkingSet.mk(), widenAfter);
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
					WorkingSet<Statement> ws)
					throws FixpointException {
		return fixpoint(entrypoints, entryState, cg, ws, DEFAULT_WIDENING_THRESHOLD);
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
					int widenAfter) throws FixpointException {
		Map<Statement, AnalysisState<A, H, V>> start = new HashMap<>();
		entrypoints.forEach(e -> start.put(e, entryState));
		return fixpoint(start, cg, ws, widenAfter);
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
					Map<Statement, AnalysisState<A, H, V>> startingPoints, CallGraph cg)
					throws FixpointException {
		return fixpoint(startingPoints, cg, FIFOWorkingSet.mk(), DEFAULT_WIDENING_THRESHOLD);
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
					Map<Statement, AnalysisState<A, H, V>> startingPoints, CallGraph cg, int widenAfter)
					throws FixpointException {
		return fixpoint(startingPoints, cg, FIFOWorkingSet.mk(), widenAfter);
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
					Map<Statement, AnalysisState<A, H, V>> startingPoints, CallGraph cg, WorkingSet<Statement> ws)
					throws FixpointException {
		return fixpoint(startingPoints, cg, ws, DEFAULT_WIDENING_THRESHOLD);
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
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> CFGWithAnalysisResults<A, H, V> fixpoint(
					Map<Statement, AnalysisState<A, H, V>> startingPoints, CallGraph cg, WorkingSet<Statement> ws,
					int widenAfter)
					throws FixpointException {
		return new CFGWithAnalysisResults<A, H, V>(this, super.fixpoint(startingPoints, cg, ws, widenAfter,
				(st, entryState, callGraph, expressions) -> st.semantics(entryState, callGraph,
						(StatementStore) expressions)));
	}

	@Override
	protected <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> FunctionalLattice<?, Statement, AnalysisState<A, H, V>> mkInternalStore(
					AnalysisState<A, H, V> entrystate) {
		return new StatementStore<>(entrystate);
	}

	@Override
	protected <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> AnalysisState<A, H, V> cleanUpPostState(Statement node,
					AnalysisState<A, H, V> computedState) throws SemanticException {
		List<VariableTableEntry> toRemove = new LinkedList<>();
		for (VariableTableEntry entry : descriptor.getVariables())
			if (entry.getScopeEnd() == node)
				toRemove.add(entry);

		Collection<Identifier> ids = new LinkedList<>();
		for (VariableTableEntry entry : toRemove) {
			SymbolicExpression v = entry.createReference(this).getVariable();
			for (SymbolicExpression expr : computedState.smallStepSemantics(v, node).getComputedExpressions())
				ids.add((Identifier) expr);
		}

		if (ids != null && !ids.isEmpty())
			return computedState.forgetIdentifiers(ids);

		return computedState;
	}

	@Override
	protected DotCFG toDot(Function<Statement, String> labelGenerator) {
		return DotCFG.fromCFG(this, labelGenerator);
	}

	@Override
	protected void preSimplify(Statement node) {
		Collection<
				VariableTableEntry> starting = descriptor.getVariables().stream().filter(v -> v.getScopeStart() == node)
						.collect(Collectors.toList());
		Collection<VariableTableEntry> ending = descriptor.getVariables().stream().filter(v -> v.getScopeEnd() == node)
				.collect(Collectors.toList());
		if (ending.isEmpty() && starting.isEmpty())
			return;

		Collection<Statement> predecessors = predecessorsOf(node);
		Collection<Statement> followers = followersOf(node);

		if (predecessors.isEmpty() && followers.isEmpty()) {
			log.warn("Simplifying the only statement of '" + this
					+ "': all variables will be made visible for the entire cfg");
			starting.forEach(v -> v.setScopeStart(null));
			ending.forEach(v -> v.setScopeEnd(null));
			return;
		}

		String format = "Simplifying the scope-%s statement of a variable with %s "
				+ "is not supported: %s will be made visible %s of '" + this + "'";
		if (!starting.isEmpty())
			if (predecessors.isEmpty()) {
				// no predecessors: move the starting scope forward
				Statement follow;
				if (followers.size() > 1) {
					log.warn(String.format(format, "starting", "no predecessors and multiple followers", starting,
							"from the start"));
					follow = null;
				} else
					follow = followers.iterator().next();
				starting.forEach(v -> v.setScopeStart(follow));
			} else {
				// move the starting scope backward
				Statement pred;
				if (predecessors.size() > 1) {
					log.warn(String.format(format, "starting", "multiple predecessors", starting, "from the start"));
					pred = null;
				} else
					pred = predecessors.iterator().next();
				starting.forEach(v -> v.setScopeStart(pred));
			}

		if (!ending.isEmpty())
			if (followers.isEmpty()) {
				// no followers: move the ending scope backward
				Statement pred;
				if (predecessors.size() > 1) {
					log.warn(String.format(format, "ending", "no followers and multiple predecessors", ending,
							"until the end"));
					pred = null;
				} else
					pred = predecessors.iterator().next();
				ending.forEach(v -> v.setScopeEnd(pred));
			} else {
				// move the ending scope forward
				Statement follow;
				if (followers.size() > 1) {
					log.warn(String.format(format, "ending", "multiple followers", ending, "until the end"));
					follow = null;
				} else
					follow = followers.iterator().next();
				ending.forEach(v -> v.setScopeEnd(follow));
			}
	}

	/**
	 * Yields a generic {@link ProgramPoint} happening inside this cfg. A
	 * generic program point can be used for semantic evaluations of
	 * instrumented {@link Statement}s, that are not tied to any concrete
	 * statement.
	 * 
	 * @return a generic program point happening in this cfg
	 */
	public ProgramPoint getGenericProgramPoint() {
		return new ProgramPoint() {

			@Override
			public CFG getCFG() {
				return CFG.this;
			}

			@Override
			public String toString() {
				return "unknown program point in " + CFG.this.getDescriptor().getSignature();
			}
		};
	}

	/**
	 * Validates this cfg, ensuring that the code contained in it is well
	 * formed. This method checks that:
	 * <ul>
	 * <li>each {@link Edge} is connected to {@link Statement}s contained in the
	 * cfg</li>
	 * <li>all {@link Statement}s with no ingoing edges are marked as
	 * entrypoints of the cfg (i.e. no deadcode)</li>
	 * <li>all {@link Statement}s that stop the execution (according to
	 * {@link Statement#stopsExecution()}) do not have outgoing edges</li>
	 * <li>all entrypoints are effectively part of this cfg</li>
	 * </ul>
	 * 
	 * @throws ProgramValidationException if one of the aforementioned checks
	 *                                        fail
	 */
	public void validate() throws ProgramValidationException {
		Collection<Statement> nodes = adjacencyMatrix.getNodes();

		// all edges should be connected to statements inside the cfg
		for (Entry<Statement, Pair<ExternalSet<Edge>, ExternalSet<Edge>>> st : adjacencyMatrix) {
			for (Edge in : st.getValue().getLeft())
				validateEdge(nodes, in);

			for (Edge out : st.getValue().getRight())
				validateEdge(nodes, out);

			// no deadcode
			if (st.getValue().getLeft().isEmpty() && !entrypoints.contains(st.getKey()))
				throw new ProgramValidationException(
						this + " contains an unreachable node that is not marked as entrypoint: " + st.getKey());

			// no outgoing edges in execution-terminating statements
			if (st.getKey().stopsExecution() && !st.getValue().getRight().isEmpty())
				throw new ProgramValidationException(
						this + " contains an execution-stopping node that has followers: " + st.getKey());
		}

		// all entrypoints should be within the cfg
		if (!nodes.containsAll(entrypoints))
			throw new ProgramValidationException(this + " has entrypoints that are not part of the graph: "
					+ new HashSet<>(entrypoints).retainAll(nodes));
	}

	private void validateEdge(Collection<Statement> nodes, Edge edge) throws ProgramValidationException {
		if (!nodes.contains(edge.getSource()))
			throw new ProgramValidationException(this + " contains an invalid edge: '" + edge
					+ "' originates in a node that is not part of the graph");
		else if (!nodes.contains(edge.getDestination()))
			throw new ProgramValidationException(this + " contains an invalid edge: '" + edge
					+ "' reaches a node that is not part of the graph");
	}
}
