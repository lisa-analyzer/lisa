package it.unive.lisa.analysis;

import java.util.Map;

import it.unive.lisa.cfg.CFG;
import it.unive.lisa.cfg.edge.Edge;
import it.unive.lisa.cfg.statement.Statement;

/**
 * A control flow graph, that has {@link Statement}s as nodes and {@link Edge}s
 * as edges. It also maps each expression to the result of a fixpoint
 * computation, in the form of an {@link AnalysisState} instance.
 * 
 * @param <H> the type of {@link HeapDomain} contained into the computed
 *            abstract state
 * @param <V> the type of {@link ValueDomain} contained into the computed
 *            abstract state
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class CFGWithAnalysisResults<H extends HeapDomain<H>, V extends ValueDomain<V>> extends CFG {

	/**
	 * The map storing the analysis results
	 */
	private final Map<Statement, AnalysisState<H, V>> results; // TODO should this be an abstract state?

	/**
	 * Builds the control flow graph, storing the given mapping between nodes and
	 * fixpoint computation results.
	 * 
	 * @param cfg     the original control flow graph
	 * @param results the results of the fixpoint computation
	 */
	public CFGWithAnalysisResults(CFG cfg, Map<Statement, AnalysisState<H, V>> results) {
		super(cfg);
		this.results = results;
	}

	/**
	 * Yields the computed result at a given statement.
	 * 
	 * @param st the statement
	 * @return the result computed at the given statement
	 */
	public final AnalysisState<H, V> getAnalysisStateAt(Statement st) {
		return results.get(st);
	}
}
