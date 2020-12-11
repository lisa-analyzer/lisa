package it.unive.lisa.test.imp;

/**
 * An exception thrown while parsing an IMP file.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class ParsingException extends Exception {

	private static final long serialVersionUID = 4950907533241537847L;

	/**
	 * Builds the exception.
	 */
	public ParsingException() {
		super();
	}

	/**
	 * Builds the exception.
	 * 
	 * @param message the message of this exception
	 * @param cause   the inner cause of this exception
	 */
	public ParsingException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Builds the exception.
	 * 
	 * @param message the message of this exception
	 */
	public ParsingException(String message) {
		super(message);
	}

	/**
	 * Builds the exception.
	 * 
	 * @param cause the inner cause of this exception
	 */
	public ParsingException(Throwable cause) {
		super(cause);
	}
}