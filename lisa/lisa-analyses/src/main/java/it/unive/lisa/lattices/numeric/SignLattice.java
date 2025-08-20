package it.unive.lisa.lattices.numeric;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

/**
 * The lattice structure the values, which can be positive, negative, zero, top
 * or bottom.
 * 
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
public class SignLattice
		implements
		BaseLattice<SignLattice> {

	/**
	 * The abstract positive element.
	 */
	public static final SignLattice POS = new SignLattice((byte) 4);

	/**
	 * The abstract negative element.
	 */
	public static final SignLattice NEG = new SignLattice((byte) 3);

	/**
	 * The abstract zero element.
	 */
	public static final SignLattice ZERO = new SignLattice((byte) 2);

	/**
	 * The abstract top element.
	 */
	public static final SignLattice TOP = new SignLattice((byte) 0);

	/**
	 * The abstract bottom element.
	 */
	public static final SignLattice BOTTOM = new SignLattice((byte) 1);

	private final byte sign;

	/**
	 * Builds the sign abstract domain, representing the top of the sign
	 * abstract domain.
	 */
	public SignLattice() {
		this((byte) 0);
	}

	/**
	 * Builds the sign instance for the given sign value.
	 * 
	 * @param sign the sign (0 = top, 1 = bottom, 2 = zero, 3 = negative, 4 =
	 *                 positive)
	 */
	private SignLattice(
			byte sign) {
		this.sign = sign;
	}

	@Override
	public SignLattice top() {
		return TOP;
	}

	@Override
	public SignLattice bottom() {
		return BOTTOM;
	}

	@Override
	public String toString() {
		return representation().toString();
	}

	@Override
	public StructuredRepresentation representation() {
		if (isBottom())
			return Lattice.bottomRepresentation();
		if (isTop())
			return Lattice.topRepresentation();

		String repr;
		if (this == ZERO)
			repr = "0";
		else if (this == POS)
			repr = "+";
		else
			repr = "-";

		return new StringRepresentation(repr);
	}

	/**
	 * Yields whether or not this is the positive sign.
	 * 
	 * @return {@code true} if that condition holds
	 */
	public boolean isPositive() {
		return this == POS;
	}

	/**
	 * Yields whether or not this is the zero sign.
	 * 
	 * @return {@code true} if that condition holds
	 */
	public boolean isZero() {
		return this == ZERO;
	}

	/**
	 * Yields whether or not this is the negative sign.
	 * 
	 * @return {@code true} if that condition holds
	 */
	public boolean isNegative() {
		return this == NEG;
	}

	/**
	 * Yields the sign opposite to this one. Top and bottom elements do not
	 * change.
	 * 
	 * @return the opposite sign
	 */
	public SignLattice opposite() {
		if (isTop() || isBottom())
			return this;
		return isPositive() ? NEG : isNegative() ? POS : ZERO;
	}

	@Override
	public SignLattice lubAux(
			SignLattice other)
			throws SemanticException {
		return SignLattice.TOP;
	}

	@Override
	public boolean lessOrEqualAux(
			SignLattice other)
			throws SemanticException {
		return false;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + sign;
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
		SignLattice other = (SignLattice) obj;
		if (sign != other.sign)
			return false;
		return true;
	}

	/**
	 * Tests if this instance is equal to the given one, returning a
	 * {@link Satisfiability} element.
	 * 
	 * @param other the instance
	 * 
	 * @return the satisfiability of {@code this = other}
	 */
	public Satisfiability eq(
			SignLattice other) {
		if (this.isBottom() || other.isBottom())
			return Satisfiability.BOTTOM;
		else if (this.isTop() || other.isTop())
			return Satisfiability.UNKNOWN;
		else if (!this.equals(other))
			return Satisfiability.NOT_SATISFIED;
		else if (isZero())
			return Satisfiability.SATISFIED;
		else
			return Satisfiability.UNKNOWN;
	}

	/**
	 * Tests if this instance is greater than the given one, returning a
	 * {@link Satisfiability} element.
	 * 
	 * @param other the instance
	 * 
	 * @return the satisfiability of {@code this > other}
	 */
	public Satisfiability gt(
			SignLattice other) {
		if (this.isBottom() || other.isBottom())
			return Satisfiability.BOTTOM;
		else if (this.isTop() || other.isTop())
			return Satisfiability.UNKNOWN;
		else if (this.isNegative())
			return other.isNegative() ? Satisfiability.UNKNOWN : Satisfiability.NOT_SATISFIED;
		else if (this.isZero())
			return other.isNegative() ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
		else
			return other.isPositive() ? Satisfiability.UNKNOWN : Satisfiability.SATISFIED;
	}

}