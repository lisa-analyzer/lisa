package it.unive.lisa.analysis.impl.numeric;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticDomain.Satisfiability;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryOperator;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.TernaryOperator;
import it.unive.lisa.symbolic.value.UnaryOperator;

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

	private static final Sign POS = new Sign(false, false);
	private static final Sign NEG = new Sign(false, false);
	private static final Sign ZERO = new Sign(false, false);
	private static final Sign TOP = new Sign();
	private static final Sign BOTTOM = new Sign(false, true);

	private final boolean isTop, isBottom;

	/**
	 * Builds the sign abstract domain, representing the top of the sign
	 * abstract domain.
	 */
	public Sign() {
		this(true, false);
	}

	private Sign(boolean isTop, boolean isBottom) {
		this.isTop = isTop;
		this.isBottom = isBottom;
	}

	@Override
	public boolean isTop() {
		return isTop;
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
	public String representation() {
		if (equals(BOTTOM))
			return Lattice.BOTTOM_STRING;
		else if (equals(ZERO))
			return "0";
		else if (equals(POS))
			return "+";
		else if (equals(NEG))
			return "-";
		else
			return Lattice.TOP_STRING;
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
	protected Sign evalTernaryExpression(TernaryOperator operator, Sign left, Sign middle, Sign right,
			ProgramPoint pp) {
		return top();
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
		if (this == ZERO)
			return 0;
		else if (this == POS)
			return 1;
		else if (this == NEG)
			return 2;
		else if (this == BOTTOM)
			return 3;
		else
			return 4;
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
		if (isBottom != other.isBottom)
			return false;
		if (isTop != other.isTop)
			return false;
		return isTop && other.isTop;
	}

	@Override
	protected Satisfiability satisfiesAbstractValue(Sign value, ProgramPoint pp) {
		return Satisfiability.UNKNOWN;
	}

	@Override
	protected Satisfiability satisfiesNullConstant(ProgramPoint pp) {
		return Satisfiability.UNKNOWN;
	}

	@Override
	protected Satisfiability satisfiesNonNullConstant(Constant constant, ProgramPoint pp) {
		return Satisfiability.UNKNOWN;
	}

	@Override
	protected Satisfiability satisfiesUnaryExpression(UnaryOperator operator, Sign arg, ProgramPoint pp) {
		return Satisfiability.UNKNOWN;
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
		case COMPARISON_LE: // e1 <= e2 same as !(e1 > e2)
			return left.gt(right).negate();
		case COMPARISON_LT: // e1 < e2 -> !(e1 >= e2) && !(e1 == e2)
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
}