package it.unive.lisa.interprocedural;

import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.program.cfg.fixpoints.CompoundState;
import it.unive.lisa.program.cfg.statement.call.CFGCall;

/**
 * A {@link ScopeId} for analyses that do not make distinction between different
 * calling contexts.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractLattice} handled by the analysis
 */
public class UniqueScope<A extends AbstractLattice<A>>
		implements
		ScopeId<A> {

	// all unique scopes are equal, so they share the same hash
	private static final String HASH = ScopeId.computeHash(UniqueScope.class.getName());

	@Override
	public ScopeId<A> startingId() {
		return this;
	}

	@Override
	public ScopeId<A> push(
			CFGCall scopeToken,
			CompoundState<A> state) {
		return this;
	}

	@Override
	public boolean isStartingId() {
		return true;
	}

	@Override
	public String getHash() {
		return HASH;
	}

}
