package it.unive.lisa.witness.v2.violation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.unive.lisa.witness.v2.InvalidWitnessException;
import java.util.List;
import java.util.Objects;

public final class Segment {

	private final List<Waypoint> waypoints;

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

	public static Segment of(
			Waypoint waypoint) {
		return new Segment(List.of(waypoint));
	}

	@JsonProperty("segment")
	public List<Waypoint> getWaypoints() {
		return waypoints;
	}

	@JsonIgnore
	public boolean isFinal() {
		return waypoints.stream().anyMatch(w -> w.getType() == WaypointType.TARGET);
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
