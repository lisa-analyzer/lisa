package it.unive.lisa.witness.v2.correctness;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import it.unive.lisa.witness.v2.Format;
import it.unive.lisa.witness.v2.InvalidWitnessException;
import it.unive.lisa.witness.v2.Location;
import it.unive.lisa.witness.v2.metadata.Task;
import java.util.Objects;
import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder({ "type", "location", "requires", "ensures", "format", "labels" })
public final class FunctionContract
		extends
		InvariantSetItem {

	private final String requires;

	private final String ensures;

	public FunctionContract(
			Location location,
			String requires,
			String ensures,
			Format format,
			Set<Label> labels) {
		super(location, format, labels);
		if (requires == null && ensures == null)
			throw new InvalidWitnessException("A function contract needs requires, ensures, or both");
		this.requires = requires;
		this.ensures = ensures;
	}

	// the type key has a single value: Jackson rejects any other one when
	// reading it as a ContractType
	@JsonCreator
	private static FunctionContract read(
			@JsonProperty(value = "type", required = true) ContractType type,
			@JsonProperty(value = "location", required = true) Location location,
			@JsonProperty("requires") String requires,
			@JsonProperty("ensures") String ensures,
			@JsonProperty(value = "format", required = true) Format format,
			@JsonProperty("labels") Set<Label> labels) {
		return new FunctionContract(location, requires, ensures, format, labels);
	}

	@JsonProperty("type")
	public ContractType getType() {
		return ContractType.FUNCTION_CONTRACT;
	}

	@JsonProperty("requires")
	public String getRequires() {
		return requires;
	}

	@JsonProperty("ensures")
	public String getEnsures() {
		return ensures;
	}

	@Override
	protected void checkContent(
			Task task) {
		if (!task.getLanguage().supportsContracts())
			throw new InvalidWitnessException("Function contracts are not supported for " + task.getLanguage());
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), requires, ensures);
	}

	@Override
	public boolean equals(
			Object obj) {
		if (!super.equals(obj))
			return false;
		FunctionContract other = (FunctionContract) obj;
		return Objects.equals(requires, other.requires) && Objects.equals(ensures, other.ensures);
	}

	@Override
	public String toString() {
		return getType() + " at " + getLocation() + ": requires " + requires + ", ensures " + ensures;
	}
}
