package it.unive.lisa.analysis;

import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import java.util.Collection;

/**
 * A domain able to determine how abstract information evolves thanks to the
 * semantics of statements and expressions.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <D> the concrete {@link SemanticDomain} instance
 * @param <E> the type of {@link SymbolicExpression} that this domain can
 *                process
 * @param <I> the type of variable {@link Identifier} that this domain can
 *                handle
 */
public interface SemanticDomain<D extends SemanticDomain<D, E, I>, E extends SymbolicExpression, I extends Identifier> {

	/**
	 * Yields a copy of this domain, where {@code id} has been assigned to
	 * {@code value}.
	 * 
	 * @param id         the identifier to assign the value to
	 * @param expression the expression to assign
	 * @param pp         the program point that where this operation is being
	 *                       evaluated
	 * 
	 * @return a copy of this domain, modified by the assignment
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	D assign(I id, E expression, ProgramPoint pp) throws SemanticException;

	/**
	 * Yields a copy of this domain, that has been modified accordingly to the
	 * semantics of the given {@code expression}.
	 * 
	 * @param expression the expression whose semantics need to be computed
	 * @param pp         the program point that where this operation is being
	 *                       evaluated
	 * 
	 * @return a copy of this domain, modified accordingly to the semantics of
	 *             {@code expression}
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	D smallStepSemantics(E expression, ProgramPoint pp) throws SemanticException;

	/**
	 * Yields a copy of this domain, modified by assuming that the given
	 * expression holds. It is required that the returned domain is in relation
	 * with this one. A safe (but imprecise) implementation of this method can
	 * always return {@code this}.
	 * 
	 * @param expression the expression to assume to hold.
	 * @param pp         the program point that where this operation is being
	 *                       evaluated
	 * 
	 * @return the (optionally) modified copy of this domain
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	D assume(E expression, ProgramPoint pp) throws SemanticException;

	/**
	 * Forgets an {@link Identifier}. This means that all information regarding
	 * the given {@code id} will be lost. This method should be invoked whenever
	 * an identifier gets out of scope.
	 * 
	 * @param id the identifier to forget
	 * 
	 * @return the semantic domain without information about the given id
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	D forgetIdentifier(Identifier id) throws SemanticException;

	/**
	 * Forgets all the given {@link Identifier}s. The default implementation of
	 * this method iterates on {@code ids}, invoking
	 * {@link #forgetIdentifier(Identifier)} on each element.
	 * 
	 * @param ids the collection of identifiers to forget
	 * 
	 * @return the semantic domain without information about the given ids
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	public default D forgetIdentifiers(Collection<Identifier> ids) throws SemanticException {
		@SuppressWarnings("unchecked")
		D result = (D) this;
		for (Identifier id : ids)
			result = result.forgetIdentifier(id);
		return result;
	}

	/**
	 * Checks if the given expression is satisfied by the abstract values of
	 * this domain, returning an instance of {@link Satisfiability}.
	 * 
	 * @param expression the expression whose satisfiability is to be evaluated
	 * @param pp         the program point that where this operation is being
	 *                       evaluated
	 * 
	 * @return {@link Satisfiability#SATISFIED} is the expression is satisfied
	 *             by the values of this domain,
	 *             {@link Satisfiability#NOT_SATISFIED} if it is not satisfied,
	 *             or {@link Satisfiability#UNKNOWN} if it is either impossible
	 *             to determine if it satisfied, or if it is satisfied by some
	 *             values and not by some others (this is equivalent to a TOP
	 *             boolean value)
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	Satisfiability satisfies(E expression, ProgramPoint pp) throws SemanticException;

	/**
	 * The satisfiability of an expression.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public enum Satisfiability implements Lattice<Satisfiability> {
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

			@Override
			public Satisfiability lub(Satisfiability other) throws SemanticException {
				if (other == UNKNOWN || other == NOT_SATISFIED)
					return UNKNOWN;
				return this;
			}

			@Override
			public Satisfiability widening(Satisfiability other) throws SemanticException {
				return lub(other);
			}

			@Override
			public boolean lessOrEqual(Satisfiability other) throws SemanticException {
				return other == this || other == UNKNOWN;
			}

			@Override
			public Satisfiability glb(Satisfiability other) {
				if (other == BOTTOM || other == NOT_SATISFIED)
					return BOTTOM;
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

			@Override
			public Satisfiability lub(Satisfiability other) throws SemanticException {
				if (other == UNKNOWN || other == SATISFIED)
					return UNKNOWN;
				return this;
			}

			@Override
			public Satisfiability widening(Satisfiability other) throws SemanticException {
				return lub(other);
			}

			@Override
			public boolean lessOrEqual(Satisfiability other) throws SemanticException {
				return other == this || other == UNKNOWN;
			}

			@Override
			public Satisfiability glb(Satisfiability other) {
				if (other == BOTTOM || other == SATISFIED)
					return BOTTOM;
				return this;
			}
		},

		/**
		 * Represent the fact that it is not possible to determine whether or
		 * not an expression is satisfied.
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

			@Override
			public Satisfiability lub(Satisfiability other) throws SemanticException {
				return this;
			}

			@Override
			public Satisfiability widening(Satisfiability other) throws SemanticException {
				return this;
			}

			@Override
			public boolean lessOrEqual(Satisfiability other) throws SemanticException {
				return other == UNKNOWN;
			}

			@Override
			public Satisfiability glb(Satisfiability other) {
				return other;
			}
		},

		/**
		 * Represent the fact that the satisfiability evaluation resulted in an
		 * error.
		 */
		BOTTOM {
			@Override
			public Satisfiability negate() {
				return this;
			}

			@Override
			public Satisfiability and(Satisfiability other) {
				return this;
			}

			@Override
			public Satisfiability or(Satisfiability other) {
				return this;
			}

			@Override
			public Satisfiability lub(Satisfiability other) throws SemanticException {
				return other;
			}

			@Override
			public Satisfiability widening(Satisfiability other) throws SemanticException {
				return other;
			}

			@Override
			public boolean lessOrEqual(Satisfiability other) throws SemanticException {
				return true;
			}

			@Override
			public Satisfiability glb(Satisfiability other) {
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
		 * 
		 * @return the logical and between the two satisfiability instances
		 */
		public abstract Satisfiability and(Satisfiability other);

		/**
		 * Performs a logical or between this satisfiability and the given one.
		 * 
		 * @param other the other satisfiability
		 * 
		 * @return the logical or between the two satisfiability instances
		 */
		public abstract Satisfiability or(Satisfiability other);

		/**
		 * Performs the greatest lower bound operation between this
		 * satisfiability and the given one.
		 * 
		 * @param other the other satisfiability
		 * 
		 * @return the result of the greatest lower bound
		 */
		public abstract Satisfiability glb(Satisfiability other);

		/**
		 * Transforms a boolean value to a {@link Satisfiability} instance.
		 * 
		 * @param bool the boolean to transform
		 * 
		 * @return {@link #SATISFIED} if {@code bool} is {@code true},
		 *             {@link #NOT_SATISFIED} otherwise
		 */
		public static Satisfiability fromBoolean(boolean bool) {
			return bool ? SATISFIED : NOT_SATISFIED;
		}

		@Override
		public Satisfiability top() {
			return UNKNOWN;
		}

		@Override
		public Satisfiability bottom() {
			return BOTTOM;
		}
	}

	/**
	 * Yields a textual representation of the content of this domain's instance.
	 * 
	 * @return the textual representation
	 */
	String representation();
}
