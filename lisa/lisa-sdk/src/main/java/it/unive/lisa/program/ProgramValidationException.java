package it.unive.lisa.program;

/**
 * A generic {@link Exception} that indicates that something has gone wrong
 * while validating a {@link Program}'s structure or when computing unit's
 * hierarchies and cfgs' overridings.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class ProgramValidationException
		extends
		Exception {

	private static final long serialVersionUID = -1505555428371757795L;

	/**
	 * Builds the exception.
	 */
	public ProgramValidationException() {
		super();
	}

	/**
	 * Builds the exception.
	 * 
	 * @param message the message of this exception
	 * @param cause   the cause of this exception
	 */
	public ProgramValidationException(
			String message,
			Throwable cause) {
		super(message, cause);
	}

	/**
	 * Builds the exception.
	 * 
	 * @param message the message of this exception
	 */
	public ProgramValidationException(
			String message) {
		super(message);
	}

	/**
	 * Builds the exception.
	 * 
	 * @param cause the cause of this exception
	 */
	public ProgramValidationException(
			Throwable cause) {
		super(cause);
	}

}
