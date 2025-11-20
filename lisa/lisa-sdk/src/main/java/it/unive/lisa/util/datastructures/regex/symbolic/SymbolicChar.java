package it.unive.lisa.util.datastructures.regex.symbolic;

/**
 * An symbolic character, that is, an object representing a single and possibly
 * unknown character.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class SymbolicChar {

	private final char ch;

	/**
	 * Builds the symbolic character.
	 * 
	 * @param ch the underlying character
	 */
	public SymbolicChar(
			char ch) {
		this.ch = ch;
	}

	/**
	 * Converts this symbolic character to a normal character.
	 * 
	 * @return the character
	 */
	public char asChar() {
		return ch;
	}

	/**
	 * Yields {@code true} if and only if this symbolic character represent the
	 * given concrete one.
	 * 
	 * @param ch the character
	 * 
	 * @return {@code true} if that condition holds
	 */
	public boolean is(
			char ch) {
		return this.ch == ch;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ch;
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
		SymbolicChar other = (SymbolicChar) obj;
		if (ch != other.ch)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return String.valueOf(ch);
	}

}
