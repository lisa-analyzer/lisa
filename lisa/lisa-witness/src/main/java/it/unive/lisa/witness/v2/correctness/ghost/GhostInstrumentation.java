package it.unive.lisa.witness.v2.correctness.ghost;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.unive.lisa.witness.v2.Entry;
import it.unive.lisa.witness.v2.EntryType;
import it.unive.lisa.witness.v2.InvalidWitnessException;
import it.unive.lisa.witness.v2.Language;
import it.unive.lisa.witness.v2.metadata.Metadata;
import java.util.List;
import java.util.Objects;

public final class GhostInstrumentation
		extends
		Entry {

	private final GhostContent content;

	public GhostInstrumentation(
			Metadata metadata,
			GhostContent content) {
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
			Language language) {
		if (!language.supportsGhost())
			throw new InvalidWitnessException("Ghost instrumentation is not supported for " + language);
		for (GhostVariable variable : getGhostVariables())
			language.checkAllows(variable.getInitial().getFormat(), variable);
		for (GhostUpdate update : getGhostUpdates())
			for (GhostAssignment assignment : update.getUpdates())
				language.checkAllows(assignment.getFormat(), assignment);
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
