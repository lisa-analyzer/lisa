package it.unive.lisa.util.datastructures.regex;

import it.unive.lisa.util.datastructures.automaton.AutomataFactory;
import it.unive.lisa.util.datastructures.automaton.Automaton;
import it.unive.lisa.util.datastructures.automaton.TransitionSymbol;
import it.unive.lisa.util.datastructures.regex.symbolic.SymbolicString;
import java.util.HashSet;
import java.util.Set;

/**
 * A {@link RegularExpression} representing the sequential composition of two
 * regular expressions.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public final class Comp extends RegularExpression {

	/**
	 * The first regular expression
	 */
	private final RegularExpression first;

	/**
	 * The second regular expression
	 */
	private final RegularExpression second;

	/**
	 * Builds the comp.
	 * 
	 * @param first  the first regular expression
	 * @param second the second regular expression
	 */
	public Comp(
			RegularExpression first,
			RegularExpression second) {
		this.first = first;
		this.second = second;
	}

	/**
	 * Yields the first regular expression.
	 * 
	 * @return the first regular expression
	 */
	public RegularExpression getFirst() {
		return first;
	}

	/**
	 * Yields the second regular expression.
	 * 
	 * @return the second regular expression
	 */
	public RegularExpression getSecond() {
		return second;
	}

	@Override
	public String toString() {
		return first.toString() + second.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((first == null) ? 0 : first.hashCode());
		result = prime * result + ((second == null) ? 0 : second.hashCode());
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
		Comp other = (Comp) obj;
		if (first == null) {
			if (other.first != null)
				return false;
		} else if (!first.equals(other.first))
			return false;
		if (second == null) {
			if (other.second != null)
				return false;
		} else if (!second.equals(other.second))
			return false;
		return true;
	}

	@Override
	public RegularExpression simplify() {

		RegularExpression first = this.first.simplify();
		RegularExpression second = this.second.simplify();

		RegularExpression result = first.comp(second);

		// emptyset.a = emptyset
		if (first.isEmptySet() || second.isEmptySet())
			result = EmptySet.INSTANCE;

		// a.(b + c) = a.b + a.c
		else if (first.isAtom() && second.isOr() && second.asOr().isAtomic())
			result = first.comp(second.asOr().getFirst()).or(first.comp(second.asOr().getSecond()));

		// a.epsilon = a
		else if (second.isEpsilon())
			result = first;
		else if (first.isEpsilon())
			result = second;

		// a*.(x.a*)* = (a + x)*
		else if (first.isStar()
				&& second.isStar()
				&& second.asStar().getOperand().isComp()
				&& second.asStar().getOperand().asComp().second.isStar()
				&& second.asStar().getOperand().asComp().second.asStar()
					.getOperand()
					.equals(first.asStar().getOperand()))
			result = first.asStar().getOperand().or(second.asStar().getOperand().asComp().first).star();

		// a.((b.a)*.b) = (a.b)*
		else if (first.isAtom()
				&& second.isComp()
				&& second.asComp().first.isStar()
				&& second.asComp().second.isAtom()
				&& second.asComp().second.asAtom()
					.comp(first.asAtom())
					.equals(second.asComp().first.asStar().getOperand()))
			result = first.asAtom().comp(second.asComp().second.asAtom()).star();

		// a*.(a*.b) = a*b
		else if (first.isStar()
				&& second.isComp()
				&& second.asComp().first.isStar()
				&& first.asStar().getOperand().equals(second.asComp().first.asStar().getOperand()))
			result = first.comp(second.asComp().second);

		// r*.r* = r*
		else if (first instanceof Star
				&& second instanceof Star
				&& first.asStar().getOperand().equals(second.asStar().getOperand()))
			result = first;

		return result;
	}

	@Override
	public <A extends Automaton<A, T>, T extends TransitionSymbol<T>> A toAutomaton(
			AutomataFactory<A, T> factory) {
		return first.toAutomaton(factory).concat(second.toAutomaton(factory));
	}

	@Override
	protected Set<PartialSubstring> substringAux(
			int charsToSkip,
			int missingChars) {
		Set<PartialSubstring> result = new HashSet<>();

		Set<PartialSubstring> firstSS = first.substringAux(charsToSkip, missingChars);
		for (PartialSubstring s : firstSS)
			if (s.getMissingChars() == 0)
				result.add(s);
			else {
				Set<PartialSubstring> secondSS = second.substringAux(s.getCharsToStart(), s.getMissingChars());
				for (PartialSubstring ss : secondSS)
					result.add(s.concat(ss));
			}

		return result;
	}

	@Override
	public boolean isEmpty() {
		return first.isEmpty() && second.isEmpty();
	}

	@Override
	public boolean is(
			String str) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int maxLength() {
		int first, second;
		if ((first = this.first.maxLength()) == Integer.MAX_VALUE
				|| (second = this.second.maxLength()) == Integer.MAX_VALUE)
			return Integer.MAX_VALUE;
		return first + second;
	}

	@Override
	public int minLength() {
		return first.minLength() + second.minLength();
	}

	@Override
	public boolean mayContain(
			String s) {
		if (first.mayContain(s) || second.mayContain(s))
			return true;

		RegularExpression tmp = topAsEmptyString().unrollStarToFixedLength(s.length());
		Set<SymbolicString> substrings = tmp.substring(0, tmp.maxLength());
		for (SymbolicString str : substrings)
			if (str.contains(s))
				return true;

		return false;
	}

	@Override
	public boolean contains(
			String s) {
		if (first.contains(s) || second.contains(s))
			return true;

		RegularExpression tmp = topAsSingleChar().unrollStarToFixedLength(0);
		Set<SymbolicString> substrings = tmp.substring(0, tmp.maxLength());
		for (SymbolicString str : substrings)
			if (!str.contains(s))
				return false;

		return true;
	}

	@Override
	public boolean mayStartWith(
			String s) {
		if (first.mayStartWith(s))
			return true;

		Set<SymbolicString> substrings = substring(0, s.length());
		for (SymbolicString str : substrings)
			if (str.startsWith(s))
				return true;

		return false;
	}

	@Override
	public boolean startsWith(
			String s) {
		if (first.startsWith(s))
			return true;

		Set<SymbolicString> substrings = substring(0, s.length());
		for (SymbolicString str : substrings)
			if (!str.startsWith(s))
				return false;

		return true;
	}

	@Override
	public boolean mayEndWith(
			String s) {
		if (second.mayEndWith(s))
			return true;

		return reverse().mayStartWith(new StringBuilder(s).reverse().toString());
	}

	@Override
	public boolean endsWith(
			String s) {
		if (second.endsWith(s))
			return true;

		return reverse().startsWith(new StringBuilder(s).reverse().toString());
	}

	@Override
	protected RegularExpression unrollStarToFixedLength(
			int length) {
		return first.unrollStarToFixedLength(length).comp(second.unrollStarToFixedLength(length));
	}

	@Override
	public RegularExpression reverse() {
		return second.reverse().comp(first.reverse());
	}

	@Override
	protected RegularExpression topAsEmptyString() {
		return first.topAsEmptyString().comp(second.topAsEmptyString());
	}

	@Override
	protected RegularExpression topAsSingleChar() {
		return first.topAsSingleChar().comp(second.topAsSingleChar());
	}

	@Override
	public RegularExpression[] explode() {
		throw new UnsupportedOperationException();
	}

	@Override
	protected int compareToAux(
			RegularExpression other) {
		int cmp;
		if ((cmp = first.compareTo(other.asComp().first)) != 0)
			return cmp;
		return second.compareTo(other.asComp().second);
	}

	@Override
	public RegularExpression repeat(
			long n) {
		if (n == 0)
			return Atom.EPSILON;

		RegularExpression r = Atom.EPSILON;
		for (long i = 0; i < n; i++)
			r = new Comp(r, this);
		return r.simplify();
	}

	@Override
	public RegularExpression trimLeft() {
		RegularExpression trimLeftFirst = first.trimLeft().simplify();
		// it means that first reads just whitespaces strings (L(first)
		// \subseteq { }*)
		if (trimLeftFirst.isEmpty())
			return this.second.trimLeft();
		// first reads at least a whitespace string
		else if (first.readsWhiteSpaceString())
			return new Or(new Comp(trimLeftFirst, second), second.trimLeft());
//			return new Comp(trimLeftFirst, new Or(second, second.trimLeft()));
		else
			return new Comp(trimLeftFirst, second);
	}

	@Override
	public RegularExpression trimRight() {
		RegularExpression trimRightSecond = second.trimRight().simplify();
		// it means that second reads just whitespaces strings (L(second)
		// \subseteq { }*)
		if (trimRightSecond.isEmpty())
			return this.first.trimRight();
		// second reads at least a whitespace string
		else if (second.readsWhiteSpaceString())
			return new Or(new Comp(first, trimRightSecond), first.trimRight());
//			return new Comp(new Or(first, first.trimRight()), trimRightSecond);
		else
			return new Comp(this.first, trimRightSecond);
	}

	@Override
	protected boolean readsWhiteSpaceString() {
		return first.readsWhiteSpaceString() && second.readsWhiteSpaceString();
	}

}
