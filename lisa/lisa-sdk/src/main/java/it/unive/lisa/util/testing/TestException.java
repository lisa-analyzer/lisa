package it.unive.lisa.util.testing;

/**
 * An exception that is thrown when a test fails.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class TestException
		extends
		RuntimeException {

	private static final long serialVersionUID = 2135849687916385496L;

	/**
	 * Creates a new test exception with the given message.
	 * 
	 * @param message the detail message
	 */
	public TestException(
			String message) {
		super(message);
	}

	/**
	 * Creates a new test exception with the given message and cause.
	 * 
	 * @param message the detail message
	 * @param cause   the cause of this exception
	 */
	public TestException(
			String message,
			Throwable cause) {
		super(message, cause);
	}
}
