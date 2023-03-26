package it.unive.lisa.interprocedural;

import java.util.List;

import it.unive.lisa.program.cfg.statement.call.CFGCall;

/**
 * A token for interprocedural analysis that tunes the level of context
 * sensitivity. This works as a mask over the call stack, keeping track only of
 * some of the calls appearing in it.
 */
public interface ContextSensitivityToken extends ScopeId {

	/**
	 * Creates a context sensitive token with the starting from the given (and
	 * possibly partial) stack. The call {@code c} is the new one being pushed
	 * on the stack.
	 * 
	 * @param stack the stack to use as base
	 * @param c     the {@link CFGCall} to be pushed at the top of the token
	 * 
	 * @return the created token
	 */
	ContextSensitivityToken pushOnStack(List<CFGCall> stack, CFGCall c);

	/**
	 * Yields the call stack considered by this token.
	 * 
	 * @return the call stack
	 */
	List<CFGCall> getKnownCalls();

	@Override
	default ContextSensitivityToken push(CFGCall c) {
		return pushOnStack(getKnownCalls(), c);
	}
}
