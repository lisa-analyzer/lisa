package it.unive.lisa.util.datastructures.automaton;

import it.unive.lisa.util.datastructures.regex.Atom;
import it.unive.lisa.util.datastructures.regex.RegularExpression;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

public final class TestAutomaton extends Automaton<TestAutomaton, TestSymbol> {

	@Override
	public TestAutomaton singleString(
			String string) {
		return new TestAutomaton(string);
	}

	@Override
	public TestAutomaton unknownString() {
		throw new UnsupportedOperationException();
	}

	@Override
	public TestAutomaton emptyLanguage() {
		SortedSet<State> newStates = new TreeSet<>();
		SortedSet<Transition<TestSymbol>> newGamma = new TreeSet<>();
		State initialState = new State(0, true, false);
		newStates.add(initialState);

		return new TestAutomaton(newStates, newGamma);
	}

	@Override
	public TestAutomaton emptyString() {
		return new TestAutomaton("");
	}

	@Override
	public TestAutomaton from(
			SortedSet<State> states,
			SortedSet<Transition<TestSymbol>> transitions) {
		return new TestAutomaton(states, transitions);
	}

	@Override
	public TestSymbol epsilon() {
		return new TestSymbol("");
	}

	@Override
	public TestSymbol concat(
			TestSymbol first,
			TestSymbol second) {
		return first.concat(second);
	}

	@Override
	public RegularExpression symbolToRegex(
			TestSymbol symbol) {
		return new Atom(symbol.getSymbol());
	}

	/**
	 * Builds a new automaton with given {@code states} and {@code transitions}.
	 *
	 * @param states      the set of states of the new automaton
	 * @param transitions the set of the transitions of the new automaton
	 */
	public TestAutomaton(
			SortedSet<State> states,
			SortedSet<Transition<TestSymbol>> transitions) {
		super(states, transitions);
	}

	/**
	 * Builds a new automaton that accepts a given string.
	 * 
	 * @param s the only string accepted by the automaton.
	 */
	public TestAutomaton(
			String s) {
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

				transitions.add(new Transition<>(last, next, new TestSymbol("" + s.charAt(i))));
				last = next;
				states.add(last);
			}
		}
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
	public boolean validateString(
			String str) {
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
}