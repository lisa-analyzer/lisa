package it.unive.lisa.test.imp;

/**
 * An exception thrown due to an inconsistency in an imp file.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class IMPSyntaxException extends RuntimeException {

	private static final long serialVersionUID = 4950907533241537847L;

	/**
	 * Builds the exception.
	 */
	public IMPSyntaxException() {
		super();
	}

	/**
	 * Builds the exception.
	 * 
	 * @param message the message of this exception
	 * @param cause   the inner cause of this exception
	 */
	public IMPSyntaxException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Builds the exception.
	 * 
	 * @param message the message of this exception
	 */
	public IMPSyntaxException(String message) {
		super(message);
	}

	/**
	 * Builds the exception.
	 * 
	 * @param cause the inner cause of this exception
	 */
	public IMPSyntaxException(Throwable cause) {
		super(cause);
	}
}