package it.unive.lisa.outputs.messages;

import it.unive.lisa.checks.verification.VerificationResult;
import it.unive.lisa.checks.verification.Violation;
import it.unive.lisa.interprocedural.ScopeId;
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
	 * Builds the message, reporting the given violation (if any), that can be
	 * verified after the analysis.
	 *
	 * @param expression the expression where this message is reported on
	 * @param message    the message of this message
	 * @param context    the context where this message was reported on (can be
	 *                       {@code null})
	 * @param violation  the violation reported by this message (can be
	 *                       {@code null} if unknown)
	 */
	public ExpressionMessage(
			Expression expression,
			String message,
			ScopeId<?> context,
			Violation violation) {
		super(expression, message, context, violation);
	}

	private ExpressionMessage(
			Expression expression,
			String message,
			ScopeId<?> context,
			Violation violation,
			VerificationResult verdict) {
		super(expression, message, context, violation, verdict);
	}

	@Override
	public ExpressionMessage withVerdict(
			VerificationResult verdict) {
		return new ExpressionMessage(getExpression(), getMessage(), getContext(), getViolation(), verdict);
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
