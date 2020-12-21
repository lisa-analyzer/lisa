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

public class Parity extends BaseNonRelationalValueDomain<Parity> {

	private enum Values {
		ODD,
		EVEN,
		TOP,
		BOT
	}

	private final Values parity;

	private Parity(Values parity) {
		this.parity = parity;
	}

	public Parity() {
		this(Values.TOP);
	}

	@Override
	public Parity top() {
		return new Parity(Values.TOP);
	}

	@Override
	public boolean isTop() {
		return getParity() == Values.TOP;
	}

	@Override
	public boolean isBottom() {
		return getParity() == Values.BOT;
	}

	@Override
	public Parity bottom() {
		return new Parity(Values.BOT);
	}

	@Override
	public String representation() {
		switch (parity) {
		case BOT:
			return "BOT";
		case ODD:
			return "Odd";
		case EVEN:
			return "Even";
		default:
			return "TOP";
		}
	}

	public Values getParity() {
		return parity;
	}

	@Override
	protected Parity evalNullConstant() {
		return top();
	}

	@Override
	protected Parity evalNonNullConstant(Constant constant) {
		if (constant.getValue() instanceof Integer) {
			Integer i = (Integer) constant.getValue();
			return i % 2 == 0 ? even() : odd();
		}

		return top();
	}

	private Parity odd() {
		return new Parity(Values.ODD);
	}

	private Parity even() {
		return new Parity(Values.EVEN);
	}

	private boolean isEven() {
		return parity == Values.EVEN;
	}

	private boolean isOdd() {
		return parity == Values.ODD;
	}

	@Override
	protected Parity evalTypeConversion(Type type, Parity arg) {
		return top();
	}

	@Override
	protected Parity evalUnaryExpression(UnaryOperator operator, Parity arg) {
		switch (operator) {
		case NUMERIC_NEG:
			return arg;
		default:
			return top();
		}
	}

	@Override
	protected Parity evalBinaryExpression(BinaryOperator operator, Parity left, Parity right) {

		switch (operator) {
		case NUMERIC_ADD:
		case NUMERIC_SUB:
			if (right.equals(left))
				return even();
			else
				return odd();
		case NUMERIC_MUL:
			if (left.isEven() || right.isEven())
				return even();
			else
				return odd();
		case NUMERIC_DIV:
			if (left.isOdd())
				return right.isOdd() ? odd() : even();
			else
				return right.isOdd() ? even() : top();
		case NUMERIC_MOD:
			return top();
		default:
			return top();
		}
	}

	@Override
	protected Parity evalTernaryExpression(TernaryOperator operator, Parity left, Parity middle, Parity right) {
		return top();
	}

	@Override
	protected Parity lubAux(Parity other) throws SemanticException {
		return equals(other) ? other : top();
	}

	@Override
	protected Parity wideningAux(Parity other) throws SemanticException {
		return lubAux(other);
	}

	@Override
	protected boolean lessOrEqualAux(Parity other) throws SemanticException {
		return equals(other);
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
	protected Satisfiability satisfiesTypeConversion(Type type, Parity right) {
		return Satisfiability.UNKNOWN;
	}

	@Override
	protected Satisfiability satisfiesUnaryExpression(UnaryOperator operator, Parity arg) {
		return Satisfiability.UNKNOWN;
	}

	@Override
	protected Satisfiability satisfiesBinaryExpression(BinaryOperator operator, Parity left, Parity right) {
		return Satisfiability.UNKNOWN;
	}

	@Override
	protected Satisfiability satisfiesTernaryExpression(TernaryOperator operator, Parity left, Parity middle,
			Parity right) {
		return Satisfiability.UNKNOWN;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((parity == null) ? 0 : parity.hashCode());
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
		Parity other = (Parity) obj;
		if (parity != other.parity)
			return false;
		return true;
	}
}