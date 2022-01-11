package it.unive.lisa.interprocedural;

import it.unive.lisa.analysis.ScopeToken;
import java.util.Objects;

/**
 * A context sensitive token representing a single {@link ScopeToken}.
 */
public class SingleScopeToken implements ContextSensitivityToken {

	private static final SingleScopeToken singleton = new SingleScopeToken(null);

	private final ScopeToken token;

	private SingleScopeToken(ScopeToken token) {
		this.token = token;
	}

	@Override
	public ContextSensitivityToken empty() {
		return new SingleScopeToken(null);
	}

	@Override
	public ContextSensitivityToken pushToken(ScopeToken c) {
		return new SingleScopeToken(c);
	}

	@Override
	public ContextSensitivityToken popToken() {
		return empty();
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
	public String toString() {
		return String.valueOf(token);
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
