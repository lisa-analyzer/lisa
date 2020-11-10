package it.unive.lisa.analysis;

import it.unive.lisa.symbolic.Identifier;
import it.unive.lisa.symbolic.SymbolicExpression;

/**
 * A domain able to determine how abstract information evolves thanks to the
 * semantics of statements and expressions.
 * 
 * @param <D> the concrete {@link SemanticDomain} instance
 * @param <E> the type of {@link SymbolicExpression} that this domain can
 *            process
 * @param <I> the type of variable {@link Identifier} that this domain can
 *            handle
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface SemanticDomain<D extends SemanticDomain<D, E, I>, E extends SymbolicExpression, I extends Identifier> {

	/**
	 * Yields a copy of this domain, where {@code id} has been assigned to
	 * {@code value}.
	 * 
	 * @param id    the identifier to assign the value to
	 * @param value the value to assign
	 * @return a copy of this domain, modified by the assignment
	 */
	D assign(I id, E value);

	/**
	 * Yields a copy of this domain, that has been modified accordingly to the
	 * semantics of the given {@code expression}.
	 * 
	 * @param expression the expression whose semantics need to be computed
	 * @return a copy of this domain, modified accordingly to the semantics of
	 *         {@code expression}
	 */
	D smallStepSemantics(E expression);

	/**
	 * Yields a copy of this domain, modified by assuming that the given expression
	 * holds. It is required that the returned domain is in relation with this one.
	 * A safe (but imprecise) implementation of this method can always return
	 * {@code this}.
	 * 
	 * @param expression the expression to assume to hold.
	 * @return the (optionally) modified copy of this domain
	 */
	D assume(E expression);

	/**
	 * Checks if the given expression is satisfied by the abstract values of this
	 * domain, returning an instance of {@link Satisfiability}.
	 * 
	 * @param expression the expression whose satisfiability is to be evaluated
	 * @return {@link Satisfiability#SATISFIED} is the expression is satisfied by
	 *         the values of this domain, {@link Satisfiability#NOT_SATISFIED} if it
	 *         is not satisfied, or {@link Satisfiability#UNKNOWN} if it is either
	 *         impossible to determine if it satisfied, or if it is satisfied by
	 *         some values and not by some others (this is equivalent to a TOP
	 *         boolean value)
	 */
	Satisfiability satisfy(E expression);

	/**
	 * The satisfiability of an expression.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public enum Satisfiability {
		/**
		 * Represent the fact that an expression is satisfied.
		 */
		SATISFIED {
			@Override
			public Satisfiability negate() {
				return NOT_SATISFIED;
			}

			@Override
			public Satisfiability and(Satisfiability other) {
				return other;
			}

			@Override
			public Satisfiability or(Satisfiability other) {
				return this;
			}
		},

		/**
		 * Represent the fact that an expression is not satisfied.
		 */
		NOT_SATISFIED {
			@Override
			public Satisfiability negate() {
				return SATISFIED;
			}

			@Override
			public Satisfiability and(Satisfiability other) {
				return this;
			}

			@Override
			public Satisfiability or(Satisfiability other) {
				return other;
			}
		},

		/**
		 * Represent the fact that it is not possible to determine whether or not an
		 * expression is satisfied.
		 */
		UNKNOWN {
			@Override
			public Satisfiability negate() {
				return this;
			}

			@Override
			public Satisfiability and(Satisfiability other) {
				if (other == NOT_SATISFIED)
					return other;

				return this;
			}

			@Override
			public Satisfiability or(Satisfiability other) {
				if (other == SATISFIED)
					return other;

				return this;
			}
		};

		/**
		 * Negates the current satisfiability, getting the opposite result.
		 * 
		 * @return the negation of this satisfiability instance
		 */
		public abstract Satisfiability negate();

		/**
		 * Performs a logical and between this satisfiability and the given one.
		 * 
		 * @param other the other satisfiability
		 * @return the logical and between the two satisfiability instances
		 */
		public abstract Satisfiability and(Satisfiability other);

		/**
		 * Performs a logical or between this satisfiability and the given one.
		 * 
		 * @param other the other satisfiability
		 * @return the logical or between the two satisfiability instances
		 */
		public abstract Satisfiability or(Satisfiability other);

		/**
		 * Transforms a boolean value to a {@link Satisfiability} instance.
		 * 
		 * @param bool the boolean to transform
		 * @return {@link #SATISFIED} if {@code bool} is {@code true},
		 *         {@link #NOT_SATISFIED} otherwise
		 */
		public static Satisfiability fromBoolean(boolean bool) {
			return bool ? SATISFIED : NOT_SATISFIED;
		}
	}
}
