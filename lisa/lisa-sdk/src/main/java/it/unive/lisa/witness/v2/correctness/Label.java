package it.unive.lisa.witness.v2.correctness;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The labels of the invariants and contracts of a correctness witness.
 * 
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public enum Label {

	/**
	 * The item is inductive, that is, it is preserved by every step of the
	 * program that starts from a state satisfying it.
	 */
	INDUCTIVE("inductive");

	/**
	 * The name of this label, as it is written in a witness.
	 */
	private final String name;

	/**
	 * Builds the label.
	 * 
	 * @param name the name of the label, as it is written in a witness
	 */
	private Label(
			String name) {
		this.name = name;
	}

	/**
	 * Returns the name of this label, as it is written in a witness.
	 * 
	 * @return the name of this label
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
