package it.unive.lisa.symbolic.value;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.ExpressionVisitor;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.type.Type;

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
	 * @param staticType the static type of this expression
	 * @param left       the left-hand side operand of this expression
	 * @param right      the right-hand side operand of this expression
	 * @param operator   the operator to apply
	 * @param location   the code location of the statement that has generated
	 *                       this expression
	 */
	public BinaryExpression(
			Type staticType,
			SymbolicExpression left,
			SymbolicExpression right,
			BinaryOperator operator,
			CodeLocation location) {
		super(staticType, location);
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
	public SymbolicExpression pushScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		BinaryExpression expr = new BinaryExpression(
				getStaticType(),
				left.pushScope(token, pp),
				right.pushScope(token, pp),
				operator,
				getCodeLocation());
		return expr;
	}

	@Override
	public SymbolicExpression popScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		BinaryExpression expr = new BinaryExpression(
				getStaticType(),
				left.popScope(token, pp),
				right.popScope(token, pp),
				operator,
				getCodeLocation());
		return expr;
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
	public boolean equals(
			Object obj) {
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

	@Override
	public <T> T accept(
			ExpressionVisitor<T> visitor,
			Object... params)
			throws SemanticException {
		T left = this.left.accept(visitor, params);
		T right = this.right.accept(visitor, params);
		return visitor.visit(this, left, right, params);
	}

	@Override
	public boolean mightNeedRewriting() {
		return left.mightNeedRewriting() || right.mightNeedRewriting();
	}

	/**
	 * Yields a copy of this expression with the given operator.
	 * 
	 * @param operator the operator to apply to the left and right operands
	 * 
	 * @return the copy of this expression with the given operator
	 */
	public BinaryExpression withOperator(
			BinaryOperator operator) {
		return new BinaryExpression(getStaticType(), left, right, operator, getCodeLocation());
	}
}
