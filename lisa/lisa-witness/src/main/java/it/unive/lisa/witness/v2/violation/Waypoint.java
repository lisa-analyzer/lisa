package it.unive.lisa.witness.v2.violation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import it.unive.lisa.witness.v2.Format;
import it.unive.lisa.witness.v2.InvalidWitnessException;
import it.unive.lisa.witness.v2.Location;
import java.util.Objects;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonTypeName("waypoint")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "type", "action", "constraint", "location", "thread_id" })
public final class Waypoint {

	private final WaypointType type;

	private final Action action;

	private final Location location;

	private final Constraint constraint;

	private final Integer threadId;

	public Waypoint(
			WaypointType type,
			Action action,
			Location location,
			Constraint constraint,
			Integer threadId) {
		this.type = Objects.requireNonNull(type, "type");
		this.action = Objects.requireNonNull(action, "action");
		this.location = Objects.requireNonNull(location, "location");
		this.constraint = constraint;
		this.threadId = threadId;
		if (threadId != null && threadId < 0)
			throw new InvalidWitnessException("The thread id must not be negative: " + threadId);
		type.check(action, constraint);
	}

	public static Waypoint functionReturn(
			Location location,
			String value,
			Format format) {
		return new Waypoint(WaypointType.FUNCTION_RETURN, Action.FOLLOW, location, Constraint.of(value, format), null);
	}

	public static Waypoint target(
			Location location) {
		return new Waypoint(WaypointType.TARGET, Action.FOLLOW, location, null, null);
	}

	@JsonProperty("type")
	public WaypointType getType() {
		return type;
	}

	@JsonProperty("action")
	public Action getAction() {
		return action;
	}

	@JsonProperty("location")
	public Location getLocation() {
		return location;
	}

	@JsonProperty("constraint")
	public Constraint getConstraint() {
		return constraint;
	}

	@JsonProperty("thread_id")
	public Integer getThreadId() {
		return threadId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, action, location, constraint, threadId);
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		Waypoint other = (Waypoint) obj;
		return type == other.type && action == other.action && location.equals(other.location)
				&& Objects.equals(constraint, other.constraint) && Objects.equals(threadId, other.threadId);
	}

	@Override
	public String toString() {
		return action + " " + type + " at " + location + (constraint == null ? "" : ": " + constraint);
	}
}
