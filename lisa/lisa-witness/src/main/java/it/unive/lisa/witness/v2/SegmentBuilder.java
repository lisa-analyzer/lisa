package it.unive.lisa.witness.v2;

import it.unive.lisa.witness.v2.violation.Action;
import it.unive.lisa.witness.v2.violation.Constraint;
import it.unive.lisa.witness.v2.violation.Segment;
import it.unive.lisa.witness.v2.violation.Waypoint;
import it.unive.lisa.witness.v2.violation.WaypointType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// the waypoints of one segment, in order; the formats follow the language of the task
public final class SegmentBuilder {

	private final Language language;

	private final List<Waypoint> waypoints = new ArrayList<>();

	SegmentBuilder(
			Language language) {
		this.language = Objects.requireNonNull(language, "language");
	}

	public SegmentBuilder assumption(
			Action action,
			Location location,
			String value) {
		return add(WaypointType.ASSUMPTION, action, location, value, null);
	}

	public SegmentBuilder assumption(
			Action action,
			Location location,
			String value,
			int threadId) {
		return add(WaypointType.ASSUMPTION, action, location, value, threadId);
	}

	public SegmentBuilder branching(
			Action action,
			Location location,
			boolean taken) {
		return add(WaypointType.BRANCHING, action, location, String.valueOf(taken), null);
	}

	public SegmentBuilder branching(
			Action action,
			Location location,
			boolean taken,
			int threadId) {
		return add(WaypointType.BRANCHING, action, location, String.valueOf(taken), threadId);
	}

	public SegmentBuilder functionEnter(
			Action action,
			Location location) {
		return add(WaypointType.FUNCTION_ENTER, action, location, null, null);
	}

	public SegmentBuilder functionEnter(
			Action action,
			Location location,
			int threadId) {
		return add(WaypointType.FUNCTION_ENTER, action, location, null, threadId);
	}

	public SegmentBuilder functionReturn(
			Action action,
			Location location,
			String value) {
		return add(WaypointType.FUNCTION_RETURN, action, location, value, null);
	}

	public SegmentBuilder functionReturn(
			Action action,
			Location location,
			String value,
			int threadId) {
		return add(WaypointType.FUNCTION_RETURN, action, location, value, threadId);
	}

	public SegmentBuilder functionReturn(
			Action action,
			Location location,
			String value,
			Format format) {
		waypoints.add(new Waypoint(WaypointType.FUNCTION_RETURN, action, location, Constraint.of(value, format), null));
		return this;
	}

	// a target waypoint is always followed
	public SegmentBuilder target(
			Location location) {
		return add(WaypointType.TARGET, Action.FOLLOW, location, null, null);
	}

	public SegmentBuilder target(
			Location location,
			int threadId) {
		return add(WaypointType.TARGET, Action.FOLLOW, location, null, threadId);
	}

	Segment build() {
		return new Segment(waypoints);
	}

	private SegmentBuilder add(
			WaypointType type,
			Action action,
			Location location,
			String value,
			Integer threadId) {
		waypoints.add(new Waypoint(type, action, location, type.constraint(value, language), threadId));
		return this;
	}
}
