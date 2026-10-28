package it.unive.lisa.witness.v2.correctness.ghost;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import it.unive.lisa.witness.v2.InvalidWitnessException;
import it.unive.lisa.witness.v2.Location;
import java.util.List;
import java.util.Objects;

/**
 * An update of ghost variables, made of the assignments performed at a
 * location.
 * 
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
@JsonPropertyOrder({ "location", "updates" })
public final class GhostUpdate {

	/**
	 * The location of the update.
	 */
	private final Location location;

	/**
	 * The assignments of the update, in order.
	 */
	private final List<GhostAssignment> updates;

	/**
	 * Builds the update.
	 * 
	 * @param location the location of the update
	 * @param updates  the assignments of the update, in order
	 * 
	 * @throws InvalidWitnessException if there is no assignment
	 */
	@JsonCreator
	public GhostUpdate(
			@JsonProperty(value = "location", required = true) Location location,
			@JsonProperty(value = "updates", required = true) List<GhostAssignment> updates) {
		this.location = Objects.requireNonNull(location, "location");
		this.updates = List.copyOf(Objects.requireNonNull(updates, "updates"));
		if (this.updates.isEmpty())
			throw new InvalidWitnessException("A ghost update at " + location + " needs at least one assignment");
	}

	/**
	 * Returns the location of the update.
	 * 
	 * @return the location
	 */
	@JsonProperty("location")
	public Location getLocation() {
		return location;
	}

	/**
	 * Returns the assignments of the update, in order.
	 * 
	 * @return the assignments, which cannot be modified
	 */
	@JsonProperty("updates")
	public List<GhostAssignment> getUpdates() {
		return updates;
	}

	@Override
	public int hashCode() {
		return Objects.hash(location, updates);
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		GhostUpdate other = (GhostUpdate) obj;
		return location.equals(other.location) && updates.equals(other.updates);
	}

	@Override
	public String toString() {
		return "at " + location + ": " + updates;
	}
}
