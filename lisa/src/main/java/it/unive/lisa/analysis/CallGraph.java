package it.unive.lisa.analysis;

import java.util.Collection;

import it.unive.lisa.cfg.CFG;
import it.unive.lisa.cfg.statement.CFGCall;

/**
 * A callgraph of the program to analyze, that knows how to resolve dynamic
 * targets of {@link CFGCall}s.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface CallGraph {

	/**
	 * Yields a collection containing all possible runtime targets of a
	 * {@link CFGCall}.
	 * 
	 * @param call the call to resolve
	 * @return a collection of all the possible runtime targets
	 */
	Collection<CFG> resolve(CFGCall call);

	/**
	 * Computes a fixpoint over the whole control flow graph, producing a
	 * {@link CFGWithAnalysisResults} for each {@link CFG} contained in this
	 * callgraph. Each result is computed with
	 * {@link CFG#fixpoint(Collection, AnalysisState, CallGraph, it.unive.lisa.util.workset.WorkingSet, int)}.
	 * Results of individual cfgs are then available through
	 * {@link #getAnalysisResultsOf(Class, CFG)}.
	 * 
	 * @param <H>        the type of {@link HeapDomain} to compute
	 * @param <V>        the type of {@link ValueDomain} to compute
	 * @param entryState the entry state for the {@link CFG}s that are the
	 *                   entrypoints of the computation
	 */
	<H extends HeapDomain<H>, V extends ValueDomain<V>> void fixpoint(AnalysisState<H, V> entryState);

	/**
	 * Yields the results of the given analysis, identified by its class, on the
	 * given {@link CFG}. Results are provided as {@link CFGWithAnalysisResults}.
	 * 
	 * @param <H>         the type of {@link HeapDomain} contained into the computed
	 *                    abstract state
	 * @param <V>         the type of {@link ValueDomain} contained into the
	 *                    computed abstract state
	 * @param valueDomain the class of the {@link ValueDomain} whose results needs
	 *                    to be retrieved
	 * @param cfg         the cfg whose fixpoint results needs to be retrieved
	 * @return the result of the fixpoint computation of {@code valueDomain} over
	 *         {@code cfg}
	 */
	<H extends HeapDomain<H>, V extends ValueDomain<V>> CFGWithAnalysisResults<H, V> getAnalysisResultsOf(
			Class<V> valueDomain, CFG cfg);
}
