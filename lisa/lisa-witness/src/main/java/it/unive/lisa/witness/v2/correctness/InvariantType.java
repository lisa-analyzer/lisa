package it.unive.lisa.witness.v2.correctness;

import com.fasterxml.jackson.annotation.JsonValue;

public enum InvariantType {

	LOCATION_INVARIANT("location_invariant", false),

	LOOP_INVARIANT("loop_invariant", false),

	LOCATION_TRANSITION_INVARIANT("location_transition_invariant", true),

	LOOP_TRANSITION_INVARIANT("loop_transition_invariant", true);

	private final String name;

	private final boolean transition;

	private InvariantType(
			String name,
			boolean transition) {
		this.name = name;
		this.transition = transition;
	}

	@JsonValue
	public String getName() {
		return name;
	}

	public boolean isTransition() {
		return transition;
	}

	@Override
	public String toString() {
		return name;
	}
}
