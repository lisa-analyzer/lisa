package it.unive.lisa.analysis.traces;

import it.unive.lisa.program.cfg.ProgramPoint;

public class LoopSummaryToken extends Token {

	public LoopSummaryToken(ProgramPoint st) {
		super(st);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + getClass().getName().hashCode();
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
		return true;
	}

	@Override
	public String toString() {
		return "[" + super.toString() + "]Summary";
	}
}
