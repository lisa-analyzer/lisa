package it.unive.lisa.outputs;

/**
 * An exception thrown when creating or dumping one of the outputs of the
 * analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class OutputDumpingException extends RuntimeException {

	private static final long serialVersionUID = -6224754350892145958L;

	/**
	 * Builds the exception.
	 */
	public OutputDumpingException() {
		super();
	}

	/**
	 * Builds the exception.
	 * 
	 * @param message the message of this exception
	 * @param cause   the cause of this exception
	 */
	public OutputDumpingException(
			String message,
			Throwable cause) {
		super(message, cause);
	}

	/**
	 * Builds the exception.
	 * 
	 * @param message the message of this exception
	 */
	public OutputDumpingException(
			String message) {
		super(message);
	}

	/**
	 * Builds the exception.
	 * 
	 * @param cause the cause of this exception
	 */
	public OutputDumpingException(
			Throwable cause) {
		super(cause);
	}

}
