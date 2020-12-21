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

public class ConstantPropagation extends BaseNonRelationalValueDomain<ConstantPropagation> {

	public static final ConstantPropagation TOP = new ConstantPropagation(true, false);

	private static final ConstantPropagation BOTTOM = new ConstantPropagation(false, true);

	private final boolean isTop, isBottom;

	private final Integer value;

	public ConstantPropagation() {
		this(null, true, false);
	}

	private ConstantPropagation(Integer value, boolean isTop, boolean isBottom) {
		this.value = value;
		this.isTop = isTop;
		this.isBottom = isBottom;
	}

	private ConstantPropagation(Integer value) {
		this(value, false, false);
	}

	private ConstantPropagation(boolean isTop, boolean isBottom) {
		this(null, isTop, isBottom);
	}

	@Override
	public ConstantPropagation top() {
		return TOP;
	}

	@Override
	public boolean isTop() {
		return isTop;
	}

	@Override
	public ConstantPropagation bottom() {
		return BOTTOM;
	}

	@Override
	public boolean isBottom() {
		return isBottom;
	}

	@Override
	public String representation() {
		return isTop() ? "Non-constant" : isBottom() ? "Bot" : value.toString();
	}

	@Override
	protected ConstantPropagation evalNullConstant() {
		return top();
	}

	@Override
	protected ConstantPropagation evalNonNullConstant(Constant constant) {
		if (constant.getValue() instanceof Integer)
			return new ConstantPropagation((Integer) constant.getValue());
		return top();
	}

	@Override
	protected ConstantPropagation evalTypeConversion(Type type, ConstantPropagation arg) {
		return top();
	}

	@Override
	protected ConstantPropagation evalUnaryExpression(UnaryOperator operator, ConstantPropagation arg) {
		switch (operator) {
		case NUMERIC_NEG:
			return new ConstantPropagation(0 - value);
		case STRING_LENGTH:
			return top();
		default:
			return top();
		}
	}

	@Override
	protected ConstantPropagation evalBinaryExpression(BinaryOperator operator, ConstantPropagation left,
			ConstantPropagation right) {
		switch (operator) {
		case NUMERIC_ADD:
			return new ConstantPropagation(left.value + right.value);
		case NUMERIC_DIV:
			if (left.value % right.value != 0)
				return top();
			else
				return new ConstantPropagation(left.value / right.value);
		case NUMERIC_MOD:
			return new ConstantPropagation(left.value % right.value);
		case NUMERIC_MUL:
			return new ConstantPropagation(left.value * right.value);
		case NUMERIC_SUB:
			return new ConstantPropagation(left.value - right.value);
		default:
			return top();
		}
	}

	@Override
	protected ConstantPropagation evalTernaryExpression(TernaryOperator operator, ConstantPropagation left,
			ConstantPropagation middle, ConstantPropagation right) {
		return top();
	}

	@Override
	protected ConstantPropagation lubAux(ConstantPropagation other) throws SemanticException {
		return equals(other) ? other : top();
	}

	@Override
	protected ConstantPropagation wideningAux(ConstantPropagation other) throws SemanticException {
		return lubAux(other);
	}

	@Override
	protected boolean lessOrEqualAux(ConstantPropagation other) throws SemanticException {
		return value == other.value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (isBottom ? 1231 : 1237);
		result = prime * result + (isTop ? 1231 : 1237);
		result = prime * result + ((value == null) ? 0 : value.hashCode());
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
		ConstantPropagation other = (ConstantPropagation) obj;
		if (isBottom != other.isBottom)
			return false;
		if (isTop != other.isTop)
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
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
	protected Satisfiability satisfiesTypeConversion(Type type, ConstantPropagation right) {
		return Satisfiability.UNKNOWN;
	}

	@Override
	protected Satisfiability satisfiesUnaryExpression(UnaryOperator operator, ConstantPropagation arg) {
		return Satisfiability.UNKNOWN;
	}

	@Override
	protected Satisfiability satisfiesBinaryExpression(BinaryOperator operator, ConstantPropagation left,
			ConstantPropagation right) {

		if (left.isTop() || right.isTop())
			return Satisfiability.UNKNOWN;

		switch (operator) {
		case COMPARISON_EQ:
			return left.value == right.value ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
		case COMPARISON_GE:
			return left.value >= right.value ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
		case COMPARISON_GT:
			return left.value > right.value ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
		case COMPARISON_LE:
			return left.value <= right.value ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
		case COMPARISON_LT:
			return left.value < right.value ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
		case COMPARISON_NE:
			return left.value != right.value ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
		default:
			return Satisfiability.UNKNOWN;
		}
	}

	@Override
	protected Satisfiability satisfiesTernaryExpression(TernaryOperator operator, ConstantPropagation left,
			ConstantPropagation middle, ConstantPropagation right) {
		return Satisfiability.UNKNOWN;
	}
}
