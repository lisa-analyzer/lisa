package it.unive.lisa.analysis.lattices;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
import it.unive.lisa.symbolic.value.operator.binary.LogicalAnd;
import it.unive.lisa.symbolic.value.operator.binary.LogicalOr;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
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
		BaseNonRelationalValueDomain<Satisfiability> {
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

	@Override
	public Satisfiability lubAux(
			Satisfiability other)
			throws SemanticException {
		return UNKNOWN;
	}

	@Override
	public Satisfiability glbAux(
			Satisfiability other)
			throws SemanticException {
		return BOTTOM;
	}

	@Override
	public boolean lessOrEqualAux(
			Satisfiability other)
			throws SemanticException {
		return false;
	}

	@Override
	public Satisfiability evalNonNullConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (constant.getValue() instanceof Boolean)
			return fromBoolean((Boolean) constant.getValue());
		return UNKNOWN;
	}

	@Override
	public Satisfiability evalUnaryExpression(
			UnaryOperator operator,
			Satisfiability arg,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (operator == LogicalNegation.INSTANCE)
			return arg.negate();
		return UNKNOWN;
	}

	@Override
	public Satisfiability evalBinaryExpression(
			BinaryOperator operator,
			Satisfiability left,
			Satisfiability right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (operator == LogicalAnd.INSTANCE)
			return left.and(right);
		if (operator == LogicalOr.INSTANCE)
			return left.or(right);
		if (operator == ComparisonEq.INSTANCE)
			if (left == UNKNOWN || right == UNKNOWN)
				return UNKNOWN;
			else
				return fromBoolean(left.equals(right));
		if (operator == ComparisonNe.INSTANCE)
			if (left == UNKNOWN || right == UNKNOWN)
				return UNKNOWN;
			else
				return fromBoolean(!left.equals(right));
		return UNKNOWN;
	}
}
