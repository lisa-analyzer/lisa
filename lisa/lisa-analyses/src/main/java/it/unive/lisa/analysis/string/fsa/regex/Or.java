package it.unive.lisa.analysis.string.fsa.regex;

/**
 * A regular expression representing an or between two other regular
 * expressions.
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Or extends RegularExpression {

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
	public Or(RegularExpression first, RegularExpression second) {
		this.first = first;
		this.second = second;
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
		String f = first.toString();
		String s = second.toString();
		if (f.compareTo(s) < 0)
			return "(" + f + "|" + s + ")";
		else
			return "(" + s + "|" + f + ")";
	}

	/**
	 * Yields {@code true} if and only if both inner regular expressions are
	 * atoms.
	 * 
	 * @return {@code true} if that condition holds
	 */
	public boolean isAtomic() {
		return first instanceof Atom && second instanceof Atom;
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
		RegularExpression result = new Or(first, second);

		if (first.equals(second))
			return first;
		if (first instanceof EmptySet)
			result = second;
		else if (second instanceof EmptySet)
			result = first;
		else if (first instanceof Atom && first.asAtom().isEmpty() && second instanceof Atom
				&& second.asAtom().isEmpty())
			result = Atom.EPSILON;
		else if (first instanceof Atom && first.asAtom().isEmpty() && second instanceof Star)
			result = second;
		else if (second instanceof Atom && second.asAtom().isEmpty() && first instanceof Star)
			result = first;

		// "" + ee* => e*
		else if (first.equals(Atom.EPSILON) && second instanceof Comp && second.asComp().getSecond() instanceof Star
				&& second.asComp().getFirst().equals(second.asComp().getSecond().asStar().getOperand()))
			result = new Star(second.asComp().getFirst());
		// "" + e*e => e*
		else if (first.equals(Atom.EPSILON) && second instanceof Comp && second.asComp().getFirst() instanceof Star
				&& second.asComp().getSecond().equals(second.asComp().getFirst().asStar().getOperand()))
			result = new Star(second.asComp().getFirst());
		// this is a common situation
		// that yields to an ugly representation of the string
		// a(b + c)* + a((b + c)*b + (b + c)*c)(b + c)*
		// this is equivalent to a(b + c)*
		// IMPORTANT!! keep this as the last case
		else if (first instanceof Comp && first.asComp().getSecond() instanceof Star
				&& first.asComp().getSecond().asStar().getOperand() instanceof Or) {
			RegularExpression a = first.asComp().getFirst();
			Star bORcSTAR = first.asComp().getSecond().asStar();
			RegularExpression b = bORcSTAR.getOperand().asOr().getFirst();
			RegularExpression c = bORcSTAR.getOperand().asOr().getSecond();

			if (second instanceof Comp && second.asComp().getFirst().equals(a)
					&& second.asComp().getSecond() instanceof Comp
					&& second.asComp().getSecond().asComp().getSecond().equals(bORcSTAR)
					&& second.asComp().getSecond().asComp().getFirst() instanceof Or) {
				Or or = second.asComp().getSecond().asComp().getFirst().asOr();
				if (or.getFirst() instanceof Comp && or.getFirst().asComp().getFirst().equals(bORcSTAR)
						&& or.getFirst().asComp().getSecond().equals(b) && or.getSecond() instanceof Comp
						&& or.getSecond().asComp().getFirst().equals(bORcSTAR)
						&& or.getSecond().asComp().getSecond().equals(c)) {
					result = first;
				}
			}
		}

		return result;
	}
}
