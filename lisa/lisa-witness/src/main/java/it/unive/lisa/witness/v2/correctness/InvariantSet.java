package it.unive.lisa.witness.v2.correctness;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.unive.lisa.witness.v2.Entry;
import it.unive.lisa.witness.v2.EntryType;
import it.unive.lisa.witness.v2.metadata.Metadata;
import it.unive.lisa.witness.v2.metadata.Task;
import java.util.List;
import java.util.Objects;

/**
 * The entry of a correctness witness holding its invariants and function
 * contracts. Each item is checked against the task of the entry, for instance
 * that its location is in one of the input files and that its format is allowed
 * for the language.
 * 
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public final class InvariantSet
		extends
		Entry {

	/**
	 * The invariants and contracts of the entry, in order.
	 */
	private final List<InvariantSetItem> content;

	/**
	 * Builds the entry.
	 * 
	 * @param metadata the metadata of the entry
	 * @param content  the invariants and contracts of the entry, in order
	 */
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

	/**
	 * Returns the invariants and contracts of the entry, in order.
	 * 
	 * @return the items, which cannot be modified
	 */
	@JsonProperty("content")
	public List<InvariantSetItem> getItems() {
		return content;
	}

	@Override
	protected void checkContent(
			Task task) {
		for (InvariantSetItem item : content) {
			checkLocation(task, item.getLocation());
			item.check(task);
		}
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
