package it.unive.lisa.interprocedural;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.CFGWithAnalysisResults;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.interprocedural.callgraph.CallResolutionException;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.OpenCall;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.collections.externalSet.ExternalSet;
import it.unive.lisa.util.collections.workset.WorkingSet;
import it.unive.lisa.util.datastructures.graph.algorithms.FixpointException;
import java.util.Collection;

/**
 * The definition of interprocedural analyses.
 * 
 * @param <A> the type of {@link AbstractState} contained into the analysis
 *                state
 * @param <H> the type of {@link HeapDomain} contained into the computed
 *                abstract state
 * @param <V> the type of {@link ValueDomain} contained into the computed
 *                abstract state
 */
public interface InterproceduralAnalysis<A extends AbstractState<A, H, V, T>,
		H extends HeapDomain<H>,
		V extends ValueDomain<V>,
		T extends TypeDomain<T>> {

	/**
	 * Initializes the interprocedural analysis of the given program.
	 *
	 * @param callgraph the callgraph used to resolve method calls
	 * @param program   the program
	 * @param policy    the {@link OpenCallPolicy} to be used for computing the
	 *                      result of {@link OpenCall}s
	 *
	 * @throws InterproceduralAnalysisException if an exception happens while
	 *                                              performing the
	 *                                              interprocedural analysis
	 */
	void init(Program program, CallGraph callgraph, OpenCallPolicy policy) throws InterproceduralAnalysisException;

	/**
	 * Computes a fixpoint over the whole control flow graph, producing a
	 * {@link CFGWithAnalysisResults} for each {@link CFG} contained in this
	 * analysis. Each result is computed with
	 * {@link CFG#fixpoint(AnalysisState, InterproceduralAnalysis, WorkingSet, int)}
	 * or one of its overloads. Results of individual cfgs are then available
	 * through {@link #getAnalysisResultsOf(CFG)}.
	 * 
	 * @param entryState         the entry state for the {@link CFG}s that are
	 *                               the entrypoints of the computation
	 * @param fixpointWorkingSet the concrete class of {@link WorkingSet} to be
	 *                               used in fixpoints.
	 * @param wideningThreshold  the number of fixpoint iteration on a given
	 *                               node after which calls to
	 *                               {@link Lattice#lub(Lattice)} gets replaced
	 *                               with {@link Lattice#widening(Lattice)}.
	 *
	 * @throws FixpointException if something goes wrong while evaluating the
	 *                               fixpoint
	 */
	void fixpoint(AnalysisState<A, H, V, T> entryState,
			Class<? extends WorkingSet<Statement>> fixpointWorkingSet,
			int wideningThreshold) throws FixpointException;

	/**
	 * Yields the results of the given analysis, identified by its class, on the
	 * given {@link CFG}. Results are provided as
	 * {@link CFGWithAnalysisResults}.
	 * 
	 * @param cfg the cfg whose fixpoint results needs to be retrieved
	 *
	 * @return the result of the fixpoint computation of {@code valueDomain}
	 *             over {@code cfg}
	 */
	Collection<CFGWithAnalysisResults<A, H, V, T>> getAnalysisResultsOf(CFG cfg);

	/**
	 * Computes an analysis state that abstracts the execution of the possible
	 * targets considering that they were given {@code parameters} as actual
	 * parameters, and the state when the call is executed is
	 * {@code entryState}.<br>
	 * <br>
	 * Note that the interprocedural analysis is also responsible for
	 * registering the call to the {@link CallGraph}, if needed.
	 *
	 * @param call        the call to evaluate
	 * @param entryState  the abstract analysis state when the call is reached
	 * @param parameters  the expressions representing the actual parameters of
	 *                        the call
	 * @param expressions the cache where analysis states of intermediate
	 *                        expressions must be stored
	 *
	 * @return an abstract analysis state representing the abstract result of
	 *             the cfg call. The
	 *             {@link AnalysisState#getComputedExpressions()} will contain
	 *             an {@link Identifier} pointing to the meta variable
	 *             containing the abstraction of the returned value, if any
	 *
	 * @throws SemanticException if something goes wrong during the computation
	 */
	AnalysisState<A, H, V, T> getAbstractResultOf(
			CFGCall call,
			AnalysisState<A, H, V, T> entryState,
			ExpressionSet<SymbolicExpression>[] parameters,
			StatementStore<A, H, V, T> expressions)
			throws SemanticException;

	/**
	 * Computes an analysis state that abstracts the execution of an unknown
	 * target considering that they were given {@code parameters} as actual
	 * parameters, and the state when the call is executed is
	 * {@code entryState}.
	 *
	 * @param call        the call to evaluate
	 * @param entryState  the abstract analysis state when the call is reached
	 * @param parameters  the expressions representing the actual parameters of
	 *                        the call
	 * @param expressions the cache where analysis states of intermediate
	 *                        expressions must be stored
	 *
	 * @return an abstract analysis state representing the abstract result of
	 *             the open call. The
	 *             {@link AnalysisState#getComputedExpressions()} will contain
	 *             an {@link Identifier} pointing to the meta variable
	 *             containing the abstraction of the returned value, if any
	 *
	 * @throws SemanticException if something goes wrong during the computation
	 */
	AnalysisState<A, H, V, T> getAbstractResultOf(
			OpenCall call,
			AnalysisState<A, H, V, T> entryState,
			ExpressionSet<SymbolicExpression>[] parameters,
			StatementStore<A, H, V, T> expressions)
			throws SemanticException;

	/**
	 * Yields a {@link Call} implementation that corresponds to the resolution
	 * of the given {@link UnresolvedCall}. This method will forward the call to
	 * {@link CallGraph#resolve(UnresolvedCall, ExternalSet[])} if needed.
	 *
	 * @param call the call to resolve
	 *
	 * @return a collection of all the possible runtime targets
	 *
	 * @throws CallResolutionException if this analysis is unable to resolve the
	 *                                     given call
	 */
	Call resolve(UnresolvedCall call, ExternalSet<Type>[] types) throws CallResolutionException;
}
