package it.unive.lisa.interprocedural;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import it.unive.lisa.program.cfg.statement.call.CFGCall;

/**
 * A context sensitive token representing an entire call chain up to a fixed
 * length {@code k}, specified in the singleton creation
 * ({@link #getSingleton(int)}).
 */
public class KDepthToken extends SlidingStackToken<List<CFGCall>> {

	private final List<CFGCall> tokens;

	private final int k;

	private KDepthToken(int k) {
		super();
		this.k = k;
		this.tokens = Collections.emptyList();
	}

	private KDepthToken(KDepthToken parent, int k, List<CFGCall> tokens, CFGCall newToken) {
		super(parent);
		this.k = k;
		int oldlen = tokens.size();
		if (oldlen == k) {
			this.tokens = new ArrayList<>(k);
			// we skip the oldest one
			tokens.stream().skip(1).forEach(this.tokens::add);
			this.tokens.add(newToken);
		} else {
			this.tokens = new ArrayList<>(tokens.size() + 1);
			tokens.stream().forEach(this.tokens::add);
			this.tokens.add(newToken);
		}
	}

	private KDepthToken(KDepthToken parent, int k, List<CFGCall> tokens) {
		super(parent);
		this.k = k;
		int oldsize = tokens.size();
		if (oldsize == 1)
			this.tokens = Collections.emptyList();
		else {
			this.tokens = new ArrayList<>(oldsize - 1);
			tokens.stream().limit(oldsize - 1).forEach(this.tokens::add);
		}
	}

	@Override
	public ContextSensitivityToken empty() {
		return new KDepthToken(k);
	}

	@Override
	public ContextSensitivityToken pushCall(CFGCall c) {
		KDepthToken res = new KDepthToken(this, k, tokens, c);
		res.registerCallStack(c, res.tokens);
		return res;
	}

	@Override
	public ContextSensitivityToken popCall(CFGCall c) {
		if (tokens.isEmpty())
			return this;
		KDepthToken res = new KDepthToken(this, k, tokens);
		res.unregisterCallStack(c, tokens);
		return res;
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
		return tokens.toString();
	}

	@Override
	public boolean equals(Object o) {
		// we ignore k as it does not matter for equality
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		KDepthToken that = (KDepthToken) o;
		return Objects.equals(tokens, that.tokens);
	}

	@Override
	public int hashCode() {
		// we ignore k as it does not matter for equality
		return Objects.hashCode(tokens);
	}
}
