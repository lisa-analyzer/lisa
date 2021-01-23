package it.unive.lisa.analysis;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.statement.Statement;
import java.util.Map;

/**
 * A control flow graph, that has {@link Statement}s as nodes and {@link Edge}s
 * as edges. It also maps each statement (and its inner expressions) to the
 * result of a fixpoint computation, in the form of an {@link AnalysisState}
 * instance.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractState} contained into the analysis
 *                state
 * @param <H> the type of {@link HeapDomain} contained into the computed
 *                abstract state
 * @param <V> the type of {@link ValueDomain} contained into the computed
 *                abstract state
 */
public class CFGWithAnalysisResults<A extends AbstractState<A, H, V>, H extends HeapDomain<H>, V extends ValueDomain<V>>
		extends CFG {

	/**
	 * The map storing the analysis results
	 */
	private final Map<Statement, AnalysisState<A, H, V>> results;

	/**
	 * Builds the control flow graph, storing the given mapping between nodes
	 * and fixpoint computation results.
	 * 
	 * @param cfg     the original control flow graph
	 * @param results the results of the fixpoint computation
	 */
	public CFGWithAnalysisResults(CFG cfg, Map<Statement, AnalysisState<A, H, V>> results) {
		super(cfg);
		this.results = results;
	}

	/**
	 * Yields the computed result at a given statement.
	 * 
	 * @param st the statement
	 * 
	 * @return the result computed at the given statement
	 */
	public final AnalysisState<A, H, V> getAnalysisStateAt(Statement st) {
		return results.get(st);
	}
}
