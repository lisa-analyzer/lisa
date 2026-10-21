package it.unive.lisa.witness.v2;

import com.fasterxml.jackson.annotation.JsonValue;

public enum EntryType {

	VIOLATION_SEQUENCE("violation_sequence"),

	INVARIANT_SET("invariant_set"),

	GHOST_INSTRUMENTATION("ghost_instrumentation");

	private final String name;

	private EntryType(
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
