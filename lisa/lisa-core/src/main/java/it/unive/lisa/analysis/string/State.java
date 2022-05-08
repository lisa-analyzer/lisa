package it.unive.lisa.analysis.string;

public class State {
// per avere q0, q1, ..., qn in maniera indipendente
	// dalla memorizzazione
	private int id;
	
	private boolean isFinal, isInitial;
	
	public State(int id, boolean isInitial, boolean isFinal) {
		this.id = id;
		this.isInitial = isInitial;
		this.isFinal = isFinal;
	}
	
	public void setFinal(boolean fin) {
		this.isFinal = fin;
	}
	
	public void setInitial(boolean init) {
		this.isInitial = init;
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
