package it.unive.lisa.analysis.string;

public class CyclicAutomatonException extends Exception {
    public CyclicAutomatonException() {
        super();
    }
    public CyclicAutomatonException(String exception) {
        super(exception);
    }
}
