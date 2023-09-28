package it.unive.lisa.analysis;

/**
 * A runtime exception that wraps a {@link SemanticException}, so that it can be
 * thrown within lambdas.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class SemanticExceptionWrapper extends RuntimeException {

	private static final long serialVersionUID = 7246029205296168336L;

	/**
	 * Builds the exception wrapper.
	 * 
	 * @param cause the cause of this exception
	 */
	public SemanticExceptionWrapper(
			SemanticException cause) {
		super("A semantic exception happened", cause);
	}

}
