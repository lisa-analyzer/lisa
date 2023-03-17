package it.unive.lisa.analysis.numeric;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticDomain.Satisfiability;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.representation.StringRepresentation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
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
import it.unive.lisa.symbolic.value.operator.ternary.TernaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;

/**
 * The basic overflow-insensitive Sign abstract domain, tracking zero, strictly
 * positive and strictly negative integer values, implemented as a
 * {@link BaseNonRelationalValueDomain}, handling top and bottom values for the
 * expression evaluation and bottom values for the expression satisfiability.
 * Top and bottom cases for least upper bounds, widening and less or equals
 * operations are handled by {@link BaseLattice} in {@link BaseLattice#lub},
 * {@link BaseLattice#widening} and {@link BaseLattice#lessOrEqual} methods,
 * respectively.
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 */
public class Sign implements BaseNonRelationalValueDomain<Sign> {

	/**
	 * The abstract positive element.
	 */
	public static final Sign POS = new Sign((byte) 4);

	/**
	 * The abstract negative element.
	 */
	public static final Sign NEG = new Sign((byte) 3);

	/**
	 * The abstract zero element.
	 */
	public static final Sign ZERO = new Sign((byte) 2);

	/**
	 * The abstract top element.
	 */
	public static final Sign TOP = new Sign((byte) 0);

	/**
	 * The abstract bottom element.
	 */
	public static final Sign BOTTOM = new Sign((byte) 1);

	private final byte sign;

	/**
	 * Builds the sign abstract domain, representing the top of the sign
	 * abstract domain.
	 */
	public Sign() {
		this((byte) 0);
	}

	/**
	 * Builds the sign instance for the given sign value.
	 * 
	 * @param sign the sign (0 = top, 1 = bottom, 2 = zero, 3 = negative, 4 =
	 *                 positive)
	 */
	public Sign(byte sign) {
		this.sign = sign;
	}

	@Override
	public Sign top() {
		return TOP;
	}

	@Override
	public Sign bottom() {
		return BOTTOM;
	}

	@Override
	public DomainRepresentation representation() {
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

	@Override
	public Sign evalNullConstant(ProgramPoint pp) {
		return top();
	}

	@Override
	public Sign evalNonNullConstant(Constant constant, ProgramPoint pp) {
		if (constant.getValue() instanceof Integer) {
			Integer i = (Integer) constant.getValue();
			return i == 0 ? ZERO : i > 0 ? POS : NEG;
		}

		return top();
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
	public Sign opposite() {
		if (isTop() || isBottom())
			return this;
		return isPositive() ? NEG : isNegative() ? POS : ZERO;
	}

	@Override
	public Sign evalUnaryExpression(UnaryOperator operator, Sign arg, ProgramPoint pp) {
		if (operator == NumericNegation.INSTANCE)
			if (arg.isPositive())
				return NEG;
			else if (arg.isNegative())
				return POS;
			else if (arg.isZero())
				return ZERO;
			else
				return TOP;
		return TOP;
	}

	@Override
	public Sign evalBinaryExpression(BinaryOperator operator, Sign left, Sign right, ProgramPoint pp) {
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
				return left.opposite();
			else if (left.equals(right))
				return top();
			else
				return left;
		else if (operator instanceof DivisionOperator)
			if (right.isZero())
				return bottom();
			else if (left.isZero())
				return ZERO;
			else if (left.equals(right))
				// top/top = top
				// +/+ = +
				// -/- = +
				return left.isTop() ? left : POS;
			else
				return top();
		else if (operator instanceof ModuloOperator)
			return right;
		else if (operator instanceof RemainderOperator)
			return left;
		else if (operator instanceof MultiplicationOperator)
			if (left.isZero() || right.isZero())
				return ZERO;
			else if (left.equals(right))
				return POS;
			else
				return NEG;
		else
			return TOP;
	}

	@Override
	public Sign lubAux(Sign other) throws SemanticException {
		return TOP;
	}

	@Override
	public boolean lessOrEqualAux(Sign other) throws SemanticException {
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
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Sign other = (Sign) obj;
		if (sign != other.sign)
			return false;
		return true;
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(BinaryOperator operator, Sign left, Sign right,
			ProgramPoint pp) {
		if (left.isTop() || right.isTop())
			return Satisfiability.UNKNOWN;

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
		else if (operator == ComparisonLe.INSTANCE)
			return left.eq(right).negate();
		else
			return Satisfiability.UNKNOWN;
	}

	/**
	 * Tests if this instance is equal to the given one, returning a
	 * {@link Satisfiability} element.
	 * 
	 * @param other the instance
	 * 
	 * @return the satisfiability of {@code this = other}
	 */
	public Satisfiability eq(Sign other) {
		if (!this.equals(other))
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
	public Satisfiability gt(Sign other) {
		if (this.equals(other))
			return this.isZero() ? Satisfiability.NOT_SATISFIED : Satisfiability.UNKNOWN;
		else if (this.isZero())
			return other.isPositive() ? Satisfiability.NOT_SATISFIED : Satisfiability.SATISFIED;
		else if (this.isPositive())
			return Satisfiability.SATISFIED;
		else
			return Satisfiability.NOT_SATISFIED;
	}

	@Override
	public Satisfiability satisfiesTernaryExpression(TernaryOperator operator, Sign left, Sign middle, Sign right,
			ProgramPoint pp) {
		return Satisfiability.UNKNOWN;
	}

	@Override
	public ValueEnvironment<Sign> assumeBinaryExpression(
			ValueEnvironment<Sign> environment,
			BinaryOperator operator,
			ValueExpression left,
			ValueExpression right,
			ProgramPoint src,
			ProgramPoint dest)
			throws SemanticException {
		Identifier id;
		Sign eval;
		boolean rightIsExpr;
		if (left instanceof Identifier) {
			eval = eval(right, environment, src);
			id = (Identifier) left;
			rightIsExpr = true;
		} else if (right instanceof Identifier) {
			eval = eval(left, environment, src);
			id = (Identifier) right;
			rightIsExpr = false;
		} else
			return environment;

		Sign starting = environment.getState(id);
		if (eval.isBottom() || starting.isBottom())
			return environment.bottom();

		Sign update = null;
		if (operator == ComparisonEq.INSTANCE)
			update = eval;
		else if (operator == ComparisonGe.INSTANCE) {
			if (rightIsExpr && eval.isPositive())
				update = eval;
			else if (!rightIsExpr && eval.isNegative())
				update = eval;
		} else if (operator == ComparisonLe.INSTANCE) {
			if (rightIsExpr && eval.isNegative())
				update = eval;
			else if (!rightIsExpr && eval.isPositive())
				update = eval;
		} else if (operator == ComparisonLt.INSTANCE) {
			if (rightIsExpr && (eval.isNegative() || eval.isZero()))
				// x < 0/-
				update = NEG;
			else if (!rightIsExpr && (eval.isPositive() || eval.isZero()))
				// 0/+ < x
				update = POS;
		} else if (operator == ComparisonGt.INSTANCE) {
			if (rightIsExpr && (eval.isPositive() || eval.isZero()))
				// x > +/0
				update = POS;
			else if (!rightIsExpr && (eval.isNegative() || eval.isZero()))
				// -/0 > x
				update = NEG;
		}

		if (update == null)
			return environment;
		else if (update.isBottom())
			return environment.bottom();
		else
			return environment.putState(id, update);
	}
}
