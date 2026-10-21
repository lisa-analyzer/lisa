package it.unive.lisa.witness.v2;

import it.unive.lisa.witness.v2.metadata.Task;
import it.unive.lisa.witness.v2.violation.Action;
import it.unive.lisa.witness.v2.violation.Constraint;
import it.unive.lisa.witness.v2.violation.Segment;
import it.unive.lisa.witness.v2.violation.Waypoint;
import it.unive.lisa.witness.v2.violation.WaypointType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A builder of one segment of a violation witness, used through
 * {@link ViolationWitnessBuilder#segment(java.util.function.Consumer)}. The
 * waypoints are added in order. The formats of their constraints follow the
 * language of the task, and each location is checked against the input files of
 * the task when it is added. A segment needs exactly one follow or cycle
 * waypoint, preceded by any number of avoid waypoints, or several target
 * waypoints.
 * 
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public final class SegmentBuilder {

	/**
	 * The task of the witness the segment belongs to.
	 */
	private final Task task;

	/**
	 * The waypoints of the segment, in the order they are added.
	 */
	private final List<Waypoint> waypoints = new ArrayList<>();

	/**
	 * Builds the builder of a segment.
	 * 
	 * @param task the task of the witness the segment belongs to
	 */
	SegmentBuilder(
			Task task) {
		this.task = Objects.requireNonNull(task, "task");
	}

	/**
	 * Adds an assumption waypoint, stating that the given expression holds at
	 * the location. The expression uses the plain expression format of the
	 * language.
	 * 
	 * @param action   the action of the waypoint
	 * @param location the location of the waypoint
	 * @param value    the expression that holds at the location
	 * 
	 * @return this builder
	 * 
	 * @throws InvalidWitnessException if the location is not in an input file
	 *                                     of the task, or if the waypoint
	 *                                     breaks a rule of the format
	 */
	public SegmentBuilder assumption(
			Action action,
			Location location,
			String value) {
		return add(WaypointType.ASSUMPTION, action, location, value, null);
	}

	/**
	 * Adds an assumption waypoint executed by the given thread, stating that
	 * the given expression holds at the location. The expression uses the plain
	 * expression format of the language.
	 * 
	 * @param action   the action of the waypoint
	 * @param location the location of the waypoint
	 * @param value    the expression that holds at the location
	 * @param threadId the thread executing the waypoint
	 * 
	 * @return this builder
	 * 
	 * @throws InvalidWitnessException if the location is not in an input file
	 *                                     of the task, or if the waypoint
	 *                                     breaks a rule of the format
	 */
	public SegmentBuilder assumption(
			Action action,
			Location location,
			String value,
			int threadId) {
		return add(WaypointType.ASSUMPTION, action, location, value, threadId);
	}

	/**
	 * Adds a branching waypoint, stating which branch of the condition at the
	 * location is taken.
	 * 
	 * @param action   the action of the waypoint
	 * @param location the location of the condition
	 * @param taken    whether the condition evaluates to {@code true}
	 * 
	 * @return this builder
	 * 
	 * @throws InvalidWitnessException if the location is not in an input file
	 *                                     of the task, or if the waypoint
	 *                                     breaks a rule of the format
	 */
	public SegmentBuilder branching(
			Action action,
			Location location,
			boolean taken) {
		return add(WaypointType.BRANCHING, action, location, String.valueOf(taken), null);
	}

	/**
	 * Adds a branching waypoint executed by the given thread, stating which
	 * branch of the condition at the location is taken.
	 * 
	 * @param action   the action of the waypoint
	 * @param location the location of the condition
	 * @param taken    whether the condition evaluates to {@code true}
	 * @param threadId the thread executing the waypoint
	 * 
	 * @return this builder
	 * 
	 * @throws InvalidWitnessException if the location is not in an input file
	 *                                     of the task, or if the waypoint
	 *                                     breaks a rule of the format
	 */
	public SegmentBuilder branching(
			Action action,
			Location location,
			boolean taken,
			int threadId) {
		return add(WaypointType.BRANCHING, action, location, String.valueOf(taken), threadId);
	}

	/**
	 * Adds a function enter waypoint, stating that the function called at the
	 * location is entered.
	 * 
	 * @param action   the action of the waypoint
	 * @param location the location of the call
	 * 
	 * @return this builder
	 * 
	 * @throws InvalidWitnessException if the location is not in an input file
	 *                                     of the task, or if the waypoint
	 *                                     breaks a rule of the format
	 */
	public SegmentBuilder functionEnter(
			Action action,
			Location location) {
		return add(WaypointType.FUNCTION_ENTER, action, location, null, null);
	}

	/**
	 * Adds a function enter waypoint executed by the given thread, stating that
	 * the function called at the location is entered.
	 * 
	 * @param action   the action of the waypoint
	 * @param location the location of the call
	 * @param threadId the thread executing the waypoint
	 * 
	 * @return this builder
	 * 
	 * @throws InvalidWitnessException if the location is not in an input file
	 *                                     of the task, or if the waypoint
	 *                                     breaks a rule of the format
	 */
	public SegmentBuilder functionEnter(
			Action action,
			Location location,
			int threadId) {
		return add(WaypointType.FUNCTION_ENTER, action, location, null, threadId);
	}

	/**
	 * Adds a function return waypoint, stating a constraint on the value
	 * returned by the call at the location, such as {@code \result == 0}. The
	 * constraint uses the extended expression format of the language.
	 * 
	 * @param action   the action of the waypoint
	 * @param location the location of the call
	 * @param value    the constraint on the returned value
	 * 
	 * @return this builder
	 * 
	 * @throws InvalidWitnessException if the location is not in an input file
	 *                                     of the task, or if the waypoint
	 *                                     breaks a rule of the format
	 */
	public SegmentBuilder functionReturn(
			Action action,
			Location location,
			String value) {
		return add(WaypointType.FUNCTION_RETURN, action, location, value, null);
	}

	/**
	 * Adds a function return waypoint executed by the given thread, stating a
	 * constraint on the value returned by the call at the location, such as
	 * {@code \result == 0}. The constraint uses the extended expression format
	 * of the language.
	 * 
	 * @param action   the action of the waypoint
	 * @param location the location of the call
	 * @param value    the constraint on the returned value
	 * @param threadId the thread executing the waypoint
	 * 
	 * @return this builder
	 * 
	 * @throws InvalidWitnessException if the location is not in an input file
	 *                                     of the task, or if the waypoint
	 *                                     breaks a rule of the format
	 */
	public SegmentBuilder functionReturn(
			Action action,
			Location location,
			String value,
			int threadId) {
		return add(WaypointType.FUNCTION_RETURN, action, location, value, threadId);
	}

	/**
	 * Adds a function return waypoint whose constraint uses the given format,
	 * such as the deprecated {@link Format#ACSL_EXPRESSION} of older C
	 * witnesses.
	 * 
	 * @param action   the action of the waypoint
	 * @param location the location of the call
	 * @param value    the constraint on the returned value
	 * @param format   the format of the constraint
	 * 
	 * @return this builder
	 * 
	 * @throws InvalidWitnessException if the location is not in an input file
	 *                                     of the task, if the format is not
	 *                                     allowed for the language, or if the
	 *                                     waypoint breaks a rule of the format
	 */
	public SegmentBuilder functionReturn(
			Action action,
			Location location,
			String value,
			Format format) {
		return functionReturn(action, location, value, format, null);
	}

	/**
	 * Adds a function return waypoint executed by the given thread, whose
	 * constraint uses the given format.
	 * 
	 * @param action   the action of the waypoint
	 * @param location the location of the call
	 * @param value    the constraint on the returned value
	 * @param format   the format of the constraint
	 * @param threadId the thread executing the waypoint
	 * 
	 * @return this builder
	 * 
	 * @throws InvalidWitnessException if the location is not in an input file
	 *                                     of the task, if the format is not
	 *                                     allowed for the language, or if the
	 *                                     waypoint breaks a rule of the format
	 */
	public SegmentBuilder functionReturn(
			Action action,
			Location location,
			String value,
			Format format,
			int threadId) {
		return functionReturn(action, location, value, format, Integer.valueOf(threadId));
	}

	/**
	 * Adds a function return waypoint whose constraint uses the given format.
	 * 
	 * @param action   the action of the waypoint
	 * @param location the location of the call
	 * @param value    the constraint on the returned value
	 * @param format   the format of the constraint
	 * @param threadId the thread executing the waypoint, or {@code null} if it
	 *                     is not given
	 * 
	 * @return this builder
	 * 
	 * @throws InvalidWitnessException if the location is not in an input file
	 *                                     of the task, if the format is not
	 *                                     allowed for the language, or if the
	 *                                     waypoint breaks a rule of the format
	 */
	private SegmentBuilder functionReturn(
			Action action,
			Location location,
			String value,
			Format format,
			Integer threadId) {
		Entry.checkLocation(task, location);
		task.getLanguage().checkAllows(format, location);
		waypoints.add(
				new Waypoint(WaypointType.FUNCTION_RETURN, action, location, Constraint.of(value, format), threadId));
		return this;
	}

	/**
	 * Adds a target waypoint, marking the location where the specification is
	 * violated. A target waypoint is always followed.
	 * 
	 * @param location the location of the violation
	 * 
	 * @return this builder
	 * 
	 * @throws InvalidWitnessException if the location is not in an input file
	 *                                     of the task
	 */
	public SegmentBuilder target(
			Location location) {
		return add(WaypointType.TARGET, Action.FOLLOW, location, null, null);
	}

	/**
	 * Adds a target waypoint executed by the given thread, marking the location
	 * where the specification is violated. A target waypoint is always
	 * followed.
	 * 
	 * @param location the location of the violation
	 * @param threadId the thread executing the waypoint
	 * 
	 * @return this builder
	 * 
	 * @throws InvalidWitnessException if the location is not in an input file
	 *                                     of the task
	 */
	public SegmentBuilder target(
			Location location,
			int threadId) {
		return add(WaypointType.TARGET, Action.FOLLOW, location, null, threadId);
	}

	/**
	 * Builds the segment made of the waypoints added so far.
	 * 
	 * @return the segment
	 * 
	 * @throws InvalidWitnessException if the waypoints do not form a segment
	 */
	Segment build() {
		return new Segment(waypoints);
	}

	/**
	 * Adds a waypoint whose constraint is built from the given value, in the
	 * format required by the type of the waypoint.
	 * 
	 * @param type     the type of the waypoint
	 * @param action   the action of the waypoint
	 * @param location the location of the waypoint
	 * @param value    the value of the constraint, or {@code null} if the type
	 *                     takes no constraint
	 * @param threadId the thread executing the waypoint, or {@code null} if it
	 *                     is not given
	 * 
	 * @return this builder
	 * 
	 * @throws InvalidWitnessException if the location is not in an input file
	 *                                     of the task, or if the waypoint
	 *                                     breaks a rule of the format
	 */
	private SegmentBuilder add(
			WaypointType type,
			Action action,
			Location location,
			String value,
			Integer threadId) {
		Entry.checkLocation(task, location);
		waypoints.add(new Waypoint(type, action, location, type.constraint(value, task.getLanguage()), threadId));
		return this;
	}
}
