package it.unive.lisa.util.datastructures.regex;

import it.unive.lisa.util.datastructures.automaton.AutomataFactory;
import it.unive.lisa.util.datastructures.automaton.Automaton;
import it.unive.lisa.util.datastructures.automaton.TransitionSymbol;
import it.unive.lisa.util.datastructures.regex.symbolic.SymbolicString;
import java.util.HashSet;
import java.util.Set;

/**
 * A {@link RegularExpression} representing a single string.
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

	/**
	 * Yields the inner string of this atom. Note that this method will return
	 * an empty string if this is {@link #EPSILON}, or {@link TopAtom#STRING} if
	 * this is an instance of {@link TopAtom}.
	 * 
	 * @return the underlying string
	 */
	public String getString() {
		return string; // this avoid propagating epsilon, but not top...
	}

	@Override
	public String toString() {
		return this == EPSILON || string.isEmpty() ? TransitionSymbol.EPSILON : string;
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

	@Override
	public <A extends Automaton<A, T>,
			T extends TransitionSymbol<T>> A toAutomaton(AutomataFactory<A, T> factory) {
		return isEmpty() ? factory.emptyString() : factory.singleString(string);
	}

	@Override
	protected Set<PartialSubstring> substringAux(int charsToSkip, int missingChars) {
		Set<PartialSubstring> result = new HashSet<>();
		
		int len = string.length();
		if (charsToSkip > len)
			// outside of the string
			result.add(new PartialSubstring(SymbolicString.mkEmptyString(), charsToSkip - len, missingChars));
		else if (charsToSkip + missingChars > len)
			// partially inside the string
			result.add(new PartialSubstring(SymbolicString.mkString(string.substring(charsToSkip)), 0,
					missingChars - len + charsToSkip));
		else
			result.add(new PartialSubstring(
					SymbolicString.mkString(string.substring(charsToSkip, charsToSkip + missingChars)), 0, 0));

		return result;
	}

	@Override
	public boolean isEmpty() {
		return string.isEmpty();
	}

	@Override
	public boolean is(String str) {
		return string.equals(str);
	}

	@Override
	public int maxLength() {
		return string.length();
	}

	@Override
	public int minLength() {
		return maxLength();
	}

	@Override
	public boolean mayContain(String s) {
		return contains(s);
	}

	@Override
	public boolean contains(String s) {
		return string.contains(s);
	}

	@Override
	public boolean mayStartWith(String s) {
		return startsWith(s);
	}

	@Override
	public boolean startsWith(String s) {
		return string.startsWith(s);
	}

	@Override
	public boolean mayEndWith(String s) {
		return endsWith(s);
	}

	@Override
	public boolean endsWith(String s) {
		return string.endsWith(s);
	}

	@Override
	protected RegularExpression unrollStarToFixedLength(int length) {
		return this;
	}

	@Override
	public RegularExpression reverse() {
		return new Atom(new StringBuilder(string).reverse().toString());
	}

	@Override
	protected RegularExpression topAsEmptyString() {
		return this;
	}

	@Override
	protected RegularExpression topAsSingleChar() {
		return this;
	}

	@Override
	public RegularExpression[] explode() {
		return string.chars().mapToObj(ch -> String.valueOf((char) ch)).map(Atom::new)
				.toArray(RegularExpression[]::new);
	}

	@Override
	protected int compareToAux(RegularExpression other) {
		return string.compareTo(other.asAtom().string);
	}
}
