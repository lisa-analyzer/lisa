package it.unive.lisa.witness.v2.correctness;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The types of the invariants of a correctness witness, written as the
 * {@code type} of an invariant. A transition invariant relates two states of
 * the program, and it is only supported by some languages.
 * 
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public enum InvariantType {

	/**
	 * An invariant holding every time its location is reached.
	 */
	LOCATION_INVARIANT("location_invariant", false),

	/**
	 * An invariant holding at the beginning of every iteration of a loop.
	 */
	LOOP_INVARIANT("loop_invariant", false),

	/**
	 * A transition invariant relating two consecutive visits of a location.
	 */
	LOCATION_TRANSITION_INVARIANT("location_transition_invariant", true),

	/**
	 * A transition invariant relating two iterations of a loop.
	 */
	LOOP_TRANSITION_INVARIANT("loop_transition_invariant", true);

	/**
	 * The name of this type, as it is written in a witness.
	 */
	private final String name;

	/**
	 * Whether the invariants of this type are transition invariants.
	 */
	private final boolean transition;

	/**
	 * Builds the type.
	 * 
	 * @param name       the name of the type, as it is written in a witness
	 * @param transition whether the invariants of the type are transition
	 *                       invariants
	 */
	private InvariantType(
			String name,
			boolean transition) {
		this.name = name;
		this.transition = transition;
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

	/**
	 * Returns whether the invariants of this type are transition invariants.
	 * 
	 * @return {@code true} if they are transition invariants
	 */
	public boolean isTransition() {
		return transition;
	}

	@Override
	public String toString() {
		return name;
	}
}
