package it.unive.lisa.witness.v2.violation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import it.unive.lisa.witness.v2.Format;
import it.unive.lisa.witness.v2.InvalidWitnessException;
import it.unive.lisa.witness.v2.Location;
import java.util.Objects;

/**
 * A waypoint of a segment, a point of an execution identified by its
 * {@link WaypointType}, its location and, for some types, a {@link Constraint}.
 * Its {@link Action} tells whether the execution must pass it, avoid it or pass
 * it infinitely often. The thread id is optional, and identifies the thread
 * executing the waypoint in a concurrent program.
 * 
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonTypeName("waypoint")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "type", "action", "constraint", "location", "thread_id" })
public final class Waypoint {

	/**
	 * The type of the waypoint.
	 */
	private final WaypointType type;

	/**
	 * The action of the waypoint.
	 */
	private final Action action;

	/**
	 * The location of the waypoint.
	 */
	private final Location location;

	/**
	 * The constraint of the waypoint, or {@code null} if it has none.
	 */
	private final Constraint constraint;

	/**
	 * The thread executing the waypoint, or {@code null} if it is not given.
	 */
	private final Integer threadId;

	/**
	 * Builds the waypoint.
	 * 
	 * @param type       the type of the waypoint
	 * @param action     the action of the waypoint
	 * @param location   the location of the waypoint
	 * @param constraint the constraint of the waypoint, or {@code null} if it
	 *                       has none
	 * @param threadId   the thread executing the waypoint, or {@code null} if
	 *                       it is not given
	 * 
	 * @throws InvalidWitnessException if the thread id is negative, or if the
	 *                                     action or the constraint are not
	 *                                     allowed for the type
	 */
	@JsonCreator
	public Waypoint(
			@JsonProperty(value = "type", required = true) WaypointType type,
			@JsonProperty(value = "action", required = true) Action action,
			@JsonProperty(value = "location", required = true) Location location,
			@JsonProperty("constraint") Constraint constraint,
			@JsonProperty("thread_id") Integer threadId) {
		this.type = Objects.requireNonNull(type, "type");
		this.action = Objects.requireNonNull(action, "action");
		this.location = Objects.requireNonNull(location, "location");
		this.constraint = constraint;
		this.threadId = threadId;
		if (threadId != null && threadId < 0)
			throw new InvalidWitnessException("The thread id must not be negative: " + threadId);
		type.check(action, constraint);
	}

	/**
	 * Builds a followed function return waypoint.
	 * 
	 * @param location the location of the call
	 * @param value    the constraint on the returned value
	 * @param format   the format of the constraint, which must be extended
	 * 
	 * @return the waypoint
	 * 
	 * @throws InvalidWitnessException if the format is not extended
	 */
	public static Waypoint functionReturn(
			Location location,
			String value,
			Format format) {
		return new Waypoint(WaypointType.FUNCTION_RETURN, Action.FOLLOW, location, Constraint.of(value, format), null);
	}

	/**
	 * Builds a followed assumption waypoint.
	 * 
	 * @param location the location of the waypoint
	 * @param value    the expression that holds at the location
	 * @param format   the format of the expression, which must not be extended
	 * 
	 * @return the waypoint
	 * 
	 * @throws InvalidWitnessException if the format is extended
	 */
	public static Waypoint assumption(
			Location location,
			String value,
			Format format) {
		return new Waypoint(WaypointType.ASSUMPTION, Action.FOLLOW, location, Constraint.of(value, format), null);
	}

	/**
	 * Builds a followed branching waypoint.
	 * 
	 * @param location the location of the condition
	 * @param taken    whether the condition evaluates to {@code true}
	 * 
	 * @return the waypoint
	 */
	public static Waypoint branching(
			Location location,
			boolean taken) {
		return new Waypoint(WaypointType.BRANCHING, Action.FOLLOW, location,
				Constraint.withoutFormat(String.valueOf(taken)), null);
	}

	/**
	 * Builds a followed function enter waypoint.
	 * 
	 * @param location the location of the call
	 * 
	 * @return the waypoint
	 */
	public static Waypoint functionEnter(
			Location location) {
		return new Waypoint(WaypointType.FUNCTION_ENTER, Action.FOLLOW, location, null, null);
	}

	/**
	 * Builds a target waypoint, which is always followed.
	 * 
	 * @param location the location of the violation
	 * 
	 * @return the waypoint
	 */
	public static Waypoint target(
			Location location) {
		return new Waypoint(WaypointType.TARGET, Action.FOLLOW, location, null, null);
	}

	/**
	 * Returns the type of the waypoint.
	 * 
	 * @return the type
	 */
	@JsonProperty("type")
	public WaypointType getType() {
		return type;
	}

	/**
	 * Returns the action of the waypoint.
	 * 
	 * @return the action
	 */
	@JsonProperty("action")
	public Action getAction() {
		return action;
	}

	/**
	 * Returns the location of the waypoint.
	 * 
	 * @return the location
	 */
	@JsonProperty("location")
	public Location getLocation() {
		return location;
	}

	/**
	 * Returns the constraint of the waypoint.
	 * 
	 * @return the constraint, or {@code null} if the waypoint has none
	 */
	@JsonProperty("constraint")
	public Constraint getConstraint() {
		return constraint;
	}

	/**
	 * Returns the thread executing the waypoint.
	 * 
	 * @return the thread id, or {@code null} if it is not given
	 */
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
