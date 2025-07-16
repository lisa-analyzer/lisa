package it.unive.lisa.analysis.numeric;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonRedundantPowerset.NonRedundantSetLattice;
import it.unive.lisa.analysis.nonRedundantPowerset.NonRelationalNonRedundantPowerset;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
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
 * An analysis computing finite non redundant powersets of {@link IntInterval}s,
 * approximating integer values as a non redundant set of intervals.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class NonRedundantIntervals
		extends
		NonRelationalNonRedundantPowerset<NonRedundantIntervals.NonRedIntvLattice, IntInterval> {

	/**
	 * Builds a new non redundant intervals analysis.
	 */
	public NonRedundantIntervals() {
		super(new Interval(), new NonRedIntvLattice());
	}

	/**
	 * A {@link NonRedundantSetLattice} that contains a set of
	 * {@link IntInterval}s.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public static class NonRedIntvLattice
			extends
			NonRedundantSetLattice<NonRedIntvLattice, IntInterval> {

		/**
		 * Builds a new non redundant intervals lattice with an empty set of
		 * elements.
		 */
		public NonRedIntvLattice() {
			super(Collections.emptySet(), IntInterval.TOP);
		}

		private NonRedIntvLattice(
				Set<IntInterval> elements) {
			super(elements, IntInterval.TOP);
		}

		@Override
		public NonRedIntvLattice mk(
				Set<IntInterval> set) {
			return new NonRedIntvLattice(set);
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
		 * S<sub>2</sub> | &exist; s<sub>1</sub> &ni; S<sub>1</sub> :
		 * s<sub>1</sub> &le; s<sub>2</sub>} &cup; {lub(s'<sub>1</sub>,
		 * s<sub>2</sub>) | s'<sub>1</sub> &ni; S<sub>1</sub>, s<sub>2</sub>
		 * &ni; S<sub>2</sub>, NOT &exist; s<sub>1</sub> &ni; S<sub>1</sub> :
		 * s<sub>1</sub> &le; s<sub>2</sub>}
		 * </p>
		 * s'<sub>1</sub> can be chosen randomly but in this case is chosen to
		 * be the closest interval to s<sub>2</sub> (closest based on
		 * {@link #middlePoint(Interval) middle point}).
		 */
		@Override
		protected NonRedIntvLattice EgliMilnerConnector(
				NonRedIntvLattice other)
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
		 * Yields the middle point of an {@link Interval}. If both extremes are
		 * non-infinite the middle point is the sum of the two divided by two.
		 * If only one of the two extreme is infinite the middle point is said
		 * to be the non-infinite extreme. If both the extremes are infinite the
		 * middle point is said to be 0.
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

	@Override
	public ValueEnvironment<NonRedIntvLattice> assumeBinaryExpression(
			ValueEnvironment<NonRedIntvLattice> state,
			BinaryExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		if (state.isBottom())
			return state;

		Identifier id;
		NonRedIntvLattice eval;
		boolean rightIsExpr;
		BinaryOperator operator = expression.getOperator();
		ValueExpression left = (ValueExpression) expression.getLeft();
		ValueExpression right = (ValueExpression) expression.getRight();
		if (left instanceof Identifier) {
			eval = eval(state, right, src, oracle);
			id = (Identifier) left;
			rightIsExpr = true;
		} else if (right instanceof Identifier) {
			eval = eval(state, left, src, oracle);
			id = (Identifier) right;
			rightIsExpr = false;
		} else
			return state;

		NonRedIntvLattice starting = state.getState(id);
		if (eval.isBottom() || starting.isBottom())
			return state.bottom();

		SortedSet<IntInterval> newSet = new TreeSet<>();

		for (IntInterval startingInterval : starting.elements)
			for (IntInterval interval : eval.elements) {
				IntInterval update = Interval.updateValue(operator, rightIsExpr, startingInterval, interval);

				if (update == null)
					// no update, keep the original interval
					newSet.add(startingInterval);
				else if (update.isBottom())
					// update is bottom, so the value is not propagated
					continue;
				else
					// update is not bottom, so add it to the new set
					newSet.add(update);
			}

		NonRedIntvLattice intervals = new NonRedIntvLattice(newSet).removeRedundancy().removeOverlapping();
		if (intervals.isBottom())
			return state.bottom();
		else
			return state.putState(id, intervals);
	}

}
