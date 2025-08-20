package it.unive.lisa.lattices.symbolic;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.InverseSetLattice;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * An {@link InverseSetLattice} of {@link Identifier}s.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class DefiniteIdSet
		extends
		InverseSetLattice<DefiniteIdSet, Identifier> {

	/**
	 * Builds the lattice.
	 *
	 * @param elements the elements that are contained in the lattice
	 */
	public DefiniteIdSet(
			Set<Identifier> elements) {
		super(elements, elements.isEmpty());
	}

	/**
	 * Builds the lattice.
	 *
	 * @param elements the elements that are contained in the lattice
	 * @param isTop    whether or not this is the top or bottom element of the
	 *                     lattice, valid only if the set of elements is empty
	 */
	public DefiniteIdSet(
			Set<Identifier> elements,
			boolean isTop) {
		super(elements, isTop);
	}

	@Override
	public DefiniteIdSet wideningAux(
			DefiniteIdSet other)
			throws SemanticException {
		return other.elements.containsAll(elements) ? other : top();
	}

	@Override
	public DefiniteIdSet top() {
		return new DefiniteIdSet(Collections.emptySet(), true);
	}

	@Override
	public DefiniteIdSet bottom() {
		return new DefiniteIdSet(Collections.emptySet(), false);
	}

	@Override
	public DefiniteIdSet mk(
			Set<Identifier> set) {
		return new DefiniteIdSet(set);
	}

	/**
	 * Adds a new {@link Identifier} to this set. This method has no side
	 * effect: a new {@link DefiniteIdSet} is created, modified and returned.
	 * 
	 * @param id the identifier to add
	 * 
	 * @return the new set
	 */
	public DefiniteIdSet add(
			Identifier id) {
		Set<Identifier> res = new HashSet<>(elements);
		res.add(id);
		return new DefiniteIdSet(res);
	}

	@Override
	public StructuredRepresentation representation() {
		if (isTop())
			return new StringRepresentation("()");
		return super.representation();
	}

}
