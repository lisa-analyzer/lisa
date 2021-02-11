package it.unive.lisa.analysis;

import it.unive.lisa.util.collections.Utils;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * A generic set lattice containing a set of elements. Lattice operations
 * correspond to standard set operations:
 * <ul>
 * <li>the lub is the set union</li>
 * <li>the &le; is the set inclusion</li>
 * <li>...</li>
 * </ul>
 * Widening on instances of this lattice depends on the cardinality of the
 * domain of the underlying elements. The provided implementation behave as the
 * domain is <b>finite</b>, thus invoking the lub. Set lattices defined on
 * infinite domains must implement a coherent widening logic.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <S> the concrete instance of {@link SetLattice}
 * @param <E> the type of elements of the domain of this lattice
 */
public abstract class SetLattice<S extends SetLattice<S, E>, E> extends BaseLattice<S> {

	/**
	 * The set of elements contained in the lattice.
	 */
	protected Set<E> elements;

	/**
	 * Builds the lattice.
	 * 
	 * @param elements the elements that are contained in the lattice
	 */
	protected SetLattice(Set<E> elements) {
		this.elements = elements;
	}

	/**
	 * Utility for creating a concrete instance of {@link SetLattice} given a
	 * set. This decouples the instance of set used during computation of the
	 * elements to put in the lattice from the actual type of set underlying the
	 * lattice.
	 * 
	 * @param set the set containing the elements that must be included in the
	 *                lattice instance
	 * 
	 * @return a new concrete instance of {@link SetLattice} containing the
	 *             elements of the given set
	 */
	protected abstract S mk(Set<E> set);

	@Override
	protected final S lubAux(S other) throws SemanticException {
		Set<E> lub = new HashSet<>(elements);
		lub.addAll(other.elements);
		return mk(lub);
	}

	@Override
	protected S wideningAux(S other) throws SemanticException {
		return lubAux(other);
	}

	@Override
	protected final boolean lessOrEqualAux(S other) throws SemanticException {
		return other.elements.containsAll(elements);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((elements == null) ? 0 : elements.hashCode());
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
		SetLattice<?, ?> other = (SetLattice<?, ?>) obj;
		if (elements == null) {
			if (other.elements != null)
				return false;
		} else if (!elements.equals(other.elements))
			return false;
		return true;
	}

	@Override
	public final String toString() {
		if (isTop())
			return Lattice.TOP_STRING;

		if (isBottom())
			return Lattice.BOTTOM_STRING;

		Set<E> tmp = new TreeSet<>(
				(l, r) -> Utils.nullSafeCompare(true, l, r, (ll, rr) -> ll.toString().compareTo(rr.toString())));
		tmp.addAll(elements);
		return tmp.toString();
	}
}
