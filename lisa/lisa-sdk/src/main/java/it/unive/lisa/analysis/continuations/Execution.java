package it.unive.lisa.analysis.continuations;

import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

/**
 * A {@link Continuation} that represents the normal execution of a program.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Execution
		extends
		Continuation {

	/**
	 * The unique instance of this continuation.
	 */
	public static final Execution INSTANCE = new Execution();

	private Execution() {
	}

	@Override
	public StructuredRepresentation representation() {
		return new StringRepresentation("normal");
	}
}
