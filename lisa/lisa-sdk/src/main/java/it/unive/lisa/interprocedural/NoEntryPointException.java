package it.unive.lisa.interprocedural;

import it.unive.lisa.util.datastructures.graph.algorithms.FixpointException;

/**
 * A {@link FixpointException} signaling that no entry points have been found.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class NoEntryPointException extends FixpointException {

	private static final long serialVersionUID = -7989565407659746161L;

	/**
	 * Builds the exception.
	 */
	public NoEntryPointException() {
		super("The program contains no entry points for the analysis");
	}
}
