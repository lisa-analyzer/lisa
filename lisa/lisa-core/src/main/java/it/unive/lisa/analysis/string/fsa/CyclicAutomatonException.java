package it.unive.lisa.analysis.string.fsa;

/**
 * Exception thrown if an automaton is cyclic while computing accepted language.
 * 
 * @author <a href="mailto:simone.leoni2@studenti.unipr.it">Simone Leoni</a>
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
public class CyclicAutomatonException extends Exception {
	/**
	 * Default constructor for CyclicAutomatonException.
	 */
	public CyclicAutomatonException() {
		super("The automaton is cyclic");
	}
}
