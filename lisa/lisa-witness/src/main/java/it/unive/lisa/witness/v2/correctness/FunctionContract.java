package it.unive.lisa.witness.v2.correctness;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import it.unive.lisa.witness.v2.Format;
import it.unive.lisa.witness.v2.InvalidWitnessException;
import it.unive.lisa.witness.v2.Language;
import it.unive.lisa.witness.v2.Location;
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
	void checkLanguage(
			Language language) {
		super.checkLanguage(language);
		if (!language.supportsContracts())
			throw new InvalidWitnessException("Function contracts are not supported for " + language);
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
