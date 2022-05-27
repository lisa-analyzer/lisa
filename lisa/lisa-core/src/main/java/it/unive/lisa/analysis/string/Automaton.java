package it.unive.lisa.analysis.string;

import java.util.HashSet;
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

	private final Set<Character> alphabet;

	/**
	 * Build a new automaton with given {@code states} and {@code transitions}.
	 * @param states the set of states of the new automaton
	 * @param transitions the set of the transitions of the new automaton
	 */
	public Automaton(Set<State> states, Set<Transition> transitions) {
		this.states = states;
		this.transitions = transitions;
		this.alphabet = new HashSet<>();
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

	// Brzozowski minimization algorithm
	public Automaton minimize() {
		return reverse().determinize().reach().reverse().determinize().reach();
	}

	// delete unreachable states from the automaton
	private Automaton reach() {
		defineAlphabet();
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

	// create a new automaton that accepts the reverse language of this
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

	// create a new deterministic automaton from this
	private Automaton determinize() {
		Set<Transition> tr = new HashSet<>();
		Set<State> st = new HashSet<>();
		
		// TODO vorrei una struttura dati che riesce che mi dia una relazione di
		// ordine per fare in modo di mappare uno a uno gli stati del dfa

		return new Automaton(st, tr);
	}

	// get the automaton alphabet using defined transitions
	private void defineAlphabet() {
		for (Transition T : transitions) {
			alphabet.add(T.getSymbol());
		}
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

	// compute the epsilon closures for a set of given states
	private Set<State> epsClosure(Set<State> st) {
		Set<State> eps = new HashSet<>();

		for (State s : st) {
			Set<State> e = epsClosure(s);
			eps.addAll(e);
		}
		return eps;
	}

}
