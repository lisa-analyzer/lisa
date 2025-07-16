package it.unive.lisa.analysis.nonRedundantPowerset;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.util.representation.SetRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * This class generalizes a lattice whose elements are in the powerset of
 * another lattice, and that only contain non redundant elements. It also
 * defines the basic lattice operation for this domain (such as lub, glb,
 * widening, lessOrEqual and other operations needed for the calculations of the
 * previous ones). This implementations follows the guidelines of this
 * <a href="https://www.cs.unipr.it/Publications/PDF/Q349.pdf">paper</a>.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <S> the type of the concrete non redundant set lattice
 * @param <L> the type of the underlying lattice whose elements are contained in
 *                the non redundant set lattice
 */
public abstract class NonRedundantSetLattice<S extends NonRedundantSetLattice<S, L>,
		L extends Lattice<L>>
		implements
		BaseLattice<S> {

	/**
	 * The set that containing the elements.
	 */
	public final Set<L> elements;

	/**
	 * An instance of the underlying lattice from which top and bottom can be
	 * retrieved.
	 */
	protected final L singleton;

	/**
	 * Builds the lattice.
	 * 
	 * @param elements  the elements contained in this lattice
	 * @param singleton the singleton element of the underlying lattice
	 */
	protected NonRedundantSetLattice(
			Set<L> elements,
			L singleton) {
		this.elements = elements;
		this.singleton = singleton;
	}

	/**
	 * Builds a new instance of the non redundant set lattice with the given
	 * elements.
	 * 
	 * @param set the elements contained in the new lattice
	 * 
	 * @return the new instance of the non redundant set lattice
	 */
	public abstract S mk(
			Set<L> set);

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((elements == null) ? 0 : elements.hashCode());
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
		NonRedundantSetLattice<?, ?> other = (NonRedundantSetLattice<?, ?>) obj;
		if (elements == null) {
			if (other.elements != null)
				return false;
		} else if (!elements.equals(other.elements))
			return false;
		return true;
	}

	@Override
	public StructuredRepresentation representation() {
		if (isBottom())
			return Lattice.bottomRepresentation();

		return new SetRepresentation(elements, Lattice::representation);
	}

	@Override
	public String toString() {
		return representation().toString();
	}

	@Override
	public S top() {
		Set<L> topSet = new HashSet<>();
		topSet.add(singleton.top());
		return mk(topSet);
	}

	@Override
	public S bottom() {
		return mk(Collections.emptySet());
	}

	@Override
	public boolean isBottom() {
		return elements.isEmpty();
	}

	@Override
	public boolean isTop() {
		for (L element : elements)
			if (element.isTop())
				return true;
		return false;
	}

	/**
	 * For two subset S<sub>1</sub> and S<sub>2</sub> of the domain of a lattice
	 * S<sub>1</sub> &le;<sub>S</sub> S<sub>2</sub> iff: &forall; s<sub>1</sub>
	 * &ni; S<sub>1</sub>, &exist; s<sub>2</sub> &ni; S<sub>2</sub> :
	 * s<sub>1</sub> &le; s<sub>2</sub>.
	 */
	@Override
	public boolean lessOrEqualAux(
			S other)
			throws SemanticException {
		for (L s1 : this.elements) {
			boolean existsGreaterElement = false;
			for (L s2 : other.elements)
				if (s1.lessOrEqual(s2)) {
					existsGreaterElement = true;
					break;
				}
			if (!existsGreaterElement)
				return false;
		}
		return true;
	}

	/**
	 * Yields {@code true} if and only if this element is in Egli-Milner
	 * relation with the given one (represented as &le;<sub>EM</sub>). For two
	 * subset S<sub>1</sub> and S<sub>2</sub> of the domain of a lattice
	 * S<sub>1</sub> &le;<sub>EM</sub> S<sub>2</sub> iff: ( S<sub>1</sub>
	 * &le;<sub>S</sub> S<sub>2</sub> ) AND ( &forall; s<sub>2</sub> &ni;
	 * S<sub>2</sub>, &exist; s<sub>1</sub> &ni; S<sub>1</sub> : s<sub>1</sub>
	 * &le; s<sub>2</sub> ). Where {@link #lessOrEqualAux(NonRedundantPowerset)
	 * &le;<sub>S</sub>} is the less or equal relation between sets. This
	 * operation is not commutative.
	 * 
	 * @param other the other concrete element
	 * 
	 * @return {@code true} if and only if that condition holds
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	public boolean lessOrEqualEgliMilner(
			S other)
			throws SemanticException {
		if (!lessOrEqual(other))
			return false;
		if (isBottom())
			return true;
		for (L s2 : other.elements) {
			boolean existsLowerElement = false;
			for (L s1 : this.elements)
				if (s1.lessOrEqual(s2)) {
					existsLowerElement = true;
					break;
				}
			if (!existsLowerElement)
				return false;
		}
		return true;
	}

	@Override
	public S lubAux(
			S other)
			throws SemanticException {
		Set<L> lub = new HashSet<>(elements);
		lub.addAll(other.elements);
		return mk(lub).removeRedundancy().removeOverlapping();
	}

	@Override
	public S glbAux(
			S other)
			throws SemanticException {
		Set<L> glb = new HashSet<>();
		for (L s1 : this.elements)
			for (L s2 : other.elements)
				glb.add(s1.glb(s2));
		return mk(glb).removeRedundancy().removeOverlapping();
	}

	/**
	 * Perform the wideninig operation between two finite non redundant subsets
	 * of the domain of a lattice following the Egli-Milner widening
	 * implementation shown in this
	 * <a href="https://www.cs.unipr.it/Publications/PDF/Q349.pdf">paper</a>.
	 * Given two subset S<sub>1</sub> and S<sub>2</sub> of the domain of a
	 * lattice widening(S<sub>1</sub>, S<sub>2</sub>) =
	 * h<sup>&nabla;</sup>(S<sub>1</sub>, T<sub>2</sub>), where
	 * h<sup>&nabla;</sup> is a widenining-connected extrapolation heuristic and
	 * T<sub>2</sub> is equal to:
	 * <ul>
	 * <li>S<sub>2</sub> &ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp; if
	 * S<sub>1</sub> &le;<sub>EM</sub> S<sub>2</sub></li>
	 * <li>S<sub>1</sub> +<sub>EM</sub> S<sub>2</sub> &ensp; otherwise</li>
	 * </ul>
	 * where &le;<sub>EM</sub> is the
	 * {@link #lessOrEqualEgliMilner(NonRedundantPowerset) Egli-Milner relation}
	 * and +<sub>EM</sub> is an Egli-Milner connector.
	 */
	@Override
	public S wideningAux(
			S other)
			throws SemanticException {
		S arg = lessOrEqualEgliMilner(other) ? other : EgliMilnerConnector(other);
		return extrapolationHeuristic(arg).removeRedundancy().removeOverlapping();
	}

	/**
	 * An Egli-Milner connector is an upper bound operator for the
	 * {@link #lessOrEqualEgliMilner(NonRedundantPowerset) Egli-Milner relation
	 * &le;<sub>EM</sub>}. An Egli-Milner connector is represented as
	 * +<sub>EM</sub>. Given two subsets S<sub>1</sub> and S<sub>2</sub> of a
	 * domain of a lattice S<sub>1</sub> +<sub>EM</sub> S<sub>2</sub> =
	 * S<sub>3</sub> such that ( S<sub>1</sub> &le;<sub>EM</sub> S<sub>3</sub> )
	 * AND ( S<sub>1</sub> &le;<sub>EM</sub> S<sub>3</sub> ). The default
	 * implementation just performs the lub on the union of the two sets.
	 * 
	 * @param other the other concrete element
	 * 
	 * @return a new set that is &le;<sub>EM</sub> than both this and other
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	protected S EgliMilnerConnector(
			S other)
			throws SemanticException {
		if (elements.isEmpty() && other.elements.isEmpty())
			return mk(Collections.emptySet());
		Set<L> newSet = new HashSet<>();

		L completeLub = elements.iterator().next().bottom();
		for (L element : elements)
			completeLub = completeLub.lub(element);
		for (L element : other.elements)
			completeLub = completeLub.lub(element);

		newSet.add(completeLub);
		return mk(newSet);
	}

	/**
	 * This method implements the widening-connected hextrapolation heuristic
	 * proposed in the
	 * <a href="https://www.cs.unipr.it/Publications/PDF/Q349.pdf">paper</a>
	 * (represented as h<sup>&nabla;</sup>). Given two subsets S<sub>1</sub> and
	 * S<sub>2</sub> of a domain of a lattice:
	 * <p>
	 * h<sup>&nabla;</sup>( S<sub>1</sub>, S<sub>2</sub>) = LUB (S<sub>2</sub>,
	 * &#937;({ s<sub>1</sub> &nabla; s<sub>2</sub> | s<sub>1</sub> &ni;
	 * S<sub>1</sub>, s<sub>2</sub> &ni; S<sub>2</sub>, s<sub>1</sub> &lt;
	 * s<sub>2</sub>}))
	 * </p>
	 * where
	 * <ul>
	 * <li>&#937; is the {@link #removeRedundancy() omega reduction} operator
	 * that removes redundancy from a set,</li>
	 * <li>&nabla; is the widening operator of the underlying lattice,</li>
	 * <li>&lt; is the strict partial order relation of the underlying
	 * lattice,</li>
	 * <li>LUB is the {@link #lubAux(NonRedundantPowerset) least upper bound}
	 * operator between non redundant subsets of the domain of the underlying
	 * lattice.</li>
	 * </ul>
	 * 
	 * @param other the other set to perform the extrapolation with
	 * 
	 * @return the new extrapolated set
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	protected S extrapolationHeuristic(
			S other)
			throws SemanticException {
		Set<L> extrapolatedSet = new HashSet<>();
		for (L s1 : this.elements)
			for (L s2 : other.elements)
				if (s1.lessOrEqual(s2) && !s2.lessOrEqual(s1))
					extrapolatedSet.add(s1.widening(s2));

		return mk(extrapolatedSet).removeRedundancy().lub(other);
	}

	/**
	 * Yields a new concrete set of elements equivalent to this but that is not
	 * redundant. An element x of a subset S of the domain of a lattice is
	 * redundant iff exists another element y in S such that x &le; y. Given a
	 * redundant set is always possible to construct an equivalent set that is
	 * not redundant. This operator is usually called omega reduction
	 * (represented as &#937;). Given a subset S of a domain of a lattice
	 * &#937;(S) = S \ {s &ni; S | ( s = bottom ) OR ( &exist; s' &ni; S. s &le;
	 * s' )}
	 *
	 * @return an equivalent element that is not redundant.
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	public S removeRedundancy()
			throws SemanticException {
		Set<L> newElementsSet = new HashSet<>();
		for (L element : elements)
			if (!element.isBottom()) {
				boolean toRemove = false;
				for (L otherElement : elements)
					if (element.lessOrEqual(otherElement) && !otherElement.lessOrEqual(element)) {
						toRemove = true;
						break;
					}
				if (!toRemove)
					newElementsSet.add(element);
			}

		return mk(newElementsSet);
	}

	/**
	 * Yields a new concrete set less or equal to this that has not overlapping
	 * elements inside. Two elements of a set are overlapping iff their glb is
	 * not bottom. It uses an auxiliary method
	 * removeOverlappingBetweenElelements that knows how to remove the
	 * overlapping between two elements of type E.
	 * 
	 * @return an equivalent set of elements that doesn't have overlapping
	 *             elements.
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	public S removeOverlapping()
			throws SemanticException {
		Set<L> newSet;
		Set<L> tmpSet = elements;

		do {
			newSet = tmpSet;
			tmpSet = new HashSet<>();
			for (L e1 : newSet) {
				boolean intersectionFound = false;
				for (L e2 : newSet)
					if (!e1.glb(e2).isBottom()) {
						L notOverlappingElement = e1.lub(e2);
						if (!tmpSet.contains(notOverlappingElement))
							tmpSet.add(notOverlappingElement);
						intersectionFound = true;
					}
				if (!intersectionFound)
					tmpSet.add(e1);
			}
		} while (tmpSet.size() != newSet.size());

		return mk(tmpSet).removeRedundancy();
	}

}
