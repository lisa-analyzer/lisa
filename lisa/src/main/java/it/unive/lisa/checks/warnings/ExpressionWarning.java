package it.unive.lisa.checks.warnings;

import it.unive.lisa.program.cfg.statement.Expression;

/**
 * A warning reported by LiSA on an expression.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public final class ExpressionWarning extends StatementWarning {

	/**
	 * Builds the warning.
	 * 
	 * @param expression the expression where this warning was reported on
	 * @param message    the message of this warning
	 */
	public ExpressionWarning(Expression expression, String message) {
		super(expression, message);
	}

	/**
	 * Yields the expression where this warning was reported on.
	 * 
	 * @return the expression
	 */
	public Expression getExpression() {
		return (Expression) getStatement();
	}

	@Override
	public String getTag() {
		return "EXPRESSION";
	}
}
