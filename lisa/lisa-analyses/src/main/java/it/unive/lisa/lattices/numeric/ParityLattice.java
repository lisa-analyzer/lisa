package it.unive.lisa.lattices.numeric;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

/**
 * A lattice structure for parity values, which can be even, odd, top, or
 * bottom.
 * 
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
public class ParityLattice
		implements
		BaseLattice<ParityLattice> {

	/**
	 * The abstract even element.
	 */
	public static final ParityLattice EVEN = new ParityLattice((byte) 3);

	/**
	 * The abstract odd element.
	 */
	public static final ParityLattice ODD = new ParityLattice((byte) 2);

	/**
	 * The abstract top element.
	 */
	public static final ParityLattice TOP = new ParityLattice((byte) 0);

	/**
	 * The abstract bottom element.
	 */
	public static final ParityLattice BOTTOM = new ParityLattice((byte) 1);

	private final byte parity;

	/**
	 * Builds the parity abstract domain, representing the top of the parity
	 * abstract domain.
	 */
	public ParityLattice() {
		this((byte) 0);
	}

	private ParityLattice(
			byte parity) {
		this.parity = parity;
	}

	@Override
	public ParityLattice top() {
		return TOP;
	}

	@Override
	public ParityLattice bottom() {
		return BOTTOM;
	}

	@Override
	public StructuredRepresentation representation() {
		if (isBottom())
			return Lattice.bottomRepresentation();
		if (isTop())
			return Lattice.topRepresentation();

		String repr;
		if (this == EVEN)
			repr = "Even";
		else
			repr = "Odd";

		return new StringRepresentation(repr);
	}

	/**
	 * Yields whether or not this is the even parity.
	 * 
	 * @return {@code true} if that condition holds
	 */
	public boolean isEven() {
		return this == EVEN;
	}

	/**
	 * Yields whether or not this is the odd parity.
	 * 
	 * @return {@code true} if that condition holds
	 */
	public boolean isOdd() {
		return this == ODD;
	}

	@Override
	public ParityLattice lubAux(
			ParityLattice other)
			throws SemanticException {
		return TOP;
	}

	@Override
	public boolean lessOrEqualAux(
			ParityLattice other)
			throws SemanticException {
		return false;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + parity;
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
		ParityLattice other = (ParityLattice) obj;
		if (parity != other.parity)
			return false;
		return true;
	}

	@Override
	public String toString() {
		if (isBottom())
			return "BOTTOM";
		if (isTop())
			return "TOP";
		return isEven() ? "EVEN" : "ODD";
	}

}
