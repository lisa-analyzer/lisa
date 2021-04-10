package it.unive.lisa.checks.semantic;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.CFGWithAnalysisResults;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.checks.syntactic.CheckTool;
import it.unive.lisa.program.cfg.CFG;
import java.util.Map;

/**
 * An extension of {@link CheckTool} that also contains the results of the
 * fixpoint computation.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractState} contained in the results
 * @param <H> the type of {@link HeapDomain} contained in the results
 * @param <V> the type of {@link ValueDomain} contained in the results
 */
public class CheckToolWithAnalysisResults<A extends AbstractState<A, H, V>,
		H extends HeapDomain<H>,
		V extends ValueDomain<V>> extends CheckTool {

	private final Map<CFG, CFGWithAnalysisResults<A, H, V>> results;

	/**
	 * Builds the tool, storing the given results.
	 * 
	 * @param results the results to store
	 */
	public CheckToolWithAnalysisResults(Map<CFG, CFGWithAnalysisResults<A, H, V>> results) {
		super();
		this.results = results;
	}

	/**
	 * Builds the tool, copying the given tool and storing the given results.
	 * 
	 * @param other   the tool to copy
	 * @param results the results to store
	 */
	public CheckToolWithAnalysisResults(CheckTool other, Map<CFG, CFGWithAnalysisResults<A, H, V>> results) {
		super(other);
		this.results = results;
	}

	/**
	 * Yields the analysis results stored in this tool for the given
	 * {@link CFG}.
	 * 
	 * @param cfg the cfg whose results are to be retrieved
	 * 
	 * @return the results on the given cfg
	 */
	public CFGWithAnalysisResults<A, H, V> getResultOf(CFG cfg) {
		return results.get(cfg);
	}
}
