package it.unive.lisa.analysis.string;

import java.util.Objects;

public class State {

	private final int id;
	
	private final boolean isFinal, isInitial;
	
	public State(int id, boolean isInitial, boolean isFinal) {
		this.id = id;
		this.isInitial = isInitial;
		this.isFinal = isFinal;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(id, isFinal, isInitial);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		State other = (State) obj;
		return id == other.id && isFinal == other.isFinal && isInitial == other.isInitial;
	}
	
	public boolean isFinal() { 
		return isFinal;
	}
	
	public boolean isInitial() {
		return isInitial;
	}
	
	public int getId() {
		return id;
	}
}
