package it.unive.lisa.analysis.lattices;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.util.representation.MapRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

/**
 * A generic functional abstract domain that performs the functional lifting of
 * the lattice on the elements of the co-domain.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <F> the concrete {@link FunctionalLattice} type
 * @param <K> the concrete type of the keys of this function
 * @param <V> the concrete {@link Lattice} type of the values of this function
 */
public abstract class FunctionalLattice<F extends FunctionalLattice<F, K, V>, K, V extends Lattice<V>>
		implements BaseLattice<F>, Iterable<Map.Entry<K, V>> {

	/**
	 * The function implemented by this lattice.
	 */
	public Map<K, V> function;

	/**
	 * The underlying lattice.
	 */
	public final V lattice;

	/**
	 * Builds the lattice.
	 * 
	 * @param lattice the underlying lattice
	 */
	public FunctionalLattice(V lattice) {
		this(lattice, null);
	}

	/**
	 * Builds the lattice by cloning the given function.
	 * 
	 * @param lattice  the underlying lattice
	 * @param function the function to clone
	 */
	public FunctionalLattice(V lattice, Map<K, V> function) {
		this.lattice = lattice;
		this.function = function != null && function.isEmpty() ? null : function;
	}

	/**
	 * Creates a new instance of the underlying function. The purpose of this
	 * method is to provide a common function implementation to every subclass
	 * that does not have implementation-specific requirements.
	 * 
	 * @param other        an optional function to copy, can be {@code null}
	 * @param preserveNull whether a null {@code other} should cause a
	 *                         {@code null} return value or an empty function
	 * 
	 * @return a new function
	 */
	public Map<K, V> mkNewFunction(Map<K, V> other, boolean preserveNull) {
		if (other == null)
			return preserveNull ? null : new HashMap<>();
		return new HashMap<>(other);
	}

	/**
	 * Yields the set of keys currently in this lattice.
	 * 
	 * @return the set of keys
	 */
	public Set<K> getKeys() {
		if (function == null)
			return Collections.emptySet();
		return function.keySet();
	}

	/**
	 * Yields the state associated to the given key. This operation is defined
	 * in term of lattices: it always returns bottom of {@link #isBottom()}
	 * holds, top if {@link #isTop()} holds, the stored value when key is part
	 * of the mapping, or {@link #stateOfUnknown(Object)} otherwise. This method
	 * is the primary way in which information mapped to a key should be
	 * extracted from this function.
	 * 
	 * @param key the key
	 * 
	 * @return the state
	 */
	public V getState(K key) {
		if (isBottom())
			return lattice.bottom();
		if (isTop())
			return lattice.top();
		if (function != null && function.containsKey(key))
			return function.get(key);
		return stateOfUnknown(key);
	}

	/**
	 * Similar to {@link #getState(Object)}, but yields a custom default value
	 * for keys that are not part of the mapping (instead of using
	 * {@link #stateOfUnknown(Object)}). This is useful for implementing
	 * specific operation that need to use an operation-neutral element as
	 * fallback.
	 * 
	 * @param key the key
	 * 
	 * @return the state
	 */
	public V getOtDefault(K key, V def) {
		if (isBottom())
			return lattice.bottom();
		if (isTop())
			return lattice.top();
		if (function != null && function.containsKey(key))
			return function.get(key);
		return def;
	}

	/**
	 * Yields the value that should be returned by {@link #getState(Object)}
	 * whenever the given key is not present in this map.
	 * 
	 * @param key the key that is missing
	 * 
	 * @return the lattice element for keys not in the mapping
	 */
	public abstract V stateOfUnknown(K key);

	/**
	 * Yields an instance of this class equal to the receiver of the call, but
	 * with {@code key} mapped to {@code state}.
	 * 
	 * @param key   the key
	 * @param state the state
	 * 
	 * @return the new instance of this class with the updated mapping
	 */
	public F putState(K key, V state) {
		// we are only adding elements here, so it is fine to not preserve null
		Map<K, V> result = mkNewFunction(function, false);
		result.put(key, state);
		return mk(lattice, result);
	}

	/**
	 * Builds a instance of this class from the given lattice instance and the
	 * given mapping.
	 * 
	 * @param lattice  an instance of lattice to be used during semantic
	 *                     operations to retrieve top and bottom values
	 * @param function the function representing the mapping contained in the
	 *                     new environment; can be {@code null}
	 * 
	 * @return a new instance of this class
	 */
	public abstract F mk(V lattice, Map<K, V> function);

	@Override
	public F lubAux(F other) throws SemanticException {
		return functionalLift(other, lattice.bottom(), this::lubKeys, (o1, o2) -> o1 == null ? o2 : o1.lub(o2));
	}

	@Override
	public F glbAux(F other) throws SemanticException {
		return functionalLift(other, lattice.top(), this::glbKeys, (o1, o2) -> o1 == null ? o2 : o1.glb(o2));
	}

	@Override
	public F wideningAux(F other) throws SemanticException {
		return functionalLift(other, lattice.bottom(), this::lubKeys, (o1, o2) -> o1 == null ? o2 : o1.widening(o2));
	}

	@Override
	public F narrowingAux(F other) throws SemanticException {
		return functionalLift(other, lattice.top(), this::glbKeys, (o1, o2) -> o1 == null ? o2 : o1.narrowing(o2));
	}

	/**
	 * Interface for the lift of lattice elements.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 *
	 * @param <V> {@link Lattice} type of the values
	 */
	@FunctionalInterface
	public interface FunctionalLift<V extends Lattice<V>> {

		/**
		 * Yields the lift of {@code first} and {@code second} lattice element.
		 * 
		 * @param first  the first lattice element
		 * @param second the second lattice element
		 * 
		 * @return the lift of {@code first} and {@code second}
		 * 
		 * @throws SemanticException if something goes wrong while lifting the
		 *                               values
		 */
		V lift(V first, V second) throws SemanticException;
	}

	/**
	 * Interface for the left of key sets.
	 * 
	 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
	 *
	 * @param <K> the key type
	 */
	@FunctionalInterface
	public interface KeyFunctionalLift<K> {

		/**
		 * Yields the lift of {@code first} and {@code second} key sets.
		 * 
		 * @param first  the first key set
		 * @param second the second key set
		 * 
		 * @return the left of {@code first} and {@code second} key sets
		 * 
		 * @throws SemanticException if something goes wrong while lifting the
		 *                               key sets
		 */
		Set<K> keyLift(Set<K> first, Set<K> second) throws SemanticException;
	}

	/**
	 * Yields the functional lift between {@code this} and {@code other}.
	 * 
	 * @param other       the other functional lattice
	 * @param missing     the lattice element to use for the lift when a key has
	 *                        no mapping in one of the two functions (e.g., for
	 *                        lub, missing should be bottom, while for glb it
	 *                        should be top)
	 * @param keyLifter   the key lifter
	 * @param valueLifter the value lifter
	 * 
	 * @return the intersection between {@code k1} and {@code k2}
	 * 
	 * @throws SemanticException if something goes wrong while lifting the
	 *                               lattice elements
	 */
	public F functionalLift(
			F other,
			V missing,
			KeyFunctionalLift<K> keyLifter,
			FunctionalLift<V> valueLifter)
			throws SemanticException {
		Map<K, V> function = mkNewFunction(null, false);
		Set<K> keys = keyLifter.keyLift(this.getKeys(), other.getKeys());
		for (K key : keys)
			try {
				V s1 = getOtDefault(key, missing);
				V s2 = other.getOtDefault(key, missing);
				function.put(key, valueLifter.lift(s1, s2));
			} catch (SemanticException e) {
				throw new SemanticException("Exception during functional lifting of key '" + key + "'", e);
			}
		return mk(lattice.lub(other.lattice), function);
	}

	/**
	 * Yields the union of the keys between {@code k1} and {@code k2}.
	 * 
	 * @param k1 the first key set
	 * @param k2 the second key set
	 * 
	 * @return the union between {@code k1} and {@code k2}
	 * 
	 * @throws SemanticException if something goes wrong while lifting the keys
	 */
	public Set<K> lubKeys(Set<K> k1, Set<K> k2) throws SemanticException {
		Set<K> keys = new HashSet<>(k1);
		keys.addAll(k2);
		return keys;
	}

	/**
	 * Yields the intersection of the keys between {@code k1} and {@code k2}.
	 * 
	 * @param k1 the first key set
	 * @param k2 the second key set
	 * 
	 * @return the intersection between {@code k1} and {@code k2}
	 * 
	 * @throws SemanticException if something goes wrong while lifting the key
	 *                               sets
	 */
	public Set<K> glbKeys(Set<K> k1, Set<K> k2) throws SemanticException {
		Set<K> keys = new HashSet<>(k1);
		keys.retainAll(k2);
		return keys;
	}

	@Override
	public boolean lessOrEqualAux(F other) throws SemanticException {
		if (function != null)
			for (K key : function.keySet())
				if (getState(key) != null && (!getState(key).lessOrEqual(other.getState(key))))
					return false;

		return true;
	}

	/**
	 * {@inheritDoc}<br>
	 * <br>
	 * By default, a functional lattice is the top lattice if the underlying
	 * lattice's {@code isTop()} holds and its function is {@code null}.
	 */
	@Override
	public boolean isTop() {
		return lattice.isTop() && function == null;
	}

	/**
	 * {@inheritDoc}<br>
	 * <br>
	 * By default, a functional lattice is the top lattice if the underlying
	 * lattice's {@code isBottom()} holds and its function is {@code null}.
	 */
	@Override
	public boolean isBottom() {
		return lattice.isBottom() && function == null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((function == null) ? 0 : function.hashCode());
		result = prime * result + ((lattice == null) ? 0 : lattice.hashCode());
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
		FunctionalLattice<?, ?, ?> other = (FunctionalLattice<?, ?, ?>) obj;
		if (function == null) {
			if (other.function != null)
				return false;
		} else if (!function.equals(other.function))
			return false;
		if (lattice == null) {
			if (other.lattice != null)
				return false;
		} else if (!lattice.equals(other.lattice))
			return false;
		return true;
	}

	@Override
	public String toString() {
		if (isTop())
			return Lattice.TOP_STRING;

		if (isBottom())
			return Lattice.BOTTOM_STRING;

		return function == null ? "{}" : function.toString();
	}

	@Override
	public Iterator<Entry<K, V>> iterator() {
		if (function == null)
			return Collections.emptyIterator();
		return function.entrySet().iterator();
	}

	/**
	 * Yields the values of this functional lattice.
	 * 
	 * @return the values of this functional lattice
	 */
	public Collection<V> getValues() {
		if (function == null)
			return Collections.emptySet();
		return function.values();
	}

	/**
	 * Yields the map associated with this functional lattice element.
	 * 
	 * @return the map associated with this functional lattice element.
	 */
	public Map<K, V> getMap() {
		return function;
	}

	@Override
	public StructuredRepresentation representation() {
		if (isTop())
			return Lattice.topRepresentation();

		if (isBottom())
			return Lattice.bottomRepresentation();

		if (function == null)
			return new StringRepresentation("empty");

		return new MapRepresentation(function, StringRepresentation::new, Lattice::representation);
	}
}
