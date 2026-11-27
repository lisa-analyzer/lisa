package it.unive.lisa.witness.v2.violation;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The actions of the waypoints of a violation witness, telling how an execution
 * relates to a waypoint.
 * 
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public enum Action {

	/**
	 * The execution passes the waypoint exactly once.
	 */
	FOLLOW("follow"),

	/**
	 * The execution does not pass the waypoint before the next follow or cycle
	 * waypoint.
	 */
	AVOID("avoid"),

	/**
	 * The execution passes the waypoint infinitely often.
	 */
	CYCLE("cycle");

	/**
	 * The name of this action, as it is written in a witness.
	 */
	private final String name;

	/**
	 * Builds the action.
	 * 
	 * @param name the name of the action, as it is written in a witness
	 */
	private Action(
			String name) {
		this.name = name;
	}

	/**
	 * Returns the name of this action, as it is written in a witness.
	 * 
	 * @return the name of this action
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
