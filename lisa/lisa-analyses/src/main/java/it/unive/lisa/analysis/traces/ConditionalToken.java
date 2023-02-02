package it.unive.lisa.analysis.traces;

import it.unive.lisa.program.cfg.ProgramPoint;

public class ConditionalToken extends Token {
	private final boolean trueBranch;

	public ConditionalToken(ProgramPoint st, boolean trueBranch) {
		super(st);
		this.trueBranch = trueBranch;
	}

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
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ConditionalToken other = (ConditionalToken) obj;
		if (trueBranch != other.trueBranch)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "[" + super.toString() + "]" + (trueBranch ? "True" : "False");
	}
}
