package it.unive.lisa.analysis.string.fsa;

import java.util.concurrent.atomic.AtomicInteger;

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

	private static final AtomicInteger idGenerator = new AtomicInteger();

	/**
	 * State constructor, it creates a new state with the given information.
	 *
	 * @param isInitial indicates if the state is initial.
	 * @param isFinal   indicates if the state is final.
	 */
	public State(boolean isInitial, boolean isFinal) {
		this.isInitial = isInitial;
		this.isFinal = isFinal;
		this.id = idGenerator.getAndIncrement();
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
			return "[" + this.hashCode() + ", id=" + id + "]";
		else if (!isFinal)
			return "[" + this.hashCode() + ", initial, id=" + id + "]";
		else if (!isInitial)
			return "[" + this.hashCode() + ", final, id=" + id + "]";
		else
			return "[" + this.hashCode() + ", initial, final, id=" + id + "]";
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
	 * Returns the states, id.
	 * 
	 * @return integer value representing the state's id.
	 */
	public int getId() {
		return this.id;
	}

	@Override
	public int compareTo(State state) {
		return Integer.compare(this.id, state.id);
	}
}
