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

	// if symbol is ' ' means it is an epsilon transition
	private final char symbol;

	/**
	 * Creates a new transition for a generic automaton.
	 * @param source state from which the transition could be applied.
	 * @param destination state reached after the transition execution.
	 * @param symbol the character that have to be read to execute the transition. {@code ' '} is used for epsilon transitions.
	 */
	public Transition(State source, State destination, char symbol) {
		this.source = source;
		this.destination = destination;
		this.symbol = symbol;
	}

	@Override
	public int hashCode() {
		return Objects.hash(destination, source, symbol);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Transition other = (Transition) obj;
		return Objects.equals(destination, other.destination) && Objects.equals(source, other.source)
				&& symbol == other.symbol;
	}
	
	/**
	 * Tells transition's source state.
	 * @return source of {@code this} transition.
	 */
	public State getSource() {
		return this.source;
	}

	/**
	 * Tells input symbol for {@code this} transition.
	 * @return symbol needed to execute {@code this} transition.
	 */
	public char getSymbol() {
		return this.symbol;
	}

	/**
	 * Tells transition's destination state.
	 * @return destination of {@code this} transition.
	 */
	public State getDestination() {
		return this.destination;
	}
}
