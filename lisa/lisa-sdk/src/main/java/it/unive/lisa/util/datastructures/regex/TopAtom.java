package it.unive.lisa.util.datastructures.regex;

import it.unive.lisa.util.datastructures.automaton.AutomataFactory;
import it.unive.lisa.util.datastructures.automaton.Automaton;
import it.unive.lisa.util.datastructures.automaton.TransitionSymbol;
import it.unive.lisa.util.datastructures.regex.symbolic.SymbolicString;
import java.util.HashSet;
import java.util.Set;

/**
 * A {@link RegularExpression} representing a sequence of unknown characters of
 * arbitrary length.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public final class TopAtom extends Atom {

	/**
	 * The string used to represent this regular expression.
	 */
	public static final String STRING = TransitionSymbol.UNKNOWN_SYMBOL;

	/**
	 * The singleton instance.
	 */
	public static final TopAtom INSTANCE = new TopAtom();

	private TopAtom() {
		super(STRING);
	}

	@Override
	public <A extends Automaton<A, T>,
			T extends TransitionSymbol<T>> A toAutomaton(AutomataFactory<A, T> factory) {
		return factory.unknownString();
	}

	@Override
	protected Set<PartialSubstring> substringAux(int charsToSkip, int missingChars) {
		Set<PartialSubstring> result = new HashSet<>();

		for (int i = 0; i <= charsToSkip; i++)
			result.add(new PartialSubstring(SymbolicString.mkEmptyString(), charsToSkip - i, missingChars));
		for (int i = 1; i <= missingChars; i++)
			result.add(new PartialSubstring(SymbolicString.mkTopString(i), 0, missingChars - i));

		return result;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public boolean is(String str) {
		return false;
	}

	@Override
	public int maxLength() {
		return Integer.MAX_VALUE;
	}

	@Override
	public int minLength() {
		return 0;
	}

	@Override
	public boolean mayContain(String s) {
		return true;
	}

	@Override
	public boolean contains(String s) {
		return s.isEmpty(); // epsilon is contained everywhere
	}

	@Override
	public boolean mayStartWith(String s) {
		return true;
	}

	@Override
	public boolean startsWith(String s) {
		return s.isEmpty(); // epsilon is contained everywhere
	}

	@Override
	public boolean mayEndWith(String s) {
		return true;
	}

	@Override
	public boolean endsWith(String s) {
		return s.isEmpty(); // epsilon is contained everywhere
	}

	@Override
	protected RegularExpression topAsSingleChar() {
		return new Atom(STRING) {

			@Override
			public boolean isEmpty() {
				return false;
			}

			@Override
			public boolean is(String str) {
				return false;
			}

			@Override
			public int maxLength() {
				return 1;
			}

			@Override
			public int minLength() {
				return 1;
			}

			@Override
			public boolean mayContain(String s) {
				return s.length() >= 1;
			}

			@Override
			public boolean contains(String s) {
				return s.isEmpty(); // epsilon is contained everywhere
			}

			@Override
			public boolean mayStartWith(String s) {
				return s.length() >= 1;
			}

			@Override
			public boolean startsWith(String s) {
				return s.isEmpty(); // epsilon is contained everywhere
			}

			@Override
			public boolean mayEndWith(String s) {
				return s.length() >= 1;
			}

			@Override
			public boolean endsWith(String s) {
				return s.isEmpty(); // epsilon is contained everywhere
			}
		};
	}

	@Override
	protected RegularExpression topAsEmptyString() {
		return Atom.EPSILON;
	}

	@Override
	public RegularExpression reverse() {
		return this;
	}

	@Override
	public RegularExpression[] explode() {
		return new RegularExpression[] { this };
	}

	@Override
	protected int compareToAux(RegularExpression other) {
		return 0;
	}
}
