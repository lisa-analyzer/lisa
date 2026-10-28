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

/**
 * A contract of a function, made of a precondition, a postcondition, or both.
 * Its location is the location of the function. Contracts are only allowed for
 * the languages that support them.
 * 
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "type", "location", "requires", "ensures", "format", "labels" })
public final class FunctionContract
		extends
		InvariantSetItem {

	/**
	 * The precondition of the function, or {@code null} if it is not given.
	 */
	private final String requires;

	/**
	 * The postcondition of the function, or {@code null} if it is not given.
	 */
	private final String ensures;

	/**
	 * Builds the contract.
	 * 
	 * @param location the location of the function
	 * @param requires the precondition of the function, or {@code null} if it
	 *                     is not given
	 * @param ensures  the postcondition of the function, or {@code null} if it
	 *                     is not given
	 * @param format   the format of the conditions
	 * @param labels   the labels of the contract, or {@code null} if it has
	 *                     none
	 * 
	 * @throws InvalidWitnessException if both conditions are missing, or if a
	 *                                     condition is blank
	 */
	public FunctionContract(
			Location location,
			String requires,
			String ensures,
			Format format,
			Set<Label> labels) {
		super(location, format, labels);
		if (requires == null && ensures == null)
			throw new InvalidWitnessException("A function contract needs requires, ensures, or both");
		if (requires != null && requires.isBlank() || ensures != null && ensures.isBlank())
			throw new InvalidWitnessException("The requires and ensures of a function contract cannot be blank");
		this.requires = requires;
		this.ensures = ensures;
	}

	/**
	 * Builds the contract read from a witness. The type of a contract has a
	 * single value, and any other one is rejected when it is read as a
	 * {@link ContractType}.
	 * 
	 * @param type     the type of the contract
	 * @param location the location of the function
	 * @param requires the precondition of the function, or {@code null} if it
	 *                     is not given
	 * @param ensures  the postcondition of the function, or {@code null} if it
	 *                     is not given
	 * @param format   the format of the conditions
	 * @param labels   the labels of the contract, or {@code null} if it has
	 *                     none
	 * 
	 * @return the contract
	 * 
	 * @throws InvalidWitnessException if both conditions are missing, or if a
	 *                                     condition is blank
	 */
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

	/**
	 * Returns the type of the contract.
	 * 
	 * @return {@link ContractType#FUNCTION_CONTRACT}
	 */
	@JsonProperty("type")
	public ContractType getType() {
		return ContractType.FUNCTION_CONTRACT;
	}

	/**
	 * Returns the precondition of the function.
	 * 
	 * @return the precondition, or {@code null} if it is not given
	 */
	@JsonProperty("requires")
	public String getRequires() {
		return requires;
	}

	/**
	 * Returns the postcondition of the function.
	 * 
	 * @return the postcondition, or {@code null} if it is not given
	 */
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
