package it.unive.lisa.witness.v2;

import com.fasterxml.jackson.annotation.JsonValue;

public enum DataModel {

	ILP32("ILP32"),

	LP64("LP64");

	private final String name;

	private DataModel(
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
