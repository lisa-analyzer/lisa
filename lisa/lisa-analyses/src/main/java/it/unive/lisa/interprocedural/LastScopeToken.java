package it.unive.lisa.interprocedural;

import java.util.Objects;

import it.unive.lisa.analysis.ScopeToken;

/**
 * A context sensitive token representing a single {@link ScopeToken}. The token
 * that is kept is always the last pushed one, enabling the analysis of infinite
 * call chains by lubbing results obtained starting from the same call site,
 * regardless of the call stack. This corresponds to having a
 * {@link KDepthToken} with {@code k = 1}.
 */
public class LastScopeToken extends SlidingStackToken<ScopeToken> {

	private final ScopeToken token;

	private LastScopeToken(ScopeToken token) {
		super();
		this.token = token;
	}

	private LastScopeToken(LastScopeToken parent, ScopeToken token) {
		super(parent);
		this.token = token;
	}

	@Override
	public LastScopeToken empty() {
		return new LastScopeToken(null);
	}

	@Override
	public LastScopeToken pushToken(ScopeToken c) {
		LastScopeToken res = new LastScopeToken(this, c);
		res.pushStackForToken(c, res.token);
		return res;
	}

	@Override
	public LastScopeToken popToken(ScopeToken c) {
		LastScopeToken res = new LastScopeToken(this, null);
		res.popStackForToken(c, token);
		return res;
	}

	/**
	 * Return an empty token.
	 * 
	 * @return an empty token
	 */
	public static LastScopeToken getSingleton() {
		return new LastScopeToken(null);
	}

	@Override
	public String toString() {
		return token == null ? "<empty>" : token.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		LastScopeToken that = (LastScopeToken) o;
		return Objects.equals(token, that.token);
	}

	@Override
	public int hashCode() {
		return Objects.hash(token);
	}

}
