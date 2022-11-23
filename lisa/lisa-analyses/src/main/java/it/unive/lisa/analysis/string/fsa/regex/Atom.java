package it.unive.lisa.analysis.string.fsa.regex;

/**
 * A regular expression representing a single string.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Atom extends RegularExpression {

	/**
	 * A unique constant for the epsilon (empty) string.
	 */
	public static final Atom EPSILON = new Atom("");

	private final String string;

	/**
	 * Builds the atom.
	 * 
	 * @param s the string to be represented by this atom
	 */
	public Atom(String s) {
		this.string = s;
	}

	@Override
	public String toString() {
		return string;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((string == null) ? 0 : string.hashCode());
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
		Atom other = (Atom) obj;
		if (string == null) {
			if (other.string != null)
				return false;
		} else if (!string.equals(other.string))
			return false;
		return true;
	}

	@Override
	public RegularExpression simplify() {
		return this;
	}

	/**
	 * Yields {@code true} if and only if this regular expression corresponds to
	 * the empty string.
	 * 
	 * @return {@code true} if that condition holds
	 */
	public boolean isEmpty() {
		return string.isEmpty();
	}
}
