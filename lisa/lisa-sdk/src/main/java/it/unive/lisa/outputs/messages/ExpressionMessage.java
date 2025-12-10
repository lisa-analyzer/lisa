package it.unive.lisa.outputs.messages;

import it.unive.lisa.program.cfg.statement.Expression;

/**
 * A message reported by LiSA on an expression.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class ExpressionMessage
		extends
		StatementMessage {

	/**
	 * Builds the message.
	 * 
	 * @param expression the expression where this message was reported on
	 * @param message    the message of this message
	 */
	public ExpressionMessage(
			Expression expression,
			String message) {
		super(expression, message);
	}

	/**
	 * Yields the expression where this message was reported on.
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
