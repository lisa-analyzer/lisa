package it.unive.lisa.analysis.nonRedundantSet;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.numeric.Interval;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.util.numeric.MathNumber;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * The finite non redundant powerset of {@link Interval} abstract domain
 * approximating integer values as a non redundant set of interval. It is
 * implemented as a {@link NonRedundantPowersetOfBaseNonRelationalValueDomain},
 * which handles most of the basic operation (such as
 * {@link NonRedundantPowersetOfBaseNonRelationalValueDomain#lubAux lub},
 * {@link NonRedundantPowersetOfBaseNonRelationalValueDomain#glbAux glb},
 * {@link NonRedundantPowersetOfBaseNonRelationalValueDomain#wideningAux
 * widening} and others operations needed to calculate the previous ones).
 */
public class NonRedundantPowersetOfInterval
		extends NonRedundantPowersetOfBaseNonRelationalValueDomain<NonRedundantPowersetOfInterval, Interval> {

	/**
	 * Constructs an empty non redundant set of intervals.
	 */
	public NonRedundantPowersetOfInterval() {
		super(new TreeSet<>(), Interval.BOTTOM);
	}

	/**
	 * Constructs a non redundant set of intervals with the given intervals.
	 * 
	 * @param elements the set of intervals
	 */
	public NonRedundantPowersetOfInterval(SortedSet<Interval> elements) {
		super(elements, Interval.BOTTOM);
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
	 * {@link #middlePoint(Interval) middle point}).
	 */
	@Override
	protected NonRedundantPowersetOfInterval EgliMilnerConnector(NonRedundantPowersetOfInterval other)
			throws SemanticException {
		SortedSet<Interval> newElementsSet = new TreeSet<>();
		SortedSet<Interval> notCoverSet = new TreeSet<>();

		// first side of the union
		for (Interval s2 : other.elementsSet) {
			boolean existsLower = false;
			for (Interval s1 : elementsSet)
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
		for (Interval s2 : notCoverSet) {
			MathNumber middlePoint = middlePoint(s2);
			MathNumber closestValue = middlePoint;
			MathNumber closestDiff = closestValue.subtract(middlePoint).abs();
			Interval closest = Interval.TOP;
			for (Interval s1 : elementsSet) {
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
		return new NonRedundantPowersetOfInterval(newElementsSet).removeRedundancy().removeOverlapping();
	}

	/**
	 * Yields the middle point of an {@link Interval}. If both extremes are
	 * non-infinite the middle point is the sum of the two divided by two. If
	 * only one of the two extreme is infinite the middle point is said to be
	 * the non-infinite extreme. If both the extremes are infinite the middle
	 * point is said to be 0.
	 * 
	 * @param interval the interval to calculate the middle point of
	 * 
	 * @return the middle point of the interval
	 */
	public MathNumber middlePoint(Interval interval) {
		if (interval.interval.isFinite())
			return interval.interval.getLow().add(interval.interval.getHigh()).divide(new MathNumber(2));
		else if (interval.interval.getHigh().isFinite() && !interval.interval.getLow().isFinite())
			return interval.interval.getHigh();
		else if (!interval.interval.getHigh().isFinite() && interval.interval.getLow().isFinite())
			return interval.interval.getLow().subtract(MathNumber.ONE);
		// both infinite
		return MathNumber.ZERO;
	}

	@Override
	public NonRedundantPowersetOfInterval top() {
		SortedSet<Interval> topSet = new TreeSet<>();
		topSet.add(Interval.TOP);
		return new NonRedundantPowersetOfInterval(topSet);
	}

	@Override
	public NonRedundantPowersetOfInterval bottom() {
		return new NonRedundantPowersetOfInterval();
	}

	@Override
	public ValueEnvironment<NonRedundantPowersetOfInterval> assumeBinaryExpression(
			ValueEnvironment<NonRedundantPowersetOfInterval> environment, BinaryOperator operator, ValueExpression left,
			ValueExpression right, ProgramPoint src, ProgramPoint dest) throws SemanticException {
		Identifier id;
		NonRedundantPowersetOfInterval eval;
		boolean rightIsExpr;
		if (left instanceof Identifier) {
			eval = eval(right, environment, src);
			id = (Identifier) left;
			rightIsExpr = true;
		} else if (right instanceof Identifier) {
			eval = eval(left, environment, src);
			id = (Identifier) right;
			rightIsExpr = false;
		} else
			return environment;

		if (eval.isBottom())
			return environment.bottom();

		SortedSet<Interval> newSet = new TreeSet<>();
		NonRedundantPowersetOfInterval starting = environment.getState(id);

		for (Interval startingInterval : starting.elementsSet) {
			for (Interval interval : eval.elementsSet) {
				boolean lowIsMinusInfinity = interval.interval.lowIsMinusInfinity();
				Interval low_inf = new Interval(interval.interval.getLow(), MathNumber.PLUS_INFINITY);
				Interval lowp1_inf = new Interval(interval.interval.getLow().add(MathNumber.ONE),
						MathNumber.PLUS_INFINITY);
				Interval inf_high = new Interval(MathNumber.MINUS_INFINITY, interval.interval.getHigh());
				Interval inf_highm1 = new Interval(MathNumber.MINUS_INFINITY,
						interval.interval.getHigh().subtract(MathNumber.ONE));

				if (operator == ComparisonEq.INSTANCE) {
					newSet.add(interval);
				} else if (operator == ComparisonGe.INSTANCE) {
					if (rightIsExpr) {
						if (lowIsMinusInfinity)
							newSet.add(startingInterval);
						else
							newSet.add(startingInterval.glb(low_inf));
					} else
						newSet.add(startingInterval.glb(inf_high));
				} else if (operator == ComparisonGt.INSTANCE) {
					if (rightIsExpr)
						newSet.add(startingInterval.glb(lowp1_inf));
					else
						newSet.add(lowIsMinusInfinity ? interval : startingInterval.glb(inf_highm1));
				} else if (operator == ComparisonLe.INSTANCE) {
					if (rightIsExpr)
						newSet.add(startingInterval.glb(inf_high));
					else if (lowIsMinusInfinity)
						newSet.add(startingInterval);
					else
						newSet.add(startingInterval.glb(low_inf));
				} else if (operator == ComparisonLt.INSTANCE) {
					if (rightIsExpr)
						newSet.add(lowIsMinusInfinity ? interval : startingInterval.glb(inf_highm1));
					else if (lowIsMinusInfinity)
						newSet.add(startingInterval);
					else
						newSet.add(startingInterval.glb(lowp1_inf));
				} else
					newSet.add(startingInterval);
			}
		}
		NonRedundantPowersetOfInterval intervals = new NonRedundantPowersetOfInterval(newSet).removeRedundancy()
				.removeOverlapping();
		environment = environment.putState(id, intervals);
		return environment;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((elementsSet == null) ? 0 : elementsSet.hashCode());
		result = prime * result + ((valueDomain == null) ? 0 : valueDomain.hashCode());
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
		NonRedundantPowersetOfInterval other = (NonRedundantPowersetOfInterval) obj;
		if (elementsSet == null) {
			if (other.elementsSet != null)
				return false;
		} else if (!elementsSet.equals(other.elementsSet))
			return false;
		if (valueDomain == null) {
			if (other.valueDomain != null)
				return false;
		} else if (!valueDomain.equals(other.valueDomain))
			return false;
		return true;
	}

	@Override
	protected NonRedundantPowersetOfInterval mk(SortedSet<Interval> elements) {
		return new NonRedundantPowersetOfInterval(elements);
	}

}
