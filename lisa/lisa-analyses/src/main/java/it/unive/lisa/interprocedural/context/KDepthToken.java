package it.unive.lisa.interprocedural.context;

import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.util.collections.CollectionUtilities;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A context sensitive token representing the last {@code k} elements of the
 * call chain, with {@code k} being specified in the singleton creation
 * ({@link #create(int)}).
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractLattice} handled by the analysis
 */
public class KDepthToken<A extends AbstractLattice<A>>
		implements
		ScopeId<A> {

	private final List<CFGCall> calls;

	private final int k;

	private KDepthToken(
			int k) {
		this.k = k;
		this.calls = Collections.emptyList();
	}

	private KDepthToken(
			int k,
			KDepthToken<A> source,
			CFGCall newToken) {
		this.k = k;

		if (k == 0) {
			// k = 0 -> insensitive
			this.calls = source.calls;
			return;
		}

		int oldlen = source.calls.size();
		if (k < 0 || oldlen < k) {
			// k < 0 -> full stack
			this.calls = new ArrayList<>(oldlen + 1);
			source.calls.forEach(this.calls::add);
			this.calls.add(newToken);
		} else {
			this.calls = new ArrayList<>(k);
			// we only keep the last k-1 elements
			source.calls.stream().skip(oldlen - k + 1).forEach(this.calls::add);
			this.calls.add(newToken);
		}
	}

	/**
	 * Creates an empty token that can track at most {@code k} calls.
	 * 
	 * @param <A> the type of {@link AbstractLattice} handled by the analysis
	 * @param k   the maximum depth
	 * 
	 * @return an empty token
	 */
	public static <A extends AbstractLattice<A>> KDepthToken<A> create(
			int k) {
		return new KDepthToken<>(k);
	}

	@Override
	public String toString() {
		if (calls.isEmpty())
			return "<empty>";
		return "["
				+ calls.stream().map(call -> call.getLocation())
						.collect(new CollectionUtilities.StringCollector<>(", "))
				+ "]";
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
		KDepthToken<?> other = (KDepthToken<?>) obj;
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
	public KDepthToken<A> startingId() {
		return create(k);
	}

	@Override
	public boolean isStartingId() {
		return calls.isEmpty();
	}

	@Override
	public KDepthToken<A> push(
			CFGCall c,
			AnalysisState<A> state) {
		return new KDepthToken<>(k, this, c);
	}

}
