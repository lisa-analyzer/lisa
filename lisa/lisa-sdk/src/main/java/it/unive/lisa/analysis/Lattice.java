package it.unive.lisa.analysis;

import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.util.collections.IterableArray;
import it.unive.lisa.util.functional.BiFunction;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredObject;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Collection;
import java.util.HashSet;

/**
 * An interface for elements that follow a lattice structure. Implementers of
 * this interface should inherit from {@link BaseLattice}, unless explicitly
 * needed.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <L> the concrete {@link Lattice} instance
 */
public interface Lattice<L extends Lattice<L>>
		extends
		StructuredObject {

	/**
	 * A string constant that can be used to represent top values.
	 */
	String TOP_STRING = "#TOP#";

	/**
	 * Yields a fresh {@link StructuredRepresentation} that can be used to
	 * represent top values through {@link #representation()}.
	 * 
	 * @return the representation
	 */
	public static StructuredRepresentation topRepresentation() {
		return new StringRepresentation(TOP_STRING);
	}

	/**
	 * A string constant that can be used to represent bottom values.
	 */
	String BOTTOM_STRING = "_|_";

	/**
	 * Yields a fresh {@link StructuredRepresentation} that can be used to
	 * represent bottom values through {@link #representation()}.
	 * 
	 * @return the representation
	 */
	public static StructuredRepresentation bottomRepresentation() {
		return new StringRepresentation(BOTTOM_STRING);
	}

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
	boolean lessOrEqual(
			L other)
			throws SemanticException;

	/**
	 * Performs the least upper bound operation between this lattice element and
	 * the given one. This operation is commutative. Note that
	 * {@link #merge(Lattice)} shares most of the logic with this method: while
	 * the latter is used to explicitly merge lattices at control flow merge
	 * points, this one is used to join arbitrary elements of the lattice at any
	 * point during the analysis. The distinction is present to allow
	 * implementers to provide different behaviors for the two operations, if
	 * needed (e.g., when a closure operation should be applied only in one of
	 * the situations).
	 * 
	 * @param other the other lattice element
	 * 
	 * @return the least upper bound
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	L lub(
			L other)
			throws SemanticException;

	/**
	 * Performs the lub operation between this lattice element and the given
	 * ones, by repeatedly invoking {@link #lub(Lattice)} following the
	 * iteration order.
	 * 
	 * @param others the other lattice elements
	 * 
	 * @return the lub between this and all other lattice elements
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	@SuppressWarnings("unchecked")
	default L lub(
			L... others)
			throws SemanticException {
		return compress(
				(L) this,
				new IterableArray<>(others),
				(
						l1,
						l2) -> l1.lub(l2));
	}

	/**
	 * Performs the lub operation between this lattice element and the given
	 * ones, by repeatedly invoking {@link #lub(Lattice)} following the
	 * iteration order.
	 * 
	 * @param others the other lattice elements
	 * 
	 * @return the lub between this and all other lattice elements
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	@SuppressWarnings("unchecked")
	default L lub(
			Iterable<L> others)
			throws SemanticException {
		return compress(
				(L) this,
				others,
				(
						l1,
						l2) -> l1.lub(l2));
	}

	/**
	 * Merges this lattice element and the given one. This operation is
	 * commutative. Merging two instances corresponds to computing their least
	 * upper bound, and it is used to explicitly merge lattices at control flow
	 * merge points. Instead, {@link #lub(Lattice)} is used to join arbitrary
	 * elements of the lattice at any point during the analysis. The distinction
	 * is present to allow implementers to provide different behaviors for the
	 * two operations, if needed (e.g., when a closure operation should be
	 * applied only in one of the situations).
	 * 
	 * @param other the other lattice element
	 * 
	 * @return the merge between this and other
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	L merge(
			L other)
			throws SemanticException;

	/**
	 * Performs the merge operation between this lattice element and the given
	 * ones, by repeatedly invoking {@link #merge(Lattice)} following the
	 * iteration order.
	 * 
	 * @param others the other lattice elements
	 * 
	 * @return the merge between this and all other lattice elements
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	@SuppressWarnings("unchecked")
	default L merge(
			L... others)
			throws SemanticException {
		return compress(
				(L) this,
				new IterableArray<>(others),
				(
						l1,
						l2) -> l1.merge(l2));
	}

	/**
	 * Performs the merge operation between this lattice element and the given
	 * ones, by repeatedly invoking {@link #merge(Lattice)} following the
	 * iteration order.
	 * 
	 * @param others the other lattice elements
	 * 
	 * @return the merge between this and all other lattice elements
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	@SuppressWarnings("unchecked")
	default L merge(
			Iterable<L> others)
			throws SemanticException {
		return compress(
				(L) this,
				others,
				(
						l1,
						l2) -> l1.merge(l2));
	}

	/**
	 * Performs the greatest lower upper bound operation between this lattice
	 * element and the given one. This operation is commutative.
	 * 
	 * @param other the other lattice element
	 * 
	 * @return the greatest lower bound
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	default L glb(
			L other)
			throws SemanticException {
		return bottom();
	}

	/**
	 * Performs the glb operation between this lattice element and the given
	 * ones, by repeatedly invoking {@link #glb(Lattice)} following the
	 * iteration order.
	 * 
	 * @param others the other lattice elements
	 * 
	 * @return the glb between this and all other lattice elements
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	@SuppressWarnings("unchecked")
	default L glb(
			L... others)
			throws SemanticException {
		return compress(
				(L) this,
				new IterableArray<>(others),
				(
						l1,
						l2) -> l1.glb(l2));
	}

	/**
	 * Performs the glb operation between this lattice element and the given
	 * ones, by repeatedly invoking {@link #glb(Lattice)} following the
	 * iteration order.
	 * 
	 * @param others the other lattice elements
	 * 
	 * @return the glb between this and all other lattice elements
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	@SuppressWarnings("unchecked")
	default L glb(
			Iterable<L> others)
			throws SemanticException {
		return compress(
				(L) this,
				others,
				(
						l1,
						l2) -> l1.glb(l2));
	}

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
	default boolean isTop() {
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
	default boolean isBottom() {
		return this == bottom();
	}

	/**
	 * Performs the widening operation between this lattice element and the
	 * given one. This operation is not commutative. The default implementation
	 * of this method delegates to {@link #lub(Lattice)}, and is thus safe for
	 * finite lattices and ACC ones.
	 * 
	 * @param other the other lattice element
	 * 
	 * @return the widening between this and other
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	default L widening(
			L other)
			throws SemanticException {
		return lub(other);
	}

	/**
	 * Performs the widening operation between this lattice element and the
	 * given ones, by repeatedly invoking {@link #widening(Lattice)} following
	 * the iteration order.
	 * 
	 * @param others the other lattice elements
	 * 
	 * @return the widening between this and all other lattice elements
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	@SuppressWarnings("unchecked")
	default L widening(
			L... others)
			throws SemanticException {
		return compress(
				(L) this,
				new IterableArray<>(others),
				(
						l1,
						l2) -> l1.widening(l2));
	}

	/**
	 * Performs the widening operation between this lattice element and the
	 * given ones, by repeatedly invoking {@link #widening(Lattice)} following
	 * the iteration order.
	 * 
	 * @param others the other lattice elements
	 * 
	 * @return the widening between this and all other lattice elements
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	@SuppressWarnings("unchecked")
	default L widening(
			Iterable<L> others)
			throws SemanticException {
		return compress(
				(L) this,
				others,
				(
						l1,
						l2) -> l1.widening(l2));
	}

	/**
	 * Performs the narrowing operation between this lattice element and the
	 * given one. This operation is not commutative. The default implementation
	 * of this method delegates to {@link #glb(Lattice)}, and is thus safe for
	 * finite lattices and DCC ones.
	 * 
	 * @param other the other lattice element
	 * 
	 * @return the narrowing between this and other
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	default L narrowing(
			L other)
			throws SemanticException {
		return glb(other);
	}

	/**
	 * Performs the narrowing operation between this lattice element and the
	 * given ones, by repeatedly invoking {@link #narrowing(Lattice)} following
	 * the iteration order.
	 * 
	 * @param others the other lattice elements
	 * 
	 * @return the narrowing between this and all other lattice elements
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	@SuppressWarnings("unchecked")
	default L narrowing(
			L... others)
			throws SemanticException {
		return compress(
				(L) this,
				new IterableArray<>(others),
				(
						l1,
						l2) -> l1.narrowing(l2));
	}

	/**
	 * Performs the narrowing operation between this lattice element and the
	 * given ones, by repeatedly invoking {@link #narrowing(Lattice)} following
	 * the iteration order.
	 * 
	 * @param others the other lattice elements
	 * 
	 * @return the narrowing between this and all other lattice elements
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	@SuppressWarnings("unchecked")
	default L narrowing(
			Iterable<L> others)
			throws SemanticException {
		return compress(
				(L) this,
				others,
				(
						l1,
						l2) -> l1.narrowing(l2));
	}

	private static <L extends Lattice<L>> L compress(
			L starter,
			Iterable<L> others,
			BiFunction<L, L, L, SemanticException> combiner)
			throws SemanticException {
		L cursor = starter;
		for (L o : others)
			cursor = combiner.apply(cursor, o);
		return cursor;
	}

	/**
	 * Yields an abstract element that should be used whenever a functional
	 * lattice using this element as values is queried for the state of a
	 * variable not currently part of its mapping. Abstraction for such a
	 * variable might have been lost, for instance, due to a call to
	 * {@link Lattice#top()} on the function itself. The default implementation
	 * of this method returns {@link Lattice#top()}.
	 * 
	 * @param id the variable that is missing from the mapping
	 * 
	 * @return a default abstraction for the variable
	 */
	default L unknownValue(
			Identifier id) {
		return top();
	}

	/**
	 * Yields a unique instance of a specific lattice, of class {@code domain},
	 * contained inside this lattice, also recursively querying inner lattices
	 * (enabling retrieval through Cartesian products or other types of
	 * combinations).<br>
	 * <br>
	 * The default implementation of this method lubs together (using
	 * {@link Lattice#lub(Lattice)}) all instances returned by
	 * {@link #getAllLatticeInstances(Class)}, defaulting to {@code null} if no
	 * instance is returned.
	 * 
	 * @param <D>    the type of lattice to retrieve
	 * @param domain the class of the lattice instance to retrieve
	 * 
	 * @return the instance of that lattice, or {@code null}
	 * 
	 * @throws SemanticException if an exception happens while lubbing the
	 *                               results
	 */
	default <D extends Lattice<D>> D getLatticeInstance(
			Class<D> domain)
			throws SemanticException {
		Collection<D> all = getAllLatticeInstances(domain);
		D result = null;
		for (D instance : all)
			if (result == null)
				result = instance;
			else
				result = result.lub(instance);

		return result;
	}

	/**
	 * Yields all of the instances of a specific lattice, of class
	 * {@code lattice}, contained inside this lattice element, also recursively
	 * querying inner lattices (enabling retrieval through Cartesian products or
	 * other types of combinations).<br>
	 * <br>
	 * The default implementation of this method returns a singleton collection
	 * containing {@code this} if {@code domain.isAssignableFrom(getClass())}
	 * holds, otherwise it returns an empty collection.
	 * 
	 * @param <D>    the type of lattice to retrieve
	 * @param domain the class of the lattice instance to retrieve
	 * 
	 * @return the instances of that lattice
	 */
	@SuppressWarnings("unchecked")
	default <D extends Lattice<D>> Collection<D> getAllLatticeInstances(
			Class<D> domain) {
		Collection<D> result = new HashSet<>();
		if (domain.isAssignableFrom(getClass()))
			result.add((D) this);

		return result;
	}

}
