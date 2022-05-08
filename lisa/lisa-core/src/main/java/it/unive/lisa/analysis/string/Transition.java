package it.unive.lisa.analysis.string;

public class Transition {
	
	private State source, destination;
	
	// if symbol is ' ' means it is an epsilon transaction
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
	
	public State getDestination() {
		return this.destination;
	}
}
