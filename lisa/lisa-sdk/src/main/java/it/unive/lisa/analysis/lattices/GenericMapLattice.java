package it.unive.lisa.analysis.lattices;

import java.util.Map;

import it.unive.lisa.analysis.Lattice;

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
public class GenericMapLattice<K, V extends Lattice<V>>
		extends FunctionalLattice<GenericMapLattice<K, V>, K, V> {

	/**
	 * Builds the map.
	 * 
	 * @param lattice the underlying lattice of values
	 */
	public GenericMapLattice(V lattice) {
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
	public GenericMapLattice(V lattice, Map<K, V> function) {
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
	public GenericMapLattice<K, V> mk(V lattice, Map<K, V> function) {
		return new GenericMapLattice<>(lattice, function);
	}

	@Override
	public V stateOfUnknown(K key) {
		return isBottom() ? lattice.bottom() : lattice.top();
	}
}
