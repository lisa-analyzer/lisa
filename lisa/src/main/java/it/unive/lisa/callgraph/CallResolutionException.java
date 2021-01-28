package it.unive.lisa.callgraph;

import it.unive.lisa.program.cfg.statement.UnresolvedCall;

/**
 * An exception that occurred while resolving an {@link UnresolvedCall}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class CallResolutionException extends Exception {

	private static final long serialVersionUID = -284151525683946469L;

	/**
	 * Builds the exception.
	 */
	public CallResolutionException() {
		super();
	}

	/**
	 * Builds the exception.
	 * 
	 * @param message the message of this exception
	 * @param cause   the cause of this exception
	 */
	public CallResolutionException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Builds the exception.
	 * 
	 * @param message the message of this exception
	 */
	public CallResolutionException(String message) {
		super(message);
	}

	/**
	 * Builds the exception.
	 * 
	 * @param cause the cause of this exception
	 */
	public CallResolutionException(Throwable cause) {
		super(cause);
	}

}
