package it.unive.lisa.analysis;

import it.unive.lisa.program.CodeElement;
import it.unive.lisa.program.cfg.statement.Statement;

/**
 * A token that can be used for pushing and popping scopes on local variables
 * through {@link SemanticDomain#pushScope(ScopeToken)} and
 * {@link SemanticDomain#popScope(ScopeToken)}. The token is identified by a
 * {@link CodeElement}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class ScopeToken {

	private final CodeElement scoper;

	/**
	 * Builds a new scope, referring to the given code element.
	 * 
	 * @param scoper the code element
	 */
	public ScopeToken(CodeElement scoper) {
		this.scoper = scoper;
	}

	/**
	 * Yields the element that this scope refers to.
	 * 
	 * @return the code element
	 */
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
		} else if (!(scoper instanceof Statement) || !(other.scoper instanceof Statement)) {
			if (!scoper.equals(other.scoper))
				return false;
		} else if (!((Statement) scoper).equals((Statement) other.scoper))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "[" + scoper.getLocation() + "]";
	}
}
