package it.unive.lisa.analysis;

import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;

/**
 * A generic Cartesian product abstract domain between two non-communicating
 * {@link SemanticDomain}s (i.e., no exchange of information between the
 * abstract domains), assigning the same {@link Identifier}s and handling
 * instances of the same {@link SymbolicExpression}s.
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 *
 * @param <T1> the concrete instance of the left-hand side abstract domain of
 *                 the Cartesian product
 * @param <T2> the concrete instance of the right-hand side abstract domain of
 *                 the Cartesian product
 * @param <E>  the type of {@link SymbolicExpression} that {@code <T1>} and
 *                 {@code <T2>}, and in turn this domain, can process
 * @param <I>  the type of {@link Identifier} that {@code <T1>} and
 *                 {@code <T2>}, and in turn this domain, handle
 */
public abstract class CartesianProduct<T1 extends SemanticDomain<T1, E, I>,
		T2 extends SemanticDomain<T2, E, I>,
		E extends SymbolicExpression,
		I extends Identifier> {

	/**
	 * The left-hand side abstract domain.
	 */
	protected T1 left;

	/**
	 * The right-hand side abstract domain.
	 */
	protected T2 right;

	/**
	 * Builds the Cartesian product abstract domain.
	 * 
	 * @param left  the left-hand side of the Cartesian product
	 * @param right the right-hand side of the Cartesian product
	 */
	protected CartesianProduct(T1 left, T2 right) {
		this.left = left;
		this.right = right;
	}

	@Override
	public String toString() {
		return representation();
	}

	/**
	 * Yields a textual representation of the content of this domain's instance.
	 * 
	 * @return the textual representation
	 */
	protected abstract String representation();

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((left == null) ? 0 : left.hashCode());
		result = prime * result + ((right == null) ? 0 : right.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CartesianProduct<?, ?, ?, ?> other = (CartesianProduct<?, ?, ?, ?>) obj;
		if (left == null) {
			if (other.left != null)
				return false;
		} else if (!left.equals(other.left))
			return false;
		if (right == null) {
			if (other.right != null)
				return false;
		} else if (!right.equals(other.right))
			return false;
		return true;
	}
}