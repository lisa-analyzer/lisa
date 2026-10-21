package it.unive.lisa.witness.v2;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.unive.lisa.witness.v2.correctness.InvariantSet;
import it.unive.lisa.witness.v2.correctness.ghost.GhostInstrumentation;
import it.unive.lisa.witness.v2.metadata.Metadata;
import it.unive.lisa.witness.v2.metadata.Task;
import it.unive.lisa.witness.v2.violation.ViolationSequence;
import java.util.Objects;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "entry_type")
@JsonSubTypes({
		@JsonSubTypes.Type(value = ViolationSequence.class, name = "violation_sequence"),
		@JsonSubTypes.Type(value = InvariantSet.class, name = "invariant_set"),
		@JsonSubTypes.Type(value = GhostInstrumentation.class, name = "ghost_instrumentation")
})
@JsonPropertyOrder({ "entry_type", "metadata", "content" })
public abstract class Entry {

	private final Metadata metadata;

	protected Entry(
			Metadata metadata) {
		this.metadata = Objects.requireNonNull(metadata, "metadata");
	}

	@JsonProperty("entry_type")
	public abstract EntryType getEntryType();

	public Metadata getMetadata() {
		return metadata;
	}

	final void check() {
		checkContent(metadata.getTask());
	}

	protected abstract void checkContent(
			Task task);

	@Override
	public int hashCode() {
		return Objects.hash(getEntryType(), metadata);
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		Entry other = (Entry) obj;
		return metadata.equals(other.metadata);
	}
}
