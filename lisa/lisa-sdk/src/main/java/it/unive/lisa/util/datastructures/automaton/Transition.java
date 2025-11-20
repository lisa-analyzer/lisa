package it.unive.lisa.util.datastructures.automaton;

import java.util.Objects;

/**
 * A class that describes an Automaton transition.
 * 
 * @author <a href="mailto:simone.leoni2@studenti.unipr.it">Simone Leoni</a>
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 * 
 * @param <T> the concrete type of {@link TransitionSymbol}s that the transition
 *                recognizes
 */
public final class Transition<T extends TransitionSymbol<T>>
		implements
		Comparable<Transition<T>> {

	private final State source;

	private final State destination;

	private final T symbol;

	/**
	 * Creates a new transition for a generic automaton.
	 * 
	 * @param source      state from which the transition could be applied.
	 * @param destination state reached after the transition execution.
	 * @param symbol      the character that have to be read to execute the
	 *                        transition.
	 */
	public Transition(
			State source,
			State destination,
			T symbol) {
		Objects.requireNonNull(source);
		Objects.requireNonNull(destination);
		this.source = source;
		this.destination = destination;
		this.symbol = symbol;
	}

	@Override
	public int hashCode() {
		return Objects.hash(destination, source, symbol);
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
		Transition<?> other = (Transition<?>) obj;
		return Objects.equals(destination, other.destination)
				&& Objects.equals(source, other.source)
				&& Objects.equals(symbol, other.symbol);
	}

	@Override
	public String toString() {
		return source.getState() + " [" + symbol + "] -> " + destination.getState();
	}

	/**
	 * Yields {@code true} if and only if this transition recognizes <b>only</b>
	 * the empty string.
	 * 
	 * @return {@code true} if and only if that condition holds
	 */
	public boolean isEpsilonTransition() {
		return symbol.isEpsilon();
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
	public T getSymbol() {
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

	@Override
	public int compareTo(
			Transition<T> transition) {
		int cmp;
		if ((cmp = source.compareTo(transition.source)) != 0)
			return cmp;
		if ((cmp = destination.compareTo(transition.destination)) != 0)
			return cmp;
		return symbol.compareTo(transition.symbol);
	}

}
