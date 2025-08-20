package it.unive.lisa;

/**
 * A generic {@link RuntimeException} that indicates that something has gone
 * wrong during the analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class AnalysisException
		extends
		RuntimeException {

	private static final long serialVersionUID = 2005239836054799858L;

	/**
	 * Builds the exception.
	 */
	public AnalysisException() {
		super();
	}

	/**
	 * Builds the exception.
	 * 
	 * @param message the message of this exception
	 * @param cause   the cause of this exception
	 */
	public AnalysisException(
			String message,
			Throwable cause) {
		super(message, cause);
	}

	/**
	 * Builds the exception.
	 * 
	 * @param message the message of this exception
	 */
	public AnalysisException(
			String message) {
		super(message);
	}

	/**
	 * Builds the exception.
	 * 
	 * @param cause the cause of this exception
	 */
	public AnalysisException(
			Throwable cause) {
		super(cause);
	}

}
