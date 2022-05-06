package it.unive.lisa.analysis.string;

import java.util.HashSet;

public class Automaton {
	
	private HashSet<State> states;
	
	private HashSet<Transition> transitions;
	
	private State currentState;
	
	public Automaton() {
		states = new HashSet<State>();
		transitions = new HashSet<Transition>();
		currentState = new State(0, false, false);
	}
	
	public void addTransition(Transition t) {
		transitions.add(t);
	}
	
	public void addState(State s) {
		if(s.isInitial())
			setInitialState(s);
		states.add(s);
	}
	
	private void setInitialState(State s) {
		currentState = s;
	}
	
	public boolean validateString(String str) {
		for(int i = 0; i < str.length(); ++i) {
			for(Transition t : transitions) {
				if(t.getSource() == currentState &&
						t.getSymbol() == str.charAt(i)) {
					currentState = t.doTransition();
					break;
				}
			}
		}
		//State f = (State)() -> states.stream().filter(s -> s.getId() == currentState).isFinal();
		for(State s : states) {
			if(s == currentState)
				return s.isFinal();
		}
		return false;
	}
}
