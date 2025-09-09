package it.unive.lisa.analysis.lattices;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A generic ready-to-use {@link SetLattice} with no additional fields, that
 * relies on an underlying boolean value for distinguishing top and bottom
 * values.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <E> the type of elements of this set
 */
public class GenericSetLattice<E>
		extends
		SetLattice<GenericSetLattice<E>, E> {

	/**
	 * Builds an empty set representing the top element.
	 */
	public GenericSetLattice() {
		super(Collections.emptySet(), true);
	}

	/**
	 * Builds a set containing only the given element.
	 * 
	 * @param element the element
	 */
	public GenericSetLattice(
			E element) {
		super(Collections.singleton(element), true);
	}

	/**
	 * Builds a set with all the given elements.
	 * 
	 * @param elements the elements
	 */
	public GenericSetLattice(
			Set<E> elements) {
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
	public GenericSetLattice(
			Set<E> elements,
			boolean isTop) {
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
	public GenericSetLattice<E> mk(
			Set<E> set) {
		return new GenericSetLattice<>(set);
	}

	/**
	 * Adds the given element to this set. This operation has no side-effects,
	 * i.e., this set is not modified.
	 * 
	 * @param element the element to add
	 * 
	 * @return a new set with the given element added
	 */
	public GenericSetLattice<E> add(
			E element) {
		Set<E> res = new HashSet<>(elements);
		res.add(element);
		return new GenericSetLattice<>(res);
	}

	/**
	 * Adds the given elements to this set. This operation has no side-effects,
	 * i.e., this set is not modified.
	 * 
	 * @param elements the elements to add
	 * 
	 * @return a new set with the given elements added
	 */
	public GenericSetLattice<E> addAll(
			Collection<E> elements) {
		Set<E> res = new HashSet<>(this.elements);
		res.addAll(elements);
		return new GenericSetLattice<>(res);
	}

	/**
	 * Removes the given element from this set. This operation has no
	 * side-effects, i.e., this set is not modified.
	 * 
	 * @param element the element to remove
	 * 
	 * @return a new set without the given element
	 */
	public GenericSetLattice<E> remove(
			E element) {
		Set<E> res = new HashSet<>(elements);
		res.remove(element);
		return new GenericSetLattice<>(res);
	}

	/**
	 * Removes all the given elements from this set. This operation has no
	 * side-effects, i.e., this set is not modified.
	 * 
	 * @param elements the elements to remove
	 * 
	 * @return a new set without the given elements
	 */
	public GenericSetLattice<E> removeAll(
			Collection<E> elements) {
		Set<E> res = new HashSet<>(this.elements);
		res.removeAll(elements);
		return new GenericSetLattice<>(res);
	}

}
