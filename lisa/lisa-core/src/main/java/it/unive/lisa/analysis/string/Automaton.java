package it.unive.lisa.analysis.string;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class Automaton {

	private final Set<State> states;

	private final Set<Transition> transitions;

	private final Set<State> initialStates, finalStates;

	private final Set<Character> alphabet;

	// constructor
	public Automaton() {
		states = new HashSet<>();
		transitions = new HashSet<>();
		initialStates = new HashSet<>();
		finalStates = new HashSet<>();
		alphabet = new HashSet<>();
	}

	public Automaton(Set<State> states, Set<Transition> transitions, Set<State> initialStates, Set<State> finalStates) {
		this.states = states;
		this.transitions = transitions;
		this.initialStates = initialStates;
		this.finalStates = finalStates;
		this.alphabet = new HashSet<>();
	}

	// add a new a transition to the automaton
	public void addTransition(Transition t) {
		transitions.add(t);
	}

	// add a new state to the automaton 
	public void addState(State s) {
		if(s.isInitial())
			initialStates.add(s);
		states.add(s);
	}

	// given a string as input the automaton checks if it is part of its language
	public boolean validateString(String str) {
		// stores all the possible states reached by the automaton after each input char
		Set<State> currentStates = epsClosure(initialStates);
		// stores all the states reached after char computation
		Set<State> dest = new HashSet<>();

		for(int i = 0; i < str.length(); ++i) {
			char c = str.charAt(i);
			for(State s : currentStates) {
				dest = transitions.stream()
						.filter(t -> t.getSource().equals(s) && t.getSymbol() == c)
						.map(t -> t.getDestination())
						.collect(Collectors.toSet());
				dest = epsClosure(dest);
			}
			currentStates = dest;
		}
		currentStates = epsClosure(currentStates);

		// checks if there is at least one final state in the set of possible reached states at the end of the validation process
		for(State s : currentStates)
			if(finalStates.contains(s))
				return true;

		return false;
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
		Set<State> is = new HashSet<>();
		Set<State> fs = new HashSet<>();
		Set<State> T;
		RS.addAll(initialStates);
		NS.addAll(initialStates);
		do {
			T = new HashSet<>();

			for(State q : NS) {
				T.addAll(transitions.stream()
						.filter(t -> t.getSource().equals(q))
						.map(t -> t.getDestination())
						.filter(s -> !RS.contains(s))
						.collect(Collectors.toSet()));
			}
			NS = T;
			RS.addAll(T);
		}  while(!NS.isEmpty());
		Set<Transition> tr = transitions;
		for(Transition t : tr)
			if(!RS.contains(t.getSource()) || !RS.contains(t.getDestination()))
				tr.remove(t);
		for(State s : RS) {
			if(s.isInitial())
				is.add(s);
			if(s.isFinal())
				fs.add(s);
		}
		return new Automaton(RS, tr, is, fs);
	}

	// create a new automaton that accepts the reverse language of this
	private Automaton reverse() {
		Set<Transition> tr = new HashSet<>();
		Set<State> st = new HashSet<>();
		Set<State> is = new HashSet<>();
		Set<State> fs = new HashSet<>();
		for(Transition t : transitions) {
			tr.add(new Transition(t.getDestination(), t.getSource(), t.getSymbol()));
		}
		for(State s : states) {
			int id = 0;
			boolean fin = false, init = false;
			if(s.isInitial())
				fin = true;
			if(s.isFinal())
				init = true;
			st.add(new State(id, init, fin));
			++id;
		}

		for(State s : st) {
			if(s.isInitial())
				is.add(s);
			if(s.isFinal())
				fs.add(s);
		}

		return new Automaton(st, tr, is, fs);
	}

	// create a new deterministic automaton from this
	private Automaton determinize() {
		// TODO: costruzione per sottoinsiemi
		return new Automaton();
	}

	// get the automaton alphabet using defined transitions
	private void defineAlphabet() {
		for(Transition T : transitions) {
			alphabet.add(T.getSymbol());
		}
	}

	// compute all the states reachable using epsilon closures from a given state
	private Set<State> epsClosure(State state) {
		Set<State> eps = new HashSet<>();
		Set<State> checked = new HashSet<>();
		Set<State> ce;
		Set<State> dest;
		// add current state
		do {
			ce = new HashSet<>();
			for(State s : eps) {
				checked.add(s);
				dest = transitions.stream()
						.filter(t -> t.getSource().equals(s) && t.getSymbol() == ' ')
						.map(t -> t.getDestination())
						.collect(Collectors.toSet());
				ce.addAll(dest);
			}
			eps.addAll(ce);
		} while(!checked.equals(eps));
		
		return eps;
	}

	// compute the epsilon closures for a set of given states
	private Set<State> epsClosure(Set<State> st) {
		Set<State> eps = new HashSet<>();

		for(State s : st) {
			Set<State> e = epsClosure(s);
			for(State q : e)
				eps.add(q);
		}
		return eps;
	}

}
