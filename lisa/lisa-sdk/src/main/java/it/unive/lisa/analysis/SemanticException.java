package it.unive.lisa.analysis;

/**
 * An exception that occurred during semantic computation.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class SemanticException
		extends
		Exception {

	private static final long serialVersionUID = 7246029205296168336L;

	/**
	 * Builds the exception.
	 */
	public SemanticException() {
		super();
	}

	/**
	 * Builds the exception.
	 * 
	 * @param message the message of this exception
	 * @param cause   the cause of this exception
	 */
	public SemanticException(
			String message,
			Throwable cause) {
		super(message, cause);
	}

	/**
	 * Builds the exception.
	 * 
	 * @param message the message of this exception
	 */
	public SemanticException(
			String message) {
		super(message);
	}

	/**
	 * Builds the exception.
	 * 
	 * @param cause the cause of this exception
	 */
	public SemanticException(
			Throwable cause) {
		super(cause);
	}

}
