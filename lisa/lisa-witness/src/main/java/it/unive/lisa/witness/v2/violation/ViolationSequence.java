package it.unive.lisa.witness.v2.violation;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.unive.lisa.witness.v2.Entry;
import it.unive.lisa.witness.v2.EntryType;
import it.unive.lisa.witness.v2.InvalidWitnessException;
import it.unive.lisa.witness.v2.Language;
import it.unive.lisa.witness.v2.metadata.Metadata;
import java.util.List;
import java.util.Objects;

public final class ViolationSequence
		extends
		Entry {

	private final List<Segment> content;

	public ViolationSequence(
			Metadata metadata,
			List<Segment> content) {
		super(metadata);
		this.content = List.copyOf(Objects.requireNonNull(content, "content"));
		if (this.content.isEmpty())
			throw new InvalidWitnessException("A violation sequence needs at least one segment");
		for (int i = 0; i < this.content.size() - 1; i++)
			if (this.content.get(i).isFinal())
				throw new InvalidWitnessException("The segment with a target must be the last one");
	}

	@Override
	public EntryType getEntryType() {
		return EntryType.VIOLATION_SEQUENCE;
	}

	@JsonProperty("content")
	public List<Segment> getSegments() {
		return content;
	}

	@Override
	protected void checkContent(
			Language language) {
		for (Segment segment : content)
			for (Waypoint waypoint : segment.getWaypoints())
				if (waypoint.getConstraint() != null && waypoint.getConstraint().getFormat() != null)
					language.checkAllows(waypoint.getConstraint().getFormat(), waypoint);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), content);
	}

	@Override
	public boolean equals(
			Object obj) {
		return super.equals(obj) && content.equals(((ViolationSequence) obj).content);
	}
}
