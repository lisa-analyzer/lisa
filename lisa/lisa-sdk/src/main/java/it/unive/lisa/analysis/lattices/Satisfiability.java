package it.unive.lisa.analysis.lattices;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.combination.constraints.WholeValueElement;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.type.BooleanType;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Collections;
import java.util.Set;

/**
 * A lattice representing sets of possible Boolean values: {@code (true)},
 * {@code (false)}, {@code (true, false)} or unknown, {@code ()} or bottom.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public enum Satisfiability
		implements
		BaseLattice<Satisfiability>,
		WholeValueElement<Satisfiability> {

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
	public Set<BinaryExpression> constraints(
			ValueExpression e,
			ProgramPoint pp)
			throws SemanticException {
		if (isTop())
			return Collections.emptySet();
		if (isBottom())
			return null;

		BooleanType boolType = pp.getProgram().getTypes().getBooleanType();
		return Collections.singleton(
			new BinaryExpression(
				boolType,
				new Constant(boolType, this == SATISFIED ? true : false, e.getCodeLocation()),
				e,
				ComparisonEq.INSTANCE,
				e.getCodeLocation()));
	}

	@Override
	public Satisfiability generate(
			Set<BinaryExpression> constraints,
			ProgramPoint pp)
			throws SemanticException {
		if (constraints == null)
			return BOTTOM;

		for (BinaryExpression expr : constraints)
			if (expr.getOperator() instanceof ComparisonEq
					&& expr.getLeft() instanceof Constant
					&& ((Constant) expr.getLeft()).getValue() instanceof Boolean) {
				Boolean val = (Boolean) ((Constant) expr.getLeft()).getValue();
				if (val.booleanValue())
					return SATISFIED;
				else
					return NOT_SATISFIED;
			}

		return UNKNOWN;
	}

}
