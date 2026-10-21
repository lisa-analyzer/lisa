package it.unive.lisa.witness.v2.correctness.ghost;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import it.unive.lisa.witness.v2.Format;
import it.unive.lisa.witness.v2.FormattedValue;
import java.util.Objects;

@JsonPropertyOrder({ "variable", "value", "format" })
public final class GhostAssignment
		extends
		FormattedValue {

	private final String variable;

	@JsonCreator
	public GhostAssignment(
			@JsonProperty(value = "variable", required = true) String variable,
			@JsonProperty(value = "value", required = true) String value,
			@JsonProperty(value = "format", required = true) Format format) {
		super(value, Objects.requireNonNull(format, "format"));
		this.variable = Objects.requireNonNull(variable, "variable");
	}

	@JsonProperty("variable")
	public String getVariable() {
		return variable;
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), variable);
	}

	@Override
	public boolean equals(
			Object obj) {
		return super.equals(obj) && variable.equals(((GhostAssignment) obj).variable);
	}

	@Override
	public String toString() {
		return variable + " = " + getValue();
	}
}
