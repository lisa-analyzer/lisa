package it.unive.lisa.interprocedural;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.call.CFGCall;

/**
 * An identifier for an {@link InterproceduralAnalysis} to distinguish different
 * results for the same {@link CFG} based on their calling contexts.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface ScopeId {

	/**
	 * Yields the id to use at the start of the analysis, for entrypoints.
	 * 
	 * @return the scope
	 */
	ScopeId startingId();

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
	 * @param c the call to append
	 * 
	 * @return the (optionally) updated id
	 */
	ScopeId push(CFGCall c);
}
