package it.unive.lisa.analysis.string.fsa.regex;

/**
 * A regular expression that can be used to represent the language recognized by
 * an automaton.
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class RegularExpression {

	/**
	 * Yields a simplified version of this regular expression. Simplification
	 * happens through heuristics.
	 * 
	 * @return a simplified regular expression equivalent to this
	 */
	public abstract RegularExpression simplify();

	@Override
	public abstract int hashCode();

	@Override
	public abstract boolean equals(Object other);

	/**
	 * Casts this regular expression to an {@link Atom} if this regular
	 * expression is an {@link Atom}. Returns {@code null} otherwise.
	 * 
	 * @return this regular expression casted to {@link Atom}, or {@code null}
	 */
	public final Atom asAtom() {
		return this instanceof Atom ? (Atom) this : null;
	}

	/**
	 * Casts this regular expression to a {@link Comp} if this regular
	 * expression is a {@link Comp}. Returns {@code null} otherwise.
	 * 
	 * @return this regular expression casted to {@link Comp}, or {@code null}
	 */
	public final Comp asComp() {
		return this instanceof Comp ? (Comp) this : null;
	}

	/**
	 * Casts this regular expression to an {@link EmptySet} if this regular
	 * expression is an {@link EmptySet}. Returns {@code null} otherwise.
	 * 
	 * @return this regular expression casted to {@link EmptySet}, or
	 *             {@code null}
	 */
	public final EmptySet asEmptySet() {
		return this instanceof EmptySet ? (EmptySet) this : null;
	}

	/**
	 * Casts this regular expression to an {@link Or} if this regular expression
	 * is an {@link Or}. Returns {@code null} otherwise.
	 * 
	 * @return this regular expression casted to {@link Or}, or {@code null}
	 */
	public final Or asOr() {
		return this instanceof Or ? (Or) this : null;
	}

	/**
	 * Casts this regular expression to a {@link Star} if this regular
	 * expression is a {@link Star}. Returns {@code null} otherwise.
	 * 
	 * @return this regular expression casted to {@link Star}, or {@code null}
	 */
	public final Star asStar() {
		return this instanceof Star ? (Star) this : null;
	}
}
