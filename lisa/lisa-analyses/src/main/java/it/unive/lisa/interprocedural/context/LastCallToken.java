package it.unive.lisa.interprocedural.context;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.program.cfg.statement.call.CFGCall;

/**
 * A context sensitive token representing a single {@link ScopeToken}. The token
 * that is kept is always the last pushed one, enabling the analysis of infinite
 * call chains by lubbing results obtained starting from the same call site,
 * regardless of the call stack. This corresponds to having a
 * {@link KDepthToken} with {@code k = 1}.
 */
public class LastCallToken
		implements
		ContextSensitivityToken {

	private final CFGCall call;

	private LastCallToken(
			CFGCall call) {
		this.call = call;
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
		return call == null
				? "<empty>"
				: "[" + call.getLocation().toString() + "]";
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LastCallToken other = (LastCallToken) obj;
		if (call == null) {
			if (other.call != null)
				return false;
		} else if (!call.equals(other.call))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		if (call == null)
			result = prime * result;
		else
			// we use the hashcode of the location as the hashcode of the
			// call is based on the ones of its targets, and a CFG hashcode
			// is not consistent between executions - this is a problem as
			// this object's hashcode is used as suffix in some filenames
			result = prime * result + call.getLocation().hashCode();
		return result;
	}

	@Override
	public ContextSensitivityToken startingId() {
		return getSingleton();
	}

	@Override
	public boolean isStartingId() {
		return call == null;
	}

	@Override
	public ContextSensitivityToken push(
			CFGCall c) {
		return new LastCallToken(c);
	}

}
