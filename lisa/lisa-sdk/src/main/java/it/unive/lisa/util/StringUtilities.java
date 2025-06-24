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

	/**
	 * Flattens the given string, removing all newlines and tabulation
	 * characters.
	 * 
	 * @param s the string to flatten
	 * 
	 * @return the flattened string
	 */
	public static String flatten(
			String s) {
		return s.replaceAll("\n|\t", "");
	}

	/**
	 * Yields the greatest common prefix of the two given strings, that is, the
	 * longest prefix that is common to both strings.
	 * 
	 * @param s1 the first string
	 * @param s2 the second string
	 * 
	 * @return the greatest common prefix of the two strings
	 */
	public static String gcp(
			String s1,
			String s2) {
		String gcp = "";
		int minlen = Math.min(s1.length(), s2.length());
		for (int i = 0; i < minlen; i++)
			if (s1.charAt(i) == s2.charAt(i))
				gcp += s1.charAt(i);
			else
				break;
		return gcp;
	}
}
