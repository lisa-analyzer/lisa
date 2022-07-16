package it.unive.lisa.analysis.string;

import java.util.Objects;

/**
 * A class that describes an Automaton transition.
 * 
 * @author <a href="mailto:simone.leoni2@studenti.unipr.it">Simone Leoni</a>
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 */
public final class Transition {

	private final State source, destination;

	// "" means epsilon transition
	private final String symbol;

	/**
	 * Creates a new transition for a generic automaton.
	 * 
	 * @param source      state from which the transition could be applied.
	 * @param destination state reached after the transition execution.
	 * @param symbol      the character that have to be read to execute the
	 *                        transition. {@code ' '} is used for epsilon
	 *                        transitions.
	 */
	public Transition(State source, State destination, String symbol) {
		this.source = source;
		this.destination = destination;
		this.symbol = symbol;
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
		return "[" + source + " -> " + destination + " : " + symbol + "]";
	}

	/**
	 * Tells transition's source state.
	 * 
	 * @return source of {@code this} transition.
	 */
	public State getSource() {
		return this.source;
	}

	/**
	 * Tells input symbol for {@code this} transition.
	 * 
	 * @return symbol needed to execute {@code this} transition.
	 */
	public String getSymbol() {
		return this.symbol;
	}

	/**
	 * Tells transition's destination state.
	 * 
	 * @return destination of {@code this} transition.
	 */
	public State getDestination() {
		return this.destination;
	}
}
