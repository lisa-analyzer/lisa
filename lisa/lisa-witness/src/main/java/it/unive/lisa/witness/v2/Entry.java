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

/**
 * An abstract entry of a witness. Its implementations are
 * {@link ViolationSequence}, {@link InvariantSet} and
 * {@link GhostInstrumentation}, one for each {@link EntryType}. An entry has
 * its {@link Metadata}, which describes the task the entry refers to, and a
 * content that depends on its type. When the entry is part of a
 * {@link Witness}, its content is checked against its task.
 * 
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "entry_type")
@JsonSubTypes({
		@JsonSubTypes.Type(value = ViolationSequence.class, name = "violation_sequence"),
		@JsonSubTypes.Type(value = InvariantSet.class, name = "invariant_set"),
		@JsonSubTypes.Type(value = GhostInstrumentation.class, name = "ghost_instrumentation")
})
@JsonPropertyOrder({ "entry_type", "metadata", "content" })
public abstract class Entry {

	/**
	 * The metadata of this entry.
	 */
	private final Metadata metadata;

	/**
	 * Builds the entry.
	 * 
	 * @param metadata the metadata of the entry
	 */
	protected Entry(
			Metadata metadata) {
		this.metadata = Objects.requireNonNull(metadata, "metadata");
	}

	/**
	 * Returns the type of this entry, written as its {@code entry_type}.
	 * 
	 * @return the type of this entry
	 */
	@JsonProperty("entry_type")
	public abstract EntryType getEntryType();

	/**
	 * Returns the metadata of this entry.
	 * 
	 * @return the metadata
	 */
	public Metadata getMetadata() {
		return metadata;
	}

	/**
	 * Checks the content of this entry against the task of its metadata.
	 * 
	 * @throws InvalidWitnessException if the content breaks a rule of the
	 *                                     format for the task
	 */
	final void check() {
		checkContent(metadata.getTask());
	}

	/**
	 * Checks the content of this entry against the given task, for instance
	 * that its locations are in the input files of the task and that its
	 * expression formats are allowed for its language.
	 * 
	 * @param task the task of this entry
	 * 
	 * @throws InvalidWitnessException if the content breaks a rule of the
	 *                                     format for the task
	 */
	protected abstract void checkContent(
			Task task);

	/**
	 * Checks that the given location is in one of the input files of the given
	 * task. File names are compared exactly as they are written.
	 * 
	 * @param task     the task the location refers to
	 * @param location the location to check
	 * 
	 * @throws InvalidWitnessException if the file of the location is not an
	 *                                     input file of the task
	 */
	protected static void checkLocation(
			Task task,
			Location location) {
		if (!task.getInputFiles().contains(location.getFileName()))
			throw new InvalidWitnessException(
					"The location " + location + " is not in an input file of the task: " + task.getInputFiles());
	}

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
