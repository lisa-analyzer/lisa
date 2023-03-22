package it.unive.lisa.interprocedural;

import java.util.Objects;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.program.cfg.statement.call.CFGCall;

/**
 * A context sensitive token representing a single {@link ScopeToken}. The token
 * that is kept is always the last pushed one, enabling the analysis of infinite
 * call chains by lubbing results obtained starting from the same call site,
 * regardless of the call stack. This corresponds to having a
 * {@link KDepthToken} with {@code k = 1}.
 */
public class LastScopeToken extends SlidingStackToken<CFGCall> {

	private final CFGCall token;

	private LastScopeToken(CFGCall token) {
		super();
		this.token = token;
	}

	private LastScopeToken(LastScopeToken parent, CFGCall token) {
		super(parent);
		this.token = token;
	}

	@Override
	public LastScopeToken empty() {
		return new LastScopeToken(null);
	}

	@Override
	public LastScopeToken pushCall(CFGCall c) {
		LastScopeToken res = new LastScopeToken(this, c);
		res.registerCallStack(c, res.token);
		return res;
	}

	@Override
	public LastScopeToken popCall(CFGCall c) {
		LastScopeToken res = new LastScopeToken(this, null);
		res.unregisterCallStack(c, token);
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
