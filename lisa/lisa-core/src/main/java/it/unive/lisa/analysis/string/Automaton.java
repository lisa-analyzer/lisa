package it.unive.lisa.analysis.string;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A class that describes an generic automaton(dfa, nfa, epsilon nfa).
 *
 * @author <a href="mailto:simone.leoni2@studenti.unipr.it">Simone Leoni</a>
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 */
public final class Automaton {

	/**
	 * The states of the automaton
	 */
	private final Set<State> states;

	/**
	 * The transitions of the automaton
	 */
	private final Set<Transition> transitions;

	/**
	 * Set to {@code true} if and only if the automaton is determinized, i.e.,
	 * the method {@link Automaton#determinize} has been called on {@code this}.
	 * This field is not used inside {@link Automaton#equals}.
	 */
	private boolean IS_DETERMINIZED;

	/**
	 * Set to {@code true} if and only if the automaton is minimum, i.e., the
	 * method {@link Automaton#minimize} has been called on {@code this}.
	 * This field is not used inside {@link Automaton#equals}.
	 */
	private boolean IS_MINIMIZED;

	@Override
	public int hashCode() {
		return Objects.hash(states, transitions);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Automaton other = (Automaton) obj;
		return Objects.equals(states, other.states) && Objects.equals(transitions, other.transitions);
	}

	/**
	 * Build a new automaton with given {@code states} and {@code transitions}.
	 *
	 * @param states      the set of states of the new automaton
	 * @param transitions the set of the transitions of the new automaton
	 */
	public Automaton(Set<State> states, Set<Transition> transitions) {
		this.states = states;
		this.transitions = transitions;
		this.IS_DETERMINIZED = false;
		this.IS_MINIMIZED = false;
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
		Set<State> currentStates = epsClosure();

		for (int i = 0; i < str.length(); ++i) {
			String c = "" + str.charAt(i);

			// stores temporally the new currentStates
			Set<State> newCurr = new HashSet<>();
			for (State s : currentStates) {

				// stores all the states reached after char computation
				Set<State> dest = transitions.stream()
						.filter(t -> t.getSource().equals(s) && t.getSymbol().equals(c))
						.map(t -> t.getDestination())
						.collect(Collectors.toSet());
				if (!dest.isEmpty()) {
					dest = epsClosure(dest);
					newCurr.addAll(dest);
				}
			}
			currentStates = newCurr;
		}

		// checks if there is at least one final state in the set of possible
		// reached states at the end of the validation process
		return currentStates.stream().anyMatch(s -> s.isFinal());
	}

	/**
	 * Brzozowski minimization algorithm.
	 *
	 * @return the minimum automaton that accepts the same language as
	 *             {@code this}.
	 */
	public Automaton minimize() {
		if (IS_MINIMIZED)
			return this;
		Automaton min = reverse().determinize().reach().reverse().determinize().reach();
		min.IS_MINIMIZED = true;
		min.IS_DETERMINIZED = true;
		return min;
	}

	/**
	 * Remove all the unreachable states from the current automaton.
	 *
	 * @return a newly created automaton without the unreachable states of
	 *             {@code this}.
	 */
	private Automaton reach() {
		// stores the reached states of the automaton
		Set<State> RS = new HashSet<>();
		// states that will be checked in the following iteration
		Set<State> NS = new HashSet<>();
		// used to store temporarily the states reached from the states in NS
		Set<State> T;
		Set<State> initialStates = epsClosure();

		RS.addAll(initialStates);
		NS.addAll(initialStates);
		do {
			T = new HashSet<>();

			for (State q : NS) {
				T.addAll(transitions.stream()
						.filter(t -> t.getSource().equals(q))
						.map(t -> t.getDestination())
						.collect(Collectors.toSet()));
			}
			NS = T;
			RS.addAll(T);
		} while (!NS.isEmpty());
		// add to the new automaton only the transitions between the states of the new Automaton
		Set<Transition> tr = transitions;
		for (Transition t : tr)
			if (!RS.contains(t.getSource()) || !RS.contains(t.getDestination()))
				tr.remove(t);

		return new Automaton(RS, tr);
	}

	/**
	 * Creates an automaton that accept the reverse language.
	 *
	 * @return a newly created automaton that accepts the reverse language of
	 *             {@code this}.
	 */
	public Automaton reverse() {
		Set<Transition> tr = new HashSet<>();
		Set<State> st = new HashSet<>();
		// used to associate states of the Automaton this to the reverse one
		Map<State, State> revStates = new HashMap<>();

		for (State s : states) {
			boolean fin = false, init = false;
			if (s.isInitial())
				fin = true;
			if (s.isFinal())
				init = true;
			State q = new State(init, fin);
			st.add(q);
			// add association between the newly created state and the state of the automaton this
			revStates.put(s, q);
		}
		// create transitions using the new states of the reverse automaton
		for (Transition t : transitions)
			tr.add(new Transition(revStates.get(t.getDestination()), revStates.get(t.getSource()), t.getSymbol()));

		return new Automaton(st, tr);
	}

	/**
	 * Creates a deterministic automaton starting from {@code this}.
	 *
	 * @return a newly deterministic automaton that accepts the same language as
	 *             {@code this}.
	 */
	Automaton determinize() {
		if (IS_DETERMINIZED)
			return this;

		// transitions of the new deterministic automaton
		Set<Transition> delta = new HashSet<>();
		// states of the new deterministic automaton
		Set<State> states = new HashSet<>();
		// store the macrostates of the new Automaton
		List<HashSet<State>> detStates = new ArrayList<>();
		// used to map the macrostate with the corresponding state of the new automaton
		Map<State, HashSet<State>> stateToMacro = new HashMap<>();
		Map<HashSet<State>, State> macroToState = new HashMap<>();
		// stores the already controlled states
		Set<State> marked = new HashSet<>();
		// automaton alphabet
		Set<String> alphabet = transitions.stream()
				.filter(t -> !t.getSymbol().equals(""))
				.map(t -> t.getSymbol())
				.collect(Collectors.toSet());

		// the first macrostate is the one associated with the epsilon closure of the initial states
		HashSet<State> initialStates = (HashSet<State>) epsClosure();
		detStates.add(initialStates);
		State q = null;
		for(State s : initialStates)
			if(s.isFinal()) {
				q = new State(true, true);
				break;
			}
		if(q == null)
			q = new State(true, false);
		states.add(q);
		stateToMacro.put(q, initialStates);
		macroToState.put(initialStates, q);
		// used to keep track of current state
		State current = q;

		// iterate until all the states have been checked
		while (!marked.equals(states)) {
			for (State s : states) {
				// get the first state that has not been checked yet
				if (!marked.contains(s)) {
					marked.add(s);
					current = s;
					break;
				}
			}
			// macrostate corresponding to the current state
			Set<State> currStates = stateToMacro.get(current);
			// find all the destination states of any non epsilon transaction starting from a current state
			for (String c : alphabet) {
				Set<State> R = epsClosure(transitions.stream()
						.filter(t -> currStates.contains(t.getSource()) && t.getSymbol().equals(c)
								&& !t.getSymbol().equals(""))
						.map(t -> t.getDestination())
						.collect(Collectors.toSet()));
				// add R to detStates only if it is a new macrostate
				if (!detStates.contains(R) && !R.isEmpty()) {
					HashSet<State> currentStates = (HashSet<State>) R;
					detStates.add(currentStates);
					State nq = null;
					// make nq final if any of the state in the correspondent macrostate is final
					for(State s : R)
						if(s.isFinal()) {
							nq = new State(true, true);
							break;
						}
					if(nq == null)
						nq = new State(true, false);
					states.add(nq);
					stateToMacro.put(nq, currentStates);
					macroToState.put(currentStates, nq);
				}
				// add transition from currStates macrostate to R that is destination macrostate
				delta.add(new Transition(macroToState.get(currStates), macroToState.get(R), c));
			}
		}

		Automaton det = new Automaton(states, delta);
		det.IS_DETERMINIZED = true;
		return det;
	}

	/**
	 * Computes the epsilon closure of this automaton starting from its initial
	 * states, namely the set of states that are reachable from all the initial
	 * states just with epsilon transitions.
	 *
	 * @return the set of states that are reachable from all the initial states
	 *             just with epsilon transitions.
	 */
	Set<State> epsClosure() {
		return epsClosure(states.stream().filter(s -> s.isInitial()).collect(Collectors.toSet()));
	}

	/**
	 * Computes the epsilon closure of this automaton starting from
	 * {@code state}, namely the set of states that are reachable from
	 * {@code state} just with epsilon transitions.
	 *
	 * @param state the state from which the method starts to compute the
	 *                  epsilon closure
	 *
	 * @return the set of states that are reachable from {@code state} just with
	 *             epsilon transitions.
	 */
	Set<State> epsClosure(State state) {
		Set<State> eps = new HashSet<>();
		eps.add(state);
		// used to make sure that a state isn't checked twice
		Set<State> checked = new HashSet<>();

		// add current state
		do {
			// used to collect new states that have to be added to eps inside
			// for
			// loop
			Set<State> temp = new HashSet<>();
			for (State s : eps) {
				if (!checked.contains(s)) {
					checked.add(s);

					// collect all the possible destination from the current
					// state
					Set<State> dest = transitions.stream()
							.filter(t -> t.getSource().equals(s) && t.getSymbol().equals(""))
							.map(t -> t.getDestination())
							.collect(Collectors.toSet());

					temp.addAll(dest);
				}
			}

			eps.addAll(temp);

		} while (!checked.containsAll(eps));

		return eps;
	}

	/**
	 * Computes the epsilon closure of this automaton starting from
	 * {@code state}, namely the set of states that are reachable from
	 * {@code st} just with epsilon transitions.
	 *
	 * @param st the set of states from which the epsilon closure is computed.
	 *
	 * @return the set of states that are reachable from {@code state} just with
	 *             epsilon transitions.
	 */
	private Set<State> epsClosure(Set<State> st) {
		Set<State> eps = new HashSet<>();

		for (State s : st)
			eps.addAll(epsClosure(s));

		return eps;
	}

	/**
	 * Yields the automaton recognizing the language that is the union of the languages recognized by {@code this} and {@code other}.
	 * @param other the other automaton
	 * @return Yields the automaton recognizing the language that is the union of the languages recognized by {@code this} and {@code other}
	 */

	public Automaton union(Automaton other) {
		if (this == other)
			return this;

		Set<State> sts = new HashSet<>();
		Set<Transition> ts = new HashSet<>();

		Map<State, State> thisInitMapping = new HashMap<>();
		Map<State, State> otherInitMapping = new HashMap<>();

		for (State s : states)
			if (s.isInitial()) {
				State st = new State(false, s.isFinal());
				sts.add(st);
				thisInitMapping.put(s, st);
			} else
				sts.add(s);

		for (State s : other.states)
			if (s.isInitial()) {
				State st = new State(false, s.isFinal());
				sts.add(st);
				otherInitMapping.put(s, st);
			} else
				sts.add(s);

		State q0 = new State(true, false);

		for (State s : sts)

			if (thisInitMapping.values().contains(s) || otherInitMapping.values().contains(s))
				ts.add(new Transition(q0, s, ""));

		sts.add(q0);

		for (Transition t : transitions) {
			State source = thisInitMapping.keySet().contains(t.getSource()) ? thisInitMapping.get(t.getSource()) : t.getSource();
			State dest = thisInitMapping.keySet().contains(t.getDestination()) ? thisInitMapping.get(t.getDestination()) : t.getDestination();
			ts.add(new Transition(source, dest, t.getSymbol()));
		}

		for (Transition t : other.transitions) {
			State source = otherInitMapping.keySet().contains(t.getSource()) ? otherInitMapping.get(t.getSource()) : t.getSource();
			State dest = otherInitMapping.keySet().contains(t.getDestination()) ? otherInitMapping.get(t.getDestination()) : t.getDestination();
			ts.add(new Transition(source, dest, t.getSymbol()));
		}

		Automaton result = new Automaton(sts, ts);
		result.IS_DETERMINIZED = false;
		result.IS_MINIMIZED = false;
		return result;
	}

	/**
	 * Returns a set of string containing all the strings accepted by
	 * {@code this} of length from 1 to {@code length}.
	 *
	 * @param length the maximum length of the strings to be returned
	 *
	 * @return a set containing the subset of strings accepted by {@code this}
	 */
	public Set<String> getLanguageAtMost(int length) {
		Set<String> lang = new HashSet<>();
		Set<State> initialStates = (HashSet<State>) epsClosure();

		for (State s : initialStates)
			lang.addAll(getLanguageAtMost(s, length));


		return lang;
	}

	/**
	 * Returns a set of string containing all the string accepted by
	 * {@code this} of length from 1 to {@code length} from a given state.
	 *
	 * @param q      state from which the strings are computed
	 * @param length maximum length of the computed strings
	 *
	 * @return a set containing a subset of strings accepted by {@code this}
	 *             starting from the state {@code q} of maximum length
	 *             {@code length}.
	 */
	public Set<String> getLanguageAtMost(State q, int length) {

		if (length == 0)
			return new HashSet<>();;

		Set<State> ws = Collections.singleton(q);
		Set<String> lang = new HashSet<>();
		lang.add("");

		while (length > 0) {

			Set<Transition> outgoing = new HashSet<>();;
			for (State s : ws)
				outgoing.addAll(getOutgoingTranstionsFrom(s));

			Set<String> newStrings = new HashSet<>();
			for (Transition t : outgoing)
				for (String s : lang)
					newStrings.add(s + t.getSymbol());

			lang.addAll(newStrings);
			ws = outgoing.stream().map(t -> t.getDestination()).collect(Collectors.toSet());
			length--;
		}

		return lang;

	}

	/**
	 * Returns all the outgoing transitions from a given state.
	 * @param q the source of the transitions.
	 * @return a Set containing all the outgoing transitions from the state q.
	 */
	private Set<Transition> getOutgoingTranstionsFrom(State q) {
		return transitions.stream()
				.filter(t -> t.getSource().equals(q))
				.collect(Collectors.toSet());
	}

	/**
	 * Checks if the Automaton {@code this} has any cycle.
	 * @return a boolean value that tells if {@code this} has any cycle.
	 */
	boolean hasCycle() {
		// visit the automaton to check if there is any cycle
		Set<State> currentStates = states.stream()
				.filter(s -> s.isInitial())
				.collect(Collectors.toSet());
		Set<State> visited = new HashSet<>();
		while(!visited.containsAll(states)) {
			Set<State> temp = new HashSet<>();
			for(State s : currentStates) {
				temp.addAll(transitions.stream()
						.filter(t -> t.getSource().equals(s))
						.map(t -> t.getDestination())
						.collect(Collectors.toSet()));
			}
			for(State s : temp)
				if(visited.contains(s))
					return true;
			visited.addAll(currentStates);
			currentStates = temp;
		}

		return false;
	}

	/**
	 * Returns the language accepted by {@code this}.
	 * @return a set representing the language accepted by the Automaton {@code this}.
	 */
	Set<String> getLanguage() {
		Set<String> lang = new HashSet<>();
		if(hasCycle())
			return lang;

		// stack used to keep track of transitions that will be "visited"
        // each element is a pair to keep track of old String and next Transition
		LinkedList<AbstractMap.SimpleImmutableEntry<String, Transition>> stack = new LinkedList<>();
		Set<State> initialStates = states.stream()
				.filter(s -> s.isInitial())
				.collect(Collectors.toSet());
		// add initial states transitions to stack
		for(State q : initialStates) {
			for(Transition t : getOutgoingTranstionsFrom(q)) {
				stack.addFirst(new AbstractMap.SimpleImmutableEntry<>("", t));
			}
		}
		// generate all the strings and add them to lang
		while(!stack.isEmpty()) {
			AbstractMap.SimpleImmutableEntry<String, Transition> top = stack.removeFirst();
			String currentString = top.getKey();
			Transition tr = top.getValue();
			// when there it finds a final state it adds the generated string to the language
			if(tr.getDestination().isFinal())
				lang.add(currentString + tr.getSymbol());
			// adds all the possible path from current transition destination to the stack
			else
				for(Transition t : getOutgoingTranstionsFrom(tr.getDestination()))
					stack.addFirst(new AbstractMap.SimpleImmutableEntry<>(currentString + tr.getSymbol(), t));
		}

		return lang;
	}

	/**
	 * Creates a new {@code Automaton} that is the same as {@code this} but is complete.
	 */
	public Automaton complete() {
		Automaton result = new Automaton(this.states, this.transitions);
		// add a new "garbage" state
		State garbage = new State(false, false);
		result.states.add(garbage);
		Set<String> alphabet = transitions.stream()
				.map(Transition::getSymbol)
				.collect(Collectors.toSet());

		// adds all the transitions to the garbage state
		for(State s : result.states)
			for(String c : alphabet)
				if(transitions.stream()
						.map(Transition::getSymbol)
						.collect(Collectors.toSet()).isEmpty())
					result.transitions.add(new Transition(s, garbage, c));

		return result;
	}

	/**
	 * Return a new Automaton that accept a language that is the complementary language of {@code this}.
	 * @return the complement Automaton of {@code this}.
	 */
	public Automaton complement() {
		// states and transitions for the newly created automaton
		Set<State> sts = new HashSet<>();
		Set<Transition> delta = new HashSet<>();
		// keep track of the corresponding newly created states
		Map<State, State> oldToNew = new HashMap<>();

		// creates all the new states
		for(State s : states) {
			boolean isInitial = false, isFinal = false;
			if(s.isInitial())
				isFinal = true;
			if(s.isFinal())
				isInitial = true;
			State q = new State(isInitial, isFinal);
			sts.add(q);
			oldToNew.put(s, q);
		}

		// creates all the new transitions
		for(Transition t : transitions) {
			delta.add(new Transition(oldToNew.get(t.getSource()), oldToNew.get(t.getDestination()), t.getSymbol()));
		}

		return new Automaton(sts, delta);
	}
}
