package it.unive.lisa.witness.v2.violation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.unive.lisa.witness.v2.InvalidWitnessException;
import java.util.List;
import java.util.Objects;

/**
 * A segment of a violation sequence. A segment is made of any number of avoid
 * waypoints followed by exactly one follow waypoint, by several target follow
 * waypoints, or by exactly one cycle waypoint. A segment with a target waypoint
 * is the final segment of its sequence.
 * 
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public final class Segment {

	/**
	 * The waypoints of the segment, in order.
	 */
	private final List<Waypoint> waypoints;

	/**
	 * Builds the segment.
	 * 
	 * @param waypoints the waypoints of the segment, in order
	 * 
	 * @throws InvalidWitnessException if the waypoints do not form a segment
	 */
	@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
	public Segment(
			@JsonProperty(value = "segment", required = true) List<Waypoint> waypoints) {
		this.waypoints = List.copyOf(Objects.requireNonNull(waypoints, "waypoints"));
		if (this.waypoints.isEmpty())
			throw new InvalidWitnessException("A segment needs at least one waypoint");
		Action last = this.waypoints.get(this.waypoints.size() - 1).getAction();
		if (last == Action.AVOID)
			throw new InvalidWitnessException("The last waypoint of a segment must be follow or cycle");
		long follows = this.waypoints.stream().filter(w -> w.getAction() == Action.FOLLOW).count();
		long cycles = this.waypoints.stream().filter(w -> w.getAction() == Action.CYCLE).count();
		boolean followsAreTargets = this.waypoints.stream()
				.filter(w -> w.getAction() == Action.FOLLOW)
				.allMatch(w -> w.getType() == WaypointType.TARGET);
		boolean follow = cycles == 0 && follows == 1;
		boolean multiFollow = cycles == 0 && follows > 1 && followsAreTargets;
		boolean cycle = cycles == 1 && follows == 0;
		if (!follow && !multiFollow && !cycle)
			throw new InvalidWitnessException(
					"A segment must have one follow waypoint, several target follow waypoints, or one cycle waypoint");
	}

	/**
	 * Builds a segment made of a single waypoint.
	 * 
	 * @param waypoint the waypoint, which must be followed or cycled
	 * 
	 * @return the segment
	 * 
	 * @throws InvalidWitnessException if the waypoint is an avoid waypoint
	 */
	public static Segment of(
			Waypoint waypoint) {
		return new Segment(List.of(waypoint));
	}

	/**
	 * Returns the waypoints of the segment, in order.
	 * 
	 * @return the waypoints, which cannot be modified
	 */
	@JsonProperty("segment")
	public List<Waypoint> getWaypoints() {
		return waypoints;
	}

	/**
	 * Returns whether this segment has a target waypoint, that is, whether it
	 * is the final segment of its sequence.
	 * 
	 * @return {@code true} if this segment has a target waypoint
	 */
	@JsonIgnore
	public boolean isFinal() {
		return waypoints.stream().anyMatch(w -> w.getType() == WaypointType.TARGET);
	}

	/**
	 * Returns whether this segment has a cycle waypoint, that is, whether it is
	 * a cycle segment.
	 * 
	 * @return {@code true} if this segment has a cycle waypoint
	 */
	@JsonIgnore
	public boolean isCycle() {
		return waypoints.stream().anyMatch(w -> w.getAction() == Action.CYCLE);
	}

	@Override
	public int hashCode() {
		return waypoints.hashCode();
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		return waypoints.equals(((Segment) obj).waypoints);
	}

	@Override
	public String toString() {
		return waypoints.toString();
	}
}
