package it.unive.lisa.test.imp.tutorial;

import it.unive.lisa.analysis.SemanticDomain.Satisfiability;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.BaseNonRelationalValueDomain;
import it.unive.lisa.cfg.type.Type;
import it.unive.lisa.symbolic.value.BinaryOperator;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.TernaryOperator;
import it.unive.lisa.symbolic.value.UnaryOperator;

public class Sign extends BaseNonRelationalValueDomain<Sign> {

	private enum Values {
		POS,
		NEG,
		ZERO,
		TOP,
		BOT
	}

	private final Values sign;

	private Sign(Values sign) {
		this.sign = sign;
	}

	public Sign() {
		this(Values.TOP);
	}

	@Override
	public Sign top() {
		return new Sign(Values.TOP);
	}

	@Override
	public boolean isTop() {
		return getSign() == Values.TOP;
	}

	@Override
	public boolean isBottom() {
		return getSign() == Values.BOT;
	}

	@Override
	public Sign bottom() {
		return new Sign(Values.BOT);
	}

	@Override
	public String representation() {
		switch (sign) {
		case BOT:
			return "Bottom";
		case NEG:
			return "-";
		case POS:
			return "+";
		case ZERO:
			return "0";
		default:
			return "Unknown sign";
		}
	}

	public Values getSign() {
		return sign;
	}

	@Override
	protected Sign evalNullConstant() {
		return bottom();
	}

	@Override
	protected Sign evalNonNullConstant(Constant constant) {
		if (constant.getValue() instanceof Integer) {
			Integer i = (Integer) constant.getValue();
			return i == 0 ? zero() : i > 0 ? pos() : neg();
		}

		return top();
	}

	private Sign pos() {
		return new Sign(Values.POS);
	}

	private Sign neg() {
		return new Sign(Values.NEG);
	}

	private Sign zero() {
		return new Sign(Values.ZERO);
	}

	private boolean isPositive() {
		return sign == Values.POS;
	}

	private boolean isZero() {
		return sign == Values.ZERO;
	}

	private boolean isNegative() {
		return sign == Values.NEG;
	}

	private Sign opposite() {
		if (isTop() || isBottom())
			return this;
		return isPositive() ? neg() : isNegative() ? pos() : zero();
	}

	@Override
	protected Sign evalTypeConversion(Type type, Sign arg) {
		return top();
	}

	@Override
	protected Sign evalUnaryExpression(UnaryOperator operator, Sign arg) {
		switch (operator) {
		case NUMERIC_NEG:
			if (arg.isPositive())
				return neg();
			else if (arg.isNegative())
				return pos();
			else if (arg.isZero())
				return zero();
			else
				return top();
		default:
			return top();
		}
	}

	@Override
	protected Sign evalBinaryExpression(BinaryOperator operator, Sign left, Sign right) {
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
				return zero();
			else if (left.equals(right))
				return top();
		case NUMERIC_MOD:
			return top();
		case NUMERIC_MUL:
			if (left.isZero() || right.isZero())
				return zero();
			else if (left.equals(right))
				return pos();
			else
				return neg();
		default:
			return top();
		}
	}

	@Override
	protected Sign evalTernaryExpression(TernaryOperator operator, Sign left, Sign middle, Sign right) {
		return top();
	}

	@Override
	protected Sign lubAux(Sign other) throws SemanticException {
		return equals(other) ? other : top();
	}

	@Override
	protected Sign wideningAux(Sign other) throws SemanticException {
		return lubAux(other);
	}

	@Override
	protected boolean lessOrEqualAux(Sign other) throws SemanticException {
		return equals(other);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((sign == null) ? 0 : sign.hashCode());
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
	protected Satisfiability satisfiesIdentifier(Identifier identifier) {
		return Satisfiability.UNKNOWN;
	}

	@Override
	protected Satisfiability satisfiesNullConstant() {
		return Satisfiability.UNKNOWN;
	}

	@Override
	protected Satisfiability satisfiesNonNullConstant(Constant constant) {
		return Satisfiability.UNKNOWN;
	}

	@Override
	protected Satisfiability satisfiesTypeConversion(Type type, Sign right) {
		return Satisfiability.UNKNOWN;
	}

	@Override
	protected Satisfiability satisfiesUnaryExpression(UnaryOperator operator, Sign arg) {
		return Satisfiability.UNKNOWN;
	}

	@Override
	protected Satisfiability satisfiesBinaryExpression(BinaryOperator operator, Sign left, Sign right) {
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
	protected Satisfiability satisfiesTernaryExpression(TernaryOperator operator, Sign left, Sign middle, Sign right) {
		return Satisfiability.UNKNOWN;
	}
}