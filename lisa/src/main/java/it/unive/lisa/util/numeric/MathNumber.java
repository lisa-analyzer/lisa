package it.unive.lisa.util.numeric;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MathNumber implements Comparable<MathNumber> {

	public static final MathNumber PLUS_INFINITY = new MathNumber(true);
	public static final MathNumber MINUS_INFINITY = new MathNumber(false);
	public static final MathNumber ZERO = new MathNumber(0);
	public static final MathNumber ONE = new MathNumber(1);
	public static final MathNumber MINUS_ONE = new MathNumber(-1);

	private final BigDecimal number;

	/**
	 * True means this number is positive or zero
	 */
	private final boolean sign;

	public MathNumber(long number) {
		this.number = BigDecimal.valueOf(number);
		this.sign = number >= 0;
	}

	public MathNumber(double number) {
		this.number = BigDecimal.valueOf(number);
		this.sign = number >= 0;
	}

	public MathNumber(BigDecimal number) {
		this.number = number;
		this.sign = number.signum() >= 0;
	}

	private MathNumber(boolean sign) {
		this.number = null;
		this.sign = sign;
	}

	public boolean isMinusInfinity() {
		return number == null && !sign;
	}

	public boolean isPlusInfinity() {
		return number == null && sign;
	}

	public boolean isInfinite() {
		return this == PLUS_INFINITY || this == MINUS_INFINITY;
	}

	public boolean isFinite() {
		return !isInfinite();
	}

	public boolean is(int n) {
		return number != null && number.equals(new BigDecimal(n));
	}

	private static MathNumber cached(MathNumber i) {
		if (i.is(0))
			return ZERO;
		if (i.is(1))
			return ONE;
		if (i.is(-1))
			return MINUS_ONE;
		return i;
	}

	public MathNumber add(MathNumber other) {
		if (isPlusInfinity() || other.isPlusInfinity())
			return PLUS_INFINITY;

		if (isMinusInfinity() || other.isMinusInfinity())
			return MINUS_INFINITY;

		return cached(new MathNumber(number.add(other.number)));
	}

	public MathNumber subtract(MathNumber other) {
		if (isPlusInfinity() || other.isPlusInfinity())
			return PLUS_INFINITY;

		if (isMinusInfinity() || other.isMinusInfinity())
			return MINUS_INFINITY;

		return cached(new MathNumber(number.subtract(other.number)));
	}

	public MathNumber multiply(MathNumber other) {
		if (is(0) || other.is(0))
			return ZERO;

		if ((isPlusInfinity() && !other.sign)
				|| (other.isPlusInfinity() && !sign)
				|| (isMinusInfinity() && other.sign)
				|| (other.isMinusInfinity() && sign))
			return MINUS_INFINITY;

		if ((isMinusInfinity() && !other.sign)
				|| (other.isMinusInfinity() && !sign)
				|| (isPlusInfinity() && other.sign)
				|| (other.isPlusInfinity() && sign))
			return PLUS_INFINITY;

		return cached(new MathNumber(number.multiply(other.number)));
	}

	public MathNumber divide(MathNumber other) {
		if (other.is(0))
			throw new ArithmeticException("MathInt divide by zero");

		if (is(0))
			return ZERO;

		if (other.isPlusInfinity() || other.isMinusInfinity())
			return ZERO;

		if (isPlusInfinity() || isMinusInfinity())
			return this;

		return cached(new MathNumber(number.divide(other.number, 100, RoundingMode.HALF_UP).stripTrailingZeros()));
	}

	@Override
	public int compareTo(MathNumber other) {
		if (equals(other))
			return 0;

		if (isMinusInfinity() || other.isPlusInfinity() || (!sign && other.sign))
			return -1;

		if (isPlusInfinity() || other.isMinusInfinity() || (sign && !other.sign))
			return 1;

		return number.compareTo(other.number);
	}

	public MathNumber min(MathNumber other) {
		if (isMinusInfinity() || other.isPlusInfinity())
			return this;

		if (other.isMinusInfinity() || isPlusInfinity())
			return other;

		return cached(new MathNumber(number.min(other.number)));
	}

	public MathNumber max(MathNumber other) {
		if (other.isMinusInfinity() || isPlusInfinity())
			return this;

		if (isMinusInfinity() || other.isPlusInfinity())
			return other;

		return cached(new MathNumber(number.max(other.number)));
	}

	public MathNumber roundUp() {
		if (isInfinite())
			return this;
		return cached(new MathNumber(number.setScale(0, RoundingMode.CEILING)));
	}

	public MathNumber roundDown() {
		if (isInfinite())
			return this;
		return cached(new MathNumber(number.setScale(0, RoundingMode.FLOOR)));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((number == null) ? 0 : number.hashCode());
		result = prime * result + (sign ? 1231 : 1237);
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
		MathNumber other = (MathNumber) obj;
		if (number == null) {
			if (other.number != null)
				return false;
		} else if (!number.equals(other.number))
			return false;
		if (sign != other.sign)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return isMinusInfinity() ? "-Inf" : isPlusInfinity() ? "+Inf" : number.toString();
	}
}
