package it.unive.lisa.interprocedural;

import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.call.CFGCall;

/**
 * An identifier for an {@link InterproceduralAnalysis} to distinguish different
 * results for the same {@link CFG} based on their calling contexts.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractLattice} computed by the analysis
 */
public interface ScopeId<A extends AbstractLattice<A>> {

	/**
	 * Yields the id to use at the start of the analysis, for entrypoints.
	 * 
	 * @return the scope
	 */
	ScopeId<A> startingId();

	/**
	 * Yields whether or not this id is the starting one, that is, if it has
	 * been generated with {@link #startingId()} or if it must be considered
	 * equivalent to the value returned by that method.
	 * 
	 * @return {@code true} if that condition holds
	 */
	boolean isStartingId();

	/**
	 * Transforms the current scope id by appending the given call.
	 * 
	 * @param c     the call to append
	 * @param state the analysis state at the call site
	 *
	 * @return the (optionally) updated id
	 */
	ScopeId<A> push(
			CFGCall c,
			AnalysisState<A> state);

}
