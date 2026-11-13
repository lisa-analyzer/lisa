package it.unive.lisa.witness.v2.violation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.unive.lisa.witness.v2.Entry;
import it.unive.lisa.witness.v2.EntryType;
import it.unive.lisa.witness.v2.InvalidWitnessException;
import it.unive.lisa.witness.v2.metadata.Metadata;
import it.unive.lisa.witness.v2.metadata.Task;
import java.util.List;
import java.util.Objects;

/**
 * The entry of a violation witness. It describes an execution that violates the
 * specification as a sequence of {@link Segment}s, which the execution matches
 * one after the other. A safety violation ends with exactly one final segment,
 * the one with the targets. A non-termination violation ends with one or more
 * cycle segments, which the execution matches infinitely often.
 * 
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public final class ViolationSequence
		extends
		Entry {

	/**
	 * The segments of the sequence, in order.
	 */
	private final List<Segment> content;

	/**
	 * Builds the sequence.
	 * 
	 * @param metadata the metadata of the entry
	 * @param content  the segments of the sequence, in order
	 * 
	 * @throws InvalidWitnessException if there is no segment, or if the
	 *                                     segments do not end as the format
	 *                                     requires
	 */
	@JsonCreator
	public ViolationSequence(
			@JsonProperty(value = "metadata", required = true) Metadata metadata,
			@JsonProperty(value = "content", required = true) List<Segment> content) {
		super(metadata);
		this.content = List.copyOf(Objects.requireNonNull(content, "content"));
		if (this.content.isEmpty())
			throw new InvalidWitnessException("A violation sequence needs at least one segment");
		checkEnd(this.content);
	}

	/**
	 * Checks that the given segments end as the format requires. A safety
	 * violation ends with exactly one final segment, the one with the targets,
	 * and has no cycle segment. A non-termination violation ends with one or
	 * more cycle segments, which follow every normal segment, and has no
	 * target.
	 * 
	 * @param segments the segments of a sequence, at least one
	 * 
	 * @throws InvalidWitnessException if the segments end in another way
	 */
	private static void checkEnd(
			List<Segment> segments) {
		for (int i = 0; i < segments.size() - 1; i++)
			if (segments.get(i).isFinal())
				throw new InvalidWitnessException("The segment with a target must be the last one");
		Segment last = segments.get(segments.size() - 1);
		boolean cycles = segments.stream().anyMatch(Segment::isCycle);
		if (last.isFinal() && cycles)
			throw new InvalidWitnessException("A violation sequence cannot have both a target and cycle segments");
		if (!last.isFinal() && !last.isCycle())
			throw new InvalidWitnessException(
					"A violation sequence must end with a segment with a target, or with cycle segments");
		boolean inCycle = false;
		for (Segment segment : segments)
			if (segment.isCycle())
				inCycle = true;
			else if (inCycle)
				throw new InvalidWitnessException("The cycle segments must come after every other segment");
	}

	@Override
	public EntryType getEntryType() {
		return EntryType.VIOLATION_SEQUENCE;
	}

	/**
	 * Returns the segments of the sequence, in order.
	 * 
	 * @return the segments, which cannot be modified
	 */
	@JsonProperty("content")
	public List<Segment> getSegments() {
		return content;
	}

	@Override
	protected void checkContent(
			Task task) {
		for (Segment segment : content)
			for (Waypoint waypoint : segment.getWaypoints()) {
				checkLocation(task, waypoint.getLocation());
				if (waypoint.getConstraint() != null && waypoint.getConstraint().getFormat() != null)
					task.getLanguage().checkAllows(waypoint.getConstraint().getFormat(), waypoint);
			}
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
