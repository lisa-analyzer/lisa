package it.unive.lisa.witness.v2;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The type of an entry of a witness, written as its {@code entry_type}. A
 * violation witness is made of one {@link #VIOLATION_SEQUENCE} entry, while a
 * correctness witness is made of one {@link #INVARIANT_SET} entry, optionally
 * followed by one {@link #GHOST_INSTRUMENTATION} entry.
 * 
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public enum EntryType {

	/**
	 * A sequence of segments leading to a violation of the specification.
	 */
	VIOLATION_SEQUENCE("violation_sequence"),

	/**
	 * A set of invariants and function contracts holding in every execution.
	 */
	INVARIANT_SET("invariant_set"),

	/**
	 * The ghost variables and their updates, used by the invariants of a C
	 * program.
	 */
	GHOST_INSTRUMENTATION("ghost_instrumentation");

	/**
	 * The name of this entry type, as it is written in a witness.
	 */
	private final String name;

	/**
	 * Builds the entry type.
	 * 
	 * @param name the name of the entry type, as it is written in a witness
	 */
	private EntryType(
			String name) {
		this.name = name;
	}

	/**
	 * Returns the name of this entry type, as it is written in a witness.
	 * 
	 * @return the name of this entry type
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
