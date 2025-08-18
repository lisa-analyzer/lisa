package it.unive.lisa.analysis.lattices;

import it.unive.lisa.analysis.Lattice;
import java.util.Collection;
import java.util.Map;
import java.util.function.Predicate;

/**
 * A generic ready-to-use {@link FunctionalLattice} with no additional fields,
 * that relies on the underlying lattice instance for distinguishing top and
 * bottom values.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <K> the type of keys of this map
 * @param <V> the type of values of this map
 */
public class GenericMapLattice<K, V extends Lattice<V>> extends FunctionalLattice<GenericMapLattice<K, V>, K, V> {

	/**
	 * Builds the map.
	 * 
	 * @param lattice the underlying lattice of values
	 */
	public GenericMapLattice(
			V lattice) {
		super(lattice);
	}

	/**
	 * Builds the map.
	 * 
	 * @param lattice  the underlying lattice of values (if {@code function} is
	 *                     null or empty, this decides whether the created
	 *                     object is top or bottom)
	 * @param function the function containing the mapping
	 */
	public GenericMapLattice(
			V lattice,
			Map<K, V> function) {
		super(lattice, function);
	}

	@Override
	public GenericMapLattice<K, V> top() {
		return new GenericMapLattice<>(lattice.top());
	}

	@Override
	public GenericMapLattice<K, V> bottom() {
		return new GenericMapLattice<>(lattice.bottom());
	}

	@Override
	public GenericMapLattice<K, V> mk(
			V lattice,
			Map<K, V> function) {
		return new GenericMapLattice<>(lattice, function);
	}

	@Override
	public V stateOfUnknown(
			K key) {
		return isBottom() ? lattice.bottom() : lattice.top();
	}

	/**
	 * Removes the entry whose key is the given key.
	 * 
	 * @param key the key of the entry to be removed
	 * 
	 * @return a copy of this map where the entry with the given key has been
	 *             removed. If this map is top or bottom, or if it has no
	 *             entries, returns this map.
	 */
	public GenericMapLattice<K, V> remove(
			K key) {
		if (isBottom() || isTop() || function == null)
			return this;

		Map<K, V> result = mkNewFunction(function, false);
		result.remove(key);
		if (result.isEmpty())
			return mk(lattice, null);
		return mk(lattice, result);
	}

	/**
	 * Removes all the entries whose keys are in the given collection.
	 * 
	 * @param keys the collection of keys whose entries should be removed from
	 *                 this map
	 * 
	 * @return a copy of this map where all the entries whose keys are in the
	 *             given collection have been removed. If this map is top or
	 *             bottom, or if it has no entries, returns this map.
	 */
	public GenericMapLattice<K, V> removeAll(
			Collection<K> keys) {
		if (isBottom() || isTop() || function == null)
			return this;

		Map<K, V> result = mkNewFunction(function, false);
		keys.forEach(result::remove);
		if (result.isEmpty())
			return mk(lattice, null);
		return mk(lattice, result);
	}

	/**
	 * Removes all the entries whose keys match the given test.
	 * 
	 * @param test the test to apply to the keys of the entries to be removed
	 * 
	 * @return a copy of this map where all the entries whose keys match the
	 *             given test have been removed. If this map is top or bottom,
	 *             or if it has no entries, returns this map.
	 */
	public GenericMapLattice<K, V> removeAllMatching(
			Predicate<K> test) {
		if (isBottom() || isTop() || function == null)
			return this;

		Map<K, V> result = mkNewFunction(function, false);
		function.keySet().removeIf(test);
		if (result.isEmpty())
			return mk(lattice, null);
		return mk(lattice, result);
	}

}
