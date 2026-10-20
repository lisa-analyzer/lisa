package it.unive.lisa.witness.v2.correctness;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ContractType {

	FUNCTION_CONTRACT("function_contract");

	private final String name;

	private ContractType(
			String name) {
		this.name = name;
	}

	@JsonValue
	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return name;
	}
}
