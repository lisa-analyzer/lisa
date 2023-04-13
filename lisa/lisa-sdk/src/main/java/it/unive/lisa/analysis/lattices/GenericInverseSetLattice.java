package it.unive.lisa.analysis.lattices;

import java.util.Collections;
import java.util.Set;

public class GenericInverseSetLattice<E> extends InverseSetLattice<GenericInverseSetLattice<E>, E> {

	public GenericInverseSetLattice() {
		super(Collections.emptySet(), true);
	}

	public GenericInverseSetLattice(E element) {
		super(Collections.singleton(element), true);
	}

	public GenericInverseSetLattice(Set<E> elements) {
		super(elements, true);
	}

	public GenericInverseSetLattice(Set<E> elements, boolean isTop) {
		super(elements, isTop);
	}

	@Override
	public GenericInverseSetLattice<E> top() {
		return new GenericInverseSetLattice<>(Collections.emptySet(), true);
	}

	@Override
	public GenericInverseSetLattice<E> bottom() {
		return new GenericInverseSetLattice<>(Collections.emptySet(), false);
	}

	@Override
	public GenericInverseSetLattice<E> mk(Set<E> set) {
		return new GenericInverseSetLattice<>(set);
	}
}
