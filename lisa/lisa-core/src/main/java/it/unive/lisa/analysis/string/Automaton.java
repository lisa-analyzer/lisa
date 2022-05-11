package it.unive.lisa.analysis.string;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class Automaton {

	private final Set<State> states;

	private final Set<Transition> transitions;

	private State initialState;

	private final Set<Character> alphabet;

	// constructor
	public Automaton() {
		states = new HashSet<>();
		transitions = new HashSet<>();
		alphabet = new HashSet<>();
	}

	// add a new a transition to the automaton
	public void addTransition(Transition t) {
		transitions.add(t);
	}

	// add a new state to the automaton 
	public void addState(State s) {
		if(s.isInitial())
			setInitialState(s);
		states.add(s);
	}

	// given a string as input the automaton validate it as part of its language or not
	public boolean validateString(String str) {
		// stores all the possible transition from the set of currentStates with a given symbol
		Set<Transition> tr = new HashSet<>();
		Set<State> dest;
		for(int i = 0; i < str.length(); ++i) {
			char c = str.charAt(i);
			dest = new HashSet<>();
			// for each state in currentStates add all his transitions with symbol c to tr
			currentStates = epsTransition(currentStates);
			for(State s : currentStates) {
				tr = transitions.stream()
						.filter(t -> t.getSource() == s && t.getSymbol() == c)
						.collect(Collectors.toCollection(() -> new HashSet<>()));
				for(Transition t : tr)
					dest.add(t.getDestination());
			}
			currentStates = dest;
		}
		currentStates = epsTransition(currentStates);
		for(State s : currentStates) {
			if(s.isFinal())
				return true;
		}
		return false;
	}

	public void minimize() {
		reverse();
		determinize();
		reach();
		reverse();
		determinize();
		reach();
	}

	// delete unreachable states from the automaton
	private void reach() {
		defineAlphabet();
		HashSet<State> RS = new HashSet<>();
		HashSet<State> NS = new HashSet<>();
		RS.add(initialState);
		do {
			for(State S: NS) {
				// prendo tutti gli stati in cui lo stato corrente  e' sorgente ma non destinazione
				// NS <- T\RS ~> nello pseudocodice
				HashSet<Transition> tr = transitions.stream()
						.filter(t -> t.getSource() == S && t.getDestination() != S)
						.collect(Collectors.toCollection(() -> new HashSet<>()));
				// creo un nuovo oggetto per poter aggiungere direttamente gli stati senza dover
				// rimuovere quelli che c'erano gia'
				NS = new HashSet<>();
				for(Transition t : tr) {
					NS.add(t.getDestination());
					RS.add(t.getDestination());
				}
			}
		} while(!NS.isEmpty());
		states = RS;
	}

	// make the automaton accept his reverse language
	private void reverse() {
		for(Transition t : transitions) {
			t = new Transition(t.getDestination(), t.getSource(), t.getSymbol());
		}
		for(State s : states) {
			if(s.isFinal()) {
				s.setFinal(false);
				s.setInitial(true);
			}
			if(s.isInitial()) {
				s.setInitial(false);
				s.setFinal(true);
			}
		}
	}

	// make the automaton deterministic
	private void determinize() {
		// TODO: costruzione per sottoinsiemi
	}

	// get the automaton alphabet using defined transitions
	private void defineAlphabet() {
		for(Transition T : transitions) {
			alphabet.add(T.getSymbol());
		}
	}

	// ? mi sembra ok, ma nella pratica non funziona
	// compute all the states reachable using epsilon transition from a given state
	private Set<State> epsTransition(State state) {
		Set<State> eps = new HashSet<>();
		Set<State> tr = transitions.stream()
				.filter(t -> t.getSource().equals(state) && t.getSymbol() == ' ')
				.map(t -> t.getSource())
				.collect(Collectors.toSet());
		
		// caso base: se non ho eps-transition restituisco l'insieme vuoto
		if(tr.isEmpty())
			return eps;
		
		// aggiungo lo stato corrente a eps
		eps.add(state);
		
		// per ogni stato calcolo le eps-transition dello stato stesso
		for(Transition t : tr) {
			State s = t.getDestination();
			for(State e : epsTransition(s))
				eps.add(e);

		return eps;
	}

	// compute the epsilon transitions for a set of given states
	private Set<State> epsTransition(Set<State> st) {
		Set<State> eps = new HashSet<>();

		for(State s : st) {
			Set<State> e = epsTransition(s);
			for(State q : e)
				eps.add(q);
		}
		return eps;
	}

}
