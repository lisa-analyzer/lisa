package it.unive.lisa.interprocedural.impl;

import it.unive.lisa.analysis.ScopeToken;

/**
 * A context sensitive token for interprocedural analysis.
 */
public interface ContextSensitiveToken {

	/**
	 * A token without any context sensitivity.
	 * 
	 * @return an empty context sensitive token
	 */
	ContextSensitiveToken empty();

	/**
	 * Creates a context sensitive token with the given scope on the top of the
	 * stack.
	 * 
	 * @param c the {@link ScopeToken} to be pushed at the top of the context
	 *              sensitive
	 * 
	 * @return a token with the given scope on the top of the call stack
	 */
	ContextSensitiveToken pushToken(ScopeToken c);
}
