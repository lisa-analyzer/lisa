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
public class LastCallToken implements ContextSensitivityToken {

	private final CFGCall call;

	private LastCallToken(CFGCall call) {
		this.call = call;
	}

	@Override
	public LastCallToken empty() {
		return new LastCallToken(null);
	}

	@Override
	public LastCallToken pushCall(CFGCall c) {
		return new LastCallToken(c);
	}

	@Override
	public LastCallToken popCall(CFGCall c) {
		return new LastCallToken(null);
	}

	/**
	 * Return an empty token.
	 * 
	 * @return an empty token
	 */
	public static LastCallToken getSingleton() {
		return new LastCallToken(null);
	}

	@Override
	public String toString() {
		return call == null ? "<empty>" : "[" + call.getLocation().toString() + "]";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		LastCallToken that = (LastCallToken) o;
		return Objects.equals(call, that.call);
	}

	@Override
	public int hashCode() {
		return Objects.hash(call);
	}
}
