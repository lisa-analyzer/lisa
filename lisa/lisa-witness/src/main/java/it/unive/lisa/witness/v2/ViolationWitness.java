package it.unive.lisa.witness.v2;

import it.unive.lisa.witness.v2.violation.ViolationSequence;
import java.util.List;
import java.util.Objects;

/**
 * A violation witness, made of a single {@link ViolationSequence} describing an
 * execution that violates the specification of the task.
 * 
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public final class ViolationWitness
		extends
		Witness {

	/**
	 * The violation sequence of this witness.
	 */
	private final ViolationSequence sequence;

	/**
	 * Builds the witness.
	 * 
	 * @param sequence the violation sequence of the witness
	 * 
	 * @throws InvalidWitnessException if the sequence breaks a rule of the
	 *                                     format for its task
	 */
	public ViolationWitness(
			ViolationSequence sequence) {
		super(List.of(Objects.requireNonNull(sequence, "sequence")));
		this.sequence = sequence;
	}

	/**
	 * Returns the violation sequence of this witness.
	 * 
	 * @return the violation sequence
	 */
	public ViolationSequence getSequence() {
		return sequence;
	}

	@Override
	public int hashCode() {
		return sequence.hashCode();
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		return sequence.equals(((ViolationWitness) obj).sequence);
	}
}
