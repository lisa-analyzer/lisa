package it.unive.lisa.test.imp.tutorial.intervals;

import it.unive.lisa.analysis.SemanticDomain.Satisfiability;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.BaseNonRelationalValueDomain;
import it.unive.lisa.cfg.type.Type;
import it.unive.lisa.symbolic.value.BinaryOperator;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.TernaryOperator;
import it.unive.lisa.symbolic.value.UnaryOperator;

public class Interval extends BaseNonRelationalValueDomain<Interval> {

	private final Intv val;

	private Interval(Intv val) {
		this.val = val;
	}

	public Interval() {
		this(Intv.mkTop());
	}

	@Override
	public Interval top() {
		return new Interval(Intv.mkTop());
	}

	@Override
	public boolean isTop() {
		return val.isTop();
	}

	@Override
	public boolean isBottom() {
		return val.isBottom();
	}

	@Override
	public Interval bottom() {
		return new Interval(Intv.mkBottom());
	}

	@Override
	public String representation() {
		return val.toString();
	}

	public Intv getInterval() {
		return val;
	}

	@Override
	protected Interval evalNullConstant() {
		return top();
	}

	@Override
	protected Interval evalNonNullConstant(Constant constant) {
		if (constant.getValue() instanceof Integer)
			return new Interval(new Intv((Integer) constant.getValue(), (Integer) constant.getValue()));

		return top();
	}

	@Override
	protected Interval evalTypeConversion(Type type, Interval arg) {
		return top();
	}

	@Override
	protected Interval evalUnaryExpression(UnaryOperator operator, Interval arg) {

		switch (operator) {
		case NUMERIC_NEG:
			return new Interval(new Intv(0, null).mul(new Intv(-1, -1)));
		case STRING_LENGTH:
			return new Interval(new Intv(0, null));
		default:
			return top();
		}
	}

	@Override
	protected Interval evalBinaryExpression(BinaryOperator operator, Interval left, Interval right) {
		if (left.isBottom() || right.isBottom())
			return bottom();

		switch (operator) {
		case NUMERIC_ADD:
			return new Interval(left.getInterval().plus(right.getInterval()));
		case NUMERIC_SUB:
			return new Interval(left.getInterval().diff(right.getInterval()));
		case NUMERIC_MUL:
			return new Interval(left.getInterval().mul(right.getInterval()));
		case NUMERIC_DIV:
			return new Interval(left.getInterval().div(right.getInterval()));
		case NUMERIC_MOD:
			return top();
		default:
			return top();
		}
	}

	@Override
	protected Interval evalTernaryExpression(TernaryOperator operator, Interval left, Interval middle, Interval right) {
		return top();
	}

	@Override
	protected Interval lubAux(Interval other) throws SemanticException {
		return new Interval(getInterval().lub(other.getInterval()));
	}

	@Override
	protected Interval wideningAux(Interval other) throws SemanticException {
		return new Interval(getInterval().widening(other.getInterval()));
	}

	@Override
	protected boolean lessOrEqualAux(Interval other) throws SemanticException {
		return getInterval().lessOrEqual(other.getInterval());
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
	protected Satisfiability satisfiesTypeConversion(Type type, Interval right) {
		return Satisfiability.UNKNOWN;
	}

	@Override
	protected Satisfiability satisfiesUnaryExpression(UnaryOperator operator, Interval arg) {
		return Satisfiability.UNKNOWN;
	}

	@Override
	protected Satisfiability satisfiesBinaryExpression(BinaryOperator operator, Interval left, Interval right) {
		return Satisfiability.UNKNOWN;
	}

	@Override
	protected Satisfiability satisfiesTernaryExpression(TernaryOperator operator, Interval left, Interval middle,
			Interval right) {
		return Satisfiability.UNKNOWN;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((val == null) ? 0 : val.hashCode());
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
		Interval other = (Interval) obj;
		if (val == null) {
			if (other.val != null)
				return false;
		} else if (!val.equals(other.val))
			return false;
		return true;
	}
}