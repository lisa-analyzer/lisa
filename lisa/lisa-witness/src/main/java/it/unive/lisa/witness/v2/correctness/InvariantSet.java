package it.unive.lisa.witness.v2.correctness;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.unive.lisa.witness.v2.Entry;
import it.unive.lisa.witness.v2.EntryType;
import it.unive.lisa.witness.v2.metadata.Metadata;
import it.unive.lisa.witness.v2.metadata.Task;
import java.util.List;
import java.util.Objects;

public final class InvariantSet
		extends
		Entry {

	private final List<InvariantSetItem> content;

	@JsonCreator
	public InvariantSet(
			@JsonProperty(value = "metadata", required = true) Metadata metadata,
			@JsonProperty(value = "content", required = true) List<InvariantSetItem> content) {
		super(metadata);
		this.content = List.copyOf(Objects.requireNonNull(content, "content"));
	}

	@Override
	public EntryType getEntryType() {
		return EntryType.INVARIANT_SET;
	}

	@JsonProperty("content")
	public List<InvariantSetItem> getItems() {
		return content;
	}

	@Override
	protected void checkContent(
			Task task) {
		for (InvariantSetItem item : content)
			item.check(task);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), content);
	}

	@Override
	public boolean equals(
			Object obj) {
		return super.equals(obj) && content.equals(((InvariantSet) obj).content);
	}
}
