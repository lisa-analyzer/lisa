package it.unive.lisa.interprocedural;

/**
 * An exception that occurred while performing an interprocedural analysis.
 *
 * @author <a href="mailto:pietro.ferrara@unive.it">Pietro Ferrara</a>
 */

public class InterproceduralAnalysisException
		extends
		Exception {

	private static final long serialVersionUID = -284151525683946469L;

	/**
	 * Builds the exception.
	 */
	public InterproceduralAnalysisException() {
		super();
	}

	/**
	 * Builds the exception.
	 *
	 * @param message the message of this exception
	 * @param cause   the cause of this exception
	 */
	public InterproceduralAnalysisException(
			String message,
			Throwable cause) {
		super(message, cause);
	}

	/**
	 * Builds the exception.
	 *
	 * @param message the message of this exception
	 */
	public InterproceduralAnalysisException(
			String message) {
		super(message);
	}

	/**
	 * Builds the exception.
	 *
	 * @param cause the cause of this exception
	 */
	public InterproceduralAnalysisException(
			Throwable cause) {
		super(cause);
	}

}
