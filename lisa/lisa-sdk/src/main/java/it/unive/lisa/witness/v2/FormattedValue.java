package it.unive.lisa.witness.v2;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/**
 * An abstract value of a witness, written as a {@code value} and its
 * {@code format}. Its implementations are
 * {@link it.unive.lisa.witness.v2.violation.Constraint},
 * {@link it.unive.lisa.witness.v2.correctness.ghost.Expression} and
 * {@link it.unive.lisa.witness.v2.correctness.ghost.GhostAssignment}. The value
 * cannot be blank. The format can be missing, as in the constraint of a
 * branching waypoint.
 * 
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public abstract class FormattedValue {

	/**
	 * The value.
	 */
	private final String value;

	/**
	 * The format of the value, or {@code null} if it has none.
	 */
	private final Format format;

	/**
	 * Builds the value.
	 * 
	 * @param value  the value
	 * @param format the format of the value, or {@code null} if it has none
	 * 
	 * @throws InvalidWitnessException if the value is blank
	 */
	protected FormattedValue(
			String value,
			Format format) {
		this.value = Objects.requireNonNull(value, "value");
		if (value.isBlank())
			throw new InvalidWitnessException("A value cannot be blank");
		this.format = format;
	}

	/**
	 * Returns the value.
	 * 
	 * @return the value
	 */
	@JsonProperty("value")
	public String getValue() {
		return value;
	}

	/**
	 * Returns the format of the value.
	 * 
	 * @return the format, or {@code null} if the value has none
	 */
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
