package it.unive.lisa.analysis.traces;

import it.unive.lisa.program.cfg.ProgramPoint;

public abstract class Token {

	private final ProgramPoint st;

	protected Token(ProgramPoint st) {
		this.st = st;
	}

	public ProgramPoint getStatement() {
		return st;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((st == null) ? 0 : st.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Token other = (Token) obj;
		if (st == null) {
			if (other.st != null)
				return false;
		} else if (!st.equals(other.st))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return st.getLocation().toString();
	}
}
