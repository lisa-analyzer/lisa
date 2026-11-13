package it.unive.lisa.triage.smt;

/**
 * An exception raised when an {@link SmtSolver} cannot complete a request
 * (e.g., because the solver reported an error, or because it terminated
 * unexpectedly).
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class SmtException
		extends
		RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * Builds the exception.
	 *
	 * @param message the message of the exception
	 */
	public SmtException(
			String message) {
		super(message);
	}

	/**
	 * Builds the exception.
	 *
	 * @param message the message of the exception
	 * @param cause   the cause of the exception
	 */
	public SmtException(
			String message,
			Throwable cause) {
		super(message, cause);
	}
}
