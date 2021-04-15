package it.unive.lisa.analysis.lattices;

import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Skip;
import java.util.Collections;
import java.util.Set;

/**
 * A set lattice containing a set of symbolic expressions.
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 *
 * @param <T> the type of the tracked symbolic expressions
 */
public class ExpressionSet<T extends SymbolicExpression> extends SetLattice<ExpressionSet<T>, T> {

	/**
	 * Builds the empty set lattice element.
	 */
	public ExpressionSet() {
		this(Collections.emptySet());
	}

	/**
	 * Builds a singleton set lattice element.
	 * 
	 * @param exp the expression
	 */
	public ExpressionSet(T exp) {
		this(Collections.singleton(exp));
	}

	/**
	 * Builds a set lattice element.
	 * 
	 * @param set the set of expression
	 */
	public ExpressionSet(Set<T> set) {
		super(set);
	}

	@Override
	@SuppressWarnings("unchecked")
	public ExpressionSet<T> top() {
		return new ExpressionSet<T>((T) new Skip());
	}

	@Override
	public ExpressionSet<T> bottom() {
		return new ExpressionSet<T>();
	}

	@Override
	protected ExpressionSet<T> mk(Set<T> set) {
		return new ExpressionSet<T>(set);
	}
}
