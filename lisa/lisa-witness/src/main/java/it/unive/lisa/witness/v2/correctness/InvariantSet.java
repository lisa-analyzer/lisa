package it.unive.lisa.witness.v2.correctness;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.unive.lisa.witness.v2.Entry;
import it.unive.lisa.witness.v2.EntryType;
import it.unive.lisa.witness.v2.Language;
import it.unive.lisa.witness.v2.metadata.Metadata;
import java.util.List;
import java.util.Objects;

public final class InvariantSet
		extends
		Entry {

	private final List<InvariantSetItem> content;

	public InvariantSet(
			Metadata metadata,
			List<InvariantSetItem> content) {
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
			Language language) {
		for (InvariantSetItem item : content)
			item.checkLanguage(language);
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
