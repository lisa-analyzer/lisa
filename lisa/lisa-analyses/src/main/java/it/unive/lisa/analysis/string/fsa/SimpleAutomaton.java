package it.unive.lisa.analysis.string.fsa;

import it.unive.lisa.analysis.numeric.Interval;
import it.unive.lisa.util.datastructures.automaton.Automaton;
import it.unive.lisa.util.datastructures.automaton.CyclicAutomatonException;
import it.unive.lisa.util.datastructures.automaton.State;
import it.unive.lisa.util.datastructures.automaton.Transition;
import it.unive.lisa.util.datastructures.regex.Atom;
import it.unive.lisa.util.datastructures.regex.RegularExpression;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.lisa.util.numeric.MathNumberConversionException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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

	/**
	 * Yields a new automaton where leading whitespaces have been removed from
	 * {@code this}.
	 * 
	 * @return a new automaton where leading whitespaces have been removed from
	 *             {@code this}
	 */
	public SimpleAutomaton trimLeft() {
		SimpleAutomaton result = copy();

		Set<Transition<StringSymbol>> toAdd = new HashSet<>();
		Set<Transition<StringSymbol>> toRemove = new HashSet<>();

		while (true) {
			for (Transition<StringSymbol> t : result.getOutgoingTransitionsFrom(result.getInitialState()))
				if (t.getSymbol().getSymbol().equals(" ")) {
					toRemove.add(t);
					toAdd.add(new Transition<StringSymbol>(t.getSource(), t.getDestination(), StringSymbol.EPSILON));
				}

			result.removeTransitions(toRemove);
			result.getTransitions().addAll(toAdd);

			result.minimize();

			boolean b = false;
			for (Transition<StringSymbol> t : result.getOutgoingTransitionsFrom(result.getInitialState())) {
				if (t.getSymbol().getSymbol().equals(" "))
					b = true;
			}

			if (!b)
				break;
		}

		return result;
	}

	/**
	 * Yields a new automaton where trailing whitespaces have been removed from
	 * {@code this}.
	 * 
	 * @return a new automaton where trailing whitespaces have been removed from
	 *             {@code this}
	 */
	public SimpleAutomaton trimRight() {

		SimpleAutomaton result = copy();

		Set<Transition<StringSymbol>> toAdd = new HashSet<>();
		Set<Transition<StringSymbol>> toRemove = new HashSet<>();

		while (true) {

			for (State qf : result.getFinalStates()) {
				for (Transition<StringSymbol> t : result.getIngoingTransitionsFrom(qf))
					if (t.getSymbol().getSymbol().equals(" ")) {
						toRemove.add(t);
						toAdd.add(
								new Transition<StringSymbol>(t.getSource(), t.getDestination(), StringSymbol.EPSILON));
					}
			}

			result.removeTransitions(toRemove);
			result.getTransitions().addAll(toAdd);

			result.minimize();

			boolean b = false;
			for (State qf : result.getFinalStates()) {

				for (Transition<StringSymbol> t : result.getIngoingTransitionsFrom(qf)) {
					if (t.getSymbol().equals(new StringSymbol(" ")))
						b = true;
				}
			}

			if (!b)
				break;
		}

		return result;
	}

	/**
	 * Yields a new automaton where leading and trailing whitespaces have been
	 * removed from {@code this}.
	 * 
	 * @return a new automaton where leading trailing whitespaces have been
	 *             removed from {@code this}
	 */
	public SimpleAutomaton trim() {
		return this.toRegex().trimRight().simplify().trimLeft().simplify().toAutomaton(this);
	}

	/**
	 * Yields a new automaton instance recognizing each string of {@code this}
	 * automaton repeated k-times, with k belonging to {@code intv}.
	 * 
	 * @param i the interval
	 * 
	 * @return a new automaton instance recognizing each string of {@code this}
	 *             automaton repeated k-times, with k belonging to {@code intv}
	 * 
	 * @throws MathNumberConversionException if {@code intv} is iterated but is
	 *                                           not finite
	 */
	public SimpleAutomaton repeat(Interval i) throws MathNumberConversionException {
		if (equals(emptyString()))
			return this;
		else if (hasCycle())
			return star();

		MathNumber high = i.interval.getHigh();
		MathNumber low = i.interval.getLow();
		SimpleAutomaton epsilon = emptyString();

		if (low.isMinusInfinity()) {
			if (high.isPlusInfinity())
				return epsilon.union(auxRepeat(new IntInterval(MathNumber.ONE, high), getInitialState(),
						new TreeSet<Transition<StringSymbol>>(), emptyLanguage()));

			if (high.isZero())
				return emptyString();
			else
				return epsilon.union(auxRepeat(new IntInterval(MathNumber.ONE, high), getInitialState(),
						new TreeSet<Transition<StringSymbol>>(), emptyLanguage()));
		}

		long lowInt = low.toLong();

		if (high.isPlusInfinity()) {
			// need exception
			if (lowInt < 0)
				return epsilon.union(auxRepeat(new IntInterval(MathNumber.ONE, high), getInitialState(),
						new TreeSet<Transition<StringSymbol>>(), emptyLanguage()));
			if (low.isZero())
				return epsilon.union(auxRepeat(new IntInterval(MathNumber.ONE, high), getInitialState(),
						new TreeSet<Transition<StringSymbol>>(), emptyLanguage()));
			if (lowInt > 0)
				return epsilon.union(auxRepeat(i.interval, getInitialState(), new TreeSet<Transition<StringSymbol>>(),
						emptyLanguage()));
		}

		long highInt = high.toLong();

		if (lowInt < 0) {
			if (highInt < 0)
				return emptyLanguage();

			if (high.isZero())
				return emptyString();
			else
				return epsilon.union(auxRepeat(new IntInterval(MathNumber.ONE, high), getInitialState(),
						new TreeSet<Transition<StringSymbol>>(), emptyLanguage()));
		}

		if (low.isZero()) {
			if (high.isZero())
				return emptyString();

			if (highInt > 0)
				return epsilon.union(auxRepeat(new IntInterval(MathNumber.ONE, high), getInitialState(),
						new TreeSet<Transition<StringSymbol>>(), emptyLanguage()));
		}

		if (lowInt > 0 && highInt > 0)
			return auxRepeat(i.interval, getInitialState(), new TreeSet<Transition<StringSymbol>>(), emptyLanguage());

		return emptyLanguage();
	}

	private SimpleAutomaton auxRepeat(IntInterval i, State currentState, SortedSet<Transition<StringSymbol>> delta,
			SimpleAutomaton result) throws MathNumberConversionException {

		if (currentState.isFinal()) {

			SortedSet<State> states = new TreeSet<>();

			for (State s : getStates()) {
				if (!s.equals(currentState))
					states.add(new State(s.getId(), s.isInitial(), false));
				else
					states.add(new State(s.getId(), s.isInitial(), s.isFinal()));
			}

			SimpleAutomaton temp = new SimpleAutomaton(states, delta);
			SimpleAutomaton tempResult = temp.copy();

			for (long k = 1; k < i.getLow().toLong(); k++)
				tempResult = tempResult.connectAutomaton(temp, tempResult.getFinalStates(), false);

			if (i.getHigh().isPlusInfinity()) {
				tempResult = tempResult.connectAutomaton(temp.copy().star(), tempResult.getFinalStates(), true);
			} else {
				for (long k = i.getLow().toLong(); k < i.getHigh().toLong(); k++)
					tempResult = tempResult.connectAutomaton(temp, tempResult.getFinalStates(), true);
			}

			tempResult = tempResult.minimize();
			result = result.union(tempResult);

		}

		for (Transition<StringSymbol> t : getOutgoingTransitionsFrom(currentState)) {
			SortedSet<Transition<StringSymbol>> clone = new TreeSet<Transition<StringSymbol>>(delta);
			clone.add(t);
			result = auxRepeat(i, t.getDestination(), clone, result).union(result);
		}

		return result;
	}

	private SimpleAutomaton connectAutomaton(SimpleAutomaton second, SortedSet<State> connectOn, boolean b) {
		SortedSet<Transition<StringSymbol>> delta = new TreeSet<>();
		SortedSet<State> states = new TreeSet<>();
		HashMap<State, State> firstMapping = new HashMap<>();
		HashMap<State, State> secondMapping = new HashMap<>();
		int c = 0;

		if (equals(emptyString())) {
			return second;
		}

		if (second.equals(emptyString())) {
			return this;
		}

		for (State s : getStates()) {
			State newState = null;
			if (b) {
				newState = new State(c++, s.isInitial(), s.isFinal());
			} else {
				if (connectOn.contains(s)) {
					newState = new State(c++, s.isInitial(), false);
				} else {
					newState = new State(c++, s.isInitial(), s.isFinal());
				}
			}
			states.add(newState);
			firstMapping.put(s, newState);
		}

		for (Transition<StringSymbol> t : getTransitions()) {
			delta.add(new Transition<>(firstMapping.get(t.getSource()), firstMapping.get(t.getDestination()),
					t.getSymbol()));
		}

		if (second.getStates().size() == 1 && second.getInitialState().isFinal()) {
			for (Transition<StringSymbol> t : second.getOutgoingTransitionsFrom(second.getInitialState())) {
				for (State s : connectOn) {
					State newState = new State(firstMapping.get(s).getId(), s.isInitial(), true);
					states.remove(firstMapping.get(s));
					states.add(newState);
					delta.add(new Transition<>(newState, newState, t.getSymbol()));
				}
				second.minimize();
			}
		} else {
			for (State s : second.getStates()) {
				State newState = new State(c++, s.isInitial(), s.isFinal());
				states.add(newState);
				secondMapping.put(s, newState);
			}

			states.remove(secondMapping.get(second.getInitialState()));
			secondMapping.remove(second.getInitialState());

			for (Transition<StringSymbol> t : second.getTransitions()) {
				if (t.getSource().equals(second.getInitialState())
						|| t.getDestination().equals(second.getInitialState())
						|| !secondMapping.containsKey(t.getSource()) || !secondMapping.containsKey(t.getDestination()))
					continue;

				if (!t.getSource().isInitial() && !t.getDestination().isInitial()) {
					delta.add(new Transition<>(secondMapping.get(t.getSource()), secondMapping.get(t.getDestination()),
							t.getSymbol()));
				}
				if (t.getSource().isInitial()) {
					// TODO better recheck this function
					for (State s : connectOn) {
						if (states.contains(secondMapping.get(t.getSource())))
							delta.add(new Transition<>(secondMapping.get(t.getSource()), firstMapping.get(s),
									t.getSymbol()));
					}
				}
			}

			for (Transition<StringSymbol> t : second.getOutgoingTransitionsFrom(second.getInitialState())) {
				for (State s : connectOn) {
					delta.add(new Transition<>(firstMapping.get(s), secondMapping.get(t.getDestination()),
							t.getSymbol()));
				}
			}
		}

		return new SimpleAutomaton(states, delta);
	}
}