package it.unive.lisa.symbolic.value;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.ExpressionVisitor;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.collections.externalSet.ExternalSet;

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
	 * @param types      the runtime types of this expression
	 * @param expression the inner expression
	 * @param operator   the operator to apply
	 */
	public UnaryExpression(ExternalSet<Type> types, SymbolicExpression expression, UnaryOperator operator, CodeLocation location) {
		super(types, location);
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
		if (operator == UnaryOperator.LOGICAL_NOT && expression instanceof BinaryExpression) {
			BinaryExpression binary = (BinaryExpression) expression;
			ValueExpression left = (ValueExpression) binary.getLeft();
			ValueExpression right = (ValueExpression) binary.getRight();
			BinaryOperator op = binary.getOperator();
			return new BinaryExpression(binary.getTypes(), left.removeNegations(), right.removeNegations(),
					(BinaryOperator) op.opposite(), location);
		}

		return this;
	}

	@Override
	public SymbolicExpression pushScope(ScopeToken token) throws SemanticException {
		return new UnaryExpression(this.getTypes(), this.expression.pushScope(token), this.operator, location);
	}

	@Override
	public SymbolicExpression popScope(ScopeToken token) throws SemanticException {
		return new UnaryExpression(this.getTypes(), this.expression.popScope(token), this.operator, location);
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
	public boolean equals(Object obj) {
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
		return operator.toString() + expression.toString();
	}

	@Override
	public <T> T accept(ExpressionVisitor<T> visitor, Object... params) throws SemanticException {
		T arg = expression.accept(visitor, params);
		return visitor.visit(this, arg, params);
	}
}
