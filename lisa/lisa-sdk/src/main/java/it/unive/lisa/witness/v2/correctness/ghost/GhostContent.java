package it.unive.lisa.witness.v2.correctness.ghost;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import it.unive.lisa.witness.v2.InvalidWitnessException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The content of a ghost instrumentation, made of the declared ghost variables
 * and of their updates. Each variable is declared once, and every update
 * assigns declared variables.
 * 
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
@JsonPropertyOrder({ "ghost_variables", "ghost_updates" })
public final class GhostContent {

	/**
	 * The declared ghost variables, in order.
	 */
	private final List<GhostVariable> ghostVariables;

	/**
	 * The updates of the ghost variables, in order.
	 */
	private final List<GhostUpdate> ghostUpdates;

	/**
	 * Builds the content.
	 * 
	 * @param ghostVariables the declared ghost variables, in order
	 * @param ghostUpdates   the updates of the ghost variables, in order
	 * 
	 * @throws InvalidWitnessException if a variable is declared twice, or if an
	 *                                     update assigns an undeclared variable
	 */
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

	/**
	 * Returns the declared ghost variables, in order.
	 * 
	 * @return the variables, which cannot be modified
	 */
	@JsonProperty("ghost_variables")
	public List<GhostVariable> getGhostVariables() {
		return ghostVariables;
	}

	/**
	 * Returns the updates of the ghost variables, in order.
	 * 
	 * @return the updates, which cannot be modified
	 */
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
