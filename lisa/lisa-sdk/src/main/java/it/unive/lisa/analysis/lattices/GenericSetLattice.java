package it.unive.lisa.analysis.lattices;

import java.util.Collections;
import java.util.Set;

public class GenericSetLattice<E> extends SetLattice<GenericSetLattice<E>, E> {

	public GenericSetLattice() {
		super(Collections.emptySet(), true);
	}

	public GenericSetLattice(E element) {
		super(Collections.singleton(element), true);
	}

	public GenericSetLattice(Set<E> elements) {
		super(elements, true);
	}

	public GenericSetLattice(Set<E> elements, boolean isTop) {
		super(elements, isTop);
	}

	@Override
	public GenericSetLattice<E> top() {
		return new GenericSetLattice<>(Collections.emptySet(), true);
	}

	@Override
	public GenericSetLattice<E> bottom() {
		return new GenericSetLattice<>(Collections.emptySet(), false);
	}

	@Override
	public GenericSetLattice<E> mk(Set<E> set) {
		return new GenericSetLattice<>(set);
	}
}
