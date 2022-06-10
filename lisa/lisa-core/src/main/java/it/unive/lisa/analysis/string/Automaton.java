package it.unive.lisa.analysis.string;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A class that describes an generic automaton(dfa, nfa, epsilon nfa).
 * 
 * @author <a href="mailto:simone.leoni2@studenti.unipr.it">Simone Leoni</a>
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 */
public final class Automaton {

	private final Set<State> states;

	private final Set<Transition> transitions;

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
	 * @param states the set of states of the new automaton
	 * @param transitions the set of the transitions of the new automaton
	 */
	public Automaton(Set<State> states, Set<Transition> transitions) {
		this.states = states;
		this.transitions = transitions;
	}

	/**
	 * Computes all the automaton transitions to validate a given string {@code str}.
	 * @param str String that has to be checked.
	 * @return a boolean value that indicates either if {@code str} has been accepted or not.
	 */
	public boolean validateString(String str) {
		// stores all the possible states reached by the automaton after each
		// input char
		Set<State> currentStates = epsClosure(states.stream().filter(s -> s.isInitial()).collect(Collectors.toSet()));
		// stores all the states reached after char computation
		Set<State> dest = new HashSet<>();
		// stores temporally the new currentStates
		Set<State> newCurr;

		for (int i = 0; i < str.length(); ++i) {
			char c = str.charAt(i);

			newCurr = new HashSet<>();
			for (State s : currentStates) {
				dest = transitions.stream()
						.filter(t -> t.getSource().equals(s) && t.getSymbol() == c)
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
	 * @return the minimum automaton that accepts the same language as {@code this}.
	 */
	public Automaton minimize() {
		return reverse().determinize().reach().reverse().determinize().reach();
	}

	/**
	 * Remove all the unreachable states from the current automaton.
	 * @return a newly created automaton without the unreachable states of {@code this}.
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
	 * @return a newly created automaton that accepts the reverse language of {@code this}.
	 */
	private Automaton reverse() {
		Set<Transition> tr = new HashSet<>();
		Set<State> st = new HashSet<>();
		Set<State> is = new HashSet<>();
		Set<State> fs = new HashSet<>();
		for (Transition t : transitions) {
			tr.add(new Transition(t.getDestination(), t.getSource(), t.getSymbol()));
		}
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
	 * @return a newly deterministic automaton that accepts the same language as {@code this}.
	 */
	Automaton determinize() {
		// transitions of the new deterministic automaton
		Set<Transition> delta = new HashSet<>();
		// states of the new deterministic automaton
		Set<State> states = new HashSet<>();
		// store the macrostates of the new Automaton
		ArrayList<HashSet<State>> detStates = new ArrayList<>();
		// stores the already controlled states
		Set<State> marked = new HashSet<>();
		// stores number of states of the new Automaton
		int count = 0;
		// automaton alphabet
		Set<Character> alphabet = transitions.stream()
				.filter(t -> t.getSymbol() != ' ')
				.map(t -> t.getSymbol())
				.collect(Collectors.toSet());

		detStates.add((HashSet<State>) epsClosure());
		states.add(new State(count, true, false));
		count++;

		while(!marked.equals(states)) {
			int current = -1;
			for(State s : states) {
				if(marked.contains(s))
					continue;
				marked.add(s);
				current = s.getId();
				break;
			}
			Set<State> currStates = detStates.get(current);
			for(Character c : alphabet) {
				Set<State> R = epsClosure(transitions.stream()
						.filter(t -> currStates.contains(t.getSource()) && t.getSymbol() == c && t.getSymbol() != ' ')
						.map(t -> t.getDestination())
						.collect(Collectors.toSet()));
				if(!detStates.contains(R) && !R.isEmpty()) {
					detStates.add((HashSet<State>) R);
					states.add(new State(count, false, false));
					count++;
				}
				State source = null;
				State destination = null;
				for(State s : states) {
					if(s.getId() == detStates.indexOf(R)) {
						destination = s;
					}
					if(s.getId() == current)
						source = s;
				}
				if(source != null && destination != null)
					delta.add(new Transition(source, destination, c));
			}
		}

		for(State s : states) {
			Set<State> macroState = detStates.get(s.getId());
			for(State q : macroState) {
				if(q.isFinal()) {
					s.setFinal();
					break;
				}
			}
		}

		return new Automaton(states, delta);
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

	// compute all the states reachable using epsilon closures from a given
	// state

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
		// collect all the possible destination from the current state
		Set<State> dest;
		// used to collect new states that have to be added to eps inside for
		// loop
		Set<State> temp;
		// add current state
		do {
			temp = new HashSet<>();
			for (State s : eps) {
				if (checked.contains(s))
					continue;

				checked.add(s);

				// stream vuoto non trova risultati anche se ci sono
				dest = transitions.stream()
						.filter(t -> t.getSource().equals(s) && t.getSymbol() == ' ')
						.map(t -> t.getDestination())
						.collect(Collectors.toSet());

				temp.addAll(dest);
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

		for (State s : st) {
			Set<State> e = epsClosure(s);
			eps.addAll(e);
		}
		return eps;
	}
}
