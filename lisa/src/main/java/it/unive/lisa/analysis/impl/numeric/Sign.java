package it.unive.lisa.analysis.impl.numeric;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticDomain.Satisfiability;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.representation.StringRepresentation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryOperator;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.TernaryOperator;
import it.unive.lisa.symbolic.value.UnaryOperator;
import it.unive.lisa.symbolic.value.ValueExpression;

/**
 * The basic Sign abstract domain, tracking zero, strictly positive and strictly
 * negative integer values, implemented as a
 * {@link BaseNonRelationalValueDomain}, handling top and bottom values for the
 * expression evaluation and bottom values for the expression satisfiability.
 * Top and bottom cases for least upper bounds, widening and less or equals
 * operations are handled by {@link BaseLattice} in {@link BaseLattice#lub},
 * {@link BaseLattice#widening} and {@link BaseLattice#lessOrEqual} methods,
 * respectively.
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 */
public class Sign extends BaseNonRelationalValueDomain<Sign> {

	private static final Sign POS = new Sign((byte) 4);
	private static final Sign NEG = new Sign((byte) 3);
	private static final Sign ZERO = new Sign((byte) 2);
	private static final Sign TOP = new Sign((byte) 0);
	private static final Sign BOTTOM = new Sign((byte) 1);

	private final byte sign;

	/**
	 * Builds the sign abstract domain, representing the top of the sign
	 * abstract domain.
	 */
	public Sign() {
		this((byte) 0);
	}

	private Sign(byte sign) {
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
			return Lattice.BOTTOM_REPR;
		if (isTop())
			return Lattice.TOP_REPR;

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
	protected Sign evalNullConstant(ProgramPoint pp) {
		return top();
	}

	@Override
	protected Sign evalNonNullConstant(Constant constant, ProgramPoint pp) {
		if (constant.getValue() instanceof Integer) {
			Integer i = (Integer) constant.getValue();
			return i == 0 ? ZERO : i > 0 ? POS : NEG;
		}

		return top();
	}

	private boolean isPositive() {
		return this == POS;
	}

	private boolean isZero() {
		return this == ZERO;
	}

	private boolean isNegative() {
		return this == NEG;
	}

	private Sign opposite() {
		if (isTop() || isBottom())
			return this;
		return isPositive() ? NEG : isNegative() ? POS : ZERO;
	}

	@Override
	protected Sign evalUnaryExpression(UnaryOperator operator, Sign arg, ProgramPoint pp) {
		switch (operator) {
		case NUMERIC_NEG:
			if (arg.isPositive())
				return NEG;
			else if (arg.isNegative())
				return POS;
			else if (arg.isZero())
				return ZERO;
			else
				return TOP;
		default:
			return TOP;
		}
	}

	@Override
	protected Sign evalBinaryExpression(BinaryOperator operator, Sign left, Sign right, ProgramPoint pp) {
		switch (operator) {
		case NUMERIC_ADD:
			if (left.isZero())
				return right;
			else if (right.isZero())
				return left;
			else if (left.equals(right))
				return left;
			else
				return top();
		case NUMERIC_SUB:
			if (left.isZero())
				return right.opposite();
			else if (right.isZero())
				return left.opposite();
			else if (left.equals(right))
				return top();
			else
				return left;
		case NUMERIC_DIV:
			if (right.isZero())
				return bottom();
			else if (left.isZero())
				return ZERO;
			else if (left.equals(right))
				// top/top = top
				// +/+ = +
				// -/- = +
				return left.isTop() ? left : POS;
			return top();
		case NUMERIC_MOD:
			return top();
		case NUMERIC_MUL:
			if (left.isZero() || right.isZero())
				return ZERO;
			else if (left.equals(right))
				return POS;
			else
				return NEG;
		default:
			return TOP;
		}
	}

	@Override
	protected Sign lubAux(Sign other) throws SemanticException {
		return TOP;
	}

	@Override
	protected Sign wideningAux(Sign other) throws SemanticException {
		return lubAux(other);
	}

	@Override
	protected boolean lessOrEqualAux(Sign other) throws SemanticException {
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
	protected Satisfiability satisfiesBinaryExpression(BinaryOperator operator, Sign left, Sign right,
			ProgramPoint pp) {
		if (left.isTop() || right.isTop())
			return Satisfiability.UNKNOWN;

		switch (operator) {
		case COMPARISON_EQ:
			return left.eq(right);
		case COMPARISON_GE:
			return left.eq(right).or(left.gt(right));
		case COMPARISON_GT:
			return left.gt(right);
		case COMPARISON_LE:
			// e1 <= e2 same as !(e1 > e2)
			return left.gt(right).negate();
		case COMPARISON_LT:
			// e1 < e2 -> !(e1 >= e2) && !(e1 == e2)
			return left.gt(right).negate().and(left.eq(right).negate());
		case COMPARISON_NE:
			return left.eq(right).negate();
		default:
			return Satisfiability.UNKNOWN;
		}
	}

	private Satisfiability eq(Sign other) {
		if (!this.equals(other))
			return Satisfiability.NOT_SATISFIED;
		else if (isZero())
			return Satisfiability.SATISFIED;
		else
			return Satisfiability.UNKNOWN;
	}

	private Satisfiability gt(Sign other) {
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
	protected Satisfiability satisfiesTernaryExpression(TernaryOperator operator, Sign left, Sign middle, Sign right,
			ProgramPoint pp) {
		return Satisfiability.UNKNOWN;
	}

	@Override
	protected ValueEnvironment<Sign> assumeBinaryExpression(
			ValueEnvironment<Sign> environment, BinaryOperator operator, ValueExpression left,
			ValueExpression right, ProgramPoint pp) throws SemanticException {
		switch (operator) {
		case COMPARISON_EQ:
			if (left instanceof Identifier)
				environment = environment.assign((Identifier) left, right, pp);
			else if (right instanceof Identifier)
				environment = environment.assign((Identifier) right, left, pp);
			return environment;
		case COMPARISON_GE:
			if (left instanceof Identifier) {
				Sign rightSign = eval(right, environment, pp);
				if (rightSign.isPositive())
					environment = environment.assign((Identifier) left, right, pp);
			} else if (right instanceof Identifier) {
				Sign leftSign = eval(left, environment, pp);
				if (leftSign.isNegative())
					environment = environment.assign((Identifier) right, left, pp);
			}
			return environment;
		case COMPARISON_LE:
			if (left instanceof Identifier) {
				Sign rightSign = eval(right, environment, pp);
				if (rightSign.isNegative())
					environment = environment.assign((Identifier) left, right, pp);
			} else if (right instanceof Identifier) {
				Sign leftSign = eval(left, environment, pp);
				if (leftSign.isPositive())
					environment = environment.assign((Identifier) right, left, pp);
			}
			return environment;

		case COMPARISON_LT:
			if (left instanceof Identifier) {
				Sign rightSign = eval(right, environment, pp);
				if (rightSign.isNegative() || rightSign.isZero())
					// x < 0/-
					environment = environment.assign((Identifier) left,
							new Constant(right.getDynamicType(), -1, right.getCodeLocation()), pp);
			} else if (right instanceof Identifier) {
				Sign leftSign = eval(left, environment, pp);
				if (leftSign.isPositive() || leftSign.isZero())
					// 0/+ < x
					environment = environment.assign((Identifier) right,
							new Constant(left.getDynamicType(), 1, left.getCodeLocation()), pp);
			}
			return environment;
		case COMPARISON_GT:
			if (left instanceof Identifier) {
				Sign rightSign = eval(right, environment, pp);
				if (rightSign.isPositive() || rightSign.isZero())
					// x > +/0
					environment = environment.assign((Identifier) left,
							new Constant(right.getDynamicType(), 1, right.getCodeLocation()), pp);
			} else if (right instanceof Identifier) {
				Sign leftSign = eval(left, environment, pp);
				if (leftSign.isNegative() || leftSign.isZero())
					// -/0 > x
					environment = environment.assign((Identifier) right,
							new Constant(left.getDynamicType(), -1, right.getCodeLocation()), pp);
			}
			return environment;
		default:
			return environment;
		}
	}
}
