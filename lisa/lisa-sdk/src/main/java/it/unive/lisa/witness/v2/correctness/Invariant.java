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

/**
 * An invariant of a correctness witness, an expression holding at its location.
 * Transition invariants are only allowed for the languages that support them.
 * 
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
@JsonPropertyOrder({ "type", "location", "value", "format", "labels" })
public final class Invariant
		extends
		InvariantSetItem {

	/**
	 * The type of the invariant.
	 */
	private final InvariantType type;

	/**
	 * The expression of the invariant.
	 */
	private final String value;

	/**
	 * Builds the invariant.
	 * 
	 * @param type     the type of the invariant
	 * @param location the location of the invariant
	 * @param value    the expression of the invariant
	 * @param format   the format of the expression
	 * @param labels   the labels of the invariant, or {@code null} if it has
	 *                     none
	 * 
	 * @throws InvalidWitnessException if the expression is blank
	 */
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
		if (value.isBlank())
			throw new InvalidWitnessException("An invariant needs a value");
	}

	/**
	 * Builds an invariant without labels.
	 * 
	 * @param type     the type of the invariant
	 * @param location the location of the invariant
	 * @param value    the expression of the invariant
	 * @param format   the format of the expression
	 * 
	 * @throws InvalidWitnessException if the expression is blank
	 */
	public Invariant(
			InvariantType type,
			Location location,
			String value,
			Format format) {
		this(type, location, value, format, null);
	}

	/**
	 * Returns the type of the invariant.
	 * 
	 * @return the type
	 */
	@JsonProperty("type")
	public InvariantType getType() {
		return type;
	}

	/**
	 * Returns the expression of the invariant.
	 * 
	 * @return the expression
	 */
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
