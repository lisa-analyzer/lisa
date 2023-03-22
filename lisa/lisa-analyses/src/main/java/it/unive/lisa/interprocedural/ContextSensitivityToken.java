package it.unive.lisa.interprocedural;

import it.unive.lisa.program.cfg.statement.call.CFGCall;

/**
 * A token for interprocedural analysis that tunes the level of context
 * sensitivity.
 */
public interface ContextSensitivityToken extends ScopeId {

	/**
	 * A token without any context sensitivity.
	 * 
	 * @return an empty context sensitive token
	 */
	ContextSensitivityToken empty();

	/**
	 * Creates a context sensitive token with the given call on the top of the
	 * stack.
	 * 
	 * @param c the {@link CFGCall} to be pushed at the top of the token
	 * 
	 * @return a token with the given call on the top of the call stack
	 */
	ContextSensitivityToken pushCall(CFGCall c);

	/**
	 * Creates a context sensitive token popping the call on top of the stack.
	 * 
	 * @param c the {@link CFGCall} to be popped from the top of the token
	 * 
	 * @return a token without the this token's top call
	 */
	ContextSensitivityToken popCall(CFGCall c);

	/**
	 * Whether or not the limit of the call chain has been reached (where
	 * {@code true} means that nothing can be pushed anymore <i>and</i> no
	 * further scoping can be applied starting from this token). When this
	 * method returns {@code true}, the result of further calls cannot be
	 * evaluated.
	 * 
	 * @return {@code true} if that condition holds
	 */
	boolean limitReached();

	@Override
	default ScopeId startingId() {
		return empty();
	}

	@Override
	default ScopeId push(CFGCall c) {
		return pushCall(c);
	}

	@Override
	default boolean isStartingId() {
		ContextSensitivityToken empty = empty();
		return this == empty || equals(empty);
	}
}
