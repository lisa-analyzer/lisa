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
public interface BaseLattice<L extends BaseLattice<L>>
		extends
		Lattice<L> {

	@Override
	default boolean lessOrEqual(
			L other)
			throws SemanticException {
		if (other == null)
			return false;

		if (this == other || this.isBottom() || other.isTop() || this.equals(other))
			return true;

		if (this.isTop() || other.isBottom())
			return false;

		return lessOrEqualAux(other);
	}

	@Override
	@SuppressWarnings("unchecked")
	default L lub(
			L other)
			throws SemanticException {
		if (other == null || other.isBottom() || this.isTop() || this == other || this.equals(other))
			return (L) this;

		if (this.isBottom() || other.isTop())
			return other;

		return lubAux(other);
	}

	@Override
	@SuppressWarnings("unchecked")
	default L chain(
			L other)
			throws SemanticException {
		if (other == null || other.isBottom() || this.isTop() || this == other || this.equals(other))
			return (L) this;

		if (this.isBottom() || other.isTop())
			return other;

		return chainAux(other);
	}

	@Override
	@SuppressWarnings("unchecked")
	default L glb(
			L other)
			throws SemanticException {
		if (other == null || this.isBottom() || other.isTop() || this == other || this.equals(other))
			return (L) this;

		if (other.isBottom() || this.isTop())
			return other;

		return glbAux(other);
	}

	@Override
	@SuppressWarnings("unchecked")
	default L widening(
			L other)
			throws SemanticException {
		if (other == null || other.isBottom() || this.isTop() || this == other || this.equals(other))
			return (L) this;

		if (this.isBottom() || other.isTop())
			return other;

		return wideningAux(other);
	}

	@Override
	@SuppressWarnings("unchecked")
	default L narrowing(
			L other)
			throws SemanticException {
		if (other == null || this.isBottom() || this == other || this.equals(other))
			return (L) this;

		if (this.isTop() || other.isBottom())
			return other;

		return narrowingAux(other);
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
	 * @return the least upper bound between this and other
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	L lubAux(
			L other)
			throws SemanticException;

	/**
	 * Performs the chain operation (see {@link Lattice#chain(Lattice)} for more
	 * information} between this lattice element and the given one, assuming
	 * that base cases have already been handled. In particular, it is
	 * guaranteed that:
	 * <ul>
	 * <li>{@code other} is not {@code null}</li>
	 * <li>{@code other} is neither <i>top</i> nor <i>bottom</i></li>
	 * <li>{@code this} is neither <i>top</i> nor <i>bottom</i></li>
	 * <li>{@code this} and {@code other} are not the same object (according
	 * both to {@code ==} and to {@link Object#equals(Object)})</li>
	 * </ul>
	 * The default implementation of this method delegates to
	 * {@link #lubAux(BaseLattice)}.
	 * 
	 * @param other the other lattice element
	 * 
	 * @return the chain between this and other
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	default L chainAux(
			L other)
			throws SemanticException {
		return lubAux(other);
	}

	/**
	 * Performs the greatest lower bound operation between this lattice element
	 * and the given one, assuming that base cases have already been handled. In
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
	 * @return the greatest lower bound between this and other
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	default L glbAux(
			L other)
			throws SemanticException {
		return bottom();
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
	 * The default implementation of this method delegates to
	 * {@link #lubAux(BaseLattice)}, and is thus safe for finite lattices and
	 * ACC ones.
	 * 
	 * @param other the other lattice element
	 * 
	 * @return the widening between this and other
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	default L wideningAux(
			L other)
			throws SemanticException {
		return lubAux(other);
	}

	/**
	 * Performs the narrowing operation between this lattice element and the
	 * given one, assuming that base cases have already been handled. In
	 * particular, it is guaranteed that:
	 * <ul>
	 * <li>{@code other} is not {@code null}</li>
	 * <li>{@code other} is neither <i>top</i> nor <i>bottom</i></li>
	 * <li>{@code this} is neither <i>top</i> nor <i>bottom</i></li>
	 * <li>{@code this} and {@code other} are not the same object (according
	 * both to {@code ==} and to {@link Object#equals(Object)})</li>
	 * </ul>
	 * The default implementation of this method delegates to
	 * {@link #glbAux(BaseLattice)}, and is thus safe for finite lattices and
	 * DCC ones.
	 * 
	 * @param other the other lattice element
	 * 
	 * @return the narrowing between this and other
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	default L narrowingAux(
			L other)
			throws SemanticException {
		return glbAux(other);
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
	boolean lessOrEqualAux(
			L other)
			throws SemanticException;

	@Override
	boolean equals(
			Object obj);

	@Override
	int hashCode();

	@Override
	String toString();

}
