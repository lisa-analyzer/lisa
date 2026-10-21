package it.unive.lisa.witness.v2;

import it.unive.lisa.witness.v2.correctness.InvariantSet;
import it.unive.lisa.witness.v2.correctness.ghost.GhostInstrumentation;
import java.util.ArrayList;
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
		if (ghost != null && !ghost.getMetadata().getTask().equals(invariants.getMetadata().getTask()))
			throw new InvalidWitnessException(
					"The invariant_set and the ghost_instrumentation of a witness must have the same task");
	}

	public CorrectnessWitness(
			List<Entry> entries) {
		this(invariants(entries), entries.size() == 2 ? (GhostInstrumentation) entries.get(1) : null);
	}

	private static InvariantSet invariants(
			List<Entry> entries) {
		boolean first = !entries.isEmpty() && entries.get(0).getEntryType() == EntryType.INVARIANT_SET;
		boolean second = entries.size() == 1
				|| entries.size() == 2 && entries.get(1).getEntryType() == EntryType.GHOST_INSTRUMENTATION;
		if (!first || !second) {
			List<EntryType> types = new ArrayList<>();
			for (Entry entry : entries)
				types.add(entry.getEntryType());
			throw new InvalidWitnessException("A witness is one violation_sequence, or one invariant_set optionally "
					+ "followed by one ghost_instrumentation, not " + types);
		}
		return (InvariantSet) entries.get(0);
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
