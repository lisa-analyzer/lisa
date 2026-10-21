package it.unive.lisa.witness.v2;

import it.unive.lisa.witness.v2.correctness.InvariantSet;
import it.unive.lisa.witness.v2.correctness.ghost.GhostInstrumentation;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A correctness witness, made of an {@link InvariantSet} and, for C programs
 * only, of a {@link GhostInstrumentation} defining the ghost variables used by
 * the invariants.
 * 
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public final class CorrectnessWitness
		extends
		Witness {

	/**
	 * The invariants and contracts of this witness.
	 */
	private final InvariantSet invariants;

	/**
	 * The ghost code of this witness, or {@code null} if it has none.
	 */
	private final GhostInstrumentation ghost;

	/**
	 * Builds a witness without ghost code.
	 * 
	 * @param invariants the invariants and contracts of the witness
	 * 
	 * @throws InvalidWitnessException if the invariants break a rule of the
	 *                                     format for their task
	 */
	public CorrectnessWitness(
			InvariantSet invariants) {
		this(invariants, null);
	}

	/**
	 * Builds a witness with the given ghost code.
	 * 
	 * @param invariants the invariants and contracts of the witness
	 * @param ghost      the ghost code of the witness, or {@code null} if it
	 *                       has none
	 * 
	 * @throws InvalidWitnessException if an entry breaks a rule of the format,
	 *                                     or if the two entries refer to
	 *                                     different tasks
	 */
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

	/**
	 * Builds a witness from a list of entries, made of an invariant set
	 * optionally followed by a ghost instrumentation.
	 * 
	 * @param entries the entries of the witness
	 * 
	 * @throws InvalidWitnessException if the entries are not an invariant set
	 *                                     optionally followed by a ghost
	 *                                     instrumentation, or if they break a
	 *                                     rule of the format
	 */
	public CorrectnessWitness(
			List<Entry> entries) {
		this(invariants(entries), entries.size() == 2 ? (GhostInstrumentation) entries.get(1) : null);
	}

	/**
	 * Returns the invariant set at the beginning of the given entries, after
	 * checking that the entries are an invariant set optionally followed by a
	 * ghost instrumentation.
	 * 
	 * @param entries the entries of a witness
	 * 
	 * @return the invariant set of the entries
	 * 
	 * @throws InvalidWitnessException if the entries do not have this shape
	 */
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

	/**
	 * Returns the invariants and contracts of this witness.
	 * 
	 * @return the invariant set
	 */
	public InvariantSet getInvariants() {
		return invariants;
	}

	/**
	 * Returns the ghost code of this witness, if it has any.
	 * 
	 * @return the ghost instrumentation, or an empty optional if the witness
	 *             has none
	 */
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
