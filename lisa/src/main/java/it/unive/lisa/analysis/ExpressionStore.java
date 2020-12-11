package it.unive.lisa.analysis;

import it.unive.lisa.cfg.statement.Expression;

/**
 * A functional lattice that stores instances of {@link Lattice} computed on
 * intermediate expressions inside root statements. Storing states in such an
 * object enables easy fixpoint computation thanks to the function lub and
 * widening operations
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <V> the type of lattice mapped from this store
 */
public class ExpressionStore<V extends Lattice<V>> extends FunctionalLattice<ExpressionStore<V>, Expression, V> {

	/**
	 * Builds the store.
	 * 
	 * @param lattice an instance of the concrete lattice that will be mapped in
	 *                    this store
	 */
	public ExpressionStore(V lattice) {
		super(lattice);
	}

	/**
	 * Stores the given lattice element for the given expression. This is a
	 * "forced" update, without performing any lattice operation if a mapping
	 * for the given expression already exists.
	 * 
	 * @param expression the expression whose state needs to be set
	 * @param state      the state to set
	 * 
	 * @return the previous state mapped to {@code expression}, or {@code null}
	 */
	public V put(Expression expression, V state) {
		return function.put(expression, state);
	}

	@Override
	public ExpressionStore<V> top() {
		return new ExpressionStore<>(lattice.top());
	}

	@Override
	public boolean isTop() {
		return lattice.isTop() && (function == null || function.isEmpty());
	}

	@Override
	public ExpressionStore<V> bottom() {
		return new ExpressionStore<>(lattice.bottom());
	}

	@Override
	public boolean isBottom() {
		return lattice.isBottom() && (function == null || function.isEmpty());
	}
}
