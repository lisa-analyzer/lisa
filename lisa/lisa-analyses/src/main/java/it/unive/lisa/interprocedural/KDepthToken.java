package it.unive.lisa.interprocedural;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.util.collections.CollectionUtilities;

/**
 * A context sensitive token representing an entire call chain up to a fixed
 * length {@code k}, specified in the singleton creation
 * ({@link #getSingleton(int)}).
 */
public class KDepthToken implements ContextSensitivityToken {

	private final List<CFGCall> calls;

	private final int k;

	private KDepthToken(int k) {
		this.k = k;
		this.calls = Collections.emptyList();
	}

	private KDepthToken(int k, List<CFGCall> tokens, CFGCall newToken) {
		this.k = k;
		int oldlen = tokens.size();
		if (oldlen == k) {
			this.calls = new ArrayList<>(k);
			// we skip the oldest one
			tokens.stream().skip(1).forEach(this.calls::add);
			this.calls.add(newToken);
		} else {
			this.calls = new ArrayList<>(tokens.size() + 1);
			tokens.stream().forEach(this.calls::add);
			this.calls.add(newToken);
		}
	}

	private KDepthToken(int k, List<CFGCall> tokens) {
		this.k = k;
		int oldsize = tokens.size();
		if (oldsize == 1)
			this.calls = Collections.emptyList();
		else {
			this.calls = new ArrayList<>(oldsize - 1);
			tokens.stream().limit(oldsize - 1).forEach(this.calls::add);
		}
	}

	@Override
	public ContextSensitivityToken empty() {
		return new KDepthToken(k);
	}

	@Override
	public ContextSensitivityToken pushCall(CFGCall c) {
		return new KDepthToken(k, calls, c);
	}

	@Override
	public ContextSensitivityToken popCall(CFGCall c) {
		if (calls.isEmpty())
			return this;
		return new KDepthToken(k, calls);
	}

	/**
	 * Return an empty token.
	 * 
	 * @param k the maximum depth
	 * 
	 * @return an empty token
	 */
	public static KDepthToken getSingleton(int k) {
		return new KDepthToken(k);
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
		// we ignore k as it does not matter for equality
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		KDepthToken that = (KDepthToken) o;
		return Objects.equals(calls, that.calls);
	}

	@Override
	public int hashCode() {
		// we ignore k as it does not matter for equality
		return Objects.hashCode(calls);
	}
}
