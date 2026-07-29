package it.unive.lisa.lattices.string;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Objects;

/**
 * A lattice structure tracking prefixes of strings.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class StrPrefix
		implements
		BaseLattice<StrPrefix> {

	/**
	 * The top element of this lattice, representing the empty prefix.
	 */
	public final static StrPrefix TOP = new StrPrefix();

	/**
	 * The bottom element of this lattice, representing an invalid prefix.
	 */
	public final static StrPrefix BOTTOM = new StrPrefix(null);

	/**
	 * The prefix string of this abstract value. If this is the bottom element,
	 * this is {@code null}.
	 */
	public final String prefix;

	/**
	 * Builds the top prefix abstract element.
	 */
	public StrPrefix() {
		this("");
	}

	/**
	 * Builds a prefix abstract element.
	 * 
	 * @param prefix the prefix
	 */
	public StrPrefix(
			String prefix) {
		this.prefix = prefix;
	}

	@Override
	public StrPrefix lubAux(
			StrPrefix other)
			throws SemanticException {
		String otherPrefixString = other.prefix;
		StringBuilder result = new StringBuilder();

		int i = 0;
		while (i <= prefix.length() - 1
				&& i <= otherPrefixString.length() - 1
				&& prefix.charAt(i) == otherPrefixString.charAt(i)) {
			result.append(prefix.charAt(i++));
		}

		if (result.length() != 0)
			return new StrPrefix(result.toString());

		else
			return TOP;
	}

	@Override
	public boolean lessOrEqualAux(
			StrPrefix other)
			throws SemanticException {
		if (other.prefix.length() <= this.prefix.length()) {
			StrPrefix lub = this.lubAux(other);

			return lub.prefix.length() == other.prefix.length();
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
		StrPrefix prefix1 = (StrPrefix) o;
		return Objects.equals(prefix, prefix1.prefix);
	}

	@Override
	public int hashCode() {
		return Objects.hash(prefix);
	}

	@Override
	public StrPrefix top() {
		return TOP;
	}

	@Override
	public boolean isTop() {
		return this == TOP || (prefix != null && prefix.length() == 0);
	}

	@Override
	public StrPrefix bottom() {
		return BOTTOM;
	}

	@Override
	public boolean isBottom() {
		return this == BOTTOM || prefix == null;
	}

	@Override
	public StructuredRepresentation representation() {
		if (isBottom())
			return Lattice.bottomRepresentation();

		return new StringRepresentation(prefix + '*');
	}

	@Override
	public String toString() {
		return representation().toString();
	}
}
