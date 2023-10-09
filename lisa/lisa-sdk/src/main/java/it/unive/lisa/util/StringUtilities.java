package it.unive.lisa.util;

/**
 * Utility methods for building and manipulating strings.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class StringUtilities {

	private StringUtilities() {
	}

	/**
	 * Yields the ordinal for the given integer by appending either {@code st},
	 * {@code nd}, {@code rd}, or {@code th} to it.
	 * 
	 * @param i the integer
	 * 
	 * @return the ordinal string
	 */
	public static String ordinal(
			int i) {
		int n = i % 100;
		if (n == 11 || n == 12 || n == 13 || n % 10 == 0 || n % 10 > 3)
			return i + "th";

		if (n % 10 == 1)
			return i + "st";

		if (n % 10 == 2)
			return i + "nd";

		return i + "rd";
	}

	/**
	 * Indents all lines of {@code target} by using {@code indent} repeated
	 * {@code amount} times.
	 * 
	 * @param target the string to indent
	 * @param indent the string to use as indentation
	 * @param amount the number of times {@code indent} should be repeated
	 * 
	 * @return the indented string
	 */
	public static String indent(
			String target,
			String indent,
			int amount) {
		String offset = indent.repeat(amount);
		return offset + target.replace("\n", "\n" + offset);
	}
}
