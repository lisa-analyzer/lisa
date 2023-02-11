package it.unive.lisa.util.datastructures.regex;

import it.unive.lisa.util.datastructures.automaton.AutomataFactory;
import it.unive.lisa.util.datastructures.automaton.Automaton;
import it.unive.lisa.util.datastructures.automaton.TransitionSymbol;
import it.unive.lisa.util.datastructures.regex.symbolic.SymbolicString;
import java.util.HashSet;
import java.util.Set;

/**
 * A {@link RegularExpression} representing a loop, repeated an arbitrary number
 * of times, over an inner regular expression.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public final class Star extends RegularExpression {

	private final RegularExpression op;

	/**
	 * Builds the star.
	 * 
	 * @param op the inner regular expression
	 */
	Star(RegularExpression op) {
		this.op = op;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((op == null) ? 0 : op.hashCode());
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
		Star other = (Star) obj;
		if (op == null) {
			if (other.op != null)
				return false;
		} else if (!op.equals(other.op))
			return false;
		return true;
	}

	@Override
	public String toString() {
		// an or will already be surrounded by parenthesis
		// and we use parenthesis only on multi-char atoms or non-atoms
		if (op.isOr() || (op.isAtom() && op.asAtom().getString().length() == 1))
			return op.toString() + "*";
		return "(" + op.toString() + ")*";
	}

	/**
	 * Yields the inner regular expression.
	 * 
	 * @return the inner regular expression
	 */
	public RegularExpression getOperand() {
		return op;
	}

	@Override
	public RegularExpression simplify() {
		RegularExpression op = this.op.simplify();
		RegularExpression result = op.star();

		// epsilon* = epsilon
		if (op.isAtom() && op.asAtom().isEmpty())
			result = Atom.EPSILON;

		// emptyset* = epsilon
		else if (op.isEmptySet())
			result = Atom.EPSILON;

		// a** = a*
		else if (op.isStar())
			result = op;

		return result;
	}

	@Override
	public <A extends Automaton<A, T>,
			T extends TransitionSymbol<T>> A toAutomaton(AutomataFactory<A, T> factory) {
		return op.toAutomaton(factory).star();
	}

	@Override
	public Set<PartialSubstring> substringAux(int charsToSkip, int missingChars) {
		Set<PartialSubstring> result = new HashSet<>(), partial = new HashSet<>();

		result.add(new PartialSubstring(SymbolicString.mkEmptyString(), charsToSkip, missingChars));

		do {
			if (!partial.isEmpty()) {
				result.addAll(partial);
				partial.clear();
			}

			PartialSubstring tmp;
			for (PartialSubstring base : result)
				for (PartialSubstring suffix : op.substringAux(base.getCharsToStart(), base.getMissingChars())) 
					if (!result.contains(tmp = base.concat(suffix)))
						partial.add(tmp);
		} while (!partial.isEmpty());

		return result;
	}

	@Override
	public boolean isEmpty() {
		return op.isEmpty();
	}

	@Override
	public boolean is(String str) {
		throw new UnsupportedOperationException();
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
		if (s.isEmpty() || op.mayContain(s))
			return true;

		int repetitions = (s.length() / op.maxLength()) + 2;
		Set<SymbolicString> substrings = substring(0, repetitions * op.maxLength() + 1);
		for (SymbolicString str : substrings)
			if (str.contains(s))
				return true;

		return false;
	}

	@Override
	public boolean contains(String s) {
		return s.isEmpty(); // epsilon is contained everywhere
	}

	@Override
	public boolean mayStartWith(String s) {
		if (s.isEmpty() || op.mayStartWith(s))
			return true;

		int repetitions = (s.length() / op.maxLength()) + 2;
		Set<SymbolicString> substrings = substring(0, repetitions * op.maxLength() + 1);
		for (SymbolicString str : substrings)
			if (str.startsWith(s))
				return true;

		return false;
	}

	@Override
	public boolean startsWith(String s) {
		return s.isEmpty(); // epsilon is contained everywhere
	}

	@Override
	public boolean mayEndWith(String s) {
		if (s.isEmpty() || op.mayEndWith(s))
			return true;

		int repetitions = (s.length() / op.maxLength()) + 2;
		Set<SymbolicString> substrings = substring(0, repetitions * op.maxLength() + 1);
		for (SymbolicString str : substrings)
			if (str.endsWith(s))
				return true;

		return false;
	}

	@Override
	public boolean endsWith(String s) {
		return s.isEmpty(); // epsilon is contained everywhere
	}

	@Override
	protected RegularExpression unrollStarToFixedLength(int length) {
		if (length == 0)
			return Atom.EPSILON;

		int repetitions = (length / op.maxLength()) + 2;
		RegularExpression result = null;

		while (repetitions > 0) {
			result = result == null ? op : result.comp(op);
			repetitions--;
		}

		return result;
	}

	@Override
	public RegularExpression reverse() {
		return op.reverse().star();
	}

	@Override
	protected RegularExpression topAsEmptyString() {
		return op.topAsEmptyString().star();
	}

	@Override
	protected RegularExpression topAsSingleChar() {
		return op.topAsSingleChar().star();
	}

	@Override
	public RegularExpression[] explode() {
		throw new UnsupportedOperationException();
	}

	@Override
	protected int compareToAux(RegularExpression other) {
		return op.compareTo(other.asStar().op);
	}
}
