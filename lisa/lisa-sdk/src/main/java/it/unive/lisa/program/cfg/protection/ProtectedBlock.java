package it.unive.lisa.program.cfg.protection;

import it.unive.lisa.program.cfg.statement.Statement;
import java.util.Collection;
import java.util.Objects;

/**
 * A protected block, which is a part of a {@link ProtectionBlock}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class ProtectedBlock {

	private Statement start;

	private Statement end;

	private final Collection<Statement> body;

	/**
	 * Builds a protected block with a start and end statement, and a body.
	 * 
	 * @param start the first statement
	 * @param end   the last statement
	 * @param body  the body of the block
	 */
	public ProtectedBlock(
			Statement start,
			Statement end,
			Collection<Statement> body) {
		this.start = start;
		this.end = end;
		this.body = body;
	}

	/**
	 * Yields the first statement to be executed as part of this protection
	 * block.
	 * 
	 * @return the first statement
	 */
	public Statement getStart() {
		return start;
	}

	/**
	 * Sets the first statement to be executed as part of this protection block.
	 * 
	 * @param start the first statement
	 */
	public void setStart(
			Statement start) {
		Objects.requireNonNull(start, "The start statement of a protected block cannot be null");
		this.start = start;
	}

	/**
	 * Yields the last statement to be executed as part of this block, if it is
	 * unique. If this block can end in multiple statements (e.g., returns,
	 * throws, breaks, continues), this method returns {@code null}.
	 * 
	 * @return the closing statement, or {@code null} if there is no unique
	 *             closing statement
	 */
	public Statement getEnd() {
		return end;
	}

	/**
	 * Sets the last statement to be executed as part of this block, if it is
	 * unique. If this block can end in multiple statements (e.g., returns,
	 * throws, breaks, continues), this method should be invoked with
	 * {@code null} to indicate that there is no unique closing statement.
	 * 
	 * @param end the closing statement, or {@code null} if there is no unique
	 *                closing statement
	 */
	public void setEnd(
			Statement end) {
		this.end = end;
	}

	/**
	 * Yields the body of this protected block.
	 * 
	 * @return the body of this protected block
	 */
	public Collection<Statement> getBody() {
		return body;
	}

	/**
	 * Checks if this protected block can be continued, meaning that it contains
	 * at least one path that does not stop execution, does not break control
	 * flow, and does not continue control flow.
	 * 
	 * @return {@code true} if this protected block can be continued,
	 *             {@code false} otherwise
	 */
	public boolean canBeContinued() {
		return end != null && !end.stopsExecution() && !end.breaksControlFlow() && !end.continuesControlFlow();
	}

	/**
	 * Checks if this protected block always continues, meaning that it does not
	 * contain any statements that stop execution, break control flow, or
	 * continue control flow.
	 * 
	 * @return {@code true} if this protected block always continues,
	 *             {@code false} otherwise
	 */
	public boolean alwaysContinues() {
		return canBeContinued()
				&& body.stream()
					.noneMatch(st -> st.stopsExecution() || st.breaksControlFlow() || st.continuesControlFlow());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((start == null) ? 0 : start.hashCode());
		result = prime * result + ((end == null) ? 0 : end.hashCode());
		result = prime * result + ((body == null) ? 0 : body.hashCode());
		return result;
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ProtectedBlock other = (ProtectedBlock) obj;
		if (start == null) {
			if (other.start != null)
				return false;
		} else if (!start.equals(other.start))
			return false;
		if (end == null) {
			if (other.end != null)
				return false;
		} else if (!end.equals(other.end))
			return false;
		if (body == null) {
			if (other.body != null)
				return false;
		} else if (!body.equals(other.body))
			return false;
		return true;
	}

}
