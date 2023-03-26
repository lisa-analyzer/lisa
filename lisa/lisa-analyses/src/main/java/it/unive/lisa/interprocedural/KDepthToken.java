package it.unive.lisa.interprocedural;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
		if (oldlen < k) {
			this.calls = new ArrayList<>(oldlen + 1);
			tokens.stream().forEach(this.calls::add);
			this.calls.add(newToken);
		} else {
			this.calls = new ArrayList<>(k);
			// we only keep the last k-1 elements
			tokens.stream().skip(oldlen - k + 1).forEach(this.calls::add);
			this.calls.add(newToken);
		}
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
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		KDepthToken other = (KDepthToken) obj;
		// we ignore k as it does not matter for equality
		if (calls == null) {
			if (other.calls != null)
				return false;
		} else if (!calls.equals(other.calls))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		// we ignore k as it does not matter for equality
		final int prime = 31;
		int result = 1;

		if (calls == null)
			result = prime * result;
		else
			for (CFGCall call : calls)
				// we use the hashcode of the location as the hashcode of the
				// call is based on the ones of its targets, and a CFG hashcode
				// is not consistent between executions - this is a problem as
				// this object's hashcode is used as suffix in some filenames
				result = prime * result + call.getLocation().hashCode();
		return result;
	}

	@Override
	public ScopeId startingId() {
		return getSingleton(k);
	}

	@Override
	public boolean isStartingId() {
		return calls.isEmpty();
	}

	@Override
	public ContextSensitivityToken pushOnStack(List<CFGCall> stack, CFGCall c) {
		return new KDepthToken(k, stack, c);
	}

	@Override
	public List<CFGCall> getKnownCalls() {
		return calls;
	}
}
