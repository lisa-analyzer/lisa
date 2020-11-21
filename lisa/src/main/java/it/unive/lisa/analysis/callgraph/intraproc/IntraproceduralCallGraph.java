package it.unive.lisa.analysis.callgraph.intraproc;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.CFGWithAnalysisResults;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.analysis.callgraph.CallGraph;
import it.unive.lisa.cfg.CFG;
import it.unive.lisa.cfg.FixpointException;
import it.unive.lisa.cfg.statement.CFGCall;
import it.unive.lisa.logging.IterationLogger;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.ValueIdentifier;

public class IntraproceduralCallGraph implements CallGraph {

	private static final Logger log = LogManager.getLogger(IntraproceduralCallGraph.class);

	private final Map<CFG, CFGWithAnalysisResults<?, ?>> results;

	public IntraproceduralCallGraph() {
		this.results = new ConcurrentHashMap<>();
	}
	
	public void addCFG(CFG cfg) {
		results.put(cfg, null);
	}

	@Override
	public Collection<CFG> resolve(CFGCall call) {
		return Collections.singleton(call.getTarget());
	}

	@Override
	public <H extends HeapDomain<H>, V extends ValueDomain<V>> void fixpoint(AnalysisState<H, V> entryState)
			throws FixpointException {
		for (CFG cfg : IterationLogger.iterate(log, results.keySet(), "Computing fixpoint over the whole program",
				"cfgs"))
			results.put(cfg, cfg.fixpoint(entryState, this));
	}

	@Override
	@SuppressWarnings("unchecked")
	public <H extends HeapDomain<H>, V extends ValueDomain<V>> CFGWithAnalysisResults<H, V> getAnalysisResultsOf(
			CFG cfg) {
		return (CFGWithAnalysisResults<H, V>) results.get(cfg);
	}

	@Override
	public <H extends HeapDomain<H>, V extends ValueDomain<V>> AnalysisState<H, V> getAbstractResultOf(CFGCall call,
			AnalysisState<H, V> entryState, SymbolicExpression[] parameters) throws SemanticException {
		if (call.getStaticType().isVoidType())
			return entryState.top();

		return new AnalysisState<>(entryState.getState().top(), new ValueIdentifier("ret_value"));
	}

}
