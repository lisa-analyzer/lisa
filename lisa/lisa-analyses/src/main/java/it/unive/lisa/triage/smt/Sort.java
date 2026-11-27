package it.unive.lisa.triage.smt;

import java.util.Objects;

/**
 * A sort of the SMT-LIB language, that is, the type of a {@link Term}.
 * Instances are identified by their SMT-LIB representation.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public final class Sort {

	/**
	 * The sort of booleans.
	 */
	public static final Sort BOOL = new Sort("Bool");

	/**
	 * The sort of mathematical integers.
	 */
	public static final Sort INT = new Sort("Int");

	/**
	 * The sort of mathematical reals.
	 */
	public static final Sort REAL = new Sort("Real");

	/**
	 * The sort of strings.
	 */
	public static final Sort STRING = new Sort("String");

	/**
	 * The sort of IEEE 754 single precision floating point numbers (8 bits of
	 * exponent, 24 of significand).
	 */
	public static final Sort FLOAT32 = new Sort("Float32");

	/**
	 * The sort of IEEE 754 double precision floating point numbers (11 bits of
	 * exponent, 53 of significand).
	 */
	public static final Sort FLOAT64 = new Sort("Float64");

	/**
	 * The sort of the rounding modes of floating point operations.
	 */
	public static final Sort ROUNDING_MODE = new Sort("RoundingMode");

	private final String smtlib;

	private final Sort index;

	private final Sort element;

	private Sort(
			String smtlib) {
		this(smtlib, null, null);
	}

	private Sort(
			String smtlib,
			Sort index,
			Sort element) {
		this.smtlib = smtlib;
		this.index = index;
		this.element = element;
	}

	/**
	 * Yields the sort of bit-vectors of the given width.
	 *
	 * @param width the number of bits
	 *
	 * @return the sort
	 * 
	 * @throws IllegalArgumentException if {@code width} is not positive
	 */
	public static Sort bitVector(
			int width) {
		if (width <= 0)
			throw new IllegalArgumentException("Bit-vectors must have a positive width");
		return new Sort("(_ BitVec " + width + ")");
	}

	/**
	 * Yields the sort of arrays from {@code index} to {@code element}.
	 *
	 * @param index   the sort of indexes
	 * @param element the sort of elements
	 *
	 * @return the sort
	 */
	public static Sort array(
			Sort index,
			Sort element) {
		return new Sort("(Array " + index.smtlib + " " + element.smtlib + ")", index, element);
	}

	/**
	 * Yields an uninterpreted sort with the given name. Uninterpreted sorts
	 * must be declared through {@link SmtSolver#declareSort(String)} before
	 * being used.
	 *
	 * @param name the name of the sort
	 *
	 * @return the sort
	 */
	static Sort uninterpreted(
			String name) {
		return new Sort(Term.symbol(name));
	}

	/**
	 * Yields {@code true} if this is the sort of floating point numbers
	 * ({@link #FLOAT32} or {@link #FLOAT64}).
	 *
	 * @return whether or not this is a floating point sort
	 */
	public boolean isFloatingPoint() {
		return equals(FLOAT32) || equals(FLOAT64);
	}

	/**
	 * Yields the number of bits of the exponent of the numbers of this floating
	 * point sort.
	 *
	 * @return the number of bits, or {@code 0} if this is not a floating point
	 *             sort
	 */
	public int getExponentBits() {
		return equals(FLOAT32) ? 8 : equals(FLOAT64) ? 11 : 0;
	}

	/**
	 * Yields the number of bits of the significand (including the hidden bit)
	 * of the numbers of this floating point sort.
	 *
	 * @return the number of bits, or {@code 0} if this is not a floating point
	 *             sort
	 */
	public int getSignificandBits() {
		return equals(FLOAT32) ? 24 : equals(FLOAT64) ? 53 : 0;
	}

	/**
	 * Yields whether this is an array sort (see {@link #array(Sort, Sort)}).
	 *
	 * @return whether this is an array sort
	 */
	public boolean isArray() {
		return element != null;
	}

	/**
	 * Yields the sort of the indexes of this array sort.
	 *
	 * @return the sort of the indexes, or {@code null} if this is not an array
	 *             sort
	 */
	public Sort getIndex() {
		return index;
	}

	/**
	 * Yields the sort of the elements of this array sort.
	 *
	 * @return the sort of the elements, or {@code null} if this is not an array
	 *             sort
	 */
	public Sort getElement() {
		return element;
	}

	/**
	 * Yields the SMT-LIB representation of this sort.
	 *
	 * @return the representation
	 */
	public String toSmtLib() {
		return smtlib;
	}

	@Override
	public int hashCode() {
		return Objects.hash(smtlib, index, element);
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
		Sort other = (Sort) obj;
		return Objects.equals(smtlib, other.smtlib)
				&& Objects.equals(index, other.index)
				&& Objects.equals(element, other.element);
	}

	@Override
	public String toString() {
		return smtlib;
	}
}
