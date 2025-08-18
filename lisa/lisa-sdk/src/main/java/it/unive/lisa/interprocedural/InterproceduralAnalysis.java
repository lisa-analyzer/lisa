package it.unive.lisa.interprocedural;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.symbols.SymbolAliasing;
import it.unive.lisa.conf.FixpointConfiguration;
import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.interprocedural.callgraph.CallResolutionException;
import it.unive.lisa.program.Application;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.OpenCall;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.collections.workset.WorkingSet;
import it.unive.lisa.util.datastructures.graph.algorithms.FixpointException;
import java.util.Collection;
import java.util.Set;

/**
 * The definition of interprocedural analyses.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the kind of {@link AbstractLattice} produced by the domain
 *                {@code D}
 * @param <D> the kind of {@link AbstractDomain} to run during the analysis
 */
public interface InterproceduralAnalysis<A extends AbstractLattice<A>, D extends AbstractDomain<A>> {

	/**
	 * Yields {@code true} if this analysis needs a {@link CallGraph} instance
	 * to function. If this method return {@code false} and a {@link CallGraph}
	 * is passed to LiSA in its configuration, the latter will be ignored and
	 * won't be initialized nor constructed.
	 * 
	 * @return whether or not this analysis needs a call graph
	 */
	boolean needsCallGraph();

	/**
	 * Initializes the interprocedural analysis of the given program. A call to
	 * this method should effectively re-initialize the interprocedural analysis
	 * as if it is yet to be used. This is useful when the same instance is used
	 * in multiple analyses.
	 *
	 * @param callgraph the callgraph used to resolve method calls
	 * @param app       the application to analyze
	 * @param policy    the {@link OpenCallPolicy} to be used for computing the
	 *                      result of {@link OpenCall}s
	 * @param analysis  the {@link Analysis} that is being run
	 *
	 * @throws InterproceduralAnalysisException if an exception happens while
	 *                                              performing the
	 *                                              interprocedural analysis
	 */
	void init(
			Application app,
			CallGraph callgraph,
			OpenCallPolicy policy,
			Analysis<A, D> analysis)
			throws InterproceduralAnalysisException;

	/**
	 * Yields the {@link Analysis} that is being run by this
	 * {@link InterproceduralAnalysis}.
	 * 
	 * @return the analysis being run
	 */
	Analysis<A, D> getAnalysis();

	/**
	 * Computes a fixpoint over the whole control flow graph, producing a
	 * {@link AnalyzedCFG} for each {@link CFG} contained in this analysis. Each
	 * result is computed with
	 * {@link CFG#fixpoint(AnalysisState, InterproceduralAnalysis, WorkingSet, FixpointConfiguration, ScopeId)}
	 * or one of its overloads. Results of individual cfgs are then available
	 * through {@link #getAnalysisResultsOf(CFG)}.
	 * 
	 * @param entryState the entry state for the {@link CFG}s that are the
	 *                       entrypoints of the computation
	 * @param conf       the {@link FixpointConfiguration} containing the
	 *                       parameters tuning fixpoint behavior
	 * 
	 * @throws FixpointException if something goes wrong while evaluating the
	 *                               fixpoint
	 */
	void fixpoint(
			AnalysisState<A> entryState,
			FixpointConfiguration conf)
			throws FixpointException;

	/**
	 * Yields the results of the given analysis, identified by its class, on the
	 * given {@link CFG}. Results are provided as {@link AnalyzedCFG}.
	 * 
	 * @param cfg the cfg whose fixpoint results needs to be retrieved
	 *
	 * @return the result of the fixpoint computation of {@code valueDomain}
	 *             over {@code cfg}
	 */
	Collection<AnalyzedCFG<A>> getAnalysisResultsOf(
			CFG cfg);

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
	AnalysisState<A> getAbstractResultOf(
			CFGCall call,
			AnalysisState<A> entryState,
			ExpressionSet[] parameters,
			StatementStore<A> expressions)
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
	AnalysisState<A> getAbstractResultOf(
			OpenCall call,
			AnalysisState<A> entryState,
			ExpressionSet[] parameters,
			StatementStore<A> expressions)
			throws SemanticException;

	/**
	 * Yields a {@link Call} implementation that corresponds to the resolution
	 * of the given {@link UnresolvedCall}. This method will forward the call to
	 * {@link CallGraph#resolve(UnresolvedCall, Set[], SymbolAliasing)} if
	 * needed.
	 *
	 * @param call     the call to resolve
	 * @param types    the runtime types of the parameters of the call
	 * @param aliasing the symbol aliasing information
	 * 
	 * @return a collection of all the possible runtime targets
	 *
	 * @throws CallResolutionException if this analysis is unable to resolve the
	 *                                     given call
	 */
	Call resolve(
			UnresolvedCall call,
			Set<Type>[] types,
			SymbolAliasing aliasing)
			throws CallResolutionException;

	/**
	 * Yields the results of the fixpoint computation over the whole
	 * application.
	 * 
	 * @return the results of the fixpoint
	 */
	FixpointResults<A> getFixpointResults();

}
