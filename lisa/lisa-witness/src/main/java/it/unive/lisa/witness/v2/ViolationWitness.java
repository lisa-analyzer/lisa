package it.unive.lisa.witness.v2;

import it.unive.lisa.witness.v2.violation.ViolationSequence;
import java.util.List;
import java.util.Objects;

public final class ViolationWitness
		extends
		Witness {

	private final ViolationSequence sequence;

	public ViolationWitness(
			ViolationSequence sequence) {
		super(List.of(Objects.requireNonNull(sequence, "sequence")));
		this.sequence = sequence;
	}

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
