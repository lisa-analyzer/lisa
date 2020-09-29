package it.unive.lisa.checks.warnings;

import it.unive.lisa.cfg.statement.Expression;

/**
 * A warning reported by LiSA on an expression.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public final class ExpressionWarning extends WarningWithLocation {

	/**
	 * The expression where this warning was reported on
	 */
	private final Expression expression;

	/**
	 * Builds the warning.
	 * 
	 * @param expression the expression where this warning was reported on
	 * @param message   the message of this warning
	 */
	public ExpressionWarning(Expression expression, String message) {
		super(expression.getSourceFile(), expression.getLine(), expression.getCol(), message);
		this.expression = expression;
	}

	/**
	 * Yields the expression where this warning was reported on.
	 * 
	 * @return the expression
	 */
	public final Expression getExpression() {
		return expression;
	}

	@Override
	public int compareTo(Warning o) {
		if (!(o instanceof ExpressionWarning))
			return super.compareTo(o);

		ExpressionWarning other = (ExpressionWarning) o;
		int cmp;

		if ((cmp = expression.compareTo(other.expression)) != 0)
			return cmp;

		return super.compareTo(other);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((expression == null) ? 0 : expression.hashCode());
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
		ExpressionWarning other = (ExpressionWarning) obj;
		if (expression == null) {
			if (other.expression != null)
				return false;
		} else if (!expression.equals(other.expression))
			return false;
		return true;
	}
	
	@Override
	public String getTag() {
		return "EXPRESSION";
	}
	
	@Override
	public String toString() {
		return getLocationWithBrackets() + " on '" + expression.getCFG().getDescriptor() + "': " + getTaggedMessage();
	}
}