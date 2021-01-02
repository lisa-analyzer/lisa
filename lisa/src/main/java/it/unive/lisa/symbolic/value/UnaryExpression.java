package it.unive.lisa.symbolic.value;

import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.collections.ExternalSet;

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
	public UnaryExpression(ExternalSet<Type> types, SymbolicExpression expression, UnaryOperator operator) {
		super(types);
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
}
