package it.unive.lisa.checks.semantic;

import java.util.Map;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.CFGWithAnalysisResults;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.checks.syntactic.CheckTool;
import it.unive.lisa.program.cfg.CFG;

public class CheckToolWithAnalysisResults<A extends AbstractState<A, H, V>,
		H extends HeapDomain<H>,
		V extends ValueDomain<V>> extends CheckTool {

	private final Map<CFG, CFGWithAnalysisResults<A, H, V>> results;

	public CheckToolWithAnalysisResults(Map<CFG, CFGWithAnalysisResults<A, H, V>> results) {
		super();
		this.results = results;
	}

	public CheckToolWithAnalysisResults(CheckTool other, Map<CFG, CFGWithAnalysisResults<A, H, V>> results) {
		super(other);
		this.results = results;
	}

	public CFGWithAnalysisResults<A, H, V> getResultOf(CFG cfg) {
		return results.get(cfg);
	}
}
