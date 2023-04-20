package it.unive.lisa.util.numeric;

import it.unive.lisa.util.collections.CollectionUtilities;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * A wrapper around {@link BigDecimal} to represent the mathematical concept of
 * a number, that can be also plus or minus infinity, in a convenient way.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class MathNumber implements Comparable<MathNumber> {

	/**
	 * The constant for plus infinity.
	 */
	public static final MathNumber PLUS_INFINITY = new MathNumber((byte) 1);

	/**
	 * The constant for minus infinity.
	 */
	public static final MathNumber MINUS_INFINITY = new MathNumber((byte) -1);

	/**
	 * The constant {@code 0}.
	 */
	public static final MathNumber ZERO = new MathNumber(0);

	/**
	 * The constant {@code 1}.
	 */
	public static final MathNumber ONE = new MathNumber(1);

	/**
	 * The constant {@code -1}.
	 */
	public static final MathNumber MINUS_ONE = new MathNumber(-1);

	/**
	 * A constant for representing numbers obtained from an operation that does
	 * not produce a result (e.g. infinity divided by infinity).
	 */
	public static final MathNumber NaN = new MathNumber((byte) 3);

	private final BigDecimal number;

	/**
	 * -1, 0, or 1 as this number is negative, zero, or positive. 3 means NaN.
	 */
	private final byte sign;

	/**
	 * Builds a math number representing the given value.
	 * 
	 * @param number the value
	 */
	public MathNumber(long number) {
		this.number = BigDecimal.valueOf(number);
		this.sign = number > 0 ? (byte) 1 : number == 0 ? (byte) 0 : (byte) -1;
	}

	/**
	 * Builds a math number representing the given value.
	 * 
	 * @param number the value
	 */
	public MathNumber(double number) {
		this.number = BigDecimal.valueOf(number);
		this.sign = number > 0 ? (byte) 1 : number == 0 ? (byte) 0 : (byte) -1;
	}

	/**
	 * Builds a math number representing the given value.
	 * 
	 * @param number the value
	 */
	public MathNumber(BigDecimal number) {
		this.number = number;
		this.sign = number.signum() > 0 ? (byte) 1 : number.signum() == 0 ? (byte) 0 : (byte) -1;
	}

	private MathNumber(byte sign) {
		this.number = null;
		this.sign = sign;
	}

	/**
	 * Yields {@code true} if this number is minus infinity.
	 * 
	 * @return {@code true} if that condition holds
	 */
	public boolean isMinusInfinity() {
		return number == null && isNegative();
	}

	/**
	 * Yields {@code true} if this number is plus infinity.
	 * 
	 * @return {@code true} if that condition holds
	 */
	public boolean isPlusInfinity() {
		return number == null && isPositive();
	}

	/**
	 * Yields {@code true} if this number is infinite (i.e. plus or minus
	 * infinity).
	 * 
	 * @return {@code true} if that condition holds
	 */
	public boolean isInfinite() {
		return isPlusInfinity() || isMinusInfinity();
	}

	/**
	 * Yields {@code true} if this number is finite (i.e. not infinity).
	 * 
	 * @return {@code true} if that condition holds
	 */
	public boolean isFinite() {
		return !isInfinite();
	}

	/**
	 * Yields {@code true} if this number is number represents exactly the given
	 * integer.
	 *
	 * @param n the integer to test
	 * 
	 * @return {@code true} if that condition holds
	 */
	public boolean is(int n) {
		return number != null && number.equals(new BigDecimal(n));
	}

	/**
	 * Yields {@code true} if this number is equal to zero.
	 * 
	 * @return {@code true} if that condition holds
	 */
	public boolean isZero() {
		return sign == (byte) 0;
	}

	/**
	 * Yields {@code true} if this number is negative.
	 * 
	 * @return {@code true} if that condition holds
	 */
	public boolean isNegative() {
		return sign == (byte) -1;
	}

	/**
	 * Yields {@code true} if this number is positive.
	 * 
	 * @return {@code true} if that condition holds
	 */
	public boolean isPositive() {
		return sign == (byte) 1;
	}

	/**
	 * Yields {@code true} if this number is not a number, that is, obtained
	 * from an operation that does not produce a result (e.g. infinity divided
	 * by infinity).
	 * 
	 * @return {@code true} if that condition holds
	 */
	public boolean isNaN() {
		return number == null && sign == (byte) 3;
	}

	private static MathNumber cached(MathNumber i) {
		if (i.isZero())
			return ZERO;
		if (i.is(1))
			return ONE;
		if (i.is(-1))
			return MINUS_ONE;
		return i;
	}

	/**
	 * Yields the result of {@code this + other}. If one of them is not a number
	 * (according to {@link #isNaN()}), then {@link #NaN} is returned.
	 * 
	 * @param other the other operand
	 * 
	 * @return {@code this + other}
	 */
	public MathNumber add(MathNumber other) {
		if (isNaN() || other.isNaN())
			return NaN;

		if (isInfinite())
			if (other.isFinite())
				return this;
			else if (equals(other))
				return this;
			else
				// addition between infinities of opposing signs is undefined
				return NaN;
		else if (other.isInfinite())
			// this is finite
			return other;

		return cached(new MathNumber(number.add(other.number)));
	}

	/**
	 * Yields the result of {@code this - other}. If one of them is not a number
	 * (according to {@link #isNaN()}), then {@link #NaN} is returned.
	 * 
	 * @param other the other operand
	 * 
	 * @return {@code this - other}
	 */
	public MathNumber subtract(MathNumber other) {
		if (isNaN() || other.isNaN())
			return NaN;

		if (isInfinite())
			if (other.isFinite())
				return this;
			else if (equals(other))
				// subtraction between infinities of same sign is undefined
				return NaN;
			else
				return this;
		else if (other.isInfinite())
			// this is finite
			return other.multiply(MINUS_ONE);

		return cached(new MathNumber(number.subtract(other.number)));
	}

	/**
	 * Yields the result of {@code this * other}. If one of them is not a number
	 * (according to {@link #isNaN()}), then {@link #NaN} is returned.
	 * 
	 * @param other the other factor
	 * 
	 * @return {@code this * other}
	 */
	public MathNumber multiply(MathNumber other) {
		if (isNaN() || other.isNaN())
			return NaN;

		if (isInfinite())
			if (other.isZero())
				// 0 times infinity is undefined
				return NaN;
			else if (sign == other.sign)
				return PLUS_INFINITY;
			else
				return MINUS_INFINITY;
		else if (other.isInfinite())
			if (isZero())
				// 0 times infinity is undefined
				return NaN;
			else if (sign == other.sign)
				return PLUS_INFINITY;
			else
				return MINUS_INFINITY;

		if (isZero() || other.isZero())
			return ZERO;

		return cached(new MathNumber(number.multiply(other.number)));
	}

	/**
	 * Yields the result of {@code this / other}. This method returns
	 * {@link #NaN} if one of the argument is not a number (according to
	 * {@link #isNaN()}), if {@code other} is zero (according to
	 * {@link #isZero()}), or if both arguments are infinite (according to
	 * {@link #isInfinite()}).
	 * 
	 * @param other the divisor
	 * 
	 * @return {@code this / other}
	 */
	public MathNumber divide(MathNumber other) {
		if (isNaN() || other.isNaN() || other.isZero() || (isInfinite() && other.isInfinite()))
			return NaN;

		if (isZero())
			return ZERO;

		if (other.isPlusInfinity() || other.isMinusInfinity())
			return ZERO;

		if (isPlusInfinity() || isMinusInfinity())
			if (isPositive() == other.isPositive())
				return PLUS_INFINITY;
			else
				return MINUS_INFINITY;

		return cached(new MathNumber(number.divide(other.number, 100, RoundingMode.HALF_UP).stripTrailingZeros()));
	}

	@Override
	public int compareTo(MathNumber other) {
		if (equals(other))
			return 0;

		if (isNaN() && !other.isNaN())
			return -1;

		if (!isNaN() && other.isNaN())
			return 1;

		if (isNaN())
			return 0;

		int s = Byte.compare(sign, other.sign);
		if (s != 0)
			return s;

		if (isMinusInfinity() || other.isPlusInfinity())
			return -1;

		if (isPlusInfinity() || other.isMinusInfinity())
			return 1;

		return CollectionUtilities.nullSafeCompare(true, number, other.number, BigDecimal::compareTo);
	}

	/**
	 * Yields the minimum number between {@code this} and {@code other}. If one
	 * of them is not a number (according to {@link #isNaN()}), then
	 * {@link #NaN} is returned.
	 * 
	 * @param other the other number
	 * 
	 * @return the minimum between {@code this} and {@code other}
	 */
	public MathNumber min(MathNumber other) {
		if (isNaN() || other.isNaN())
			return NaN;

		if (isMinusInfinity() || other.isPlusInfinity())
			return this;

		if (other.isMinusInfinity() || isPlusInfinity())
			return other;

		return cached(new MathNumber(number.min(other.number)));
	}

	/**
	 * Yields the maximum number between {@code this} and {@code other}. If one
	 * of them is not a number (according to {@link #isNaN()}), then
	 * {@link #NaN} is returned.
	 * 
	 * @param other the other number
	 * 
	 * @return the maximum between {@code this} and {@code other}
	 */
	public MathNumber max(MathNumber other) {
		if (isNaN() || other.isNaN())
			return NaN;

		if (other.isMinusInfinity() || isPlusInfinity())
			return this;

		if (isMinusInfinity() || other.isPlusInfinity())
			return other;

		return cached(new MathNumber(number.max(other.number)));
	}

	/**
	 * Yields {@code true} if this number is less than or equals to other.
	 * 
	 * @param other the other number
	 * 
	 * @return true if @code{this} is less or equals than @code{other}.
	 */
	public boolean leq(MathNumber other) {
		return this.max(other).equals(other);
	}

	/**
	 * Yields {@code true} if this number is greater than other.
	 * 
	 * @param other the other number
	 * 
	 * @return true if
	 */
	public boolean gt(MathNumber other) {
		return geq(other) && !equals(other);
	}

	/**
	 * Yields {@code true} if this number is less than other.
	 * 
	 * @param other the other number
	 * 
	 * @return true if
	 */
	public boolean lt(MathNumber other) {
		return leq(other) && !equals(other);
	}

	/**
	 * Yields {@code true} if this number is greater than or equal to other.
	 * 
	 * @param other the other number
	 * 
	 * @return true if
	 */
	public boolean geq(MathNumber other) {
		return this.max(other).equals(this);
	}

	/**
	 * Yields the absolute value of this number. If it is not a number
	 * (according to {@link #isNaN()}), then {@link #NaN} is returned.
	 * 
	 * @return the absolute value of {@code this}
	 */
	public MathNumber abs() {
		if (isNaN())
			return NaN;

		if (isPlusInfinity())
			return this;

		if (isMinusInfinity())
			return PLUS_INFINITY;

		return cached(new MathNumber(number.abs()));
	}

	/**
	 * Rounds down this number to the next integer value towards plus infinity
	 * (see {@link RoundingMode#CEILING}). If this number is infinite or is not
	 * a number (according to {@link #isInfinite()} and {@link #isNaN()},
	 * respectively), {@code this} is returned without rounding.
	 * 
	 * @return this number rounded up towards plus infinity
	 */
	public MathNumber roundUp() {
		if (isInfinite() || isNaN())
			return this;
		return cached(new MathNumber(number.setScale(0, RoundingMode.CEILING)));
	}

	/**
	 * Rounds down this number to the next integer value towards minus infinity
	 * (see {@link RoundingMode#FLOOR}). If this number is infinite or is not a
	 * number (according to {@link #isInfinite()} and {@link #isNaN()},
	 * respectively), {@code this} is returned without rounding.
	 * 
	 * @return this number rounded down towards minus infinity
	 */
	public MathNumber roundDown() {
		if (isInfinite() || isNaN())
			return this;
		return cached(new MathNumber(number.setScale(0, RoundingMode.FLOOR)));
	}

	/**
	 * Yields the integer value of this math number.
	 * 
	 * @return the integer value of this math number
	 * 
	 * @throws MathNumberConversionException when this math number is NaN or
	 *                                           infinite
	 */
	public int toInt() throws MathNumberConversionException {
		if (isNaN() || isInfinite())
			throw new MathNumberConversionException(this);
		return number.intValue();
	}

	/**
	 * Yields the double value of this math number.
	 * 
	 * @return the double value of this math number
	 * 
	 * @throws MathNumberConversionException when this math number is NaN or
	 *                                           infinite
	 */
	public double toDouble() throws MathNumberConversionException {
		if (isNaN() || isInfinite())
			throw new MathNumberConversionException(this);
		return number.doubleValue();
	}

	/**
	 * Yields the byte value of this math number.
	 * 
	 * @return the byte value of this math number
	 * 
	 * @throws MathNumberConversionException when this math number is NaN or
	 *                                           infinite
	 */
	public byte toByte() throws MathNumberConversionException {
		if (isNaN() || isInfinite())
			throw new MathNumberConversionException(this);
		return number.byteValue();
	}

	/**
	 * Yields the short value of this math number.
	 * 
	 * @return the short value of this math number
	 * 
	 * @throws MathNumberConversionException when this math number is NaN or
	 *                                           infinite
	 */
	public short toShort() throws MathNumberConversionException {
		if (isNaN() || isInfinite())
			throw new MathNumberConversionException(this);
		return number.shortValue();
	}

	/**
	 * Yields the float value of this math number.
	 * 
	 * @return the float value of this math number
	 * 
	 * @throws MathNumberConversionException when this math number is NaN or
	 *                                           infinite
	 */
	public float toFloat() throws MathNumberConversionException {
		if (isNaN() || isInfinite())
			throw new MathNumberConversionException(this);
		return number.floatValue();
	}

	/**
	 * Yields the long value of this math number.
	 * 
	 * @return the long value of this math number
	 * 
	 * @throws MathNumberConversionException when this math number is NaN or
	 *                                           infinite
	 */
	public long toLong() throws MathNumberConversionException {
		if (isNaN() || isInfinite())
			throw new MathNumberConversionException(this);
		return number.longValue();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((number == null) ? 0 : number.hashCode());
		result = prime * result + sign;
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
		return isNaN() ? "NaN" : isMinusInfinity() ? "-Inf" : isPlusInfinity() ? "+Inf" : number.toString();
	}

	/**
	 * Yields the BigDecimal of this abstract value.
	 * 
	 * @return the BigDecimal of this abstract value
	 * 
	 * @throws IllegalStateException If this number is not a number or is plus
	 *                                   infinite or is minus infinite
	 *                                   (according to {@link #isNaN()},
	 *                                   {@link #isPlusInfinity()} and
	 *                                   {@link #isMinusInfinity()}
	 *                                   respectively).
	 */
	public BigDecimal getNumber() {
		if (isNaN())
			throw new IllegalStateException();

		if (isPlusInfinity())
			throw new IllegalStateException();

		if (isMinusInfinity())
			throw new IllegalStateException();

		return number;
	}
}
