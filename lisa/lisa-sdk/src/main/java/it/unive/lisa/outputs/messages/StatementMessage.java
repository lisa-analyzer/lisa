package it.unive.lisa.outputs.messages;

import it.unive.lisa.checks.verification.ReproducingInput;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.program.cfg.statement.Statement;
import java.util.List;

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
		this(statement, message, false, null);
	}

	/**
	 * Builds the message.
	 * 
	 * @param statement         the statement where this message was reported on
	 * @param message           the message of this message
	 * @param definite          whether or not this message is definite, that
	 *                              is, if the reported behavior happens in at
	 *                              least one execution of the program
	 * @param reproducingInputs the inputs reproducing the behavior reported by
	 *                              this message (can be {@code null} if no
	 *                              inputs are known)
	 */
	public StatementMessage(
			Statement statement,
			String message,
			boolean definite,
			List<ReproducingInput> reproducingInputs) {
		this(statement, message, null, definite, reproducingInputs);
	}

	/**
	 * Builds the message.
	 *
	 * @param statement         the statement where this message was reported on
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
	public StatementMessage(
			Statement statement,
			String message,
			ScopeId<?> context,
			boolean definite,
			List<ReproducingInput> reproducingInputs) {
		super(statement.getLocation(), message, context, definite, reproducingInputs);
		this.statement = statement;
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
