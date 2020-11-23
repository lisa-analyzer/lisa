package it.unive.lisa.symbolic.value;

import it.unive.lisa.symbolic.SymbolicExpression;

public class BinaryExpression extends ValueExpression {

	private final SymbolicExpression left, right;

	private final BinaryOperator operator;

	public BinaryExpression(SymbolicExpression left, SymbolicExpression right, BinaryOperator operator) {
		this.left = left;
		this.right = right;
		this.operator = operator;
	}

	public SymbolicExpression getLeft() {
		return left;
	}
	
	public SymbolicExpression getRight() {
		return right;
	}

	public BinaryOperator getOperator() {
		return operator;
	}
}
