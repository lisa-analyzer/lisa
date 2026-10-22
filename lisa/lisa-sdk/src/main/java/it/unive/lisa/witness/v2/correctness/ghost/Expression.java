package it.unive.lisa.witness.v2.correctness.ghost;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import it.unive.lisa.witness.v2.Format;
import it.unive.lisa.witness.v2.FormattedValue;
import java.util.Objects;

/**
 * An expression of ghost code, used as the initial value of a ghost variable.
 * Its format is required.
 * 
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
@JsonPropertyOrder({ "value", "format" })
public final class Expression
		extends
		FormattedValue {

	/**
	 * Builds the expression.
	 * 
	 * @param value  the expression
	 * @param format the format of the expression
	 * 
	 * @throws it.unive.lisa.witness.v2.InvalidWitnessException if the
	 *                                                              expression
	 *                                                              is blank
	 */
	@JsonCreator
	public Expression(
			@JsonProperty(value = "value", required = true) String value,
			@JsonProperty(value = "format", required = true) Format format) {
		super(value, Objects.requireNonNull(format, "format"));
	}
}
