package it.unive.lisa.witness.v2.violation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import it.unive.lisa.witness.v2.Format;
import it.unive.lisa.witness.v2.FormattedValue;
import java.util.Objects;

/**
 * The constraint of a waypoint, made of a value and its {@link Format}. The
 * format is missing only for branching waypoints, whose value is {@code true}
 * or {@code false}.
 * 
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "value", "format" })
public final class Constraint
		extends
		FormattedValue {

	/**
	 * Builds the constraint.
	 * 
	 * @param value  the value of the constraint
	 * @param format the format of the value, or {@code null} for branching
	 *                   waypoints
	 * 
	 * @throws it.unive.lisa.witness.v2.InvalidWitnessException if the value is
	 *                                                              blank
	 */
	@JsonCreator
	public Constraint(
			@JsonProperty(value = "value", required = true) String value,
			@JsonProperty("format") Format format) {
		super(value, format);
	}

	/**
	 * Builds a constraint with a format.
	 * 
	 * @param value  the value of the constraint
	 * @param format the format of the value
	 * 
	 * @return the constraint
	 * 
	 * @throws it.unive.lisa.witness.v2.InvalidWitnessException if the value is
	 *                                                              blank
	 */
	public static Constraint of(
			String value,
			Format format) {
		return new Constraint(value, Objects.requireNonNull(format, "format"));
	}

	/**
	 * Builds a constraint without format, as the one of a branching waypoint.
	 * 
	 * @param value the value of the constraint
	 * 
	 * @return the constraint
	 * 
	 * @throws it.unive.lisa.witness.v2.InvalidWitnessException if the value is
	 *                                                              blank
	 */
	public static Constraint withoutFormat(
			String value) {
		return new Constraint(value, null);
	}
}
