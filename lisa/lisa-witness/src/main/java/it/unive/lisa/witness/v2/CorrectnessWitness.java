package it.unive.lisa.witness.v2;

import it.unive.lisa.witness.v2.correctness.InvariantSet;
import it.unive.lisa.witness.v2.correctness.ghost.GhostInstrumentation;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class CorrectnessWitness
		extends
		Witness {

	private final InvariantSet invariants;

	private final GhostInstrumentation ghost;

	public CorrectnessWitness(
			InvariantSet invariants) {
		this(invariants, null);
	}

	public CorrectnessWitness(
			InvariantSet invariants,
			GhostInstrumentation ghost) {
		super(ghost == null ? List.of(Objects.requireNonNull(invariants, "invariants"))
				: List.of(Objects.requireNonNull(invariants, "invariants"), ghost));
		this.invariants = invariants;
		this.ghost = ghost;
	}

	public InvariantSet getInvariants() {
		return invariants;
	}

	public Optional<GhostInstrumentation> getGhost() {
		return Optional.ofNullable(ghost);
	}

	@Override
	public int hashCode() {
		return Objects.hash(invariants, ghost);
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		CorrectnessWitness other = (CorrectnessWitness) obj;
		return invariants.equals(other.invariants) && Objects.equals(ghost, other.ghost);
	}
}
