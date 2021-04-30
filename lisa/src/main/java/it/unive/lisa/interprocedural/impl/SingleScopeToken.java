package it.unive.lisa.interprocedural.impl;

import java.util.Objects;

import it.unive.lisa.analysis.ScopeToken;

/**
 * A context sensitive token representing a single {@link ScopeToken}. 
 */
public class SingleScopeToken implements ContextSensitiveToken {

	private static final SingleScopeToken singleton = new SingleScopeToken(null);

	private final ScopeToken token;

	private SingleScopeToken(ScopeToken token) {
		this.token = token;
	}

	@Override
	public ContextSensitiveToken empty() {
		return new SingleScopeToken(null);
	}

	@Override
	public ContextSensitiveToken pushToken(ScopeToken c) {
		return new SingleScopeToken(c);
	}

	/**
	 * Return an empty token.
	 * 
	 * @return an empty token
	 */
	public static SingleScopeToken getSingleton() {
		return singleton;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		SingleScopeToken that = (SingleScopeToken) o;
		return Objects.equals(token, that.token);
	}

	@Override
	public int hashCode() {
		return Objects.hash(token);
	}

}
