package it.unive.lisa.lattices.string;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Objects;

/**
 * A lattice structure tracking suffixes of strings.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class StrSuffix
		implements
		BaseLattice<StrSuffix> {

	/**
	 * The top element of the lattice, representing an empty suffix.
	 */
	public final static StrSuffix TOP = new StrSuffix();

	/**
	 * The bottom element of the lattice, representing an invalid suffix.
	 */
	public final static StrSuffix BOTTOM = new StrSuffix(null);

	/**
	 * The suffix string represented by this element. If this is {@code null},
	 * then this element represents the bottom element of the lattice.
	 */
	public final String suffix;

	/**
	 * Builds the top suffix abstract element.
	 */
	public StrSuffix() {
		this("");
	}

	/**
	 * Builds a suffix abstract element.
	 *
	 * @param suffix the suffix
	 */
	public StrSuffix(
			String suffix) {
		this.suffix = suffix;
	}

	@Override
	public StrSuffix lubAux(
			StrSuffix other)
			throws SemanticException {
		String otherSuffix = other.suffix;
		StringBuilder result = new StringBuilder();

		int i = suffix.length() - 1;
		int j = otherSuffix.length() - 1;

		while (i >= 0 && j >= 0 && suffix.charAt(i) == otherSuffix.charAt(j)) {
			result.append(suffix.charAt(i--));
			j--;
		}

		if (result.length() != 0)
			return new StrSuffix(result.reverse().toString());

		else
			return TOP;
	}

	@Override
	public boolean lessOrEqualAux(
			StrSuffix other)
			throws SemanticException {
		if (other.suffix.length() <= this.suffix.length()) {
			StrSuffix lub = this.lubAux(other);
			return lub.suffix.length() == other.suffix.length();
		}

		return false;
	}

	@Override
	public boolean equals(
			Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		StrSuffix suffix1 = (StrSuffix) o;
		return Objects.equals(suffix, suffix1.suffix);
	}

	@Override
	public int hashCode() {
		return Objects.hash(suffix);
	}

	@Override
	public StrSuffix top() {
		return TOP;
	}

	@Override
	public boolean isTop() {
		return this == TOP || (suffix != null && suffix.length() == 0);
	}

	@Override
	public StrSuffix bottom() {
		return BOTTOM;
	}

	@Override
	public boolean isBottom() {
		return this == BOTTOM || suffix == null;
	}

	@Override
	public StructuredRepresentation representation() {
		if (isBottom())
			return Lattice.bottomRepresentation();

		return new StringRepresentation('*' + suffix);
	}

	@Override
	public String toString() {
		return representation().toString();
	}
}
