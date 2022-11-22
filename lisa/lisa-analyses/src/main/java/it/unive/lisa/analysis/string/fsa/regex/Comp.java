package it.unive.lisa.analysis.string.fsa.regex;

/**
 * A regular expression representing the sequential composition of two regular
 * expressions.
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Comp extends RegularExpression {

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
	public Comp(RegularExpression first, RegularExpression second) {
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
	public boolean equals(Object obj) {
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

		RegularExpression result = new Comp(first, second);

		if (first instanceof EmptySet || second instanceof EmptySet)
			result = EmptySet.INSTANCE;
		else if (second instanceof Or)
			result = new Or(new Comp(first, second.asOr().getFirst()), new Comp(first, second.asOr().getSecond()));
		else if (first instanceof Atom && second instanceof Or && second.asOr().isAtomic())
			result = new Or(new Atom(first.toString() + second.asOr().getFirst().toString()),
					new Atom(this.first.toString() + second.asOr().getSecond().toString()));
		else if (second instanceof Atom && second.asAtom().isEmpty())
			result = first;
		else if (first instanceof Atom && first.asAtom().isEmpty())
			result = second;
		else if (first instanceof Star && second instanceof Star && second.asStar().getOperand() instanceof Comp
				&& second.asStar().getOperand().asComp().second instanceof Star
				&& second.asStar().getOperand().asComp().second.asStar().getOperand()
						.equals(first.asStar().getOperand()))
			result = new Star(new Or(first.asStar().getOperand(), second.asStar().getOperand().asComp().first));
//		// id=([T];id=)*[T]; => (id=[T];)*
		else if (first instanceof Atom && second instanceof Comp && second.asComp().first instanceof Star
				&& second.asComp().second instanceof Atom
				&& new Atom(second.asComp().second.asAtom().toString() + first.asAtom().toString())
						.equals(second.asComp().first.asStar().getOperand()))
			result = new Star(new Atom(first.asAtom().toString() + second.asComp().second.asAtom().toString()));
		else if (first instanceof Star && second instanceof Comp && second.asComp().first instanceof Star
				&& first.asStar().getOperand().equals(second.asComp().first.asStar().getOperand()))
			result = new Comp(first, second.asComp().second);
//
		// (r)*(r)* -> (r)*
		else if (first instanceof Star && second instanceof Star
				&& first.asStar().getOperand().equals(second.asStar().getOperand()))
			result = first;

		return result;
	}
}
