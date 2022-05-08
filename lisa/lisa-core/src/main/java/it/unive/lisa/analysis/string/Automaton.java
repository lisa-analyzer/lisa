package it.unive.lisa.analysis.string;

import java.util.HashSet;
import java.util.stream.Collectors;

public class Automaton {
	
	private HashSet<State> states;
	
	private HashSet<Transition> transitions;
	
	// for non deterministic automaton, I can virtually have more than one current state
	private HashSet<State> currentStates;
	
	private State initialState;
	
	private HashSet<Character> alphabet;
	
	// constructor
	public Automaton() {
		states = new HashSet<State>();
		transitions = new HashSet<Transition>();
		currentStates = new HashSet<State>();
		alphabet = new HashSet<Character>();
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
	
	// set initial state of the automaton
	private void setInitialState(State s) {
		initialState = s;
		currentStates.add(s);
	}
	
	// given a string as input the automaton validate it as part of its language or not
	public boolean validateString(String str) {
		// store all the possible transition from the set of currentStates with a given symbol
		HashSet<Transition> tr = new HashSet<Transition>();
		for(int i = 0; i < str.length(); ++i) {
			char c = str.charAt(i);
			// for each state in currentStates add all his transitions with symbol c to tr
			for(State S : currentStates) {
				HashSet<Transition> ts = transitions.stream()
	   												.filter(t -> t.getSource() == S && t.getSymbol() == c)
	   												.collect(Collectors.toCollection(() -> new HashSet<Transition>()));
				for(Transition t : ts)
					tr.add(t);
			}
			currentStates = new HashSet<State>();
			for(Transition t : tr)
				currentStates.add(t.getDestination());
		}
		//State f = (State)() -> states.stream().filter(s -> s.getId() == currentState).isFinal();
		for(State s : currentStates) {
			if(s.isFinal())
				return true;
		}
		return false;
	}
	
	// delete unreachable states from the automaton
	// TODO: make it works also with NFA
	public void reach() {
		defineAlphabet();
		HashSet<State> RS = new HashSet<State>();
		HashSet<State> NS = new HashSet<State>();
		RS.add(initialState);
		do {
			for(State S: NS) {
				// prendo tutti gli stati in cui lo stato corrente  e' sorgente ma non destinazione
				// NS <- T\RS ~> nello pseudocodice
				HashSet<Transition> tr = transitions.stream()
										   			.filter(t -> t.getSource() == S && t.getDestination() != S)
										   			.collect(Collectors.toCollection(() -> new HashSet<Transition>()));
				// creo un nuovo oggetto per poter aggiungere direttamente gli stati senza dover
				// rimuovere quelli che c'erano gia'
				NS = new HashSet<State>();
				for(Transition t : tr) {
					NS.add(t.getDestination());
					RS.add(t.getDestination());
				}
			}
		} while(!NS.isEmpty());
		states = RS;
	}
	
	// make the automaton accept his reverse language
	public void reverse() {
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
	public void determinize() {
		// TODO: costruzione per sottoinsiemi
	}
	
	// get the automaton alphabet using defined transitions
	private void defineAlphabet() {
		for(Transition T : transitions) {
			alphabet.add(T.getSymbol());
		}
	}
}
