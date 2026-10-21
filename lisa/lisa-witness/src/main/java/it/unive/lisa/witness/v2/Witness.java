package it.unive.lisa.witness.v2;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.unive.lisa.witness.v2.metadata.Metadata;
import it.unive.lisa.witness.v2.violation.ViolationSequence;
import java.util.List;

public abstract class Witness {

	private final List<Entry> entries;

	protected Witness(
			List<Entry> entries) {
		this.entries = List.copyOf(entries);
		for (Entry entry : this.entries)
			entry.check();
	}

	@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
	static Witness of(
			List<Entry> entries) {
		if (entries == null || entries.isEmpty())
			throw new InvalidWitnessException("The witness has no entries");
		for (Entry entry : entries)
			if (!Metadata.FORMAT_VERSION.equals(entry.getMetadata().getFormatVersion()))
				throw new InvalidWitnessException("Only format version " + Metadata.FORMAT_VERSION
						+ " is supported, not " + entry.getMetadata().getFormatVersion());
		if (entries.size() == 1 && entries.get(0).getEntryType() == EntryType.VIOLATION_SEQUENCE)
			return new ViolationWitness((ViolationSequence) entries.get(0));
		return new CorrectnessWitness(entries);
	}

	public final List<Entry> getEntries() {
		return entries;
	}
}
