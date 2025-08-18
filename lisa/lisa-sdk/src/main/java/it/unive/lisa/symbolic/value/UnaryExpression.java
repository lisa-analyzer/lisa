package it.unive.lisa.symbolic.value;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.ExpressionVisitor;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.operator.NegatableOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.Type;

/**
 * A unary expression that applies a {@link UnaryOperator} to a
 * {@link SymbolicExpression}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class UnaryExpression extends ValueExpression {

	/**
	 * The inner expression
	 */
	private final SymbolicExpression expression;

	/**
	 * The operator to apply
	 */
	private final UnaryOperator operator;

	/**
	 * Builds the unary expression.
	 * 
	 * @param staticType the static type of this expression
	 * @param expression the inner expression
	 * @param operator   the operator to apply
	 * @param location   the code location of the statement that has generated
	 *                       this expression
	 */
	public UnaryExpression(
			Type staticType,
			SymbolicExpression expression,
			UnaryOperator operator,
			CodeLocation location) {
		super(staticType, location);
		this.expression = expression;
		this.operator = operator;
	}

	/**
	 * Yields the inner expression, that is transformed by this expression by
	 * applying {@link #getOperator()}.
	 * 
	 * @return the inner expression
	 */
	public SymbolicExpression getExpression() {
		return expression;
	}

	/**
	 * Yields the operator that is applied to {@link #getExpression()}.
	 * 
	 * @return the operator to apply
	 */
	public UnaryOperator getOperator() {
		return operator;
	}

	@Override
	public ValueExpression removeNegations() {
		if (operator instanceof LogicalNegation && expression instanceof BinaryExpression) {
			ValueExpression left = (ValueExpression) ((BinaryExpression) expression).getLeft();
			ValueExpression right = (ValueExpression) ((BinaryExpression) expression).getRight();
			BinaryOperator op = ((BinaryExpression) expression).getOperator();
			BinaryOperator oppositeOp = op instanceof NegatableOperator
					? (BinaryOperator) ((NegatableOperator) op).opposite()
					: op;
			ValueExpression oppositeLeft = left.removeNegations();
			ValueExpression oppositeRight = right.removeNegations();
			if (op == oppositeOp && left == oppositeLeft && right == oppositeRight)
				// if nothing changed, preserve reference equality
				return this;
			return new BinaryExpression(
				expression.getStaticType(),
				oppositeLeft,
				oppositeRight,
				oppositeOp,
				getCodeLocation());
		}

		return this;
	}

	@Override
	public SymbolicExpression pushScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		UnaryExpression expr = new UnaryExpression(
			getStaticType(),
			expression.pushScope(token, pp),
			operator,
			getCodeLocation());
		return expr;
	}

	@Override
	public SymbolicExpression popScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		UnaryExpression expr = new UnaryExpression(
			getStaticType(),
			expression.popScope(token, pp),
			operator,
			getCodeLocation());
		return expr;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((expression == null) ? 0 : expression.hashCode());
		result = prime * result + ((operator == null) ? 0 : operator.hashCode());
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
		UnaryExpression other = (UnaryExpression) obj;
		if (expression == null) {
			if (other.expression != null)
				return false;
		} else if (!expression.equals(other.expression))
			return false;
		if (operator != other.operator)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return operator + " " + expression;
	}

	@Override
	public <T> T accept(
			ExpressionVisitor<T> visitor,
			Object... params)
			throws SemanticException {
		T arg = expression.accept(visitor, params);
		return visitor.visit(this, arg, params);
	}

	@Override
	public boolean mightNeedRewriting() {
		return expression.mightNeedRewriting();
	}

	/**
	 * Yields a copy of this expression with the given operator.
	 * 
	 * @param operator the operator to apply to the argument of this expression
	 * 
	 * @return the copy of this expression with the given operator
	 */
	public UnaryExpression withOperator(
			UnaryOperator operator) {
		return new UnaryExpression(getStaticType(), expression, operator, getCodeLocation());
	}

	@Override
	public SymbolicExpression removeTypingExpressions() {
		SymbolicExpression e = expression.removeTypingExpressions();
		if (expression == e)
			return this;
		return new UnaryExpression(getStaticType(), e, operator, getCodeLocation());
	}

	@Override
	public SymbolicExpression replace(
			SymbolicExpression source,
			SymbolicExpression target) {
		if (this.equals(source))
			return target;

		SymbolicExpression e = expression.replace(source, target);
		if (expression == e)
			return this;
		return new UnaryExpression(getStaticType(), e, operator, getCodeLocation());
	}

}
