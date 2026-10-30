package it.unive.lisa.triage.smt;

/**
 * An {@link SmtException} raised when the requested solver is not available
 * in the current environment (e.g., its executable cannot be found).
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class SmtSolverUnavailableException
		extends
		SmtException {

	private static final long serialVersionUID = 1L;

	/**
	 * Builds the exception.
	 *
	 * @param message the message of the exception
	 * @param cause   the cause of the exception
	 */
	public SmtSolverUnavailableException(
			String message,
			Throwable cause) {
		super(message, cause);
	}
}
