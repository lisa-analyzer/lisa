package it.unive.lisa.interprocedural;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.util.collections.CollectionUtilities;

/**
 * A context sensitive token representing an entire call chain until a recursion
 * is encountered. This corresponds to having an unlimited {@link KDepthToken},
 * that only stops when a token that is already part of the chain is pushed.
 */
public class RecursionFreeToken implements ContextSensitivityToken {

	private static final RecursionFreeToken SINGLETON = new RecursionFreeToken();

	private final List<CFGCall> calls;

	private RecursionFreeToken() {
		calls = Collections.emptyList();
	}

	private RecursionFreeToken(List<CFGCall> tokens, CFGCall newToken) {
		this.calls = new ArrayList<>(tokens.size() + 1);
		tokens.stream().forEach(this.calls::add);
		this.calls.add(newToken);
	}

	private RecursionFreeToken(List<CFGCall> tokens) {
		int oldsize = tokens.size();
		if (oldsize == 1)
			this.calls = Collections.emptyList();
		else {
			this.calls = new ArrayList<>(oldsize - 1);
			tokens.stream().limit(oldsize - 1).forEach(this.calls::add);
		}
	}

	private RecursionFreeToken(RecursionFreeToken other) {
		calls = other.calls;
	}

	@Override
	public ContextSensitivityToken empty() {
		return new RecursionFreeToken();
	}

	@Override
	public ContextSensitivityToken pushCall(CFGCall c) {
		// we try to prevent recursions here: it's better
		// to look for them starting from the end of the array
		for (int i = calls.size() - 1; i >= 0; i--)
			if (calls.get(i).equals(c))
				return new RecursionFreeToken(this);
		return new RecursionFreeToken(calls, c);
	}

	@Override
	public ContextSensitivityToken popCall(CFGCall c) {
		if (calls.isEmpty())
			return this;
		return new RecursionFreeToken(calls);
	}

	/**
	 * Return an empty token.
	 * 
	 * @return an empty token
	 */
	public static RecursionFreeToken getSingleton() {
		return SINGLETON;
	}

	@Override
	public String toString() {
		if (calls.isEmpty())
			return "<empty>";
		return "[" + calls.stream().map(call -> call.getLocation())
				.collect(new CollectionUtilities.StringCollector<>(", ")) + "]";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		RecursionFreeToken that = (RecursionFreeToken) o;
		return Objects.equals(calls, that.calls);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(calls);
	}
}
