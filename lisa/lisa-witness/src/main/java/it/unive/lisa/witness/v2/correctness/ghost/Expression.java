package it.unive.lisa.witness.v2.correctness.ghost;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import it.unive.lisa.witness.v2.Format;
import it.unive.lisa.witness.v2.FormattedValue;
import java.util.Objects;

@JsonPropertyOrder({ "value", "format" })
public final class Expression
		extends
		FormattedValue {

	public Expression(
			String value,
			Format format) {
		super(value, Objects.requireNonNull(format, "format"));
	}
}
