package it.unive.lisa.analysis.traces;

import it.unive.lisa.program.cfg.ProgramPoint;

/**
 * A {@link TraceToken} representing the traversal of a loop condition,
 * summarizing all possible iterations.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class LoopSummary
		extends
		TraceToken {

	/**
	 * Builds the summary.
	 * 
	 * @param pp the program point associated with the summary
	 */
	public LoopSummary(
			ProgramPoint pp) {
		super(pp);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + getClass().getName().hashCode();
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
		return true;
	}

	@Override
	public String toString() {
		return "[" + super.toString() + "]Summary";
	}

}
