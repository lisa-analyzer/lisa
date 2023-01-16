package it.unive.lisa.util.datastructures.regex;

import it.unive.lisa.util.datastructures.automaton.AutomataFactory;
import it.unive.lisa.util.datastructures.automaton.Automaton;
import it.unive.lisa.util.datastructures.automaton.TransitionSymbol;
import java.util.HashSet;
import java.util.Set;

/**
 * A {@link RegularExpression} representing an or between two other regular
 * expressions.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public final class Or extends RegularExpression {

	/**
	 * The first regular expression
	 */
	private final RegularExpression first;

	/**
	 * The second regular expression
	 */
	private final RegularExpression second;

	/**
	 * Builds the or.
	 * 
	 * @param first  the first regular expression
	 * @param second the second regular expression
	 */
	Or(RegularExpression first, RegularExpression second) {
		// make things deterministic: order the branches
		if (first.compareTo(second) <= 0) {
			this.first = first;
			this.second = second;
		} else {
			this.first = second;
			this.second = first;
		}
	}

	/**
	 * Yields the second regular expression.
	 * 
	 * @return the second regular expression
	 */
	public RegularExpression getSecond() {
		return second;
	}

	/**
	 * Yields the first regular expression.
	 * 
	 * @return the first regular expression
	 */
	public RegularExpression getFirst() {
		return first;
	}

	@Override
	public String toString() {
		return "(" + first.toString() + " + " + second.toString() + ")";
	}

	/**
	 * Yields {@code true} if and only if both inner regular expressions are
	 * atoms.
	 * 
	 * @return {@code true} if that condition holds
	 */
	public boolean isAtomic() {
		return first.isAtom() && second.isAtom();
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
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Or other = (Or) obj;
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
		if (first.compareTo(second) > 0) {
			RegularExpression tmp = first;
			first = second;
			second = tmp;
		}
		RegularExpression result = first.or(second);

		// a + a = a
		if (first.equals(second))
			return first;

		// a + emptyset = a
		if (first.isEmptySet())
			result = second;
		else if (second.isEmptySet())
			result = first;

		// epsilon + epsilon = epsilon
		else if (first.isEpsilon() && second.isEpsilon())
			result = Atom.EPSILON;

		// epsilon + a* = a*
		else if (first.isEpsilon() && second.isStar())
			result = second;
		else if (second.isEpsilon() && first.isStar())
			result = first;

		// "" + ee* = e*
		else if (first.isEpsilon() && second.isComp() && second.asComp().getSecond().isStar()
				&& second.asComp().getFirst().equals(second.asComp().getSecond().asStar().getOperand()))
			result = second.asComp().getFirst().star();

		// "" + e*e = e*
		else if (first.isEpsilon() && second.isComp() && second.asComp().getFirst().isStar()
				&& second.asComp().getSecond().equals(second.asComp().getFirst().asStar().getOperand()))
			result = second.asComp().getFirst().star();

		// this is a common situation
		// that yields to an ugly representation of the string
		// a(b + c)* + a((b + c)*b + (b + c)*c)(b + c)*
		// this is equivalent to a(b + c)*
		// IMPORTANT!! keep this as the last case
		else if (first.isComp() && first.asComp().getSecond().isStar()
				&& first.asComp().getSecond().asStar().getOperand().isOr()) {
			RegularExpression a = first.asComp().getFirst();
			Star bORcSTAR = first.asComp().getSecond().asStar();
			RegularExpression b = bORcSTAR.getOperand().asOr().getFirst();
			RegularExpression c = bORcSTAR.getOperand().asOr().getSecond();

			if (second.isComp() && second.asComp().getFirst().equals(a)
					&& second.asComp().getSecond().isComp()
					&& second.asComp().getSecond().asComp().getSecond().equals(bORcSTAR)
					&& second.asComp().getSecond().asComp().getFirst().isOr()) {
				Or or = second.asComp().getSecond().asComp().getFirst().asOr();
				if (or.getFirst().isComp() && or.getFirst().asComp().getFirst().equals(bORcSTAR)
						&& or.getFirst().asComp().getSecond().equals(b) && or.getSecond().isComp()
						&& or.getSecond().asComp().getFirst().equals(bORcSTAR)
						&& or.getSecond().asComp().getSecond().equals(c)) {
					result = first;
				}
			}
		}

		return result;
	}

	@Override
	public <A extends Automaton<A, T>,
			T extends TransitionSymbol<T>> A toAutomaton(AutomataFactory<A, T> factory) {
		return first.toAutomaton(factory).union(second.toAutomaton(factory));
	}

	@Override
	public Set<PartialSubstring> substringAux(int charsToSkip, int missingChars) {
		Set<PartialSubstring> result = new HashSet<>();
		result.addAll(first.substringAux(charsToSkip, missingChars));
		result.addAll(second.substringAux(charsToSkip, missingChars));
		return result;
	}

	@Override
	public boolean isEmpty() {
		return first.isEmpty() && second.isEmpty();
	}

	@Override
	public boolean is(String str) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int maxLength() {
		return Math.max(first.maxLength(), second.maxLength());
	}

	@Override
	public int minLength() {
		return Math.min(first.minLength(), second.minLength());
	}

	@Override
	public boolean mayContain(String s) {
		return first.mayContain(s) || second.mayContain(s);
	}

	@Override
	public boolean contains(String s) {
		return first.contains(s) && second.contains(s);
	}

	@Override
	public boolean mayStartWith(String s) {
		return first.mayStartWith(s) || second.mayEndWith(s);
	}

	@Override
	public boolean startsWith(String s) {
		return first.startsWith(s) && second.startsWith(s);
	}

	@Override
	public boolean mayEndWith(String s) {
		return first.mayEndWith(s) || second.mayEndWith(s);
	}

	@Override
	public boolean endsWith(String s) {
		return first.endsWith(s) && second.endsWith(s);
	}

	@Override
	protected RegularExpression unrollStarToFixedLength(int length) {
		return first.unrollStarToFixedLength(length).or(second.unrollStarToFixedLength(length));
	}

	@Override
	public RegularExpression reverse() {
		return first.reverse().or(second.reverse());
	}

	@Override
	protected RegularExpression topAsEmptyString() {
		return first.topAsEmptyString().or(second.topAsEmptyString());
	}

	@Override
	protected RegularExpression topAsSingleChar() {
		return first.topAsSingleChar().or(second.topAsSingleChar());
	}

	@Override
	public RegularExpression[] explode() {
		throw new UnsupportedOperationException();
	}

	@Override
	protected int compareToAux(RegularExpression other) {
		int cmp;
		if ((cmp = first.compareTo(other.asOr().first)) != 0)
			return cmp;
		return second.compareTo(other.asOr().second);
	}
}
