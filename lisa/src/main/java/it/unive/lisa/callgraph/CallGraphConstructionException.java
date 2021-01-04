package it.unive.lisa.callgraph;

/**
 * An exception that occurred while building the callgraph.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class CallGraphConstructionException extends Exception {

	private static final long serialVersionUID = 3751365560725223528L;

	/**
	 * Builds the exception.
	 */
	public CallGraphConstructionException() {
		super();
	}

	/**
	 * Builds the exception.
	 * 
	 * @param message the message of this exception
	 * @param cause   the cause of this exception
	 */
	public CallGraphConstructionException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Builds the exception.
	 * 
	 * @param message the message of this exception
	 */
	public CallGraphConstructionException(String message) {
		super(message);
	}

	/**
	 * Builds the exception.
	 * 
	 * @param cause the cause of this exception
	 */
	public CallGraphConstructionException(Throwable cause) {
		super(cause);
	}

}
