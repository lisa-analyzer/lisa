package it.unive.lisa.analysis.continuations;

import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

/**
 * A {@link Continuation} that represents the halting of a program.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Halt
		extends
		Continuation {

	/**
	 * The unique instance of this continuation.
	 */
	public static final Halt INSTANCE = new Halt();

	private Halt() {
	}

	@Override
	public StructuredRepresentation representation() {
		return new StringRepresentation("halt");
	}
}
