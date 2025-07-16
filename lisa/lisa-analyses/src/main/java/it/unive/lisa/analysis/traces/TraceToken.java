package it.unive.lisa.analysis.traces;

import it.unive.lisa.program.cfg.ProgramPoint;

/**
 * A token of an {@link ExecutionTrace}, tracking the traversal of a condition
 * represented by a {@link ProgramPoint}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class TraceToken {

	private final ProgramPoint pp;

	/**
	 * Builds a token associated with the given program point.
	 * 
	 * @param pp the program point
	 */
	protected TraceToken(
			ProgramPoint pp) {
		this.pp = pp;
	}

	/**
	 * Yields the program point associated with this token.
	 * 
	 * @return the program point
	 */
	public ProgramPoint getProgramPoint() {
		return pp;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((pp == null) ? 0 : pp.hashCode());
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
		TraceToken other = (TraceToken) obj;
		if (pp == null) {
			if (other.pp != null)
				return false;
		} else if (!pp.equals(other.pp))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return pp.toString();
	}

}
