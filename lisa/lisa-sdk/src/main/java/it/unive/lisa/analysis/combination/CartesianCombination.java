package it.unive.lisa.analysis.combination;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.util.representation.ListRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Collection;

/**
 * A base class for cartesian combinations of two lattices. This class takes
 * care of propagating all lattice operations to the two components, and
 * provides a unified interface for working with the combined lattice. It does
 * not perform any kind of reduction between the two, that must instead be
 * provided by subclasses.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <C>  the type of the cartesian combination
 * @param <T1> the type of the first lattice
 * @param <T2> the type of the second lattice
 */
public abstract class CartesianCombination<C extends CartesianCombination<C, T1, T2>,
		T1 extends Lattice<T1>,
		T2 extends Lattice<T2>>
		implements
		BaseLattice<C> {

	/**
	 * The first lattice in the combination.
	 */
	public final T1 first;

	/**
	 * The second lattice in the combination.
	 */
	public final T2 second;

	/**
	 * Creates a new cartesian combination of two lattices.
	 * 
	 * @param first  the first lattice in the combination
	 * @param second the second lattice in the combination
	 */
	protected CartesianCombination(
			T1 first,
			T2 second) {
		this.first = first;
		this.second = second;
	}

	/**
	 * Creates a new cartesian combination of two lattices.
	 * 
	 * @param first  the first lattice in the combination
	 * @param second the second lattice in the combination
	 * 
	 * @return the new cartesian combination
	 */
	public abstract C mk(
			T1 first,
			T2 second);

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((first == null) ? 0 : first.hashCode());
		result = prime * result + ((second == null) ? 0 : second.hashCode());
		return result;
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CartesianCombination<?, ?, ?> other = (CartesianCombination<?, ?, ?>) obj;
		if (first == null) {
			if (other.first != null)
				return false;
		} else if (!first.equals(other.first))
			return false;
		if (second == null) {
			if (other.second != null)
				return false;
		} else if (!second.equals(other.second))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return representation().toString();
	}

	@Override
	public C top() {
		return mk(first.top(), second.top());
	}

	@Override
	public boolean isTop() {
		return first.isTop() && second.isTop();
	}

	@Override
	public C bottom() {
		return mk(first.bottom(), second.bottom());
	}

	@Override
	public boolean isBottom() {
		return first.isBottom() && second.isBottom();
	}

	@Override
	public StructuredRepresentation representation() {
		return new ListRepresentation(first.representation(), second.representation());
	}

	@Override
	public C lubAux(
			C other)
			throws SemanticException {
		return mk(first.lub(other.first), second.lub(other.second));
	}

	@Override
	public C wideningAux(
			C other)
			throws SemanticException {
		return mk(first.widening(other.first), second.widening(other.second));
	}

	@Override
	public C glbAux(
			C other)
			throws SemanticException {
		return mk(first.glb(other.first), second.glb(other.second));
	}

	@Override
	public C narrowingAux(
			C other)
			throws SemanticException {
		return mk(first.narrowing(other.first), second.narrowing(other.second));
	}

	@Override
	public boolean lessOrEqualAux(
			C other)
			throws SemanticException {
		return first.lessOrEqual(other.first) && second.lessOrEqual(other.second);
	}

	@Override
	public <D extends Lattice<D>> Collection<D> getAllLatticeInstances(
			Class<D> lattice) {
		Collection<D> result = BaseLattice.super.getAllLatticeInstances(lattice);
		result.addAll(first.getAllLatticeInstances(lattice));
		result.addAll(second.getAllLatticeInstances(lattice));
		return result;
	}

}
