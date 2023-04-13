package it.unive.lisa.analysis.lattices;

import it.unive.lisa.analysis.Lattice;
import java.util.Map;

public class GenericMapLattice<K, V extends Lattice<V>>
		extends FunctionalLattice<GenericMapLattice<K, V>, K, V> {

	public GenericMapLattice(V lattice) {
		super(lattice);
	}

	private GenericMapLattice(V lattice, Map<K, V> function) {
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
}
