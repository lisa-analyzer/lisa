package it.unive.lisa.witness.v2;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

public abstract class FormattedValue {

	private final String value;

	private final Format format;

	protected FormattedValue(
			String value,
			Format format) {
		this.value = Objects.requireNonNull(value, "value");
		if (value.isBlank())
			throw new InvalidWitnessException("A value cannot be blank");
		this.format = format;
	}

	@JsonProperty("value")
	public String getValue() {
		return value;
	}

	@JsonProperty("format")
	public Format getFormat() {
		return format;
	}

	@Override
	public int hashCode() {
		return Objects.hash(value, format);
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		FormattedValue other = (FormattedValue) obj;
		return value.equals(other.value) && format == other.format;
	}

	@Override
	public String toString() {
		return value + (format == null ? "" : " (" + format + ")");
	}
}
