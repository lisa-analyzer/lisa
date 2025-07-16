package it.unive.lisa.util.datastructures.regex.symbolic;

import it.unive.lisa.util.collections.IterableArray;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

/**
 * An extended string, that is, a string composed of an array of
 * {@link SymbolicChar}.
 * 
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public final class SymbolicString
		implements
		Comparable<SymbolicString>,
		Iterable<SymbolicChar> {

	/**
	 * The underlying {@link SymbolicChar} array
	 */
	private final SymbolicChar[] value;

	/**
	 * Builds a new empty extend string.
	 * 
	 * @return the extended string
	 */
	public static SymbolicString mkEmptyString() {
		return new SymbolicString(new SymbolicChar[0]);
	}

	/**
	 * Builds a new extend string composed of {@code length} unknown characters.
	 * 
	 * @param length the desired length of the string
	 * 
	 * @return the extended string
	 */
	public static SymbolicString mkTopString(
			int length) {
		SymbolicChar[] value = new SymbolicChar[length];
		for (int i = 0; i < value.length; i++)
			value[i] = UnknownSymbolicChar.INSTANCE;

		return new SymbolicString(value);
	}

	/**
	 * Builds a set of plain strings from a given set of extended strings.
	 * 
	 * @param extStrings the extended strings
	 * 
	 * @return the strings set
	 */
	public static Set<String> toStrings(
			Iterable<SymbolicString> extStrings) {
		Set<String> result = new HashSet<String>();

		for (SymbolicString e : extStrings)
			result.add(e.toString());

		return result;
	}

	/**
	 * Builds a new extend string corresponding to the given string.
	 * 
	 * @param str the string
	 * 
	 * @return the extended string
	 */
	public static SymbolicString mkString(
			String str) {
		SymbolicChar[] value = new SymbolicChar[str.length()];
		for (int i = 0; i < value.length; i++)
			value[i] = new SymbolicChar(str.charAt(i));

		return new SymbolicString(value);
	}

	/**
	 * Builds an array of extend strings corresponding to the given ones.
	 * 
	 * @param strings the strings
	 * 
	 * @return the extended strings
	 */
	public static SymbolicString[] mkStrings(
			String... strings) {
		SymbolicString[] result = new SymbolicString[strings.length];
		for (int i = 0; i < result.length; i++)
			result[i] = mkString(strings[i]);

		return result;
	}

	/**
	 * Builds a new extend string corresponding to the given character.
	 * 
	 * @param ch the character
	 * 
	 * @return the extended string
	 */
	public static SymbolicString mkStringFromChar(
			char ch) {
		return new SymbolicString(
				new SymbolicChar[] {
						new SymbolicChar(ch)
				});
	}

	private SymbolicString(
			SymbolicChar[] value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return StringUtils.join(value, "");
	}

	@Override
	public int compareTo(
			SymbolicString other) {
		int lim = Math.min(length(), other.length());

		for (int k = 0; k < lim; k++) {
			char c1 = value[k].asChar();
			char c2 = other.value[k].asChar();
			if (c1 != c2)
				return c1 - c2;
		}

		return length() - other.length();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(value);
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
		SymbolicString other = (SymbolicString) obj;
		if (!Arrays.equals(value, other.value))
			return false;
		return true;
	}

	/**
	 * Joins together two extended strings.
	 * 
	 * @param str the other extended string
	 * 
	 * @return the concatenation
	 */
	public SymbolicString concat(
			SymbolicString str) {
		int olen = str.length();
		if (olen == 0)
			return this;

		SymbolicChar[] val = this.value;
		SymbolicChar[] oval = str.value;
		int len = val.length + oval.length;
		SymbolicChar[] buf = Arrays.copyOf(val, len);
		System.arraycopy(oval, 0, buf, val.length, oval.length);
		return new SymbolicString(buf);
	}

	/**
	 * Yields a new extended string where all subsequent occurrences of the
	 * unknown character have been collapsed into a single one.
	 * 
	 * @return the extended string
	 */
	public SymbolicString collapseTopChars() {
		List<SymbolicChar> chars = new ArrayList<>();
		for (SymbolicChar ch : value)
			chars.add(ch);

		Iterator<SymbolicChar> it = chars.iterator();
		SymbolicChar last = null, tmp;
		while (it.hasNext()) {
			if ((tmp = it.next()) instanceof UnknownSymbolicChar)
				if (last != null && last instanceof UnknownSymbolicChar)
					it.remove();
				else
					last = tmp;
			else
				last = tmp;
		}

		return new SymbolicString(chars.toArray(new SymbolicChar[chars.size()]));
	}

	/**
	 * Yields the length of this extended string.
	 * 
	 * @return the length
	 */
	public int length() {
		return value.length;
	}

	private boolean startsWith(
			String prefix,
			int toffset) {
		if (toffset < 0 || toffset > length() - prefix.length())
			return false;

		for (int i = 0; i < prefix.length(); i++)
			if (!value[toffset + i].is(prefix.charAt(i)))
				return false;

		return true;
	}

	/**
	 * Yields {@code true} if and only if this extended string starts with the
	 * given prefix.
	 * 
	 * @param prefix the prefix
	 * 
	 * @return {@code true} if that condition holds
	 */
	public boolean startsWith(
			String prefix) {
		return startsWith(prefix, 0);
	}

	/**
	 * Yields {@code true} if and only if this extended string ends with the
	 * given suffix.
	 * 
	 * @param suffix the suffix
	 * 
	 * @return {@code true} if that condition holds
	 */
	public boolean endsWith(
			String suffix) {
		return startsWith(suffix, length() - suffix.length());
	}

	private int indexOf(
			String str) {
		char first = str.charAt(0);
		int max = (length() - str.length());
		for (int i = 0; i <= max; i++) {
			// Look for first character.
			if (!value[i].is(first))
				while (++i <= max && !value[i].is(first))
					;

			// Found first character, now look at the rest of v2
			if (i <= max) {
				int j = i + 1;
				int end = j + str.length() - 1;

				for (int k = 1; j < end && value[j].is(str.charAt(k)); j++, k++)
					;

				if (j == end)
					// Found whole string.
					return i;
			}
		}

		return -1;
	}

	/**
	 * Yields {@code true} if and only if this extended string contains the
	 * given sequence.
	 * 
	 * @param s the sequence
	 * 
	 * @return {@code true} if that condition holds
	 */
	public boolean contains(
			CharSequence s) {
		return indexOf(s.toString()) >= 0;
	}

	@Override
	public Iterator<SymbolicChar> iterator() {
		return new IterableArray<>(value).iterator();
	}

}
