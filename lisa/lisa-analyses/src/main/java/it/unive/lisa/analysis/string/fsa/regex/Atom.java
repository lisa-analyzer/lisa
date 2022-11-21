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

	private String string;

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
		return string.hashCode();
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof Atom && string.equals(((Atom) other).string);
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
