package it.unive.lisa.analysis;

import java.util.Collection;
import java.util.Map;

import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.statement.Statement;

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
	 * The map storing the entry state of each entry point
	 */
	private final Map<Statement, AnalysisState<A, H, V>> entryStates;

	/**
	 * Builds the control flow graph, storing the given mapping between nodes
	 * and fixpoint computation results.
	 * 
	 * @param cfg     the original control flow graph
	 * @param results the results of the fixpoint computation
	 */
	public CFGWithAnalysisResults(CFG cfg, Map<Statement, AnalysisState<A, H, V>> entryStates,
			Map<Statement, AnalysisState<A, H, V>> results) {
		super(cfg);
		this.results = results;
		this.entryStates = entryStates;
	}

	/**
	 * Yields the computed result before a given statement (entry state).
	 *
	 * @param st the statement
	 *
	 * @return the result computed before the given statement
	 * 
	 * @throws SemanticException if the lub operator fails
	 */
	public final AnalysisState<A, H, V> getAnalysisStateBefore(Statement st) throws SemanticException {
		if (getEntrypoints().contains(st))
			return entryStates.get(st);
		return lub(predecessorsOf(st), false);
	}

	/**
	 * Yields the computed result at a given statement (exit state).
	 *
	 * @param st the statement
	 *
	 * @return the result computed at the given statement
	 */
	public final AnalysisState<A, H, V> getAnalysisStateAfter(Statement st) {
		return results.get(st);
	}

	/**
	 * Yields the entry state.
	 * 
	 * @return the entry state of the CFG
	 * 
	 * @throws SemanticException if the lub operator fails
	 */
	public final AnalysisState<A, H, V> getEntryState() throws SemanticException {
		return lub(this.getEntrypoints(), true);
	}

	/**
	 * Yields the exit state.
	 * 
	 * @return the entry state of the CFG
	 * 
	 * @throws SemanticException if the lub operator fails
	 */
	public final AnalysisState<A, H, V> getExitState() throws SemanticException {
		return lub(this.getNormalExitpoints(), false);
	}

	private AnalysisState<A, H, V> lub(Collection<Statement> statements, boolean entry) throws SemanticException {
		AnalysisState<A, H, V> result = null;
		for (Statement st : statements)
			if (result == null)
				result = entry ? getAnalysisStateBefore(st) : getAnalysisStateAfter(st);
			else
				result = result.lub(entry ? getAnalysisStateBefore(st) : getAnalysisStateAfter(st));
		return result;

	}

}
