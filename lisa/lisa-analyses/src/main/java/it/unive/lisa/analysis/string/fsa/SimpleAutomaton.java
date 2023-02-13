package it.unive.lisa.analysis.string.fsa;

import it.unive.lisa.util.datastructures.automaton.Automaton;
import it.unive.lisa.util.datastructures.automaton.CyclicAutomatonException;
import it.unive.lisa.util.datastructures.automaton.State;
import it.unive.lisa.util.datastructures.automaton.Transition;
import it.unive.lisa.util.datastructures.regex.Atom;
import it.unive.lisa.util.datastructures.regex.RegularExpression;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * A class that describes an generic automaton(dfa, nfa, epsilon nfa) using a
 * standard alphabet of single characters.
 *
 * @author <a href="mailto:simone.leoni2@studenti.unipr.it">Simone Leoni</a>
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public final class SimpleAutomaton extends Automaton<SimpleAutomaton, StringSymbol> {

	@Override
	public SimpleAutomaton singleString(String string) {
		return new SimpleAutomaton(string);
	}

	@Override
	public SimpleAutomaton unknownString() {
		SortedSet<State> newStates = new TreeSet<>();
		SortedSet<Transition<StringSymbol>> newGamma = new TreeSet<>();
		State initialState = new State(0, true, true);

		newStates.add(initialState);

		for (char alphabet = '!'; alphabet <= '~'; ++alphabet)
			newGamma.add(new Transition<>(initialState, initialState, new StringSymbol(alphabet)));

		return new SimpleAutomaton(newStates, newGamma);
	}

	@Override
	public SimpleAutomaton emptyLanguage() {
		SortedSet<State> newStates = new TreeSet<>();
		State initialState = new State(0, true, false);
		newStates.add(initialState);

		return new SimpleAutomaton(newStates, Collections.emptySortedSet());
	}

	@Override
	public SimpleAutomaton emptyString() {
		return new SimpleAutomaton("");
	}

	@Override
	public SimpleAutomaton from(SortedSet<State> states, SortedSet<Transition<StringSymbol>> transitions) {
		return new SimpleAutomaton(states, transitions);
	}

	@Override
	public StringSymbol epsilon() {
		return StringSymbol.EPSILON;
	}

	@Override
	public StringSymbol concat(StringSymbol first, StringSymbol second) {
		return first.concat(second);
	}

	@Override
	public RegularExpression symbolToRegex(StringSymbol symbol) {
		return new Atom(symbol.getSymbol());
	}

	/**
	 * Builds a new automaton with given {@code states} and {@code transitions}.
	 *
	 * @param states      the set of states of the new automaton
	 * @param transitions the set of the transitions of the new automaton
	 */
	public SimpleAutomaton(SortedSet<State> states, SortedSet<Transition<StringSymbol>> transitions) {
		super(states, transitions);
	}

	/**
	 * Builds a new automaton that accepts a given string.
	 * 
	 * @param s the only string accepted by the automaton.
	 */
	public SimpleAutomaton(String s) {
		super();

		if (s.isEmpty())
			states.add(new State(0, true, true));
		else {
			State last = new State(0, true, false);
			State next;
			states.add(last);
			for (int i = 0; i < s.length(); ++i) {
				if (i != s.length() - 1)
					next = new State(i + 1, false, false);
				else
					next = new State(i + 1, false, true);

				transitions.add(new Transition<>(last, next, new StringSymbol(s.charAt(i))));
				last = next;
				states.add(last);
			}
		}

		deterministic = Optional.of(true);
		minimized = Optional.of(true);
	}

	/**
	 * Computes all the automaton transitions to validate a given string
	 * {@code str}.
	 *
	 * @param str String that has to be checked.
	 * 
	 * @return a boolean value that indicates either if {@code str} has been
	 *             accepted or not.
	 */
	public boolean validateString(String str) {
		// stores all the possible states reached by the automaton after each
		// input char
		Set<State> currentStates = epsilonClosure(getInitialStates());

		for (int i = 0; i < str.length(); ++i) {
			String c = "" + str.charAt(i);

			// stores temporally the new currentStates
			Set<State> newCurr = new TreeSet<>();
			for (State s : currentStates) {

				// stores all the states reached after char computation
				Set<State> dest = transitions.stream()
						.filter(t -> t.getSource().equals(s) && t.getSymbol().getSymbol().equals(c))
						.map(Transition::getDestination).collect(Collectors.toSet());
				if (!dest.isEmpty()) {
					dest = epsilonClosure(dest);
					newCurr.addAll(dest);
				}
			}
			currentStates = newCurr;
		}

		// checks if there is at least one final state in the set of possible
		// reached states at the end of the validation process
		return currentStates.stream().anyMatch(State::isFinal);
	}

	/**
	 * Yields a new automaton where all occurrences of strings recognized by
	 * {@code toReplace} are replaced with the automaton {@code str}, assuming
	 * that {@code toReplace} is finite (i.e., no loops nor top-transitions).
	 * The resulting automaton is then collapsed.<br>
	 * <br>
	 * 
	 * @param toReplace the automaton recognizing the strings to replace
	 * @param str       the automaton that must be used as replacement
	 * 
	 * @return the replaced automaton
	 * 
	 * @throws CyclicAutomatonException if {@code toReplace} contains loops
	 */
	public SimpleAutomaton replace(SimpleAutomaton toReplace, SimpleAutomaton str) throws CyclicAutomatonException {
		if (this.hasCycle() || toReplace.hasCycle() || str.hasCycle())
			return unknownString();

		SimpleAutomaton result = emptyLanguage();
		for (String a : getLanguage())
			for (String b : toReplace.getLanguage())
				for (String c : str.getLanguage())
					if (a.contains(b))
						result = result.union(new SimpleAutomaton(a.replace(b, c)));
		return result;
	}
}