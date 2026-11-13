package it.unive.lisa.witness.v2.correctness.ghost;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.unive.lisa.witness.v2.Entry;
import it.unive.lisa.witness.v2.EntryType;
import it.unive.lisa.witness.v2.Format;
import it.unive.lisa.witness.v2.InvalidWitnessException;
import it.unive.lisa.witness.v2.metadata.Metadata;
import it.unive.lisa.witness.v2.metadata.Task;
import java.util.List;
import java.util.Objects;

/**
 * The entry of a correctness witness holding its ghost code. It is only allowed
 * for C, and all its expressions use {@link Format#C_EXPRESSION}.
 * 
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public final class GhostInstrumentation
		extends
		Entry {

	/**
	 * The ghost variables and updates of the entry.
	 */
	private final GhostContent content;

	/**
	 * Builds the entry.
	 * 
	 * @param metadata the metadata of the entry
	 * @param content  the ghost variables and updates of the entry
	 */
	@JsonCreator
	public GhostInstrumentation(
			@JsonProperty(value = "metadata", required = true) Metadata metadata,
			@JsonProperty(value = "content", required = true) GhostContent content) {
		super(metadata);
		this.content = Objects.requireNonNull(content, "content");
	}

	@Override
	public EntryType getEntryType() {
		return EntryType.GHOST_INSTRUMENTATION;
	}

	/**
	 * Returns the ghost variables and updates of the entry.
	 * 
	 * @return the content
	 */
	@JsonProperty("content")
	public GhostContent getContent() {
		return content;
	}

	/**
	 * Returns the declared ghost variables, in order.
	 * 
	 * @return the variables, which cannot be modified
	 */
	@JsonIgnore
	public List<GhostVariable> getGhostVariables() {
		return content.getGhostVariables();
	}

	/**
	 * Returns the updates of the ghost variables, in order.
	 * 
	 * @return the updates, which cannot be modified
	 */
	@JsonIgnore
	public List<GhostUpdate> getGhostUpdates() {
		return content.getGhostUpdates();
	}

	@Override
	protected void checkContent(
			Task task) {
		if (!task.getLanguage().supportsGhost())
			throw new InvalidWitnessException("Ghost instrumentation is not supported for " + task.getLanguage());
		for (GhostVariable variable : getGhostVariables())
			checkFormat(variable.getInitial().getFormat(), variable);
		for (GhostUpdate update : getGhostUpdates()) {
			checkLocation(task, update.getLocation());
			for (GhostAssignment assignment : update.getUpdates())
				checkFormat(assignment.getFormat(), assignment);
		}
	}

	/**
	 * Checks that the given format is {@link Format#C_EXPRESSION}, the only
	 * format of ghost code.
	 * 
	 * @param format  the format
	 * @param element the element using the format, reported in the message of
	 *                    the exception
	 * 
	 * @throws InvalidWitnessException if the format is another one
	 */
	private static void checkFormat(
			Format format,
			Object element) {
		if (format != Format.C_EXPRESSION)
			throw new InvalidWitnessException(
					"Ghost code only uses c_expression, not " + format + " (in " + element + ")");
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), content);
	}

	@Override
	public boolean equals(
			Object obj) {
		return super.equals(obj) && content.equals(((GhostInstrumentation) obj).content);
	}
}
