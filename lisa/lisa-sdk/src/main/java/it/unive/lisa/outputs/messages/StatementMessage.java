package it.unive.lisa.outputs.messages;

import it.unive.lisa.checks.verification.VerificationResult;
import it.unive.lisa.checks.verification.Violation;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.program.cfg.statement.Statement;

/**
 * A message reported by LiSA on a statement.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class StatementMessage
		extends
		MessageWithLocation {

	/**
	 * The statement where this message was reported on
	 */
	private final Statement statement;

	/**
	 * Builds the message.
	 * 
	 * @param statement the statement where this message was reported on
	 * @param message   the message of this message
	 */
	public StatementMessage(
			Statement statement,
			String message) {
		this(statement, message, null, null, null);
	}

	/**
	 * Builds the message, reporting the given violation (if any), that can be
	 * verified after the analysis.
	 *
	 * @param statement the statement where this message is reported on
	 * @param message   the message of this message
	 * @param context   the context where this message was reported on (can be
	 *                      {@code null})
	 * @param violation the violation reported by this message (can be
	 *                      {@code null} if unknown)
	 */
	public StatementMessage(
			Statement statement,
			String message,
			ScopeId<?> context,
			Violation violation) {
		this(statement, message, context, violation, null);
	}

	/**
	 * Builds the message.
	 *
	 * @param statement the statement where this message is reported on
	 * @param message   the message of this message
	 * @param context   the context where this message was reported on (can be
	 *                      {@code null})
	 * @param violation the violation reported by this message (can be
	 *                      {@code null} if unknown)
	 * @param verdict   the result of the verification of the violation (can be
	 *                      {@code null} if it has not been verified)
	 */
	protected StatementMessage(
			Statement statement,
			String message,
			ScopeId<?> context,
			Violation violation,
			VerificationResult verdict) {
		super(statement.getLocation(), message, context, violation, verdict);
		this.statement = statement;
	}

	/**
	 * Yields a copy of this message carrying the given result of the
	 * verification of the violation it reports (see {@link #getVerdict()}).
	 *
	 * @param verdict the result of the verification
	 *
	 * @return the verified message
	 */
	public StatementMessage withVerdict(
			VerificationResult verdict) {
		return new StatementMessage(statement, getMessage(), getContext(), getViolation(), verdict);
	}

	/**
	 * Yields the statement where this message was reported on.
	 * 
	 * @return the statement
	 */
	public Statement getStatement() {
		return statement;
	}

	@Override
	public int compareTo(
			Message o) {
		int cmp;
		if ((cmp = super.compareTo(o)) != 0)
			return cmp;

		if (!(o instanceof StatementMessage))
			return getClass().getName().compareTo(o.getClass().getName());

		StatementMessage other = (StatementMessage) o;
		if ((cmp = statement.compareTo(other.statement)) != 0)
			return cmp;

		return 0;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((statement == null) ? 0 : statement.hashCode());
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
		StatementMessage other = (StatementMessage) obj;
		if (statement == null) {
			if (other.statement != null)
				return false;
		} else if (!statement.equals(other.statement))
			return false;
		return true;
	}

	@Override
	public String getCFGSignature() {
		return statement.getCFG().getDescriptor().getFullSignatureWithParNames();
	}

	@Override
	public String getTargetText() {
		return statement.toString();
	}

	@Override
	public String getTag() {
		return "STATEMENT";
	}

	@Override
	public String toString() {
		return getLocationWithBrackets()
				+ " on '"
				+ getCFGSignature()
				+ "': "
				+ getTaggedMessage();
	}

}
