package it.unive.lisa.symbolic.value;

import it.unive.lisa.symbolic.SymbolicExpression;

public class UnarySymbolicExpression extends ValueExpression {

	private final SymbolicExpression expression;

	private final UnaryOperator operator;

	public UnarySymbolicExpression(SymbolicExpression expression, UnaryOperator operator) {
		this.expression = expression;
		this.operator = operator;
	}

	public SymbolicExpression getExpression() {
		return expression;
	}

	public UnaryOperator getOperator() {
		return operator;
	}
}
