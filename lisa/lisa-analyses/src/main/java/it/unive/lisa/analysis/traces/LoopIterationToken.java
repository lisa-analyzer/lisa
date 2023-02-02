package it.unive.lisa.analysis.traces;

import it.unive.lisa.program.cfg.ProgramPoint;

public class LoopIterationToken extends Token {
	private final int iteration;

	public LoopIterationToken(ProgramPoint st, int iteration) {
		super(st);
		this.iteration = iteration;
	}

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
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		LoopIterationToken other = (LoopIterationToken) obj;
		if (iteration != other.iteration)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "[" + super.toString() + "]Iter" + iteration;
	}
}