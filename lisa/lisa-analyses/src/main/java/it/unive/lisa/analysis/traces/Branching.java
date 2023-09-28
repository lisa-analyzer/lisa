package it.unive.lisa.analysis.traces;

import it.unive.lisa.program.cfg.ProgramPoint;

/**
 * A {@link TraceToken} representing the traversal of a if-then-else condition,
 * associated with the branch taken.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Branching extends TraceToken {

	private final boolean trueBranch;

	/**
	 * Builds the branching.
	 * 
	 * @param pp         the program point associated with the branching
	 * @param trueBranch whether the condition is traversed to reach the
	 *                       {@code true} branch or not
	 */
	public Branching(
			ProgramPoint pp,
			boolean trueBranch) {
		super(pp);
		this.trueBranch = trueBranch;
	}

	/**
	 * Yields whether the condition is traversed to reach the {@code true}
	 * branch or not.
	 * 
	 * @return {@code true} if that condition holds
	 */
	public boolean isTrueBranch() {
		return trueBranch;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (trueBranch ? 1231 : 1237);
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
		Branching other = (Branching) obj;
		if (trueBranch != other.trueBranch)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "[" + super.toString() + "]" + (trueBranch ? "True" : "False");
	}
}
