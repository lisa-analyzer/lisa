package it.unive.lisa.util.datastructures.automaton;

/**
 * A class that describes an {@link Automaton} state.
 * 
 * @author <a href="mailto:simone.leoni2@studenti.unipr.it">Simone Leoni</a>
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 */
public final class State implements Comparable<State> {

	/**
	 * Whether this state is final.
	 */
	private final boolean isFinal;

	/**
	 * Whether this state is initial.
	 */
	private final boolean isInitial;

	/**
	 * The id for this state.
	 */
	private final int id;

	/**
	 * State constructor, it creates a new state with the given information.
	 *
	 * @param id        the id of this state
	 * @param isInitial indicates if the state is initial.
	 * @param isFinal   indicates if the state is final.
	 */
	public State(
			int id,
			boolean isInitial,
			boolean isFinal) {
		this.isInitial = isInitial;
		this.isFinal = isFinal;
		this.id = id;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + (isFinal ? 1231 : 1237);
		result = prime * result + (isInitial ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		State other = (State) obj;
		if (id != other.id)
			return false;
		if (isFinal != other.isFinal)
			return false;
		if (isInitial != other.isInitial)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return getState() + (isInitial ? "[init]" : "") + (isFinal ? "[final]" : "");
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
	 * @return boolean value that indicates if it is an initial state
	 */
	public boolean isInitial() {
		return isInitial;
	}

	/**
	 * Returns the states, id.
	 * 
	 * @return integer value representing the state's id.
	 */
	public int getId() {
		return this.id;
	}

	@Override
	public int compareTo(
			State state) {
		int cmp;
		if ((cmp = Integer.compare(this.id, state.id)) != 0)
			return cmp;
		if ((cmp = Boolean.compare(this.isInitial, state.isInitial)) != 0)
			return cmp;
		return Boolean.compare(this.isFinal, state.isFinal);
	}

	/**
	 * Yields a textual representation of this state.
	 * 
	 * @return the textual representation
	 */
	public String getState() {
		return "q" + id;
	}
}
