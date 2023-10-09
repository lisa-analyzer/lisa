package it.unive.lisa.analysis;

import it.unive.lisa.analysis.lattices.FunctionalLattice;
import it.unive.lisa.program.cfg.statement.Statement;
import java.util.Map;

/**
 * A functional lattice that stores instances of {@link AnalysisState} computed
 * on statements. Storing states in such an object enables easy fixpoint
 * computation thanks to the function lub and widening operations.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractState}
 */
public class StatementStore<A extends AbstractState<A>>
		extends
		FunctionalLattice<StatementStore<A>, Statement, AnalysisState<A>> {

	/**
	 * Builds the store.
	 * 
	 * @param state an instance of the underlying lattice
	 */
	public StatementStore(
			AnalysisState<A> state) {
		super(state);
	}

	private StatementStore(
			AnalysisState<A> state,
			Map<Statement, AnalysisState<A>> function) {
		super(state, function);
	}

	/**
	 * Stores the given state for the given statement. This is a "forced"
	 * update, without performing any lattice operation if a mapping for the
	 * given expression already exists.
	 * 
	 * @param st    the statement whose state needs to be set
	 * @param state the state to set
	 * 
	 * @return the previous state mapped to {@code expression}, or {@code null}
	 */
	public AnalysisState<A> put(
			Statement st,
			AnalysisState<A> state) {
		if (function == null)
			function = mkNewFunction(null, false);
		return function.put(st, state);
	}

	/**
	 * Removes the stored state for the given statement.
	 * 
	 * @param st the statement whose state needs to be forgotten
	 */
	public void forget(
			Statement st) {
		if (function == null)
			return;
		function.remove(st);
		if (function.isEmpty())
			function = null;
	}

	@Override
	public StatementStore<A> top() {
		return new StatementStore<>(lattice.top());
	}

	@Override
	public StatementStore<A> bottom() {
		return new StatementStore<>(lattice.bottom());
	}

	@Override
	public StatementStore<A> mk(
			AnalysisState<A> lattice,
			Map<Statement, AnalysisState<A>> function) {
		return new StatementStore<>(lattice, function);
	}

	@Override
	public AnalysisState<A> stateOfUnknown(
			Statement key) {
		return lattice.bottom();
	}
}
