package it.unive.lisa.interprocedural.inlining;

import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.program.cfg.fixpoints.CompoundState;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.util.collections.CollectionUtilities;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

/**
 * The context sensitivity token used by {@link InliningAnalysis}: a
 * {@link ScopeId} that keeps track of the whole call stack and of the entry
 * state of each stack frame, so that every distinct call stack yields a
 * distinct context.
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

	private CallStackId(
			CallStackId<A> source,
			int toPop) {
		this.calls = new ArrayList<>(source.calls.size() - toPop);
		for (int i = 0; i < source.calls.size() - toPop; i++)
			this.calls.add(source.calls.get(i));
	}

	/**
	 * Yields the number of calls in this call stack.
	 * 
	 * @return the number of calls in this call stack
	 */
	public int size() {
		return calls.size();
	}

	/**
	 * Yields the call at the given index, counting from the end of the stack,
	 * with its entry state.
	 * 
	 * @param index the index of the call to retrieve, counting from the end of
	 *                  the stack
	 * 
	 * @return the call at the given index, counting from the end of the stack
	 */
	public Pair<CFGCall, AnalysisState<A>> getCallFromEnd(
			int index) {
		return calls.get(calls.size() - index - 1);
	}

	/**
	 * Yields the call at the given index, counting from the start of the stack,
	 * with its entry state.
	 * 
	 * @param index the index of the call to retrieve, counting from the start
	 *                  of the stack
	 * 
	 * @return the call at the given index, counting from the start of the stack
	 */
	public Pair<CFGCall, AnalysisState<A>> getCall(
			int index) {
		return calls.get(index);
	}

	/**
	 * Yields all the calls in this id, in the order they appear in it (i.e.,
	 * from less recent to most recent), with their entry state.
	 * 
	 * @return the calls
	 */
	public List<Pair<CFGCall, AnalysisState<A>>> getCalls() {
		return calls;
	}

	/**
	 * Yields all the calls in this id, in the reverse order w.r.t. their
	 * appearence (i.e., from most recent to less recent), with their entry
	 * state.
	 * 
	 * @return the calls
	 */
	public List<Pair<CFGCall, AnalysisState<A>>> getReversedCalls() {
		List<Pair<CFGCall, AnalysisState<A>>> calls = new ArrayList<>(this.calls);
		Collections.reverse(calls);
		return calls;
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
			CompoundState<A> state) {
		return new CallStackId<>(this, c, state.postState);
	}

	/**
	 * Pops the specified amount of entries from this call stack id.
	 *
	 * @param amount the number of entries to pop
	 * 
	 * @return a new id with the specified amount of entries popped
	 */
	public CallStackId<A> pop(
			int amount) {
		return new CallStackId<>(this, amount);
	}

}
