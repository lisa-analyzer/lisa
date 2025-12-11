package it.unive.lisa.interprocedural.inlining;

import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.util.collections.CollectionUtilities;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A {@link ScopeId} that keeps track of the whole call stack and of the entry
 * state of each stack frame.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractLattice} handled by the analysis
 */
public class CallStackId<A extends AbstractLattice<A>>
		implements
		ScopeId<A> {

	private final List<Pair<CFGCall, AnalysisState<A>>> calls;

	private CallStackId() {
		this.calls = Collections.emptyList();
	}

	private CallStackId(
			CallStackId<A> source,
			CFGCall newToken,
			AnalysisState<A> state) {
		this.calls = new ArrayList<>(source.calls.size() + 1);
		source.calls.forEach(this.calls::add);
		this.calls.add(Pair.of(newToken, state));
	}

	/**
	 * Creates an empty scope id with no calls in it.
	 * 
	 * @param <A> the type of {@link AbstractLattice} handled by the analysis
	 * 
	 * @return an empty token
	 */
	public static <A extends AbstractLattice<A>> CallStackId<A> create() {
		return new CallStackId<>();
	}

	@Override
	public String toString() {
		if (calls.isEmpty())
			return "<empty>";
		return "["
				+ calls.stream().map(call -> call.getLeft().getLocation())
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
		CallStackId<?> other = (CallStackId<?>) obj;
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
			for (Pair<CFGCall, AnalysisState<A>> call : calls)
				// we use the hashcode of the location as the hashcode of the
				// call is based on the ones of its targets, and a CFG hashcode
				// is not consistent between executions - this is a problem as
				// this object's hashcode is used as suffix in some filenames
				result = prime * result + call.getLeft().getLocation().hashCode();
		return result;
	}

	@Override
	public CallStackId<A> startingId() {
		return create();
	}

	@Override
	public boolean isStartingId() {
		return calls.isEmpty();
	}

	@Override
	public CallStackId<A> push(
			CFGCall c,
			AnalysisState<A> state) {
		return new CallStackId<>(this, c, state);
	}

}
