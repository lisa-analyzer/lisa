package it.unive.lisa.lattices.numeric;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonRedundantPowerset.NonRedundantSetLattice;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.lisa.util.representation.SetRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Collections;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A {@link NonRedundantSetLattice} that contains a set of {@link IntInterval}s.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class NonRedundantIntervalSet
		extends
		NonRedundantSetLattice<NonRedundantIntervalSet, IntInterval> {

	/**
	 * Builds a new non redundant intervals lattice with an empty set of
	 * elements.
	 */
	public NonRedundantIntervalSet() {
		super(Collections.emptySet(), IntInterval.TOP);
	}

	/**
	 * Builds a new non redundant intervals lattice with the given set of
	 * elements.
	 * 
	 * @param elements the set of elements of this lattice
	 */
	public NonRedundantIntervalSet(
			Set<IntInterval> elements) {
		super(elements, IntInterval.TOP);
	}

	@Override
	public NonRedundantIntervalSet mk(
			Set<IntInterval> set) {
		return new NonRedundantIntervalSet(set);
	}

	@Override
	public StructuredRepresentation representation() {
		if (isTop())
			return new SetRepresentation(Set.of(new StringRepresentation("[-Inf, +Inf]")));
		return super.representation();
	}

	/**
	 * This specific Egli-Milner connector follows this definition:<br>
	 * given two subsets S<sub>1</sub> and S<sub>2</sub> of a domain of a
	 * lattice:
	 * <p>
	 * S<sub>1</sub> +<sub>EM</sub> S<sub>2</sub> = {s<sub>2</sub> &ni;
	 * S<sub>2</sub> | &exist; s<sub>1</sub> &ni; S<sub>1</sub> : s<sub>1</sub>
	 * &le; s<sub>2</sub>} &cup; {lub(s'<sub>1</sub>, s<sub>2</sub>) |
	 * s'<sub>1</sub> &ni; S<sub>1</sub>, s<sub>2</sub> &ni; S<sub>2</sub>, NOT
	 * &exist; s<sub>1</sub> &ni; S<sub>1</sub> : s<sub>1</sub> &le;
	 * s<sub>2</sub>}
	 * </p>
	 * s'<sub>1</sub> can be chosen randomly but in this case is chosen to be
	 * the closest interval to s<sub>2</sub> (closest based on
	 * {@link #middlePoint(IntInterval) middle point}).
	 */
	@Override
	protected NonRedundantIntervalSet EgliMilnerConnector(
			NonRedundantIntervalSet other)
			throws SemanticException {
		SortedSet<IntInterval> newElementsSet = new TreeSet<>();
		SortedSet<IntInterval> notCoverSet = new TreeSet<>();

		// first side of the union
		for (IntInterval s2 : other.elements) {
			boolean existsLower = false;
			for (IntInterval s1 : elements)
				if (s1.lessOrEqual(s2)) {
					existsLower = true;
					break;
				}
			if (existsLower)
				newElementsSet.add(s2);
			else
				notCoverSet.add(s2);
		}

		// second side of the union
		for (IntInterval s2 : notCoverSet) {
			MathNumber middlePoint = middlePoint(s2);
			MathNumber closestValue = middlePoint;
			MathNumber closestDiff = closestValue.subtract(middlePoint).abs();
			IntInterval closest = IntInterval.TOP;
			for (IntInterval s1 : elements) {
				if (closestValue.compareTo(middlePoint) == 0) {
					closest = s1;
					closestValue = middlePoint(s1);
					closestDiff = closestValue.subtract(middlePoint).abs();
				} else {
					MathNumber s1Diff = middlePoint(s1).subtract(middlePoint).abs();
					if (s1Diff.compareTo(closestDiff) < 0) {
						closest = s1;
						closestValue = middlePoint(s1);
						closestDiff = closestValue.subtract(middlePoint).abs();
					}
				}
			}
			newElementsSet.add(s2.lub(closest));
		}
		return mk(newElementsSet).removeRedundancy().removeOverlapping();
	}

	/**
	 * Yields the middle point of an {@link IntInterval}. If both extremes are
	 * non-infinite the middle point is the sum of the two divided by two. If
	 * only one of the two extreme is infinite the middle point is said to be
	 * the non-infinite extreme. If both the extremes are infinite the middle
	 * point is said to be 0.
	 * 
	 * @param interval the interval to calculate the middle point of
	 * 
	 * @return the middle point of the interval
	 */
	protected MathNumber middlePoint(
			IntInterval interval) {
		if (interval.isFinite())
			return interval.getLow().add(interval.getHigh()).divide(new MathNumber(2));
		else if (interval.getHigh().isFinite() && !interval.getLow().isFinite())
			return interval.getHigh();
		else if (!interval.getHigh().isFinite() && interval.getLow().isFinite())
			return interval.getLow().subtract(MathNumber.ONE);
		// both infinite
		return MathNumber.ZERO;
	}

}
