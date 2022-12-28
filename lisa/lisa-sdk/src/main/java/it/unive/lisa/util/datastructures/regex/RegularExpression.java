package it.unive.lisa.util.datastructures.regex;

import it.unive.lisa.util.datastructures.automaton.AutomataFactory;
import it.unive.lisa.util.datastructures.automaton.Automaton;
import it.unive.lisa.util.datastructures.automaton.TransitionSymbol;
import it.unive.lisa.util.datastructures.regex.symbolic.SymbolicString;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A regular expression that can be recognized by an {@link Automaton}, or that
 * can be used to represent the language recognized by an automaton.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class RegularExpression implements TransitionSymbol<RegularExpression> {

	@Override
	public final int compareTo(RegularExpression o) {
		if (getClass() != o.getClass())
			return getClass().getName().compareTo(o.getClass().getName());
		return compareToAux(o);
	}

	/**
	 * Auxiliary {@link #compareTo(RegularExpression)} that can safely assume
	 * that {@code other} is an object of the same class as {@code this}.
	 * 
	 * @param other the other regular expression
	 * 
	 * @return a negative integer, zero, or a positive integer as this object is
	 *             less than, equal to, or greater than the specified object
	 */
	protected abstract int compareToAux(RegularExpression other);

	@Override
	public boolean isEpsilon() {
		return this == Atom.EPSILON || (this.isAtom() && asAtom().isEmpty());
	}

	/**
	 * Yields a simplified version of this regular expression. Simplification
	 * happens through heuristics.
	 * 
	 * @return a simplified regular expression equivalent to this
	 */
	public abstract RegularExpression simplify();

	/**
	 * Transforms this regular expression into its equivalent automaton.
	 * 
	 * @param <A>     the concrete type of {@link Automaton} that this method
	 *                    yields
	 * @param <T>     the concrete type of {@link TransitionSymbol}s that
	 *                    instances of {@code A} have on their transitions
	 * @param factory the factory that can be used to create the automaton
	 * 
	 * @return the automaton
	 */
	public abstract <A extends Automaton<A, T>,
			T extends TransitionSymbol<T>> A toAutomaton(AutomataFactory<A, T> factory);

	/**
	 * Casts this regular expression to an {@link Atom} if this regular
	 * expression is an {@link Atom}. Returns {@code null} otherwise.
	 * 
	 * @return this regular expression casted to {@link Atom}, or {@code null}
	 */
	public final Atom asAtom() {
		return isAtom() ? (Atom) this : null;
	}

	/**
	 * Yields {@code true} if and only if this regular expression is an instance
	 * of {@link Atom}.
	 * 
	 * @return {@code true} if that condition holds
	 */
	public final boolean isAtom() {
		return this instanceof Atom;
	}

	/**
	 * Casts this regular expression to a {@link Comp} if this regular
	 * expression is a {@link Comp}. Returns {@code null} otherwise.
	 * 
	 * @return this regular expression casted to {@link Comp}, or {@code null}
	 */
	public final Comp asComp() {
		return isComp() ? (Comp) this : null;
	}

	/**
	 * Yields {@code true} if and only if this regular expression is an instance
	 * of {@link Comp}.
	 * 
	 * @return {@code true} if that condition holds
	 */
	public final boolean isComp() {
		return this instanceof Comp;
	}

	/**
	 * Casts this regular expression to an {@link EmptySet} if this regular
	 * expression is an {@link EmptySet}. Returns {@code null} otherwise.
	 * 
	 * @return this regular expression casted to {@link EmptySet}, or
	 *             {@code null}
	 */
	public final EmptySet asEmptySet() {
		return isEmptySet() ? (EmptySet) this : null;
	}

	/**
	 * Yields {@code true} if and only if this regular expression is an instance
	 * of {@link EmptySet}.
	 * 
	 * @return {@code true} if that condition holds
	 */
	public final boolean isEmptySet() {
		return this instanceof EmptySet;
	}

	/**
	 * Casts this regular expression to an {@link Or} if this regular expression
	 * is an {@link Or}. Returns {@code null} otherwise.
	 * 
	 * @return this regular expression casted to {@link Or}, or {@code null}
	 */
	public final Or asOr() {
		return isOr() ? (Or) this : null;
	}

	/**
	 * Yields {@code true} if and only if this regular expression is an instance
	 * of {@link Or}.
	 * 
	 * @return {@code true} if that condition holds
	 */
	public final boolean isOr() {
		return this instanceof Or;
	}

	/**
	 * Casts this regular expression to a {@link Star} if this regular
	 * expression is a {@link Star}. Returns {@code null} otherwise.
	 * 
	 * @return this regular expression casted to {@link Star}, or {@code null}
	 */
	public final Star asStar() {
		return isStar() ? (Star) this : null;
	}

	/**
	 * Yields {@code true} if and only if this regular expression is an instance
	 * of {@link Star}.
	 * 
	 * @return {@code true} if that condition holds
	 */
	public final boolean isStar() {
		return this instanceof Star;
	}

	/**
	 * Returns the set of all possible substrings of this regular expression,
	 * starting at the given index (inclusive) and ending at the given index
	 * (exclusive).
	 * 
	 * @param start the start index
	 * @param end   the end index
	 * 
	 * @return the set of all substrings
	 */
	public final Set<SymbolicString> substring(int start, int end) {
		return substringAux(start, end).stream().filter(ps -> ps.missingChars == 0).map(t -> t.getSubstring())
				.collect(Collectors.toSet());
	}

	/**
	 * A class that represents an intermediate result of the computation of
	 * {@link RegularExpression#substring}.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public final static class PartialSubstring {
		/**
		 * The current substring
		 */
		private final SymbolicString substring;

		/**
		 * The number of missing characters to complete the substring
		 */
		private final int missingChars;

		/**
		 * The number of characters to skip before starting to collect the
		 * substring
		 */
		private final int charsToStart;

		/**
		 * Builds the partial substring.
		 * 
		 * @param substring    the current substring
		 * @param missingChars the number of missing characters to complete the
		 *                         substring
		 * @param charsToStart the number of characters to skip before starting
		 *                         to collect the substring
		 */
		protected PartialSubstring(SymbolicString substring, int charsToStart, int missingChars) {
			this.substring = substring;
			this.missingChars = missingChars;
			this.charsToStart = charsToStart;
		}

		/**
		 * Joins this partial substring with the given one. This results in
		 * concatenating the two partial substrings, while keeping values from
		 * {@code other} for {@code charsToStart} and {@code missingChars}.
		 * 
		 * @param other the other partial substring
		 * 
		 * @return the joined partial substring
		 */
		protected PartialSubstring concat(PartialSubstring other) {
			return new PartialSubstring(substring.concat(other.substring), other.charsToStart, other.missingChars);
		}

		/**
		 * Yields the current partial substring.
		 * 
		 * @return the current partial substring
		 */
		protected SymbolicString getSubstring() {
			return substring;
		}

		/**
		 * Yields the number of missing characters to complete the substring.
		 * 
		 * @return the number of missing characters to complete the substring
		 */
		protected int getMissingChars() {
			return missingChars;
		}

		/**
		 * Yields the number of characters to skip before starting to collect
		 * the substring.
		 * 
		 * @return the number of characters to skip before starting to collect
		 *             the substring
		 */
		protected int getCharsToStart() {
			return charsToStart;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + charsToStart;
			result = prime * result + missingChars;
			result = prime * result + ((substring == null) ? 0 : substring.hashCode());
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
			PartialSubstring other = (PartialSubstring) obj;
			if (charsToStart != other.charsToStart)
				return false;
			if (missingChars != other.missingChars)
				return false;
			if (substring == null) {
				if (other.substring != null)
					return false;
			} else if (!substring.equals(other.substring))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "\"" + substring + "\" [" + charsToStart + " to start, " + missingChars + " missing]";
		}
	}

	/**
	 * Returns the set of all possible substrings of this regular expression,
	 * starting at the given index (inclusive) and ending at the given index
	 * (exclusive). Each substring is decorated with the number of characters
	 * that are still missing to reach the beginning of the substring, and the
	 * number of characters that still need to be added to the string to reach
	 * the desired length.
	 * 
	 * @param charsToSkip  the number of characters to skip before starting to
	 *                         collect the substring
	 * @param missingChars the number of missing characters to complete the
	 *                         substring
	 * 
	 * @return the set of partial substrings
	 */
	protected abstract Set<PartialSubstring> substringAux(int charsToSkip, int missingChars);

	/**
	 * Yields {@code true} if and only if this regular expression corresponds to
	 * the empty string or to no strings at all.
	 * 
	 * @return {@code true} if that condition holds
	 */
	public abstract boolean isEmpty();

	/**
	 * Yields {@code true} if and only if this regular expression corresponds to
	 * the given string.
	 * 
	 * @param str the string
	 * 
	 * @return {@code true} if that condition holds
	 */
	public abstract boolean is(String str);

	/**
	 * Yields the maximum length of this regular expression.
	 * 
	 * @return the maximum length
	 */
	public abstract int maxLength();

	/**
	 * Yields the minimum length of this regular expression.
	 * 
	 * @return the minimum length
	 */
	public abstract int minLength();

	/**
	 * Yields {@code true} if and only if this regular expression <b>may</b>
	 * contain the given string.
	 * 
	 * @param s the string
	 * 
	 * @return {@code true} if that condition holds
	 */
	public abstract boolean mayContain(String s);

	/**
	 * Yields {@code true} if and only if this regular expression <b>always</b>
	 * contains the given string.
	 * 
	 * @param s the string
	 * 
	 * @return {@code true} if that condition holds
	 */
	public abstract boolean contains(String s);

	/**
	 * Yields {@code true} if and only if this regular expression <b>may</b>
	 * start with the given string.
	 * 
	 * @param s the string
	 * 
	 * @return {@code true} if that condition holds
	 */
	public abstract boolean mayStartWith(String s);

	/**
	 * Yields {@code true} if and only if this regular expression <b>always</b>
	 * starts with the given string.
	 * 
	 * @param s the string
	 * 
	 * @return {@code true} if that condition holds
	 */
	public abstract boolean startsWith(String s);

	/**
	 * Yields {@code true} if and only if this regular expression <b>may</b> end
	 * with the given string.
	 * 
	 * @param s the string
	 * 
	 * @return {@code true} if that condition holds
	 */
	public abstract boolean mayEndWith(String s);

	/**
	 * Yields {@code true} if and only if this regular expression <b>always</b>
	 * ends with the given string.
	 * 
	 * @param s the string
	 * 
	 * @return {@code true} if that condition holds
	 */
	public abstract boolean endsWith(String s);

	/**
	 * Yields a new regular expression where all {@link Star} have been unrolled
	 * to a sequence of their inner regular expression of length {@code length}.
	 * 
	 * @param length the length
	 * 
	 * @return the regular expression with unrolled stars
	 */
	protected abstract RegularExpression unrollStarToFixedLength(int length);

	/**
	 * Yields a new regular expression where all {@link TopAtom} are assumed to
	 * have length one.
	 * 
	 * @return the regular expression with shrinked top regular expressions
	 */
	protected abstract RegularExpression topAsSingleChar();

	/**
	 * Yields a new regular expression where all {@link TopAtom} are assumed to
	 * have length zero.
	 * 
	 * @return the regular expression with empty top regular expressions
	 */
	protected abstract RegularExpression topAsEmptyString();

	/**
	 * Yields a new regular expression that is the exploded version of this one,
	 * that is, where all atoms have been broken down to the composition of the
	 * characters that compose their inner strings.
	 * 
	 * @return the exploded regular expression
	 */
	public abstract RegularExpression[] explode();

	/**
	 * Builds the Kleene closure of this regular expression ({@link Star}),
	 * recognizing {@code this} zero or more times.
	 * 
	 * @return the closure of this regular expression
	 */
	public final RegularExpression star() {
		if (isEpsilon())
			return this;
		return new Star(this);
	}

	/**
	 * Joins together two regular expression, that is, it builds a {@link Comp}
	 * recognizing this regular expression first, and than {@code other}.
	 * 
	 * @param other the other regular expression
	 * 
	 * @return the joined regular expression
	 */
	public final RegularExpression comp(RegularExpression other) {
		if (this.isEpsilon())
			return other;
		if (other.isEpsilon())
			return this;
		return new Comp(this, other);
	}

	/**
	 * Builds the disjunction ({@link Or}) between {@code this} and the given
	 * regular expression.
	 * 
	 * @param other the other regular expression
	 * 
	 * @return the disjunction
	 */
	public final RegularExpression or(RegularExpression other) {
		if (this.isEpsilon() && other.isEpsilon())
			return this;
		return new Or(this, other);
	}
}
