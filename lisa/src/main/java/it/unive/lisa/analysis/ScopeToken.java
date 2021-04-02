package it.unive.lisa.analysis;

import it.unive.lisa.program.CodeElement;

public class ScopeToken {

	private final CodeElement scoper;
	
	public ScopeToken(CodeElement scoper) {
		this.scoper = scoper;
	}
	
	public CodeElement getScoper() {
		return scoper;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((scoper == null) ? 0 : scoper.hashCode());
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
		ScopeToken other = (ScopeToken) obj;
		if (scoper == null) {
			if (other.scoper != null)
				return false;
		} else if (!scoper.equals(other.scoper))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "((" + scoper + "))";
	}
}
