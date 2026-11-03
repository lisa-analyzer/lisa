package it.unive.lisa.triage.model;

import it.unive.lisa.program.cfg.statement.call.CFGCall;
import java.util.Collections;
import java.util.List;

/**
 * A call to cfgs reached while replaying a node (see
 * {@link ReplayInterprocedural}), as recorded in its {@link NodeTransitions}.
 * Calls are discovered by replaying the semantics of the node, and not
 * syntactically, since frontends might synthesize calls inside the semantics of
 * a statement (e.g., the call to a constructor performed by an object
 * creation). A call is <i>expandable</i> if the code of its callees can be part
 * of the model, in frames entered from the frame of the node (see
 * {@link ReplayInterprocedural} for the conditions), and <i>expanded</i> if
 * the replay entered (or resumed after) its callees instead of summarizing
 * them.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class ReplayedCall {

	private final CFGCall call;

	private final List<Frame> callees;

	private final String obstacle;

	private final boolean expanded;

	/**
	 * Builds the record of a call.
	 *
	 * @param call     the call
	 * @param callees  the frames of the callees, entered from the frame of the
	 *                     replayed node (one for each target that has a result
	 *                     in the context of the call)
	 * @param obstacle the reason why the call is not expandable, or
	 *                     {@code null} if it is expandable
	 * @param expanded whether or not the replay entered (or resumed after) the
	 *                     callees
	 */
	public ReplayedCall(
			CFGCall call,
			List<Frame> callees,
			String obstacle,
			boolean expanded) {
		this.call = call;
		this.callees = Collections.unmodifiableList(callees);
		this.obstacle = obstacle;
		this.expanded = expanded;
	}

	/**
	 * Yields the call.
	 *
	 * @return the call
	 */
	public CFGCall getCall() {
		return call;
	}

	/**
	 * Yields the frames of the callees of the call, entered from the frame of
	 * the replayed node: the call is expanded if all of them are.
	 *
	 * @return the frames
	 */
	public List<Frame> getCalleeFrames() {
		return callees;
	}

	/**
	 * Yields {@code true} if the code of the callees can be part of the model.
	 *
	 * @return whether or not the call is expandable
	 */
	public boolean isExpandable() {
		return obstacle == null;
	}

	/**
	 * Yields the reason why the call is not expandable, or {@code null} if it
	 * is expandable.
	 *
	 * @return the reason
	 */
	public String getObstacle() {
		return obstacle;
	}

	/**
	 * Yields {@code true} if the replay entered the callees of the call, or
	 * resumed after they returned, instead of summarizing them.
	 *
	 * @return whether or not the call has been expanded
	 */
	public boolean isExpanded() {
		return expanded;
	}

	@Override
	public String toString() {
		return call + (expanded ? " (expanded)" : obstacle == null ? " (expandable)" : " (" + obstacle + ")");
	}
}
