package it.unive.lisa.analysis.string;

public class Transition {
	
	private State source, destination;
	
	private char symbol;
	 
	public Transition(State source, State destination, char symbol) {
		this.source = source;
		this.destination = destination;
		this.symbol = symbol;
	}
	
	// getSource e getSymbol usati per la ricerca all'interno del container
	// transitions in Automata
	public State getSource() {
		return this.source;
	}
	
	public char getSymbol() {
		return this.symbol;
	}
	
	public State doTransition() {
		return this.destination;
	}
}
