package it.unive.lisa.interprocedural;

import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.util.collections.CollectionUtilities;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
}
