package it.unive.lisa.util.numeric;

public class IntInterval {

	public static final IntInterval INFINITY = new IntInterval();
	public static final IntInterval ZERO = new IntInterval(0, 0);
	public static final IntInterval ONE = new IntInterval(1, 1);
	public static final IntInterval MINUS_ONE = new IntInterval(-1, -1);

	private final MathNumber low;

	private final MathNumber high;

	private IntInterval() {
		this(MathNumber.MINUS_INFINITY, MathNumber.PLUS_INFINITY);
	}

	public IntInterval(int low, int high) {
		this(new MathNumber(low), new MathNumber(high));
	}

	public IntInterval(Integer low, Integer high) {
		this(low == null ? MathNumber.MINUS_INFINITY : new MathNumber(low),
				high == null ? MathNumber.PLUS_INFINITY : new MathNumber(high));
	}

	public IntInterval(MathNumber low, MathNumber high) {
		if (low.compareTo(high) > 0)
			throw new IllegalArgumentException("Lower bound is bigger than higher bound");

		this.low = low;
		this.high = high;
	}

	/**
	 * Yields the high bound of this interval.
	 * 
	 * @return the high bound of this interval.
	 */
	public MathNumber getHigh() {
		return high;
	}

	/**
	 * Yields the low bound of this interval.
	 * 
	 * @return the low bound of this interval.
	 */
	public MathNumber getLow() {
		return low;
	}

	public boolean lowIsMinusInfinity() {
		return low.isMinusInfinity();
	}

	public boolean highIsPlusInfinity() {
		return high.isPlusInfinity();
	}

	public boolean isInfinite() {
		return this == INFINITY || (highIsPlusInfinity() || lowIsMinusInfinity());
	}

	public boolean isFinite() {
		return !isInfinite();
	}

	public boolean isInfinity() {
		return this == INFINITY;
	}

	public boolean isSingleton() {
		return isFinite() && low.equals(high);
	}

	public boolean is(int n) {
		return isSingleton() && low.is(n);
	}

	private static IntInterval cacheAndRound(IntInterval i) {
		if (i.is(0))
			return ZERO;
		if (i.is(1))
			return ONE;
		if (i.is(-1))
			return MINUS_ONE;
		return new IntInterval(i.low.roundDown(), i.high.roundUp());
	}

	public IntInterval plus(IntInterval other) {
		if (isInfinity() || other.isInfinity())
			return INFINITY;

		return cacheAndRound(new IntInterval(low.add(other.low), high.add(other.high)));
	}

	public IntInterval diff(IntInterval other) {
		if (isInfinity() || other.isInfinity())
			return INFINITY;

		return cacheAndRound(new IntInterval(low.subtract(other.high), high.subtract(other.low)));
	}

	private static MathNumber min(MathNumber... nums) {
		if (nums.length == 0)
			throw new IllegalArgumentException("No numbers provided");

		MathNumber min = nums[0];
		for (int i = 1; i < nums.length; i++)
			min = min.min(nums[i]);

		return min;
	}

	private static MathNumber max(MathNumber... nums) {
		if (nums.length == 0)
			throw new IllegalArgumentException("No numbers provided");

		MathNumber max = nums[0];
		for (int i = 1; i < nums.length; i++)
			max = max.max(nums[i]);

		return max;
	}

	public IntInterval mul(IntInterval other) {
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

	public IntInterval div(IntInterval other, boolean ignoreZero, boolean errorOnZero) {
		if (errorOnZero && (other.is(0) || other.includes(ZERO)))
			throw new ArithmeticException("IntInterval divide by zero");

		if (is(0))
			return ZERO;

		if (!other.includes(ZERO))
			return mul(new IntInterval(MathNumber.ONE.divide(other.high), MathNumber.ONE.divide(other.low)));
		else if (other.high.is(0))
			return mul(new IntInterval(MathNumber.MINUS_INFINITY, MathNumber.ONE.divide(other.low)));
		else if (other.low.is(0))
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
				return cacheAndRound(new IntInterval(lower.low.compareTo(higher.low) > 0 ? higher.low : lower.low,
						lower.high.compareTo(higher.high) < 0 ? higher.high : lower.high));
		}
	}

	public boolean includes(IntInterval other) {
		return low.compareTo(other.low) <= 0 && high.compareTo(other.high) >= 0;
	}

	public boolean intersects(IntInterval other) {
		return includes(other) || other.includes(this) 
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
	public boolean equals(Object obj) {
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
	public String toString() {
		return "[" + low + ", " + high + "]";
	}
}
