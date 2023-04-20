package it.unive.lisa.analysis.lattices;

import java.util.Collections;
import java.util.Set;

/**
 * A generic ready-to-use {@link InverseSetLattice} with no additional fields,
 * that relies on an underlying boolean value for distinguishing top and bottom
 * values.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <E> the type of elements of this set
 */
public class GenericInverseSetLattice<E> extends InverseSetLattice<GenericInverseSetLattice<E>, E> {

	/**
	 * Builds an empty set representing the top element.
	 */
	public GenericInverseSetLattice() {
		super(Collections.emptySet(), true);
	}

	/**
	 * Builds a set containing only the given element.
	 * 
	 * @param element the element
	 */
	public GenericInverseSetLattice(E element) {
		super(Collections.singleton(element), true);
	}

	/**
	 * Builds a set with all the given elements.
	 * 
	 * @param elements the elements
	 */
	public GenericInverseSetLattice(Set<E> elements) {
		super(elements, true);
	}

	/**
	 * Builds a set with all the given elements, also specifying whether an
	 * empty set should be considered top or bottom.
	 * 
	 * @param elements the elements
	 * @param isTop    if {@code elements} is empty, specifies if this set
	 *                     should be considered top or bottom
	 */
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
