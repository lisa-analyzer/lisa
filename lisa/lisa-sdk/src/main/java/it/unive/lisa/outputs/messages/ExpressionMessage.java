package it.unive.lisa.outputs.messages;

import it.unive.lisa.checks.verification.ReproducingInput;
import it.unive.lisa.checks.verification.VerificationResult;
import it.unive.lisa.checks.verification.Violation;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.program.cfg.statement.Expression;
import java.util.List;

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
	 * Builds the message.
	 * 
	 * @param expression        the expression where this message was reported
	 *                              on
	 * @param message           the message of this message
	 * @param definite          whether or not this message is definite, that
	 *                              is, if the reported behavior happens in at
	 *                              least one execution of the program
	 * @param reproducingInputs the inputs reproducing the behavior reported by
	 *                              this message (can be {@code null} if no
	 *                              inputs are known)
	 */
	public ExpressionMessage(
			Expression expression,
			String message,
			boolean definite,
			List<ReproducingInput> reproducingInputs) {
		this(expression, message, null, definite, reproducingInputs);
	}

	/**
	 * Builds the message.
	 *
	 * @param expression        the expression where this message was reported
	 *                              on
	 * @param message           the message of this message
	 * @param context           the context where this message was reported on
	 *                              (can be {@code null})
	 * @param definite          whether or not this message is definite, that
	 *                              is, if the reported behavior happens in at
	 *                              least one execution of the program
	 * @param reproducingInputs the inputs reproducing the behavior reported by
	 *                              this message (can be {@code null} if no
	 *                              inputs are known)
	 */
	public ExpressionMessage(
			Expression expression,
			String message,
			ScopeId<?> context,
			boolean definite,
			List<ReproducingInput> reproducingInputs) {
		super(expression, message, context, definite, reproducingInputs);
	}

	/**
	 * Builds a possible message reporting the given violation, that can be
	 * verified after the analysis.
	 *
	 * @param expression the expression where this message is reported on
	 * @param message    the message of this message
	 * @param context    the context where this message was reported on (can
	 *                       be {@code null})
	 * @param violation  the violation reported by this message
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
			boolean definite,
			boolean refuted,
			List<ReproducingInput> reproducingInputs,
			Violation violation) {
		super(expression, message, context, definite, refuted, reproducingInputs, violation);
	}

	@Override
	public ExpressionMessage verified(
			VerificationResult result) {
		switch (result.getOutcome()) {
		case CONFIRMED:
			return new ExpressionMessage(getExpression(), getMessage(), getContext(), true, false,
					result.getReproducingInputs(), getViolation());
		case REFUTED:
			return new ExpressionMessage(getExpression(), getMessage(), getContext(), false, true, null,
					getViolation());
		case POSSIBLE:
		default:
			return this;
		}
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
