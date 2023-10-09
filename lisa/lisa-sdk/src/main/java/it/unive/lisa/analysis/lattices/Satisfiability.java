package it.unive.lisa.analysis.lattices;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

/**
 * A lattice representing sets of possible Boolean values: {@code (true)},
 * {@code (false)}, {@code (true, false)} or unknown, {@code ()} or bottom.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public enum Satisfiability
		implements
		Lattice<Satisfiability> {
	/**
	 * Represent the fact that an expression is satisfied.
	 */
	SATISFIED {
		@Override
		public Satisfiability negate() {
			return NOT_SATISFIED;
		}

		@Override
		public Satisfiability and(
				Satisfiability other) {
			return other;
		}

		@Override
		public Satisfiability or(
				Satisfiability other) {
			return this;
		}

		@Override
		public Satisfiability lub(
				Satisfiability other)
				throws SemanticException {
			if (other == UNKNOWN || other == NOT_SATISFIED)
				return UNKNOWN;
			return this;
		}

		@Override
		public boolean lessOrEqual(
				Satisfiability other)
				throws SemanticException {
			return other == this || other == UNKNOWN;
		}

		@Override
		public Satisfiability glb(
				Satisfiability other) {
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
		public Satisfiability and(
				Satisfiability other) {
			return this;
		}

		@Override
		public Satisfiability or(
				Satisfiability other) {
			return other;
		}

		@Override
		public Satisfiability lub(
				Satisfiability other)
				throws SemanticException {
			if (other == UNKNOWN || other == SATISFIED)
				return UNKNOWN;
			return this;
		}

		@Override
		public boolean lessOrEqual(
				Satisfiability other)
				throws SemanticException {
			return other == this || other == UNKNOWN;
		}

		@Override
		public Satisfiability glb(
				Satisfiability other) {
			if (other == BOTTOM || other == SATISFIED)
				return BOTTOM;
			return this;
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
		public Satisfiability and(
				Satisfiability other) {
			if (other == NOT_SATISFIED)
				return other;

			return this;
		}

		@Override
		public Satisfiability or(
				Satisfiability other) {
			if (other == SATISFIED)
				return other;

			return this;
		}

		@Override
		public Satisfiability lub(
				Satisfiability other)
				throws SemanticException {
			return this;
		}

		@Override
		public boolean lessOrEqual(
				Satisfiability other)
				throws SemanticException {
			return other == UNKNOWN;
		}

		@Override
		public Satisfiability glb(
				Satisfiability other) {
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
		public Satisfiability and(
				Satisfiability other) {
			return this;
		}

		@Override
		public Satisfiability or(
				Satisfiability other) {
			return this;
		}

		@Override
		public Satisfiability lub(
				Satisfiability other)
				throws SemanticException {
			return other;
		}

		@Override
		public boolean lessOrEqual(
				Satisfiability other)
				throws SemanticException {
			return true;
		}

		@Override
		public Satisfiability glb(
				Satisfiability other) {
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
	public abstract Satisfiability and(
			Satisfiability other);

	/**
	 * Performs a logical or between this satisfiability and the given one.
	 * 
	 * @param other the other satisfiability
	 * 
	 * @return the logical or between the two satisfiability instances
	 */
	public abstract Satisfiability or(
			Satisfiability other);

	/**
	 * Performs the greatest lower bound operation between this satisfiability
	 * and the given one.
	 * 
	 * @param other the other satisfiability
	 * 
	 * @return the result of the greatest lower bound
	 */
	public abstract Satisfiability glb(
			Satisfiability other);

	/**
	 * Transforms a boolean value to a {@link Satisfiability} instance.
	 * 
	 * @param bool the boolean to transform
	 * 
	 * @return {@link #SATISFIED} if {@code bool} is {@code true},
	 *             {@link #NOT_SATISFIED} otherwise
	 */
	public static Satisfiability fromBoolean(
			boolean bool) {
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
	 * Yields whether or not this element can represent a {@code true} result.
	 * 
	 * @return {@code true} if that condition holds
	 */
	public boolean mightBeTrue() {
		return this == SATISFIED || this == UNKNOWN;
	}

	/**
	 * Yields whether or not this element can represent a {@code false} result.
	 * 
	 * @return {@code true} if that condition holds
	 */
	public boolean mightBeFalse() {
		return this == NOT_SATISFIED || this == UNKNOWN;
	}

	@Override
	public StructuredRepresentation representation() {
		return new StringRepresentation(name());
	}
}
