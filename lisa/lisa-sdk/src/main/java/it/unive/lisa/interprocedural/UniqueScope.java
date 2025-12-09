package it.unive.lisa.interprocedural;

import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.program.cfg.statement.call.CFGCall;

/**
 * A {@link ScopeId} for analyses that do not make distinction between different
 * calling contexts.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class UniqueScope<A extends AbstractLattice<A>>
		implements
		ScopeId<A> {

	@Override
	public ScopeId<A> startingId() {
		return this;
	}

	@Override
	public ScopeId<A> push(
			CFGCall scopeToken,
			AnalysisState<A> state) {
		return this;
	}

	@Override
	public boolean isStartingId() {
		return true;
	}

}
