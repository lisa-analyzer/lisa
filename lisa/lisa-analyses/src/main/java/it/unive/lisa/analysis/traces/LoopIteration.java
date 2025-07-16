package it.unive.lisa.analysis.traces;

import it.unive.lisa.program.cfg.ProgramPoint;

/**
 * A {@link TraceToken} representing the traversal of a loop condition at the
 * beginning of a specific iteration.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class LoopIteration
		extends
		TraceToken {

	private final int iteration;

	/**
	 * Builds the iteration.
	 * 
	 * @param pp        the program point associated with the iteration
	 * @param iteration the iteration number
	 */
	public LoopIteration(
			ProgramPoint pp,
			int iteration) {
		super(pp);
		this.iteration = iteration;
	}

	/**
	 * Yields the iteration number.
	 * 
	 * @return the number
	 */
	public int getIteration() {
		return iteration;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + iteration;
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
		LoopIteration other = (LoopIteration) obj;
		if (iteration != other.iteration)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "[" + super.toString() + "]Iter" + iteration;
	}

}
