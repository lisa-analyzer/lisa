package it.unive.lisa.analysis.continuations;

import it.unive.lisa.program.cfg.edge.BeginFinallyEdge;
import it.unive.lisa.program.cfg.edge.EndFinallyEdge;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

/**
 * A {@link Continuation} that models one possible execution path leading to a
 * finally block. Each execution is determined by the control flow of the
 * program: {@link BeginFinallyEdge}s will create continuations of this class
 * with the same path index, and {@link EndFinallyEdge}s will merge
 * continuations with matching indexes into the normal {@link Execution} flow.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Finally
		extends
		Continuation {

	private final int pathIdx;

	/**
	 * Builds a new continuation for the given path index.
	 * 
	 * @param pathIdx the path index
	 */
	public Finally(
			int pathIdx) {
		this.pathIdx = pathIdx;
	}

	/**
	 * Yields the path index associated with this continuation.
	 * 
	 * @return the path index
	 */
	public int getPathIdx() {
		return pathIdx;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + pathIdx;
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
		Finally other = (Finally) obj;
		if (pathIdx != other.pathIdx)
			return false;
		return true;
	}

	@Override
	public StructuredRepresentation representation() {
		return new StringRepresentation("finally[" + pathIdx + "]");
	}
}
