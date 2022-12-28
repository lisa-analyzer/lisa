package it.unive.lisa.util.datastructures.automaton;

/**
 * A symbol that can be read by {@link Transition}s of an {@link Automaton}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <T> the concrete type of the symbol
 */
public interface TransitionSymbol<T> extends Comparable<T> {

	/**
	 * The string used to represent an unknown transaction symbol, that can take
	 * the form of any other (e.g., any character, any string, ...).
	 */
	static final String UNKNOWN_SYMBOL = "\u0372";

	/**
	 * The string used to represent an empty transaction symbol, usually called
	 * epsilon.
	 */
	static final String EPSILON = "\u03b5";

	/**
	 * Yields {@code true} if and only if this symbol represents the empty
	 * string epsilon.
	 * 
	 * @return {@code true} if that condition holds.
	 */
	boolean isEpsilon();

	/**
	 * Yields a new symbol that corresponds to this one, but read back-to-front.
	 * 
	 * @return the reversed symbol
	 */
	T reverse();
}
