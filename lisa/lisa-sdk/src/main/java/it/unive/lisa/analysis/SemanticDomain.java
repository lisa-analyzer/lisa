package it.unive.lisa.analysis;

import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.function.Predicate;

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
	 * @param src        the program point that where this operation is being
	 *                       evaluated, corresponding to the one that generated
	 *                       the given expression
	 * @param dest       the program point where the execution will move after
	 *                       the expression has been assumed
	 * 
	 * @return the (optionally) modified copy of this domain
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	D assume(E expression, ProgramPoint src, ProgramPoint dest) throws SemanticException;

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
	 * Forgets all {@link Identifier}s that match the given predicate. This
	 * means that all information regarding the those identifiers will be lost.
	 * This method should be invoked whenever an identifier gets out of scope.
	 * 
	 * @param test the test to identify the targets of the removal
	 * 
	 * @return the semantic domain without information about the ids
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	D forgetIdentifiersIf(Predicate<Identifier> test) throws SemanticException;

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
	default D forgetIdentifiers(Iterable<Identifier> ids) throws SemanticException {
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
	 * Pushes a new scope, identified by the give token, in the domain. This
	 * causes information about all variables not associated with a scope (and
	 * thus visible) to be mapped to the given scope and hidden away, until the
	 * scope is popped with {@link #popScope(ScopeToken)}.
	 *
	 * @param token the token identifying the scope to push
	 * 
	 * @return a copy of this domain where the local variables have been hidden
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	D pushScope(ScopeToken token) throws SemanticException;

	/**
	 * Pops the scope identified by the given token from the domain. This causes
	 * all the visible variables (i.e. that are not mapped to a scope) to be
	 * removed from the domain, while the local variables that were associated
	 * to the given scope token (and thus hidden) will become visible again.
	 *
	 * @param token the token of the scope to be restored
	 * 
	 * @return a copy of this domain where the local variables have been
	 *             removed, while the variables mapped to the given scope are
	 *             visible again
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	D popScope(ScopeToken token) throws SemanticException;

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

		/**
		 * Yields whether or not this element can represent a {@code true}
		 * result.
		 * 
		 * @return {@code true} if that condition holds
		 */
		public boolean mightBeTrue() {
			return this == SATISFIED || this == UNKNOWN;
		}

		/**
		 * Yields whether or not this element can represent a {@code false}
		 * result.
		 * 
		 * @return {@code true} if that condition holds
		 */
		public boolean mightBeFalse() {
			return this == NOT_SATISFIED || this == UNKNOWN;
		}
	}

	/**
	 * Yields a {@link DomainRepresentation} of the information contained in
	 * this domain's instance.
	 * 
	 * @return the representation
	 */
	DomainRepresentation representation();

	/**
	 * Yields a unique instance of the specific domain, of class {@code domain},
	 * contained inside the domain, also recursively querying inner domains
	 * (enabling retrieval of semantic domains through Cartesian products or
	 * other types of combinations).<br>
	 * <br>
	 * The default implementation of this method lubs together (using
	 * {@link Lattice#lub(Lattice)}) all instances returned by
	 * {@link #getAllDomainInstances(Class)}, defaulting to {@code null} if no
	 * instance is returned.
	 * 
	 * @param <T>    the type of domain to retrieve
	 * @param domain the class of the domain instance to retrieve
	 * 
	 * @return the instance of that domain, or {@code null}
	 * 
	 * @throws SemanticException if an exception happens while lubbing the
	 *                               results
	 */
	default <T extends SemanticDomain<T, ?, ?> & Lattice<T>> T getDomainInstance(Class<T> domain)
			throws SemanticException {
		Collection<T> all = getAllDomainInstances(domain);
		T result = null;
		for (T instance : all)
			if (result == null)
				result = instance;
			else
				result = result.lub(instance);

		return result;
	}

	/**
	 * Yields all of the instances of a specific domain, of class
	 * {@code domain}, contained inside this domain, also recursively querying
	 * inner domains (enabling retrieval of semantic domains through Cartesian
	 * products or other types of combinations).<br>
	 * <br>
	 * The default implementation of this method returns a singleton collection
	 * containing {@code this} if {@code domain.isAssignableFrom(getClass())}
	 * holds, otherwise it returns an empty collection.
	 * 
	 * @param <T>    the type of domain to retrieve
	 * @param domain the class of the domain instance to retrieve
	 * 
	 * @return the instances of that domain
	 */
	@SuppressWarnings("unchecked")
	default <T extends SemanticDomain<?, ?, ?>> Collection<T> getAllDomainInstances(Class<T> domain) {
		Collection<T> result = new HashSet<>();
		if (domain.isAssignableFrom(getClass()))
			result.add((T) this);

		return result;
	}
}
