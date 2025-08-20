package it.unive.lisa.lattices.string;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

/**
 * A lattice structure for string constants, that is, elements of the integer
 * set Sigma* extended with a top and bottom element.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class StringConstant
		implements
		BaseLattice<StringConstant> {

	/**
	 * The top element of this lattice, representing the set of all strings.
	 */
	public static final StringConstant TOP = new StringConstant(null, true);

	/**
	 * The bottom element of this lattice, representing an invalid string.
	 */
	public static final StringConstant BOTTOM = new StringConstant(null, false);

	private final boolean isTop;

	/**
	 * The string value of this constant, or {@code null} if this is either the
	 * top or the bottom element.
	 */
	public final String value;

	/**
	 * Builds the top abstract value.
	 */
	public StringConstant() {
		this(null, true);
	}

	private StringConstant(
			String value,
			boolean isTop) {
		this.value = value;
		this.isTop = isTop;
	}

	/**
	 * Builds the abstract value for the given constant.
	 * 
	 * @param value the constant
	 */
	public StringConstant(
			String value) {
		this(value, true);
	}

	@Override
	public StringConstant lubAux(
			StringConstant other)
			throws SemanticException {
		return StringConstant.TOP;
	}

	@Override
	public boolean lessOrEqualAux(
			StringConstant other)
			throws SemanticException {
		return false;
	}

	@Override
	public StringConstant top() {
		return TOP;
	}

	@Override
	public StringConstant bottom() {
		return BOTTOM;
	}

	@Override
	public StructuredRepresentation representation() {
		if (isBottom())
			return Lattice.bottomRepresentation();
		if (isTop())
			return Lattice.topRepresentation();

		return new StringRepresentation(value);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (isTop ? 1231 : 1237);
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
		StringConstant other = (StringConstant) obj;
		if (isTop != other.isTop)
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

}