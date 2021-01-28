package it.unive.lisa.symbolic.value;

import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.collections.ExternalSet;

/**
 * A bynary expression that applies a {@link BinaryOperator} to two
 * {@link SymbolicExpression}s.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class BinaryExpression extends ValueExpression {

	/**
	 * The left-hand side operand of this expression
	 */
	private final SymbolicExpression left;

	/**
	 * The right-hand side operand of this expression
	 */
	private final SymbolicExpression right;

	/**
	 * The operator to apply
	 */
	private final BinaryOperator operator;

	/**
	 * Builds the binary expression.
	 * 
	 * @param types    the runtime types of this expression
	 * @param left     the left-hand side operand of this expression
	 * @param right    the right-hand side operand of this expression
	 * @param operator the operator to apply
	 */
	public BinaryExpression(ExternalSet<Type> types, SymbolicExpression left, SymbolicExpression right,
			BinaryOperator operator) {
		super(types);
		this.left = left;
		this.right = right;
		this.operator = operator;
	}

	/**
	 * Yields the left-hand side operand of this expression.
	 * 
	 * @return the left operand
	 */
	public SymbolicExpression getLeft() {
		return left;
	}

	/**
	 * Yields the right-hand side operand of this expression.
	 * 
	 * @return the right operand
	 */
	public SymbolicExpression getRight() {
		return right;
	}

	/**
	 * Yields the operator that is applied to {@link #getLeft()} and
	 * {@link #getRight()}.
	 * 
	 * @return the operator to apply
	 */
	public BinaryOperator getOperator() {
		return operator;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((left == null) ? 0 : left.hashCode());
		result = prime * result + ((operator == null) ? 0 : operator.hashCode());
		result = prime * result + ((right == null) ? 0 : right.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		BinaryExpression other = (BinaryExpression) obj;
		if (left == null) {
			if (other.left != null)
				return false;
		} else if (!left.equals(other.left))
			return false;
		if (operator != other.operator)
			return false;
		if (right == null) {
			if (other.right != null)
				return false;
		} else if (!right.equals(other.right))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return left + " " + operator + " " + right;
	}
}
