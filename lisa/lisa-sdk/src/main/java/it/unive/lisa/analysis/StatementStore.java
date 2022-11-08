package it.unive.lisa.analysis;

import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.lattices.FunctionalLattice;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.analysis.value.ValueDomain;
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
 * @param <H> the type of the {@link HeapDomain}
 * @param <V> the type of the {@link ValueDomain}
 * @param <T> the type of {@link TypeDomain}
 */
public class StatementStore<A extends AbstractState<A, H, V, T>,
		H extends HeapDomain<H>,
		V extends ValueDomain<V>,
		T extends TypeDomain<T>>
		extends FunctionalLattice<StatementStore<A, H, V, T>, Statement, AnalysisState<A, H, V, T>> {

	/**
	 * Builds the store.
	 * 
	 * @param state an instance of the underlying lattice
	 */
	public StatementStore(AnalysisState<A, H, V, T> state) {
		super(state);
	}

	private StatementStore(AnalysisState<A, H, V, T> state, Map<Statement, AnalysisState<A, H, V, T>> function) {
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
	public AnalysisState<A, H, V, T> put(Statement st, AnalysisState<A, H, V, T> state) {
		if (function == null)
			function = mkNewFunction(null, false);
		return function.put(st, state);
	}

	/**
	 * Removes the stored state for the given statement.
	 * 
	 * @param st the statement whose state needs to be forgotten
	 */
	public void forget(Statement st) {
		if (function == null)
			return;
		function.remove(st);
		if (function.isEmpty())
			function = null;
	}

	@Override
	public StatementStore<A, H, V, T> top() {
		return new StatementStore<>(lattice.top());
	}

	@Override
	public StatementStore<A, H, V, T> bottom() {
		return new StatementStore<>(lattice.bottom());
	}

	@Override
	public StatementStore<A, H, V, T> mk(AnalysisState<A, H, V, T> lattice,
			Map<Statement, AnalysisState<A, H, V, T>> function) {
		return new StatementStore<>(lattice, function);
	}
}
