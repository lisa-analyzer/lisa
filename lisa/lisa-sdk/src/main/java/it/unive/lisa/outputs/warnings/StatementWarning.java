package it.unive.lisa.outputs.warnings;

import it.unive.lisa.program.cfg.statement.Statement;

/**
 * A warning reported by LiSA on a statement.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class StatementWarning
		extends
		WarningWithLocation {

	/**
	 * The statement where this warning was reported on
	 */
	private final Statement statement;

	/**
	 * Builds the warning.
	 * 
	 * @param statement the statement where this warning was reported on
	 * @param message   the message of this warning
	 */
	public StatementWarning(
			Statement statement,
			String message) {
		super(statement.getLocation(), message);
		this.statement = statement;
	}

	/**
	 * Yields the statement where this warning was reported on.
	 * 
	 * @return the statement
	 */
	public Statement getStatement() {
		return statement;
	}

	@Override
	public int compareTo(
			Warning o) {
		int cmp;
		if ((cmp = super.compareTo(o)) != 0)
			return cmp;

		if (!(o instanceof StatementWarning))
			return getClass().getName().compareTo(o.getClass().getName());

		StatementWarning other = (StatementWarning) o;
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
		StatementWarning other = (StatementWarning) obj;
		if (statement == null) {
			if (other.statement != null)
				return false;
		} else if (!statement.equals(other.statement))
			return false;
		return true;
	}

	@Override
	public String getTag() {
		return "STATEMENT";
	}

	@Override
	public String toString() {
		return getLocationWithBrackets()
				+ " on '"
				+ statement.getCFG().getDescriptor().getFullSignatureWithParNames()
				+ "': "
				+ getTaggedMessage();
	}

}
