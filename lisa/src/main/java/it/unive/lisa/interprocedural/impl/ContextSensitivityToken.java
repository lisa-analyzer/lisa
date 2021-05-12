package it.unive.lisa.interprocedural.impl;

import it.unive.lisa.analysis.ScopeToken;

/**
 * A token for interprocedural analysis that tunes the level of context
 * sensitivity.
 */
public interface ContextSensitivityToken {

	/**
	 * A token without any context sensitivity.
	 * 
	 * @return an empty context sensitive token
	 */
	ContextSensitivityToken empty();

	/**
	 * Creates a context sensitive token with the given scope on the top of the
	 * stack.
	 * 
	 * @param c the {@link ScopeToken} to be pushed at the top of the context
	 *              sensitive
	 * 
	 * @return a token with the given scope on the top of the call stack
	 */
	ContextSensitivityToken pushToken(ScopeToken c);

	/**
	 * Creates a context sensitive token popping the scope on top of the stack.
	 * 
	 * @return a token without the this token's top scope
	 */
	ContextSensitivityToken popToken();
}
