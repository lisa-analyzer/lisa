package it.unive.lisa;

/**
 * An {@link AnalysisException} that happens during the setup of the analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class AnalysisSetupException extends AnalysisException {

	private static final long serialVersionUID = 2005239836054799858L;

	/**
	 * Builds the exception.
	 */
	public AnalysisSetupException() {
		super();
	}

	/**
	 * Builds the exception.
	 * 
	 * @param message the message of this exception
	 * @param cause   the cause of this exception
	 */
	public AnalysisSetupException(
			String message,
			Throwable cause) {
		super(message, cause);
	}

	/**
	 * Builds the exception.
	 * 
	 * @param message the message of this exception
	 */
	public AnalysisSetupException(
			String message) {
		super(message);
	}

	/**
	 * Builds the exception.
	 * 
	 * @param cause the cause of this exception
	 */
	public AnalysisSetupException(
			Throwable cause) {
		super(cause);
	}
}
