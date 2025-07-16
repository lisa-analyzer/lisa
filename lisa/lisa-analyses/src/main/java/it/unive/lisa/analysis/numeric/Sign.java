package it.unive.lisa.analysis.numeric;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.numeric.Sign.SignLattice;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.DivisionOperator;
import it.unive.lisa.symbolic.value.operator.ModuloOperator;
import it.unive.lisa.symbolic.value.operator.MultiplicationOperator;
import it.unive.lisa.symbolic.value.operator.RemainderOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

/**
 * The basic overflow-insensitive Sign abstract domain, tracking zero, strictly
 * positive and strictly negative integer values, implemented as a
 * {@link BaseNonRelationalValueDomain}.
 * 
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
public class Sign
		implements
		BaseNonRelationalValueDomain<SignLattice> {

	/**
	 * The lattice structure the values, which can be positive, negative, zero,
	 * top or bottom.
	 * 
	 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
	 */
	public static class SignLattice
			implements
			BaseLattice<SignLattice> {

		/**
		 * The abstract positive element.
		 */
		public static final SignLattice POS = new SignLattice((byte) 4);

		/**
		 * The abstract negative element.
		 */
		public static final SignLattice NEG = new SignLattice((byte) 3);

		/**
		 * The abstract zero element.
		 */
		public static final SignLattice ZERO = new SignLattice((byte) 2);

		/**
		 * The abstract top element.
		 */
		public static final SignLattice TOP = new SignLattice((byte) 0);

		/**
		 * The abstract bottom element.
		 */
		public static final SignLattice BOTTOM = new SignLattice((byte) 1);

		private final byte sign;

		/**
		 * Builds the sign abstract domain, representing the top of the sign
		 * abstract domain.
		 */
		public SignLattice() {
			this((byte) 0);
		}

		/**
		 * Builds the sign instance for the given sign value.
		 * 
		 * @param sign the sign (0 = top, 1 = bottom, 2 = zero, 3 = negative, 4
		 *                 = positive)
		 */
		private SignLattice(
				byte sign) {
			this.sign = sign;
		}

		@Override
		public SignLattice top() {
			return TOP;
		}

		@Override
		public SignLattice bottom() {
			return BOTTOM;
		}

		@Override
		public String toString() {
			return representation().toString();
		}

		@Override
		public StructuredRepresentation representation() {
			if (isBottom())
				return Lattice.bottomRepresentation();
			if (isTop())
				return Lattice.topRepresentation();

			String repr;
			if (this == ZERO)
				repr = "0";
			else if (this == POS)
				repr = "+";
			else
				repr = "-";

			return new StringRepresentation(repr);
		}

		/**
		 * Yields whether or not this is the positive sign.
		 * 
		 * @return {@code true} if that condition holds
		 */
		public boolean isPositive() {
			return this == POS;
		}

		/**
		 * Yields whether or not this is the zero sign.
		 * 
		 * @return {@code true} if that condition holds
		 */
		public boolean isZero() {
			return this == ZERO;
		}

		/**
		 * Yields whether or not this is the negative sign.
		 * 
		 * @return {@code true} if that condition holds
		 */
		public boolean isNegative() {
			return this == NEG;
		}

		/**
		 * Yields the sign opposite to this one. Top and bottom elements do not
		 * change.
		 * 
		 * @return the opposite sign
		 */
		public SignLattice opposite() {
			if (isTop() || isBottom())
				return this;
			return isPositive() ? NEG : isNegative() ? POS : ZERO;
		}

		@Override
		public SignLattice lubAux(
				SignLattice other)
				throws SemanticException {
			return SignLattice.TOP;
		}

		@Override
		public boolean lessOrEqualAux(
				SignLattice other)
				throws SemanticException {
			return false;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + sign;
			return result;
		}

		@Override
		public boolean equals(
				Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SignLattice other = (SignLattice) obj;
			if (sign != other.sign)
				return false;
			return true;
		}

		/**
		 * Tests if this instance is equal to the given one, returning a
		 * {@link Satisfiability} element.
		 * 
		 * @param other the instance
		 * 
		 * @return the satisfiability of {@code this = other}
		 */
		public Satisfiability eq(
				SignLattice other) {
			if (this.isBottom() || other.isBottom())
				return Satisfiability.BOTTOM;
			else if (this.isTop() || other.isTop())
				return Satisfiability.UNKNOWN;
			else if (!this.equals(other))
				return Satisfiability.NOT_SATISFIED;
			else if (isZero())
				return Satisfiability.SATISFIED;
			else
				return Satisfiability.UNKNOWN;
		}

		/**
		 * Tests if this instance is greater than the given one, returning a
		 * {@link Satisfiability} element.
		 * 
		 * @param other the instance
		 * 
		 * @return the satisfiability of {@code this > other}
		 */
		public Satisfiability gt(
				SignLattice other) {
			if (this.isBottom() || other.isBottom())
				return Satisfiability.BOTTOM;
			else if (this.isTop() || other.isTop())
				return Satisfiability.UNKNOWN;
			else if (this.isNegative())
				return other.isNegative() ? Satisfiability.UNKNOWN : Satisfiability.NOT_SATISFIED;
			else if (this.isZero())
				return other.isNegative() ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
			else
				return other.isPositive() ? Satisfiability.UNKNOWN : Satisfiability.SATISFIED;
		}

	}

	@Override
	public SignLattice evalNullConstant(
			ProgramPoint pp,
			SemanticOracle oracle) {
		return top();
	}

	@Override
	public SignLattice evalNonNullConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (constant.getValue() instanceof Integer) {
			Integer i = (Integer) constant.getValue();
			return i == 0 ? SignLattice.ZERO : i > 0 ? SignLattice.POS : SignLattice.NEG;
		}

		return SignLattice.TOP;
	}

	@Override
	public SignLattice evalUnaryExpression(
			UnaryExpression expression,
			SignLattice arg,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (expression.getOperator() == NumericNegation.INSTANCE)
			if (arg.isPositive())
				return SignLattice.NEG;
			else if (arg.isNegative())
				return SignLattice.POS;
			else if (arg.isZero())
				return SignLattice.ZERO;
			else
				return SignLattice.TOP;
		return SignLattice.TOP;
	}

	@Override
	public SignLattice evalBinaryExpression(
			BinaryExpression expression,
			SignLattice left,
			SignLattice right,
			ProgramPoint pp,
			SemanticOracle oracle) {
		BinaryOperator operator = expression.getOperator();
		if (operator instanceof AdditionOperator)
			if (left.isZero())
				return right;
			else if (right.isZero())
				return left;
			else if (left.equals(right))
				return left;
			else
				return top();
		else if (operator instanceof SubtractionOperator)
			if (left.isZero())
				return right.opposite();
			else if (right.isZero())
				return left;
			else if (left.equals(right))
				return top();
			else
				return left;
		else if (operator instanceof DivisionOperator)
			if (right.isZero())
				return bottom();
			else if (left.isZero())
				return SignLattice.ZERO;
			else if (left.equals(right))
				// top/top = top
				// +/+ = +
				// -/- = +
				return left.isTop() ? left : SignLattice.POS;
			else if (!left.isTop() && left.equals(right.opposite()))
				// +/- = -
				// -/+ = -
				return SignLattice.NEG;
			else
				return top();
		else if (operator instanceof ModuloOperator)
			return right;
		else if (operator instanceof RemainderOperator)
			return left;
		else if (operator instanceof MultiplicationOperator)
			if (left.isZero() || right.isZero())
				return SignLattice.ZERO;
			else if (left.equals(right))
				return SignLattice.POS;
			else
				return SignLattice.NEG;
		else
			return SignLattice.TOP;
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(
			BinaryExpression expression,
			SignLattice left,
			SignLattice right,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (left.isTop() || right.isTop())
			return Satisfiability.UNKNOWN;

		BinaryOperator operator = expression.getOperator();
		if (operator == ComparisonEq.INSTANCE)
			return left.eq(right);
		else if (operator == ComparisonGe.INSTANCE)
			return left.eq(right).or(left.gt(right));
		else if (operator == ComparisonGt.INSTANCE)
			return left.gt(right);
		else if (operator == ComparisonLe.INSTANCE)
			// e1 <= e2 same as !(e1 > e2)
			return left.gt(right).negate();
		else if (operator == ComparisonLt.INSTANCE)
			// e1 < e2 -> !(e1 >= e2) && !(e1 == e2)
			return left.gt(right).negate().and(left.eq(right).negate());
		else if (operator == ComparisonNe.INSTANCE)
			return left.eq(right).negate();
		else
			return Satisfiability.UNKNOWN;
	}

	@Override
	public ValueEnvironment<SignLattice> assumeBinaryExpression(
			ValueEnvironment<SignLattice> environment,
			BinaryExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		Satisfiability sat = satisfies(environment, expression, src, oracle);
		if (sat == Satisfiability.NOT_SATISFIED)
			return environment.bottom();
		if (sat == Satisfiability.SATISFIED)
			return environment;

		Identifier id;
		SignLattice eval;
		boolean rightIsExpr;
		BinaryOperator operator = expression.getOperator();
		ValueExpression left = (ValueExpression) expression.getLeft();
		ValueExpression right = (ValueExpression) expression.getRight();
		if (left instanceof Identifier) {
			eval = eval(environment, right, src, oracle);
			id = (Identifier) left;
			rightIsExpr = true;
		} else if (right instanceof Identifier) {
			eval = eval(environment, left, src, oracle);
			id = (Identifier) right;
			rightIsExpr = false;
		} else
			return environment;

		SignLattice starting = environment.getState(id);
		if (eval.isBottom() || starting.isBottom())
			return environment.bottom();

		SignLattice update = null;
		if (operator == ComparisonEq.INSTANCE)
			update = starting.glb(eval);
		else {
			// the rule for an operator op is:
			// - if `start op eval`, `update = U { start n v | v op eval, v in {
			// +, 0, -} }`
			// - if `eval op start`, `update = U { start n v | eval op v, v in {
			// +, 0, -} }`

			SignLattice[] all = new SignLattice[] {
					SignLattice.NEG, SignLattice.ZERO, SignLattice.POS
			};
			if (operator == ComparisonGe.INSTANCE)
				if (rightIsExpr) {
					for (SignLattice s : all)
						if (s.gt(eval).or(s.eq(eval)).mightBeTrue())
							update = update == null ? starting.glb(s) : update.lub(starting.glb(s));
				} else {
					for (SignLattice s : all)
						if (eval.gt(s).or(eval.eq(s)).mightBeTrue())
							update = update == null ? starting.glb(s) : update.lub(starting.glb(s));
				}
			else if (operator == ComparisonLe.INSTANCE)
				if (rightIsExpr) {
					for (SignLattice s : all)
						// we invert <= to > and look at the failing ones
						if (s.gt(eval).mightBeFalse())
							update = update == null ? starting.glb(s) : update.lub(starting.glb(s));
				} else {
					for (SignLattice s : all)
						// we invert <= to > and look at the failing ones
						if (eval.gt(s).mightBeFalse())
							update = update == null ? starting.glb(s) : update.lub(starting.glb(s));
				}
			else if (operator == ComparisonLt.INSTANCE)
				if (rightIsExpr) {
					for (SignLattice s : all)
						// we invert < to >= and look at the failing ones
						if (s.gt(eval).or(s.eq(eval)).mightBeFalse())
							update = update == null ? starting.glb(s) : update.lub(starting.glb(s));
				} else {
					for (SignLattice s : all)
						// we invert < to >= and look at the failing ones
						if (eval.gt(s).or(eval.eq(s)).mightBeFalse())
							update = update == null ? starting.glb(s) : update.lub(starting.glb(s));
				}
			else if (operator == ComparisonGt.INSTANCE)
				if (rightIsExpr) {
					for (SignLattice s : all)
						if (s.gt(eval).mightBeTrue())
							update = update == null ? starting.glb(s) : update.lub(starting.glb(s));
				} else {
					for (SignLattice s : all)
						if (eval.gt(s).mightBeTrue())
							update = update == null ? starting.glb(s) : update.lub(starting.glb(s));
				}
		}

		if (update == null)
			return environment;
		else if (update.isBottom())
			return environment.bottom();
		else
			return environment.putState(id, update);
	}

	@Override
	public SignLattice top() {
		return SignLattice.TOP;
	}

	@Override
	public SignLattice bottom() {
		return SignLattice.BOTTOM;
	}

}
