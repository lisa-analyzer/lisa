package it.unive.lisa.analysis;

import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.statement.Statement;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

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
	 * An optional string meant to identify this specific result, based on how
	 * it has been produced
	 */
	private String id;

	/**
	 * Builds the control flow graph, storing the given mapping between nodes
	 * and fixpoint computation results.
	 * 
	 * @param cfg         the original control flow graph
	 * @param entryStates the entry state for each entry point of the cfg
	 * @param results     the results of the fixpoint computation
	 */
	public CFGWithAnalysisResults(CFG cfg, Map<Statement, AnalysisState<A, H, V>> entryStates,
			Map<Statement, AnalysisState<A, H, V>> results) {
		super(cfg);
		this.results = results;
		this.entryStates = entryStates;
	}

	/**
	 * Yields a string meant to identify this specific result, based on how it
	 * has been produced. This method might return {@code null}.
	 * 
	 * @return the identifier of this result
	 */
	public String getId() {
		return id;
	}

	/**
	 * Sets the string meant to identify this specific result, based on how it
	 * has been produced.
	 * 
	 * @param id the identifier of this result (might be {@code null})
	 */
	public void setId(String id) {
		this.id = id;
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

	public CFGWithAnalysisResults<A, H, V> lub(CFG reference, CFGWithAnalysisResults<A, H, V> other)
			throws SemanticException {
		if (!getDescriptor().equals(other.getDescriptor()))
			throw new SemanticException("Cannot perform the least upper bound of two graphs with different descriptor");

		if (!getDescriptor().equals(reference.getDescriptor()))
			throw new SemanticException("The reference CFG does not match the results that are to be lubbed");

		Map<Statement, AnalysisState<A, H, V>> entries = new HashMap<>(entryStates);
		for (Entry<Statement, AnalysisState<A, H, V>> entry : other.entryStates.entrySet())
			if (entries.containsKey(entry.getKey()))
				entries.put(entry.getKey(), entries.get(entry.getKey()).lub(entry.getValue()));
			else
				entries.put(entry.getKey(), entry.getValue());

		Map<Statement, AnalysisState<A, H, V>> results = new HashMap<>(this.results);
		for (Entry<Statement, AnalysisState<A, H, V>> entry : other.results.entrySet())
			if (results.containsKey(entry.getKey()))
				results.put(entry.getKey(), results.get(entry.getKey()).lub(entry.getValue()));
			else
				results.put(entry.getKey(), entry.getValue());

		return new CFGWithAnalysisResults<>(reference, entries, results);
	}
}
