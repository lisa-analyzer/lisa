package it.unive.lisa.interprocedural.context;

import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.util.collections.CollectionUtilities;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A context sensitive token representing an entire call chain, regardless of
 * its length. This corresponds to having an unlimited {@link KDepthToken}, that
 * will thus never merge results for of different calls due to matching
 * contexts.
 */
public class FullStackToken
		implements
		ContextSensitivityToken {

	private static final FullStackToken SINGLETON = new FullStackToken();

	private final List<CFGCall> calls;

	private FullStackToken() {
		calls = Collections.emptyList();
	}

	private FullStackToken(
			FullStackToken source,
			CFGCall newToken) {
		this.calls = new ArrayList<>(source.calls.size() + 1);
		source.calls.forEach(this.calls::add);
		this.calls.add(newToken);
	}

	/**
	 * Return an empty token.
	 * 
	 * @return an empty token
	 */
	public static FullStackToken getSingleton() {
		return SINGLETON;
	}

	@Override
	public String toString() {
		if (calls.isEmpty())
			return "<empty>";
		return "[" + calls.stream().map(call -> call.getLocation())
				.collect(new CollectionUtilities.StringCollector<>(", ")) + "]";
	}

	@Override
	public int hashCode() {
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
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FullStackToken other = (FullStackToken) obj;
		if (calls == null) {
			if (other.calls != null)
				return false;
		} else if (!calls.equals(other.calls))
			return false;
		return true;
	}

	@Override
	public ContextSensitivityToken startingId() {
		return getSingleton();
	}

	@Override
	public boolean isStartingId() {
		return calls.isEmpty();
	}

	@Override
	public ContextSensitivityToken push(
			CFGCall c) {
		return new FullStackToken(this, c);
	}

}
