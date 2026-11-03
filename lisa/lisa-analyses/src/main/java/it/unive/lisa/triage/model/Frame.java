package it.unive.lisa.triage.model;

import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import java.util.Objects;

/**
 * A frame of an {@link AlarmModel}: the code of a cfg, analyzed in a given
 * context, reached through a chain of calls from the cfg where the model
 * starts (its root frame). Each frame has its own copy of the locations of its
 * cfg, whose states are the ones computed by the analysis for its context.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public final class Frame {

	private final Frame parent;

	private final CFGCall call;

	private final AnalyzedCFG<?> result;

	private final int depth;

	private Frame(
			Frame parent,
			CFGCall call,
			AnalyzedCFG<?> result) {
		this.parent = parent;
		this.call = call;
		this.result = result;
		this.depth = parent == null ? 0 : parent.depth + 1;
	}

	/**
	 * Builds the root frame of a model.
	 *
	 * @param result the result of the cfg where the model starts
	 *
	 * @return the frame
	 */
	public static Frame root(
			AnalyzedCFG<?> result) {
		return new Frame(null, null, result);
	}

	/**
	 * Builds the frame of a callee of this frame.
	 *
	 * @param call   the call of this frame that reaches the callee
	 * @param callee the result of the callee, in the context of the call
	 *
	 * @return the frame
	 */
	public Frame enter(
			CFGCall call,
			AnalyzedCFG<?> callee) {
		return new Frame(this, call, callee);
	}

	/**
	 * Yields the frame of the caller, or {@code null} if this is the root
	 * frame.
	 *
	 * @return the frame of the caller
	 */
	public Frame getParent() {
		return parent;
	}

	/**
	 * Yields the call that reaches this frame from its parent, or {@code null}
	 * if this is the root frame.
	 *
	 * @return the call
	 */
	public CFGCall getCall() {
		return call;
	}

	/**
	 * Yields the result of the cfg of this frame, in the context of this frame.
	 *
	 * @return the result
	 */
	public AnalyzedCFG<?> getResult() {
		return result;
	}

	/**
	 * Yields the number of calls from the root frame to this one.
	 *
	 * @return the depth
	 */
	public int getDepth() {
		return depth;
	}

	/**
	 * Yields {@code true} if this is the root frame.
	 *
	 * @return whether or not this is the root frame
	 */
	public boolean isRoot() {
		return parent == null;
	}

	@Override
	public int hashCode() {
		// results are identified by their cfg and context
		return Objects.hash(parent, call, result == null ? null : result.getDescriptor(),
				result == null ? null : result.getId());
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		Frame other = (Frame) obj;
		return Objects.equals(parent, other.parent)
				&& Objects.equals(call, other.call)
				&& sameResult(result, other.result);
	}

	private static boolean sameResult(
			AnalyzedCFG<?> first,
			AnalyzedCFG<?> second) {
		if (first == second)
			return true;
		if (first == null || second == null)
			return false;
		return Objects.equals(first.getDescriptor(), second.getDescriptor())
				&& Objects.equals(first.getId(), second.getId());
	}

	@Override
	public String toString() {
		String name = result.getDescriptor().getName();
		return isRoot() ? name : parent + " > " + name;
	}
}
