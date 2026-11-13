package it.unive.lisa.witness.v2;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The data models of C tasks, which fix the sizes of the integer and pointer
 * types.
 * 
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public enum DataModel {

	/**
	 * Integers, long integers and pointers of 32 bits.
	 */
	ILP32("ILP32"),

	/**
	 * Integers of 32 bits, long integers and pointers of 64 bits.
	 */
	LP64("LP64");

	/**
	 * The name of this data model, as it is written in a witness.
	 */
	private final String name;

	/**
	 * Builds the data model.
	 * 
	 * @param name the name of the data model, as it is written in a witness
	 */
	private DataModel(
			String name) {
		this.name = name;
	}

	/**
	 * Returns the name of this data model, as it is written in a witness.
	 * 
	 * @return the name of this data model
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
