package it.unive.lisa.analysis.string;

import java.util.Objects;

public class Transition {
	
	private final State source, destination;
	
	@Override
	public int hashCode() {
		return Objects.hash(destination, source, symbol);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Transition other = (Transition) obj;
		return Objects.equals(destination, other.destination) && Objects.equals(source, other.source)
				&& symbol == other.symbol;
	}
	
	// if symbol is ' ' means it is an epsilon transition
	private final char symbol;
	 
	public Transition(State source, State destination, char symbol) {
		this.source = source;
		this.destination = destination;
		this.symbol = symbol;
	}
	
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
