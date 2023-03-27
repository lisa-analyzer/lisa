package it.unive.lisa.interprocedural;

import it.unive.lisa.analysis.ScopeToken;

/**
 * A {@link ScopeId} for analyses that do not make distinction between different
 * calling contexts.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class UniqueScope implements ScopeId {

	@Override
	public ScopeId startingId() {
		return this;
	}

	@Override
	public ScopeId push(ScopeToken scopeToken) {
		return this;
	}

	@Override
	public boolean isStartingId() {
		return true;
	}
}
