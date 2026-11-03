package it.unive.lisa.triage.model;

import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import java.util.Objects;

/**
 * A location of an {@link AlarmModel}: a node of the cfg of a {@link Frame},
 * that is, the program point before the node, in the frame. Nodes containing
 * calls whose callees are part of the model also have locations after the
 * callees return, where the evaluation of the node resumes.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public final class ModelLocation {

	private final Frame frame;

	private final Statement node;

	private final CFGCall resumed;

	/**
	 * Builds the location before the given node.
	 *
	 * @param frame the frame of the location
	 * @param node  the node, a root statement of the cfg of the frame
	 */
	public ModelLocation(
			Frame frame,
			Statement node) {
		this(frame, node, null);
	}

	/**
	 * Builds the location where the evaluation of the given node resumes after
	 * the given call returns.
	 *
	 * @param frame   the frame of the location
	 * @param node    the node, a root statement of the cfg of the frame
	 * @param resumed the call, contained in the node, whose callees returned
	 *                    (can be {@code null}, for the location before the
	 *                    node)
	 */
	public ModelLocation(
			Frame frame,
			Statement node,
			CFGCall resumed) {
		this.frame = frame;
		this.node = node;
		this.resumed = resumed;
	}

	/**
	 * Yields the frame of this location.
	 *
	 * @return the frame
	 */
	public Frame getFrame() {
		return frame;
	}

	/**
	 * Yields the node of this location.
	 *
	 * @return the node
	 */
	public Statement getNode() {
		return node;
	}

	/**
	 * Yields the call after which the evaluation of the node resumes, or
	 * {@code null} if this location is the one before the node.
	 *
	 * @return the call
	 */
	public CFGCall getResumed() {
		return resumed;
	}

	@Override
	public int hashCode() {
		return Objects.hash(frame, node, resumed);
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		ModelLocation other = (ModelLocation) obj;
		return Objects.equals(frame, other.frame)
				&& Objects.equals(node, other.node)
				&& Objects.equals(resumed, other.resumed);
	}

	@Override
	public String toString() {
		String prefix = frame.isRoot() ? "" : "[" + frame + "] ";
		return prefix + node + (resumed == null ? "" : " (after " + resumed + ")");
	}
}
