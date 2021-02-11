package it.unive.lisa.analysis;

/**
 * An interface for elements that follow a lattice structure. Implementers of
 * this interface should inherit from {@link BaseLattice}, unless explicitly
 * needed.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <L> the concrete {@link Lattice} instance
 */
public interface Lattice<L extends Lattice<L>> {

	/**
	 * A string constant that can be used to represent top values.
	 */
	public static final String TOP_STRING = "#TOP#";

	/**
	 * A string constant that can be used to represent bottom values.
	 */
	public static final String BOTTOM_STRING = "_|_";

	/**
	 * Performs the least upper bound operation between this lattice element and
	 * the given one. This operation is commutative.
	 * 
	 * @param other the other lattice element
	 * 
	 * @return the least upper bound
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	L lub(L other) throws SemanticException;

	/**
	 * Performs the widening operation between this lattice element and the
	 * given one. This operation is not commutative.
	 * 
	 * @param other the other lattice element
	 * 
	 * @return the widening between this and other
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	L widening(L other) throws SemanticException;

	/**
	 * Yields {@code true} if and only if this lattice element is in relation
	 * with (usually represented through &le;) the given one. This operation is
	 * not commutative.
	 * 
	 * @param other the other lattice element
	 * 
	 * @return {@code true} if and only if that condition holds
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	boolean lessOrEqual(L other) throws SemanticException;

	/**
	 * Yields the top element of this lattice. The returned element should be
	 * unique across different calls to this method, since {@link #isTop()} uses
	 * reference equality by default. If the value returned by this method is
	 * not a singleton, override {@link #isTop()} accordingly to provide a
	 * coherent test.
	 * 
	 * @return the top element
	 */
	L top();

	/**
	 * Yields the bottom element of this lattice. The returned element should be
	 * unique across different calls to this method, since {@link #isBottom()}
	 * uses reference equality by default. If the value returned by this method
	 * is not a singleton, override {@link #isBottom()} accordingly to provide a
	 * coherent test.
	 * 
	 * @return the bottom element
	 */
	L bottom();

	/**
	 * Yields {@code true} if and only if this object represents the top of the
	 * lattice. The default implementation of this method uses reference
	 * equality between {@code this} and the value returned by {@link #top()},
	 * thus assuming that the top element is a singleton. If this is not the
	 * case, override this method accordingly to provide a coherent test.
	 * 
	 * @return {@code true} if this is the top of the lattice
	 */
	public default boolean isTop() {
		return this == top();
	}

	/**
	 * Yields {@code true} if and only if this object represents the bottom of
	 * the lattice. The default implementation of this method uses reference
	 * equality between {@code this} and the value returned by
	 * {@link #bottom()}, thus assuming that the bottom element is a singleton.
	 * If this is not the case, override this method accordingly to provide a
	 * coherent test.
	 * 
	 * @return {@code true} if this is the bottom of the lattice
	 */
	public default boolean isBottom() {
		return this == bottom();
	}
}
