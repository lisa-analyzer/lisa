package it.unive.lisa.util.datastructures.automaton;

import java.util.SortedSet;

/**
 * A factory for creating instances of {@link Automaton}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the concrete type of the automata created by this factory
 * @param <T> the concrete type of {@link TransitionSymbol}s that instances of
 *                {@code A} have on their transitions
 */
public interface AutomataFactory<A extends Automaton<A, T>, T extends TransitionSymbol<T>> {

	/**
	 * Yields a new automaton recognizing only the given string.
	 * 
	 * @param string the string to recognize
	 * 
	 * @return the new automaton
	 */
	A singleString(
			String string);

	/**
	 * Yields a new automaton recognizing a statically-unknown string.
	 * 
	 * @return the new automaton
	 */
	A unknownString();

	/**
	 * Yields a new automaton recognizing the empty language.
	 * 
	 * @return the new automaton
	 */
	A emptyLanguage();

	/**
	 * Yields a new automaton recognizing only the empty string.
	 * 
	 * @return the new automaton
	 */
	A emptyString();

	/**
	 * Builds a new automaton with the given states and transitions.
	 * 
	 * @param states      the states
	 * @param transitions the transitions
	 * 
	 * @return the new automaton
	 */
	A from(
			SortedSet<State> states,
			SortedSet<Transition<T>> transitions);

}
