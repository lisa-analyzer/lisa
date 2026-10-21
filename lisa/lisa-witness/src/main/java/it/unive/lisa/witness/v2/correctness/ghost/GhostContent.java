package it.unive.lisa.witness.v2.correctness.ghost;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import java.util.Objects;

@JsonPropertyOrder({ "ghost_variables", "ghost_updates" })
public final class GhostContent {

	private final List<GhostVariable> ghostVariables;

	private final List<GhostUpdate> ghostUpdates;

	@JsonCreator
	public GhostContent(
			@JsonProperty(value = "ghost_variables", required = true) List<GhostVariable> ghostVariables,
			@JsonProperty(value = "ghost_updates", required = true) List<GhostUpdate> ghostUpdates) {
		this.ghostVariables = List.copyOf(Objects.requireNonNull(ghostVariables, "ghostVariables"));
		this.ghostUpdates = List.copyOf(Objects.requireNonNull(ghostUpdates, "ghostUpdates"));
	}

	@JsonProperty("ghost_variables")
	public List<GhostVariable> getGhostVariables() {
		return ghostVariables;
	}

	@JsonProperty("ghost_updates")
	public List<GhostUpdate> getGhostUpdates() {
		return ghostUpdates;
	}

	@Override
	public int hashCode() {
		return Objects.hash(ghostVariables, ghostUpdates);
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		GhostContent other = (GhostContent) obj;
		return ghostVariables.equals(other.ghostVariables) && ghostUpdates.equals(other.ghostUpdates);
	}
}
