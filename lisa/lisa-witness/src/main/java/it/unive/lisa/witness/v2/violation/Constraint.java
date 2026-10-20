package it.unive.lisa.witness.v2.violation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import it.unive.lisa.witness.v2.Format;
import it.unive.lisa.witness.v2.FormattedValue;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "value", "format" })
public final class Constraint
		extends
		FormattedValue {

	public Constraint(
			String value,
			Format format) {
		super(value, format);
	}

	public static Constraint of(
			String value,
			Format format) {
		return new Constraint(value, Objects.requireNonNull(format, "format"));
	}

	public static Constraint withoutFormat(
			String value) {
		return new Constraint(value, null);
	}
}
