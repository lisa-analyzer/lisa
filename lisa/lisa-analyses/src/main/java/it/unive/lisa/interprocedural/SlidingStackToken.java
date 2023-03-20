package it.unive.lisa.interprocedural;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import it.unive.lisa.analysis.ScopeToken;

public abstract class SlidingStackToken<T> implements ContextSensitivityToken {

	private final Map<ScopeToken, Collection<T>> callStacks;

	private boolean lastPush;

	protected SlidingStackToken() {
		callStacks = new HashMap<>();
	}

	protected SlidingStackToken(SlidingStackToken<T> other) {
		callStacks = new HashMap<>(other.callStacks);
	}

	protected void pushStackForToken(ScopeToken c, T stack) {
		lastPush = callStacks.computeIfAbsent(c, tk -> new HashSet<>()).add(stack);
	}

	protected void popStackForToken(ScopeToken c, T stack) {
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
