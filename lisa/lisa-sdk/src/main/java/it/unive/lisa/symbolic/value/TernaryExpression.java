package it.unive.lisa.symbolic.value;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.ExpressionVisitor;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.operator.ternary.TernaryOperator;
import it.unive.lisa.type.Type;

/**
 * A bynary expression that applies a {@link TernaryExpression} to three
 * {@link SymbolicExpression}s.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class TernaryExpression extends ValueExpression {

	/**
	 * The left-hand side operand of this expression
	 */
	private final SymbolicExpression left;

	/**
	 * The middle operand of this expression
	 */
	private final SymbolicExpression middle;

	/**
	 * The right-hand side operand of this expression
	 */
	private final SymbolicExpression right;

	/**
	 * The operator to apply
	 */
	private final TernaryOperator operator;

	/**
	 * Builds the binary expression.
	 * 
	 * @param staticType the static type of this expression
	 * @param left       the left-hand side operand of this expression
	 * @param middle     the middle operand of this expression
	 * @param right      the right-hand side operand of this expression
	 * @param operator   the operator to apply
	 * @param location   the code location of the statement that has generated
	 *                       this expression
	 */
	public TernaryExpression(
			Type staticType,
			SymbolicExpression left,
			SymbolicExpression middle,
			SymbolicExpression right,
			TernaryOperator operator,
			CodeLocation location) {
		super(staticType, location);
		this.left = left;
		this.middle = middle;
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
	 * Yields the middle operand of this expression.
	 * 
	 * @return the middle operand
	 */
	public SymbolicExpression getMiddle() {
		return middle;
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
	 * Yields the operator that is applied to {@link #getLeft()},
	 * {@link #getMiddle()} and {@link #getRight()}.
	 * 
	 * @return the operator to apply
	 */
	public TernaryOperator getOperator() {
		return operator;
	}

	@Override
	public SymbolicExpression pushScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		TernaryExpression expr = new TernaryExpression(
				getStaticType(),
				left.pushScope(token, pp),
				middle.pushScope(token, pp),
				right.pushScope(token, pp),
				operator, getCodeLocation());
		return expr;
	}

	@Override
	public SymbolicExpression popScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		TernaryExpression expr = new TernaryExpression(
				getStaticType(),
				left.popScope(token, pp),
				middle.popScope(token, pp),
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
		result = prime * result + ((middle == null) ? 0 : middle.hashCode());
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
		TernaryExpression other = (TernaryExpression) obj;
		if (left == null) {
			if (other.left != null)
				return false;
		} else if (!left.equals(other.left))
			return false;
		if (operator != other.operator)
			return false;
		if (middle == null) {
			if (other.middle != null)
				return false;
		} else if (!middle.equals(other.middle))
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
		return left + " " + operator + "(" + middle + ", " + right + ")";
	}

	@Override
	public <T> T accept(
			ExpressionVisitor<T> visitor,
			Object... params)
			throws SemanticException {
		T left = this.left.accept(visitor, params);
		T middle = this.middle.accept(visitor, params);
		T right = this.right.accept(visitor, params);
		return visitor.visit(this, left, middle, right, params);
	}

	@Override
	public boolean mightNeedRewriting() {
		return left.mightNeedRewriting() || middle.mightNeedRewriting() || right.mightNeedRewriting();
	}

	/**
	 * Yields a copy of this expression with the given operator.
	 * 
	 * @param operator the operator to apply to the left, middle, and right
	 *                     operands
	 * 
	 * @return the copy of this expression with the given operator
	 */
	public TernaryExpression withOperator(
			TernaryOperator operator) {
		return new TernaryExpression(getStaticType(), left, middle, right, operator, getCodeLocation());
	}
}
