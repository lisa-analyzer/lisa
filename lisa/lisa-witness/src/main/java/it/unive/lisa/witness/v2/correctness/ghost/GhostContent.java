package it.unive.lisa.witness.v2.correctness.ghost;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import it.unive.lisa.witness.v2.InvalidWitnessException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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
		Set<String> declared = new HashSet<>();
		for (GhostVariable variable : this.ghostVariables)
			if (!declared.add(variable.getName()))
				throw new InvalidWitnessException("The ghost variable " + variable.getName() + " is declared twice");
		for (GhostUpdate update : this.ghostUpdates)
			for (GhostAssignment assignment : update.getUpdates())
				if (!declared.contains(assignment.getVariable()))
					throw new InvalidWitnessException(
							"The ghost update at " + update.getLocation() + " assigns the undeclared variable "
									+ assignment.getVariable());
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
