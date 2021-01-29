package it.unive.lisa.analysis;

/**
 * A base implementation of the {@link Lattice} interface, handling base cases
 * of the methods exposed by that interface. All implementers of {@link Lattice}
 * should inherit from this class for ensuring a consistent behavior on the base
 * cases, unless explicitly needed.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <L> the concrete {@link BaseLattice} instance
 */
public abstract class BaseLattice<L extends BaseLattice<L>> implements Lattice<L> {

	@Override
	@SuppressWarnings("unchecked")
	public final L lub(L other) throws SemanticException {
		if (other == null || other.isBottom() || this.isTop() || this == other || this.equals(other))
			return (L) this;

		if (this.isBottom() || other.isTop())
			return other;

		return lubAux(other);
	}

	/**
	 * Performs the least upper bound operation between this lattice element and
	 * the given one, assuming that base cases have already been handled. In
	 * particular, it is guaranteed that:
	 * <ul>
	 * <li>{@code other} is not {@code null}</li>
	 * <li>{@code other} is neither <i>top</i> nor <i>bottom</i></li>
	 * <li>{@code this} is neither <i>top</i> nor <i>bottom</i></li>
	 * <li>{@code this} and {@code other} are not the same object (according
	 * both to {@code ==} and to {@link Object#equals(Object)})</li>
	 * </ul>
	 * 
	 * @param other the other lattice element
	 * 
	 * @return the widening between this and other
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	protected abstract L lubAux(L other) throws SemanticException;

	@Override
	@SuppressWarnings("unchecked")
	public final L widening(L other) throws SemanticException {
		if (other == null || other.isBottom() || this.isTop() || this == other || this.equals(other))
			return (L) this;

		if (this.isBottom() || other.isTop())
			return other;

		return wideningAux(other);
	}

	/**
	 * Performs the widening operation between this lattice element and the
	 * given one, assuming that base cases have already been handled. In
	 * particular, it is guaranteed that:
	 * <ul>
	 * <li>{@code other} is not {@code null}</li>
	 * <li>{@code other} is neither <i>top</i> nor <i>bottom</i></li>
	 * <li>{@code this} is neither <i>top</i> nor <i>bottom</i></li>
	 * <li>{@code this} and {@code other} are not the same object (according
	 * both to {@code ==} and to {@link Object#equals(Object)})</li>
	 * </ul>
	 * 
	 * @param other the other lattice element
	 * 
	 * @return the least upper bound
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	protected abstract L wideningAux(L other) throws SemanticException;

	@Override
	public final boolean lessOrEqual(L other) throws SemanticException {
		if (other == null)
			return false;

		if (this == other || this.isBottom() || other.isTop() || this.equals(other))
			return true;

		if (this.isTop() || other.isBottom())
			return false;

		return lessOrEqualAux(other);
	}

	/**
	 * Yields {@code true} if and only if this lattice element is in relation
	 * with (usually represented through &le;) the given one, assuming that base
	 * cases have already been handled. In particular, it is guaranteed that:
	 * <ul>
	 * <li>{@code other} is not {@code null}</li>
	 * <li>{@code other} is neither <i>top</i> nor <i>bottom</i></li>
	 * <li>{@code this} is neither <i>top</i> nor <i>bottom</i></li>
	 * <li>{@code this} and {@code other} are not the same object (according
	 * both to {@code ==} and to {@link Object#equals(Object)})</li>
	 * </ul>
	 * 
	 * @param other the other lattice element
	 * 
	 * @return {@code true} if and only if that condition holds
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	protected abstract boolean lessOrEqualAux(L other) throws SemanticException;

	@Override
	public abstract boolean equals(Object obj);

	@Override
	public abstract int hashCode();

	@Override
	public abstract String toString();
}
