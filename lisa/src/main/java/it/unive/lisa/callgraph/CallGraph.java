package it.unive.lisa.callgraph;

import it.unive.lisa.DefaultImplementation;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.CFGWithAnalysisResults;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.callgraph.impl.intraproc.IntraproceduralCallGraph;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.CFGCall;
import it.unive.lisa.program.cfg.statement.Call;
import it.unive.lisa.program.cfg.statement.OpenCall;
import it.unive.lisa.program.cfg.statement.UnresolvedCall;
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
@DefaultImplementation(IntraproceduralCallGraph.class)
public interface CallGraph {

	/**
	 * Builds the call graph of the given program.
	 * 
	 * @param program the program to analyze
	 * 
	 * @throws CallGraphConstructionException if an exception happens while
	 *                                            building the call graph
	 */
	void build(Program program) throws CallGraphConstructionException;

	/**
	 * Yields a {@link Call} implementation that corresponds to the resolution
	 * of the given {@link UnresolvedCall}. This method will return:
	 * <ul>
	 * <li>a {@link CFGCall}, if at least one {@link CFG} that matches
	 * {@link UnresolvedCall#getTargetName()} is found. The returned
	 * {@link CFGCall} will be linked to all the possible runtime targets
	 * matching {@link UnresolvedCall#getTargetName()};</li>
	 * <li>an {@link OpenCall}, if no {@link CFG} matching
	 * {@link UnresolvedCall#getTargetName()} is found.</li>
	 * </ul>
	 * 
	 * @param call the call to resolve
	 * 
	 * @return a collection of all the possible runtime targets
	 * 
	 * @throws CallResolutionException if this call graph is unable to resolve
	 *                                     the given call
	 */
	Call resolve(UnresolvedCall call) throws CallResolutionException;

	/**
	 * Computes a fixpoint over the whole control flow graph, producing a
	 * {@link CFGWithAnalysisResults} for each {@link CFG} contained in this
	 * callgraph. Each result is computed with
	 * {@link CFG#fixpoint(AnalysisState, CallGraph)} or one of its overloads.
	 * Results of individual cfgs are then available through
	 * {@link #getAnalysisResultsOf(CFG)}.
	 * 
	 * @param <A>        the type of {@link AbstractState} to compute
	 * @param <H>        the type of {@link HeapDomain} to compute
	 * @param <V>        the type of {@link ValueDomain} to compute
	 * @param entryState the entry state for the {@link CFG}s that are the
	 *                       entrypoints of the computation
	 * 
	 * @throws FixpointException if something goes wrong while evaluating the
	 *                               fixpoint
	 */
	<A extends AbstractState<A, H, V>, H extends HeapDomain<H>, V extends ValueDomain<V>> void fixpoint(
			AnalysisState<A, H, V> entryState)
			throws FixpointException;

	/**
	 * Yields the results of the given analysis, identified by its class, on the
	 * given {@link CFG}. Results are provided as
	 * {@link CFGWithAnalysisResults}.
	 * 
	 * @param <A> the type of {@link AbstractState} contained into the analysis
	 *                state
	 * @param <H> the type of {@link HeapDomain} contained into the computed
	 *                abstract state
	 * @param <V> the type of {@link ValueDomain} contained into the computed
	 *                abstract state
	 * @param cfg the cfg whose fixpoint results needs to be retrieved
	 * 
	 * @return the result of the fixpoint computation of {@code valueDomain}
	 *             over {@code cfg}
	 */
	<A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> CFGWithAnalysisResults<A, H, V> getAnalysisResultsOf(CFG cfg);

	/**
	 * Clears all the data from the last fixpoint computation, effectively
	 * re-initializing the call graph. The call graph structure obtained throug
	 * {@link #build(Program)} is not lost.
	 */
	void clear();

	/**
	 * Resolves the given call to all of its possible runtime targets, and then
	 * computes an analysis state that abstracts the execution of the possible
	 * targets considering that they were given {@code parameters} as actual
	 * parameters. The abstract value of each parameter is computed on
	 * {@code entryState}.
	 * 
	 * @param <A>        the type of {@link AbstractState} contained into the
	 *                       analysis state
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
	<A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> AnalysisState<A, H, V> getAbstractResultOf(CFGCall call,
					AnalysisState<A, H, V> entryState, Collection<SymbolicExpression>[] parameters)
					throws SemanticException;
}
