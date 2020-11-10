package it.unive.lisa.analysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * A generic functional abstract domain that performs the functional lifting of
 * the lattice on the elements of the co-domain.
 * 
 * @param <F> the concrete {@link FunctionalLattice} type
 * @param <K> the concrete type of the keys of this function
 * @param <V> the concrete {@link Lattice} type of the values of this function
 */
public abstract class FunctionalLattice<F extends FunctionalLattice<F, K, V>, K, V extends Lattice<V>>
		extends BaseLattice<F> {

	/**
	 * The function implemented by this lattice
	 */
	protected final Map<K, V> function;

	/**
	 * The underlying lattice
	 */
	protected final V lattice;

	/**
	 * Builds the lattice.
	 * 
	 * @param lattice the underlying lattice
	 */
	protected FunctionalLattice(V lattice) {
		this.lattice = lattice;
		this.function = new HashMap<>();
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
	 * Yields the set of keys currently in this lattice
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
	 * @return the state
	 */
	public final V getState(K key) {
		if (this != bottom() && function.containsKey(key))
			return function.get(key);
		else
			return lattice.bottom();
	}

	@Override
	public final F lubAux(F other) {
		return functionalLift(other, (o1, o2) -> o1 == null ? o2 : o1.lub(o2));
	}

	@Override
	public final F wideningAux(F succ) {
		return functionalLift(succ, (o1, o2) -> o1 == null ? o2 : o1.widening(o2));
	}

	private final F functionalLift(F other, BiFunction<V, V, V> lift) {
		F result = bottom();

		Set<K> keys = new HashSet<>(function.keySet());
		keys.addAll(other.function.keySet());
		for (K key : keys)
			result.function.put(key, lift.apply(getState(key), other.getState(key)));

		return result;
	}

	@Override
	public final boolean lessOrEqualAux(F other) {
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
		if (this == bottom())
			return "#BOTTOM#";

		if (this == top())
			return "#TOP#";

		return function.toString();
	}
}
