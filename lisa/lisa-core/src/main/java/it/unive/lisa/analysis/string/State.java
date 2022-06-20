package it.unive.lisa.analysis.string;

import java.util.Objects;

/**
 * A class that describes an {@link Automaton} state.
 * 
 * @author <a href="mailto:simone.leoni2@studenti.unipr.it">Simone Leoni</a>
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 */
public final class State {

	/**
	 * Identifier of this state
	 */
	private final int id;

	/**
	 * Whether this state is final
	 */
	private final boolean isFinal;

	/**
	 * Whether this state is initial
	 */
	private final boolean isInitial;

	/**
	 * State constructor, it creates a new state with the given informations.
	 * 
	 * @param id        identifier of the new state.
	 * @param isInitial indicates if the state is initial.
	 * @param isFinal   indicates if the state is final.
	 */
	public State(int id, boolean isInitial, boolean isFinal) {
		this.id = id;
		this.isInitial = isInitial;
		this.isFinal = isFinal;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, isFinal, isInitial);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		State other = (State) obj;
		return id == other.id && isFinal == other.isFinal && isInitial == other.isInitial;
	}

	@Override
	public String toString() {
		if (!isFinal && !isInitial)
			return "[" + id + "]";
		else if (!isFinal)
			return "[" + id + ", initial]";
		else if (!isInitial)
			return "[" + id + ", final]";
		else
			return "[" + id + ", initial, final]";
	}

	/**
	 * Tells if the state is either final or not.
	 * 
	 * @return boolean value that indicates if it is a final state.
	 */
	public boolean isFinal() {
		return isFinal;
	}

	/**
	 * Tells if the state is either initial or not.
	 * 
	 * @return boolean value that indicates if it is an initial state.
	 */
	public boolean isInitial() {
		return isInitial;
	}

	/**
	 * Tells the id of the state such that it is possible to distinguish it from
	 * other states.
	 * 
	 * @return state id
	 */
	public int getId() {
		return id;
	}
}
