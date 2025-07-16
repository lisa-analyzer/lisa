package it.unive.lisa.util.numeric;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.combination.constraints.WholeValueElement;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.collections4.iterators.EmptyIterator;

/**
 * An interval with integer bounds.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class IntInterval
		implements
		Iterable<Long>,
		Comparable<IntInterval>,
		WholeValueElement<IntInterval>,
		BaseLattice<IntInterval> {

	/**
	 * The interval {@code [-Inf, +Inf]}.
	 */
	public static final IntInterval INFINITY = new IntInterval();

	/**
	 * The interval {@code [0, 0]}.
	 */
	public static final IntInterval ZERO = new IntInterval(0, 0);

	/**
	 * The interval {@code [1, 1]}.
	 */
	public static final IntInterval ONE = new IntInterval(1, 1);

	/**
	 * The interval {@code [-1, -1]}.
	 */
	public static final IntInterval MINUS_ONE = new IntInterval(-1, -1);

	/**
	 * The interval {@code [NaN, NaN]}, denoting undefined results of
	 * computations (e.g., {@code [0,+inf]/[-inf,0]}).
	 */
	public static final IntInterval NaN = new IntInterval(MathNumber.NaN, MathNumber.NaN);

	/**
	 * The abstract top ({@code [-Inf, +Inf]}) element.
	 */
	public static final IntInterval TOP = INFINITY;

	/**
	 * The abstract bottom element.
	 */
	public static final IntInterval BOTTOM = new IntInterval((Integer) null, null);

	private final MathNumber low;

	private final MathNumber high;

	private IntInterval() {
		this(MathNumber.MINUS_INFINITY, MathNumber.PLUS_INFINITY);
	}

	/**
	 * Builds a new interval. Order of the bounds is adjusted (i.e., if
	 * {@code low} is greater then {@code high}, then the interval
	 * {@code [high, low]} is created).
	 * 
	 * @param low  the lower bound
	 * @param high the upper bound
	 */
	public IntInterval(
			int low,
			int high) {
		this(new MathNumber(low), new MathNumber(high));
	}

	/**
	 * Builds a new interval. Order of the bounds is adjusted (i.e., if
	 * {@code low} is greater then {@code high}, then the interval
	 * {@code [high, low]} is created). Note that if both bounds are
	 * {@code null}, the bottom element is created.
	 * 
	 * @param low  the lower bound (if {@code null}, -inf will be used)
	 * @param high the upper bound (if {@code null}, +inf will be used)
	 */
	public IntInterval(
			Integer low,
			Integer high) {
		this(handleNulls(low, high, true), handleNulls(low, high, false));
	}

	private static MathNumber handleNulls(
			Integer low,
			Integer high,
			boolean isLowBound) {
		if (low == null && high == null)
			return null;
		if (isLowBound)
			return low == null ? MathNumber.MINUS_INFINITY : new MathNumber(low);
		else
			return high == null ? MathNumber.PLUS_INFINITY : new MathNumber(high);
	}

	/**
	 * Builds a new interval. Order of the bounds is adjusted (i.e., if
	 * {@code low} is greater then {@code high}, then the interval
	 * {@code [high, low]} is created). Note that if both bounds are
	 * {@code null}, the bottom element is created.
	 * 
	 * @param low  the lower bound
	 * @param high the upper bound
	 */
	public IntInterval(
			MathNumber low,
			MathNumber high) {
		if (low == null && high == null) {
			// we only alllow null if they are both null
			// to support the bottom element
			this.low = null;
			this.high = null;
		} else {
			Objects.requireNonNull(low, "Low bound must not be null");
			Objects.requireNonNull(high, "High bound must not be null");
			if (low.isNaN() || high.isNaN()) {
				this.low = MathNumber.NaN;
				this.high = MathNumber.NaN;
			} else if (low.compareTo(high) <= 0) {
				this.low = low;
				this.high = high;
			} else {
				this.low = high;
				this.high = low;
			}
		}
	}

	/**
	 * Yields the upper bound of this interval. This might be null if
	 * {@link #isBottom()} yields {@code true}.
	 * 
	 * @return the upper bound of this interval
	 */
	public MathNumber getHigh() {
		return high;
	}

	/**
	 * Yields the lower bound of this interval. This might be null if
	 * {@link #isBottom()} yields {@code true}.
	 * 
	 * @return the lower bound of this interval
	 */
	public MathNumber getLow() {
		return low;
	}

	/**
	 * Yields {@code true} if the lower bound of this interval is set to minus
	 * infinity.
	 * 
	 * @return {@code true} if that condition holds
	 */
	public boolean lowIsMinusInfinity() {
		return !isBottom() && low.isMinusInfinity();
	}

	/**
	 * Yields {@code true} if the upper bound of this interval is set to plus
	 * infinity.
	 * 
	 * @return {@code true} if that condition holds
	 */
	public boolean highIsPlusInfinity() {
		return !isBottom() && high.isPlusInfinity();
	}

	/**
	 * Yields {@code true} if this is interval is not finite, that is, if at
	 * least one bound is set to infinity.
	 * 
	 * @return {@code true} if that condition holds
	 */
	public boolean isInfinite() {
		return !isBottom() && (this == INFINITY || (highIsPlusInfinity() || lowIsMinusInfinity()));
	}

	/**
	 * Yields {@code true} if this is interval is finite, that is, if neither
	 * bound is set to infinity.
	 * 
	 * @return {@code true} if that condition holds
	 */
	public boolean isFinite() {
		return !isBottom() && !isInfinite();
	}

	/**
	 * Yields {@code true} if this is the interval representing infinity, that
	 * is, {@code [-Inf, +Inf]}.
	 * 
	 * @return {@code true} if that condition holds
	 */
	public boolean isInfinity() {
		return this == INFINITY;
	}

	/**
	 * Yields {@code true} if this is a singleton interval, that is, if the
	 * lower bound and the upper bound are the same.
	 * 
	 * @return {@code true} if that condition holds
	 */
	public boolean isSingleton() {
		return isFinite() && low.equals(high);
	}

	/**
	 * Yields {@code true} if this is a singleton interval containing only
	 * {@code n}.
	 *
	 * @param n the integer to test
	 * 
	 * @return {@code true} if that condition holds
	 */
	public boolean is(
			int n) {
		return !isBottom() && isSingleton() && low.is(n);
	}

	private static IntInterval cacheAndRound(
			IntInterval i) {
		if (i.isBottom() || i.isTop())
			return i;
		if (i.is(0))
			return ZERO;
		if (i.is(1))
			return ONE;
		if (i.is(-1))
			return MINUS_ONE;
		return new IntInterval(i.low.roundDown(), i.high.roundUp());
	}

	/**
	 * Performs the interval addition between {@code this} and {@code other}.
	 * 
	 * @param other the other interval
	 * 
	 * @return {@code this + other}
	 */
	public IntInterval plus(
			IntInterval other) {
		if (isBottom() || other.isBottom())
			return BOTTOM;
		if (isInfinity() || other.isInfinity())
			return INFINITY;

		return cacheAndRound(new IntInterval(low.add(other.low), high.add(other.high)));
	}

	/**
	 * Performs the interval subtraction between {@code this} and {@code other}.
	 * 
	 * @param other the other interval
	 * 
	 * @return {@code this - other}
	 */
	public IntInterval diff(
			IntInterval other) {
		if (isBottom() || other.isBottom())
			return BOTTOM;
		if (isInfinity() || other.isInfinity())
			return INFINITY;

		return cacheAndRound(new IntInterval(low.subtract(other.high), high.subtract(other.low)));
	}

	private static MathNumber min(
			MathNumber... nums) {
		if (nums.length == 0)
			throw new IllegalArgumentException("No numbers provided");

		MathNumber min = nums[0];
		for (int i = 1; i < nums.length; i++)
			min = min.min(nums[i]);

		return min;
	}

	private static MathNumber max(
			MathNumber... nums) {
		if (nums.length == 0)
			throw new IllegalArgumentException("No numbers provided");

		MathNumber max = nums[0];
		for (int i = 1; i < nums.length; i++)
			max = max.max(nums[i]);

		return max;
	}

	/**
	 * Performs the interval multiplication between {@code this} and
	 * {@code other}.
	 * 
	 * @param other the other interval
	 * 
	 * @return {@code this * other}
	 */
	public IntInterval mul(
			IntInterval other) {
		if (isBottom() || other.isBottom())
			return BOTTOM;
		if (is(0) || other.is(0))
			return ZERO;
		if (isInfinity() || other.isInfinity())
			return INFINITY;

		if (low.compareTo(MathNumber.ZERO) >= 0 && other.low.compareTo(MathNumber.ZERO) >= 0)
			return cacheAndRound(new IntInterval(low.multiply(other.low), high.multiply(other.high)));

		MathNumber ll = low.multiply(other.low);
		MathNumber lh = low.multiply(other.high);
		MathNumber hl = high.multiply(other.low);
		MathNumber hh = high.multiply(other.high);
		return cacheAndRound(new IntInterval(min(ll, lh, hl, hh), max(ll, lh, hl, hh)));
	}

	/**
	 * Performs the interval division between {@code this} and {@code other}.
	 * 
	 * @param other       the other interval
	 * @param ignoreZero  if {@code true}, causes the division to ignore the
	 *                        fact that {@code other} might contain 0, producing
	 *                        a smaller result
	 * @param errorOnZero whether or not an {@link ArithmeticException} should
	 *                        be thrown immediately if {@code other} contains
	 *                        zero
	 * 
	 * @return {@code this / other}
	 * 
	 * @throws ArithmeticException if {@code other} contains 0 and
	 *                                 {@code errorOnZero} is set to
	 *                                 {@code true}
	 */
	public IntInterval div(
			IntInterval other,
			boolean ignoreZero,
			boolean errorOnZero) {
		if (isBottom() || other.isBottom())
			return BOTTOM;
		if (errorOnZero && (other.is(0) || other.includes(ZERO)))
			throw new ArithmeticException("IntInterval divide by zero");

		if (is(0))
			return ZERO;
		if (other.is(0))
			return TOP;

		if (!other.includes(ZERO))
			return mul(new IntInterval(MathNumber.ONE.divide(other.high), MathNumber.ONE.divide(other.low)));
		else if (other.high.isZero())
			return mul(new IntInterval(MathNumber.MINUS_INFINITY, MathNumber.ONE.divide(other.low)));
		else if (other.low.isZero())
			return mul(new IntInterval(MathNumber.ONE.divide(other.high), MathNumber.PLUS_INFINITY));
		else if (ignoreZero)
			return mul(new IntInterval(MathNumber.ONE.divide(other.low), MathNumber.ONE.divide(other.high)));
		else {
			IntInterval lower = mul(new IntInterval(MathNumber.MINUS_INFINITY, MathNumber.ONE.divide(other.low)));
			IntInterval higher = mul(new IntInterval(MathNumber.ONE.divide(other.high), MathNumber.PLUS_INFINITY));

			if (lower.includes(higher))
				return lower;
			else if (higher.includes(lower))
				return higher;
			else
				return cacheAndRound(
						new IntInterval(
								lower.low.compareTo(higher.low) > 0 ? higher.low : lower.low,
								lower.high.compareTo(higher.high) < 0 ? higher.high : lower.high));
		}
	}

	/**
	 * Yields {@code true} if this interval includes the given one.
	 * 
	 * @param other the other interval
	 * 
	 * @return {@code true} if it is included, {@code false} otherwise
	 */
	public boolean includes(
			IntInterval other) {
		if (isBottom() || other.isBottom())
			return false;
		return low.compareTo(other.low) <= 0 && high.compareTo(other.high) >= 0;
	}

	/**
	 * Yields {@code true} if this interval intersects with the given one.
	 * 
	 * @param other the other interval
	 * 
	 * @return {@code true} if those intersects, {@code false} otherwise
	 */
	public boolean intersects(
			IntInterval other) {
		if (isBottom() || other.isBottom())
			return false;
		return includes(other)
				|| other.includes(this)
				|| (high.compareTo(other.low) >= 0 && high.compareTo(other.high) <= 0)
				|| (other.high.compareTo(low) >= 0 && other.high.compareTo(high) <= 0);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((high == null) ? 0 : high.hashCode());
		result = prime * result + ((low == null) ? 0 : low.hashCode());
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
		IntInterval other = (IntInterval) obj;
		if (high == null) {
			if (other.high != null)
				return false;
		} else if (!high.equals(other.high))
			return false;
		if (low == null) {
			if (other.low != null)
				return false;
		} else if (!low.equals(other.low))
			return false;
		return true;
	}

	@Override
	public Iterator<Long> iterator() {
		if (isBottom())
			return EmptyIterator.emptyIterator();
		if (!low.isFinite() || !high.isFinite() || low.isNaN() || high.isNaN())
			throw new InfiniteIterationException(this);
		try {
			return new IntIntervalIterator(low.toLong(), high.toLong());
		} catch (MathNumberConversionException e) {
			throw new InfiniteIterationException(this);
		}
	}

	@Override
	public int compareTo(
			IntInterval o) {
		if (isBottom())
			return o.isBottom() ? 0 : -1;
		if (isTop())
			return o.isTop() ? 0 : 1;
		if (o.isBottom())
			return 1;
		if (o.isTop())
			return -1;
		int cmp;
		if ((cmp = low.compareTo(o.low)) != 0)
			return cmp;
		return high.compareTo(o.high);
	}

	@Override
	public IntInterval top() {
		return TOP;
	}

	@Override
	public boolean isTop() {
		return lowIsMinusInfinity() && highIsPlusInfinity();
	}

	@Override
	public IntInterval bottom() {
		return BOTTOM;
	}

	@Override
	public boolean isBottom() {
		return low == null && high == null;
	}

	@Override
	public StructuredRepresentation representation() {
		if (isBottom())
			return Lattice.bottomRepresentation();

		return new StringRepresentation(
				"[" + low + ", " + high + "]");
	}

	@Override
	public String toString() {
		return representation().toString();
	}

	@Override
	public IntInterval lubAux(
			IntInterval other)
			throws SemanticException {
		MathNumber newLow = getLow().min(other.getLow());
		MathNumber newHigh = getHigh().max(other.getHigh());
		return newLow.isMinusInfinity() && newHigh.isPlusInfinity() ? top() : new IntInterval(newLow, newHigh);
	}

	@Override
	public IntInterval glbAux(
			IntInterval other) {
		MathNumber newLow = getLow().max(other.getLow());
		MathNumber newHigh = getHigh().min(other.getHigh());

		if (newLow.compareTo(newHigh) > 0)
			return bottom();
		return newLow.isMinusInfinity() && newHigh.isPlusInfinity() ? top() : new IntInterval(newLow, newHigh);
	}

	@Override
	public IntInterval wideningAux(
			IntInterval other)
			throws SemanticException {
		MathNumber newLow, newHigh;
		if (other.getHigh().compareTo(getHigh()) > 0)
			newHigh = MathNumber.PLUS_INFINITY;
		else
			newHigh = getHigh();

		if (other.getLow().compareTo(getLow()) < 0)
			newLow = MathNumber.MINUS_INFINITY;
		else
			newLow = getLow();

		return newLow.isMinusInfinity() && newHigh.isPlusInfinity() ? top() : new IntInterval(newLow, newHigh);
	}

	@Override
	public IntInterval narrowingAux(
			IntInterval other)
			throws SemanticException {
		MathNumber newLow, newHigh;
		newHigh = getHigh().isInfinite() ? other.getHigh() : getHigh();
		newLow = getLow().isInfinite() ? other.getLow() : getLow();
		return new IntInterval(newLow, newHigh);
	}

	@Override
	public boolean lessOrEqualAux(
			IntInterval other)
			throws SemanticException {
		return other.includes(this);
	}

	@Override
	public Set<BinaryExpression> constraints(
			ValueExpression e,
			ProgramPoint pp)
			throws SemanticException {
		if (isTop())
			return Collections.emptySet();
		if (isBottom())
			return null;

		BinaryExpression lbound, ubound;
		try {
			ubound = new BinaryExpression(
					pp.getProgram().getTypes().getBooleanType(),
					new Constant(pp.getProgram().getTypes().getIntegerType(), getHigh().toInt(), pp.getLocation()),
					e,
					ComparisonGe.INSTANCE,
					e.getCodeLocation());
		} catch (MathNumberConversionException e1) {
			ubound = null;
		}

		try {
			lbound = new BinaryExpression(
					pp.getProgram().getTypes().getBooleanType(),
					new Constant(pp.getProgram().getTypes().getIntegerType(), getLow().toInt(), pp.getLocation()),
					e,
					ComparisonLe.INSTANCE,
					e.getCodeLocation());
		} catch (MathNumberConversionException e1) {
			lbound = null;
		}

		if (getLow().isMinusInfinity())
			return Collections.singleton(ubound);
		if (getHigh().isPlusInfinity())
			return Collections.singleton(lbound);
		return Set.of(lbound, ubound);
	}

	@Override
	public IntInterval generate(
			Set<BinaryExpression> constraints,
			ProgramPoint pp)
			throws SemanticException {
		if (constraints == null)
			return bottom();

		Integer ge = null, le = null;
		for (BinaryExpression expr : constraints)
			if (expr.getLeft() instanceof Constant && ((Constant) expr.getLeft()).getValue() instanceof Integer) {
				Integer val = (Integer) ((Constant) expr.getLeft()).getValue();
				if (expr.getOperator() instanceof ComparisonEq)
					return new IntInterval(val, val);
				else if (expr.getOperator() instanceof ComparisonGe)
					ge = val;
				else if (expr.getOperator() instanceof ComparisonLe)
					le = val;
			}

		if (ge == null && le == null)
			return TOP;

		return new IntInterval(le, ge);
	}

}
