package it.unive.lisa.witness.v2.correctness;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import it.unive.lisa.witness.v2.Format;
import it.unive.lisa.witness.v2.InvalidWitnessException;
import it.unive.lisa.witness.v2.Location;
import it.unive.lisa.witness.v2.metadata.Task;
import java.util.Objects;
import java.util.Set;

@JsonPropertyOrder({ "type", "location", "value", "format", "labels" })
public final class Invariant
		extends
		InvariantSetItem {

	private final InvariantType type;

	private final String value;

	@JsonCreator
	public Invariant(
			@JsonProperty(value = "type", required = true) InvariantType type,
			@JsonProperty(value = "location", required = true) Location location,
			@JsonProperty(value = "value", required = true) String value,
			@JsonProperty(value = "format", required = true) Format format,
			@JsonProperty("labels") Set<Label> labels) {
		super(location, format, labels);
		this.type = Objects.requireNonNull(type, "type");
		this.value = Objects.requireNonNull(value, "value");
	}

	public Invariant(
			InvariantType type,
			Location location,
			String value,
			Format format) {
		this(type, location, value, format, null);
	}

	@JsonProperty("type")
	public InvariantType getType() {
		return type;
	}

	@JsonProperty("value")
	public String getValue() {
		return value;
	}

	@Override
	protected void checkContent(
			Task task) {
		if (type.isTransition() && !task.getLanguage().supportsTransitionInvariants())
			throw new InvalidWitnessException("Transition invariants are not supported for " + task.getLanguage());
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), type, value);
	}

	@Override
	public boolean equals(
			Object obj) {
		if (!super.equals(obj))
			return false;
		Invariant other = (Invariant) obj;
		return type == other.type && value.equals(other.value);
	}

	@Override
	public String toString() {
		return type + " at " + getLocation() + ": " + value;
	}
}
