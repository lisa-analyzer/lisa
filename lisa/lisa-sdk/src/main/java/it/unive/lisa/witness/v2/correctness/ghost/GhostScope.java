package it.unive.lisa.witness.v2.correctness.ghost;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The scopes of the ghost variables of a correctness witness.
 * 
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public enum GhostScope {

	/**
	 * A variable visible in the whole program.
	 */
	GLOBAL("global");

	/**
	 * The name of this scope, as it is written in a witness.
	 */
	private final String name;

	/**
	 * Builds the scope.
	 * 
	 * @param name the name of the scope, as it is written in a witness
	 */
	private GhostScope(
			String name) {
		this.name = name;
	}

	/**
	 * Returns the name of this scope, as it is written in a witness.
	 * 
	 * @return the name of this scope
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
