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

public final class GhostInstrumentation
		extends
		Entry {

	private final GhostContent content;

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

	@JsonProperty("content")
	public GhostContent getContent() {
		return content;
	}

	@JsonIgnore
	public List<GhostVariable> getGhostVariables() {
		return content.getGhostVariables();
	}

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
			task.checkLocation(update.getLocation());
			for (GhostAssignment assignment : update.getUpdates())
				checkFormat(assignment.getFormat(), assignment);
		}
	}

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
