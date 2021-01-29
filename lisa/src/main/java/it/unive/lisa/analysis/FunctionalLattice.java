package it.unive.lisa.analysis;

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

	@Override
	public F lubAux(F other) throws SemanticException {
		return functionalLift(other, (o1, o2) -> o1 == null ? o2 : o1.lub(o2));
	}

	@Override
	public F wideningAux(F other) throws SemanticException {
		return functionalLift(other, (o1, o2) -> o1 == null ? o2 : o1.widening(o2));
	}

	private interface FunctionalLift<V extends Lattice<V>> {
		V lift(V first, V second) throws SemanticException;
	}

	private final F functionalLift(F other, FunctionalLift<V> lift) throws SemanticException {
		F result = bottom();
		result.function = mkNewFunction(null);
		Set<K> keys = new HashSet<>(function.keySet());
		keys.addAll(other.function.keySet());
		for (K key : keys)
			try {
				result.function.put(key, lift.lift(getState(key), other.getState(key)));
			} catch (SemanticException e) {
				throw new SemanticException("Exception during functional lifting of key '" + key + "'", e);
			}

		return result;
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
		// we use the name of the lattice's class since we do not care about the
		// single
		// instance, but more about the type itself
		result = prime * result + ((lattice == null) ? 0 : lattice.getClass().getName().hashCode());
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
		} else if (lattice.getClass() != other.lattice.getClass())
			// we use the lattice's class since we do not care about the single
			// instance, but more about the type itself
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
}
