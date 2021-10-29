package it.unive.lisa.checks.semantic;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.CFGWithAnalysisResults;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.checks.syntactic.CheckTool;
import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.statement.call.Call;
import java.util.Collection;
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

	private final Map<CFG, Collection<CFGWithAnalysisResults<A, H, V>>> results;

	private final CallGraph callgraph;

	/**
	 * Builds the tool, storing the given results.
	 * 
	 * @param results   the results to store
	 * @param callgraph the callgraph that has been built during the analysis
	 */
	public CheckToolWithAnalysisResults(Map<CFG, Collection<CFGWithAnalysisResults<A, H, V>>> results,
			CallGraph callgraph) {
		this.results = results;
		this.callgraph = callgraph;
	}

	/**
	 * Builds the tool, copying the given tool and storing the given results.
	 * 
	 * @param other     the tool to copy
	 * @param results   the results to store
	 * @param callgraph the callgraph that has been built during the analysis
	 */
	public CheckToolWithAnalysisResults(CheckTool other,
			Map<CFG, Collection<CFGWithAnalysisResults<A, H, V>>> results,
			CallGraph callgraph) {
		super(other);
		this.results = results;
		this.callgraph = callgraph;
	}

	/**
	 * Yields the analysis results stored in this tool for the given
	 * {@link CFG}.
	 * 
	 * @param cfg the cfg whose results are to be retrieved
	 * 
	 * @return the results on the given cfg
	 */
	public Collection<CFGWithAnalysisResults<A, H, V>> getResultOf(CFG cfg) {
		return results.get(cfg);
	}

	/**
	 * Yields all the {@link CodeMember}s that call the given one, according to
	 * the {@link CallGraph} that has been built during the analysis.
	 * 
	 * @param cm the target code member
	 * 
	 * @return the collection of callers code members
	 */
	public Collection<CodeMember> getCallers(CodeMember cm) {
		return callgraph.getCallers(cm);
	}

	/**
	 * Yields all the {@link CodeMember}s that are called by the given one,
	 * according to the {@link CallGraph} that has been built during the
	 * analysis.
	 * 
	 * @param cm the target code member
	 * 
	 * @return the collection of called code members
	 */
	public Collection<CodeMember> getCallees(CodeMember cm) {
		return callgraph.getCallees(cm);
	}

	/**
	 * Yields all the {@link Call}s that targets the given {@link CodeMember},
	 * according to the {@link CallGraph} that has been built during the
	 * analysis.
	 * 
	 * @param cm the target code member
	 * 
	 * @return the collection of calls that target the code member
	 */
	public Collection<Call> getCallSites(CodeMember cm) {
		return callgraph.getCallSites(cm);
	}
}
