package it.unive.lisa.witness.v2.correctness;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The types of the contracts of a correctness witness, written as the
 * {@code type} of a contract.
 * 
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public enum ContractType {

	/**
	 * The precondition and postcondition of a function.
	 */
	FUNCTION_CONTRACT("function_contract");

	/**
	 * The name of this type, as it is written in a witness.
	 */
	private final String name;

	/**
	 * Builds the type.
	 * 
	 * @param name the name of the type, as it is written in a witness
	 */
	private ContractType(
			String name) {
		this.name = name;
	}

	/**
	 * Returns the name of this type, as it is written in a witness.
	 * 
	 * @return the name of this type
	 */
	@JsonValue
	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return name;
	}
}
