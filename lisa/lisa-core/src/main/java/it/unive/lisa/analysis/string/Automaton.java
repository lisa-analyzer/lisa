package it.unive.lisa.analysis.string;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collector;
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
	 * the method {@link Automaton#determinize} has been called on {@code this}
	 */
	private boolean IS_DETERMINIZED;

	/**
	 * Set to {@code true} if and only if the automaton is minimum, i.e., the
	 * method {@link Automaton#minimize} has been called on {@code this}
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
		Set<State> currentStates = epsClosure(states.stream().filter(s -> s.isInitial()).collect(Collectors.toSet()));

		for (int i = 0; i < str.length(); ++i) {
			// TODO chiedere al prof per correttezza o se usare qualche funzione
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
		Set<State> RS = new HashSet<>();
		Set<State> NS = new HashSet<>();
		Set<State> T;
		Set<State> initialStates = states.stream().filter(s -> s.isInitial()).collect(Collectors.toSet());

		RS.addAll(initialStates);
		NS.addAll(initialStates);
		do {
			T = new HashSet<>();

			for (State q : NS) {
				T.addAll(transitions.stream()
						.filter(t -> t.getSource().equals(q))
						.map(t -> t.getDestination())
						.filter(s -> !RS.contains(s))
						.collect(Collectors.toSet()));
			}
			NS = T;
			RS.addAll(T);
		} while (!NS.isEmpty());
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
	private Automaton reverse() {
		Set<Transition> tr = new HashSet<>();
		Set<State> st = new HashSet<>();
		Set<State> is = new HashSet<>();
		Set<State> fs = new HashSet<>();

		for (Transition t : transitions)
			tr.add(new Transition(t.getDestination(), t.getSource(), t.getSymbol()));

		for (State s : states) {
			int id = 0;
			boolean fin = false, init = false;
			if (s.isInitial())
				fin = true;
			if (s.isFinal())
				init = true;
			st.add(new State(id, init, fin));
			++id;
		}

		for (State s : st) {
			if (s.isInitial())
				is.add(s);
			if (s.isFinal())
				fs.add(s);
		}

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
		// stores the already controlled states
		Set<State> marked = new HashSet<>();
		// stores number of states of the new Automaton
		int count = 0;
		// automaton alphabet
		Set<String> alphabet = transitions.stream()
				.filter(t -> !t.getSymbol().equals(""))
				.map(t -> t.getSymbol())
				.collect(Collectors.toSet());

		detStates.add((HashSet<State>) epsClosure());
		states.add(new State(count, true, false));
		count++;

		while (!marked.equals(states)) {
			int current = -1;
			for (State s : states) {
				if (!marked.contains(s)) {
					marked.add(s);
					current = s.getId();
					break;
				}
			}

			Set<State> currStates = detStates.get(current);
			for (String c : alphabet) {
				Set<State> R = epsClosure(transitions.stream()
						.filter(t -> currStates.contains(t.getSource()) && t.getSymbol().equals(c)
								&& !t.getSymbol().equals(""))
						.map(t -> t.getDestination())
						.collect(Collectors.toSet()));
				if (!detStates.contains(R) && !R.isEmpty()) {
					detStates.add((HashSet<State>) R);
					states.add(new State(count, false, false));
					count++;
				}
				State source = null;
				State destination = null;
				for (State s : states) {
					if (s.getId() == detStates.indexOf(R)) {
						destination = s;
					}
					if (s.getId() == current)
						source = s;
				}
				if (source != null && destination != null)
					delta.add(new Transition(source, destination, c));
			}
		}

		Set<State> sts = new HashSet<>();
		for (State s : states) {
			Set<State> macroState = detStates.get(s.getId());
			State st = null;
			for (State q : macroState) {
				if (q.isFinal()) {
					st = new State(s.getId(), s.isInitial(), true);
					break;
				}
			}
			if (st == null)
				sts.add(s);
			else
				sts.add(st);
		}

		// update transitions with final states where needed
		Set<Transition> tr = new HashSet<>();
		for (Transition t : delta) {
			State source = t.getSource();
			State dest = t.getDestination();
			for (State q : sts) {
				if (q.getId() == t.getSource().getId() && q.isFinal())
					source = q;
				if (q.getId() == t.getDestination().getId() && q.isFinal())
					dest = q;
			}
			tr.add(new Transition(source, dest, t.getSymbol()));
		}

		Automaton det = new Automaton(sts, tr);
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
				State st = new State(s.getId(), false, s.isFinal());
				sts.add(st);
				thisInitMapping.put(s, st);
			} else
				sts.add(s);

		for (State s : other.states)
			if (s.isInitial()) {
				State st = new State(s.getId(), false, s.isFinal());
				sts.add(st);
				otherInitMapping.put(s, st);
			} else
				sts.add(s);

		State q0 = new State(0, true, false);

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
		Set<State> initialStates = states.stream()
				.filter(s -> s.isInitial())
				.collect(Collectors.toSet());

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

	private Set<Transition> getOutgoingTranstionsFrom(State q) {
		return transitions.stream()
				.filter(t -> t.getSource().equals(q))
				.collect(Collectors.toSet());
	}

}
