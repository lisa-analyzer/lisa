package it.unive.lisa.interprocedural;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import it.unive.lisa.program.cfg.statement.call.CFGCall;

/**
 * A {@link ContextSensitivityToken} that keeps track of the current call stacks
 * for each call.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <T> the type of the call stack object
 */
public abstract class SlidingStackToken<T> implements ContextSensitivityToken {
	// FIXME should this be part of the interprocedural analysis?
	private final Map<CFGCall, Collection<T>> callStacks; 

	private boolean lastPush;

	/**
	 * Builds the token.
	 */
	protected SlidingStackToken() {
		callStacks = new HashMap<>();
	}

	/**
	 * Builds the token, shallow-copying the stacks of the given one.
	 * 
	 * @param other the other token
	 */
	protected SlidingStackToken(SlidingStackToken<T> other) {
		callStacks = new HashMap<>(other.callStacks);
	}

	/**
	 * Registers the given call stack for the given call. If the stack is
	 * already present, all calls to {@link #limitReached()} will return
	 * {@code false} until the next invocation of this method.
	 * 
	 * @param c     the call that the stack is associated with
	 * @param stack the stack to register
	 */
	protected void registerCallStack(CFGCall c, T stack) {
		lastPush = callStacks.computeIfAbsent(c, tk -> new HashSet<>()).add(stack);
	}

	/**
	 * Removes the given stack from the ones registered for the given call.
	 * 
	 * @param c     the call that the stack is associated with
	 * @param stack the stack to unregister
	 */
	protected void unregisterCallStack(CFGCall c, T stack) {
		Collection<T> stacks = callStacks.get(c);
		if (stacks.size() == 1)
			callStacks.remove(c);
		else
			stacks.remove(stack);
	}

	@Override
	public final boolean limitReached() {
		return !lastPush;
	}
}
