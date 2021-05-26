package it.unive.lisa.analysis.lattices;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
		extends BaseLattice<F> implements Iterable<Map.Entry<K, V>> {

	/**
	 * The function implemented by this lattice.
	 */
	protected Map<K, V> function;

	/**
	 * The underlying lattice.
	 */
	protected final V lattice;

	/**
	 * Builds the lattice.
	 * 
	 * @param lattice the underlying lattice
	 */
	protected FunctionalLattice(V lattice) {
		this.lattice = lattice;
		this.function = mkNewFunction(null);
	}

	/**
	 * Builds the lattice by cloning the given function.
	 * 
	 * @param lattice  the underlying lattice
	 * @param function the function to clone
	 */
	protected FunctionalLattice(V lattice, Map<K, V> function) {
		this.lattice = lattice;
		this.function = function;
	}

	/**
	 * Creates a new instance of the underlying function. The purpose of this
	 * method is to provide a common function implementation to every subclass
	 * that does not have implementation-specific requirements.
	 * 
	 * @param other an optional function to copy, can be {@code null}
	 * 
	 * @return a new function, either empty or containing the same data of the
	 *             given one
	 */
	protected Map<K, V> mkNewFunction(Map<K, V> other) {
		if (other == null)
			return new HashMap<>();
		return new HashMap<>(other);
	}

	/**
	 * Yields the set of keys currently in this lattice.
	 * 
	 * @return the set of keys
	 */
	public final Set<K> getKeys() {
		if (function == null)
			return Collections.emptySet();
		return function.keySet();
	}

	/**
	 * Yields the state associated to the given key.
	 * 
	 * @param key the key
	 * 
	 * @return the state
	 */
	public final V getState(K key) {
		if (isBottom())
			return lattice.bottom();
		if (isTop())
			return lattice.top();
		if (function.containsKey(key))
			return function.get(key);
		return lattice.bottom();
	}

	/**
	 * Yields an instance of this class equal to the receiver of the call, but
	 * with {@code key} mapped to {@code state}.
	 * 
	 * @param key   the key
	 * @param state the state
	 * 
	 * @return the new instance of this class with the updated mapping
	 */
	public final F putState(K key, V state) {
		F result = bottom();
		result.function = mkNewFunction(null);

		result.function.put(key, state);
		for (K k : getKeys())
			if (!k.equals(key))
				result.function.put(k, getState(k));
		return result;
	}

	@Override
	public F lubAux(F other) throws SemanticException {
		return functionalLift(other, (f1, f2) -> lubKeys(f1, f2), (o1, o2) -> o1 == null ? o2 : o1.lub(o2));
	}

	@Override
	public F wideningAux(F other) throws SemanticException {
		return functionalLift(other, (f1, f2) -> lubKeys(f1, f2), (o1, o2) -> o1 == null ? o2 : o1.widening(o2));
	}

	/**
	 * Interface for the lift of lattice elements.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 *
	 * @param <V> {@link Lattice} type of the values
	 */
	protected interface FunctionalLift<V extends Lattice<V>> {

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
	protected interface KeyFunctionalLift<K> {

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
	 * @param keyLifter   the key lifter
	 * @param valueLifter the value lifter
	 * 
	 * @return the intersection between {@code k1} and {@code k2}
	 * 
	 * @throws SemanticException if something goes wrong while lifting the
	 *                               lattice elements
	 */
	protected final F functionalLift(F other, KeyFunctionalLift<K> keyLifter, FunctionalLift<V> valueLifter)
			throws SemanticException {
		F result = bottom();
		result.function = mkNewFunction(null);
		Set<K> keys = keyLifter.keyLift(this.getKeys(), other.getKeys());
		for (K key : keys)
			try {
				result.function.put(key, valueLifter.lift(getState(key), other.getState(key)));
			} catch (SemanticException e) {
				throw new SemanticException("Exception during functional lifting of key '" + key + "'", e);
			}
		return result;
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
	protected Set<K> lubKeys(Set<K> k1, Set<K> k2) throws SemanticException {
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
	protected Set<K> glbKeys(Set<K> k1, Set<K> k2) throws SemanticException {
		Set<K> keys = new HashSet<>(k1);
		keys.retainAll(k2);
		return keys;
	}

	@Override
	public boolean lessOrEqualAux(F other) throws SemanticException {
		for (K key : function.keySet())
			if (getState(key) != null && (!getState(key).lessOrEqual(other.getState(key))))
				return false;

		return true;
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

		return function.toString();
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
	public Collection<V> values() {
		if (function == null)
			return Collections.emptySet();
		return function.values();
	}

	/**
	 * Yields the keys of this functional lattice.
	 * 
	 * @return the keys of this functional lattice
	 */
	public Set<K> keys() {
		if (function == null)
			return Collections.emptySet();
		return function.keySet();
	}

	/**
	 * Yields the map associated with this functional lattice element.
	 * 
	 * @return the map associated with this functional lattice element.
	 */
	public Map<K, V> getMap() {
		return function;
	}
}
