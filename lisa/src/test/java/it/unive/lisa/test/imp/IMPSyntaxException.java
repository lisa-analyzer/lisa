package it.unive.lisa.test.imp;

/**
 * An exception thrown due to an inconsistency in an imp file.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class IMPException extends RuntimeException {

	private static final long serialVersionUID = 4950907533241537847L;

	/**
	 * Builds the exception.
	 */
	public IMPException() {
		super();
	}

	/**
	 * Builds the exception.
	 * 
	 * @param message the message of this exception
	 * @param cause   the inner cause of this exception
	 */
	public IMPException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Builds the exception.
	 * 
	 * @param message the message of this exception
	 */
	public IMPException(String message) {
		super(message);
	}

	/**
	 * Builds the exception.
	 * 
	 * @param cause the inner cause of this exception
	 */
	public IMPException(Throwable cause) {
		super(cause);
	}
}