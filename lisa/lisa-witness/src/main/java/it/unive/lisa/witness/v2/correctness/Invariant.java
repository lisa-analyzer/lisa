package it.unive.lisa.witness.v2.correctness;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import it.unive.lisa.witness.v2.Format;
import it.unive.lisa.witness.v2.InvalidWitnessException;
import it.unive.lisa.witness.v2.Language;
import it.unive.lisa.witness.v2.Location;
import java.util.Objects;
import java.util.Set;

@JsonPropertyOrder({ "type", "location", "value", "format", "labels" })
public final class Invariant
		extends
		InvariantSetItem {

	private final InvariantType type;

	private final String value;

	public Invariant(
			InvariantType type,
			Location location,
			String value,
			Format format,
			Set<Label> labels) {
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
	void checkLanguage(
			Language language) {
		super.checkLanguage(language);
		if (type.isTransition() && !language.supportsTransitionInvariants())
			throw new InvalidWitnessException("Transition invariants are not supported for " + language);
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
