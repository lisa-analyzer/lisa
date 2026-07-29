package it.unive.lisa.lattices.numeric;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

/**
 * A lattice structure for integer constants, that is, elements of the integer
 * set Z extended with a top and bottom element.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class IntegerConstant
		implements
		BaseLattice<IntegerConstant> {

	/**
	 * The top element of this lattice, representing the set of all integers.
	 */
	public static final IntegerConstant TOP = new IntegerConstant(false);

	/**
	 * The bottom element of this lattice, representing an erroneous value.
	 */
	public static final IntegerConstant BOTTOM = new IntegerConstant(true);

	private final boolean isBottom;

	/**
	 * The integer value of this constant, or {@code null} if this is either the
	 * top or the bottom element.
	 */
	public final Integer value;

	/**
	 * Builds the top abstract value.
	 */
	public IntegerConstant() {
		this(null, false);
	}

	private IntegerConstant(
			boolean isBottom) {
		this(null, isBottom);
	}

	private IntegerConstant(
			Integer value,
			boolean isBottom) {
		this.value = value;
		this.isBottom = isBottom;
	}

	/**
	 * Builds the abstract value for the given constant.
	 * 
	 * @param value the constant
	 */
	public IntegerConstant(
			Integer value) {
		this(value, false);
	}

	@Override
	public IntegerConstant top() {
		return TOP;
	}

	@Override
	public boolean isTop() {
		return value == null && !isBottom;
	}

	@Override
	public IntegerConstant bottom() {
		return BOTTOM;
	}

	@Override
	public boolean isBottom() {
		return value == null && isBottom;
	}

	@Override
	public IntegerConstant lubAux(
			IntegerConstant other)
			throws SemanticException {
		return TOP;
	}

	@Override
	public boolean lessOrEqualAux(
			IntegerConstant other)
			throws SemanticException {
		return false;
	}

	@Override
	public StructuredRepresentation representation() {
		if (isBottom())
			return Lattice.bottomRepresentation();
		if (isTop())
			return Lattice.topRepresentation();

		return new StringRepresentation(value.toString());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (isBottom ? 1231 : 1237);
		result = prime * result + ((value == null) ? 0 : value.hashCode());
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
		IntegerConstant other = (IntegerConstant) obj;
		if (isBottom != other.isBottom)
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return representation().toString();
	}
}
