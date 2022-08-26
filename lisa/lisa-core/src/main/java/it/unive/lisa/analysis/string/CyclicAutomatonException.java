package it.unive.lisa.analysis.string;

/**
 * Exception thrown if an automaton is cyclic while computing accepted language.
 */
public class CyclicAutomatonException extends Exception {
    /**
     * Default constructor for CyclicAutomatonException
     */
    public CyclicAutomatonException() {
        super("The automaton is cyclic");
    }
}
