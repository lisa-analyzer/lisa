package it.unive.lisa.witness.v2.violation;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Action {

	FOLLOW("follow"),

	AVOID("avoid"),

	CYCLE("cycle");

	private final String name;

	private Action(
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
