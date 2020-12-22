package it.unive.lisa.callgraph;

import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.CFGWithAnalysisResults;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.cfg.CFG;
import it.unive.lisa.cfg.CFG.SemanticFunction;
import it.unive.lisa.cfg.statement.CFGCall;
import it.unive.lisa.cfg.statement.Call;
import it.unive.lisa.cfg.statement.OpenCall;
import it.unive.lisa.cfg.statement.UnresolvedCall;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.util.datastructures.graph.FixpointException;
import java.util.Collection;

/**
 * A callgraph of the program to analyze, that knows how to resolve dynamic
 * targets of {@link UnresolvedCall}s.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface CallGraph {

	/**
	 * Adds a new cfg to this call graph.
	 * 
	 * @param cfg the cfg to add
	 */
	void addCFG(CFG cfg);

	/**
	 * Yields a {@link Call} implementation that corresponds to the resolution
	 * of the given {@link UnresolvedCall}. This method will return:
	 * <ul>
	 * <li>a {@link CFGCall}, if at least one {@link CFG} that matches
	 * {@link UnresolvedCall#getQualifiedName()} is found. The returned
	 * {@link CFGCall} will be linked to all the possible runtime targets
	 * matching {@link UnresolvedCall#getQualifiedName()};</li>
	 * <li>an {@link OpenCall}, if no {@link CFG} matching
	 * {@link UnresolvedCall#getQualifiedName()} is found.</li>
	 * </ul>
	 * 
	 * @param call the call to resolve
	 * 
	 * @return a collection of all the possible runtime targets
	 */
	Call resolve(UnresolvedCall call);

	/**
	 * Computes a fixpoint over the whole control flow graph, producing a
	 * {@link CFGWithAnalysisResults} for each {@link CFG} contained in this
	 * callgraph. Each result is computed with
	 * {@link CFG#fixpoint(AnalysisState, CallGraph, SemanticFunction)} or one
	 * of its overloads. Results of individual cfgs are then available through
	 * {@link #getAnalysisResultsOf(CFG)}.
	 * 
	 * @param <H>        the type of {@link HeapDomain} to compute
	 * @param <V>        the type of {@link ValueDomain} to compute
	 * @param entryState the entry state for the {@link CFG}s that are the
	 *                       entrypoints of the computation
	 * @param semantics  the {@link SemanticFunction} that will be used for
	 *                       computing the abstract post-state of statements
	 * 
	 * @throws FixpointException if something goes wrong while evaluating the
	 *                               fixpoint
	 */
	<H extends HeapDomain<H>, V extends ValueDomain<V>> void fixpoint(AnalysisState<H, V> entryState,
			SemanticFunction<H, V> semantics)
			throws FixpointException;

	/**
	 * Yields the results of the given analysis, identified by its class, on the
	 * given {@link CFG}. Results are provided as
	 * {@link CFGWithAnalysisResults}.
	 * 
	 * @param <H> the type of {@link HeapDomain} contained into the computed
	 *                abstract state
	 * @param <V> the type of {@link ValueDomain} contained into the computed
	 *                abstract state
	 * @param cfg the cfg whose fixpoint results needs to be retrieved
	 * 
	 * @return the result of the fixpoint computation of {@code valueDomain}
	 *             over {@code cfg}
	 */
	<H extends HeapDomain<H>, V extends ValueDomain<V>> CFGWithAnalysisResults<H, V> getAnalysisResultsOf(CFG cfg);

	/**
	 * Clears all the data from the last fixpoint computation, effectively
	 * re-initializing the call graph. The set of {@link CFG} under analysis
	 * (added through {@link #addCFG(CFG)}) is not lost.
	 */
	void clear();

	/**
	 * Resolves the given call to all of its possible runtime targets, and then
	 * computes an analysis state that abstracts the execution of the possible
	 * targets considering that they were given {@code parameters} as actual
	 * parameters. The abstract value of each parameter is computed on
	 * {@code entryState}.
	 * 
	 * @param <H>        the type of {@link HeapDomain} contained into the
	 *                       computed abstract state
	 * @param <V>        the type of {@link ValueDomain} contained into the
	 *                       computed abstract state
	 * @param call       the call to resolve and evaluate
	 * @param entryState the abstract analysis state when the call is reached
	 * @param parameters the expressions representing the actual parameters of
	 *                       the call
	 * 
	 * @return an abstract analysis state representing the abstract result of
	 *             the cfg call. The
	 *             {@link AnalysisState#getComputedExpressions()} will contain
	 *             an {@link Identifier} pointing to the meta variable
	 *             containing the abstraction of the returned value
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	<H extends HeapDomain<H>, V extends ValueDomain<V>> AnalysisState<H, V> getAbstractResultOf(CFGCall call,
			AnalysisState<H, V> entryState, Collection<SymbolicExpression>[] parameters) throws SemanticException;
}
