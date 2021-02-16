package it.unive.lisa.interprocedural;

import it.unive.lisa.DefaultImplementation;
import it.unive.lisa.analysis.*;
import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.interprocedural.callgraph.CallResolutionException;
import it.unive.lisa.interprocedural.callgraph.impl.RTACallGraph;
import it.unive.lisa.interprocedural.impl.ModularWorstCaseAnalysis;
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

@DefaultImplementation(ModularWorstCaseAnalysis.class)
public interface InterproceduralAnalysis {


    /**
     * Builds the interprocedural analysis of the given program.
     *
     * @param callgraph the callgraph used to resolve method calls
     *
     * @throws InterproceduralAnalysisException if an exception happens while
     *                                            performing the interprocedural analysis
     */
    void build(Program program, CallGraph callgraph) throws InterproceduralAnalysisException;


    /**
     * Computes a fixpoint over the whole control flow graph, producing a
     * {@link CFGWithAnalysisResults} for each {@link CFG} contained in this
     * callgraph. Each result is computed with
     * {@link CFG#fixpoint(AnalysisState, InterproceduralAnalysis)} or one of its overloads.
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
     * {@link #build(Program, CallGraph)} is not lost.
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
     * @param unresolvedCall the call to resolve
     *
     * @return a collection of all the possible runtime targets
     *
     * @throws CallResolutionException if this call graph is unable to resolve
     *                                     the given call
     */
    Call resolve(UnresolvedCall unresolvedCall) throws CallResolutionException;
}
