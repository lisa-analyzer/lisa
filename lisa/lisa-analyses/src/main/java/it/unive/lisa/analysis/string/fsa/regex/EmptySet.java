package it.unive.lisa.analysis.string.fsa.regex;

/**
 * A regular expression representing the empty set of strings.
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class EmptySet extends RegularExpression {

	/**
	 * The singleton instance.
	 */
	public static final EmptySet INSTANCE = new EmptySet();

	private EmptySet() {
	}

	@Override
	public RegularExpression simplify() {
		return this;
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof EmptySet;
	}

	@Override
	public String toString() {
		return "∅";
	}
}
