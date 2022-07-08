package it.unive.lisa.analysis.string;

/**
 * A class that describes an {@link Automaton} state.
 * 
 * @author <a href="mailto:simone.leoni2@studenti.unipr.it">Simone Leoni</a>
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 */
public final class State {

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
	public State(boolean isInitial, boolean isFinal) {
		this.isInitial = isInitial;
		this.isFinal = isFinal;
	}

	@Override
	public int hashCode() {
		return System.identityHashCode(this);
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj;
	}

	@Override
	public String toString() {
		if (!isFinal && !isInitial)
			return "[" + this.hashCode() + "]";
		else if (!isFinal)
			return "[" + this.hashCode() + ", initial]";
		else if (!isInitial)
			return "[" + this.hashCode() + ", final]";
		else
			return "[" + this.hashCode() + ", initial, final]";
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
}
