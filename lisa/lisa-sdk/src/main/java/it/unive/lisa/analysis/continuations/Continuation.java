package it.unive.lisa.analysis.continuations;

import it.unive.lisa.analysis.ProgramState;
import it.unive.lisa.util.representation.StructuredObject;

/**
 * A continuation is a token expressing how the execution flows from the
 * instruction that produced a {@link ProgramState} to the next ones. These are
 * used to distinguish states that pertain to the "normal" execution flow, from
 * the ones that generated errors or that correspond to program termination. For
 * example, a {@link Halt} continuation expresses that the execution of the
 * whole program stops when an instruction is reached, and the
 * {@link ProgramState} is not updated anymore. Instead, an {@link Exception}
 * continuation expresses that an error occurred during the execution, and the
 * program state denotes what led to the error and must be brought to
 * error-handling blocks, if they exist.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class Continuation
		implements
		StructuredObject {

	@Override
	public final String toString() {
		return representation().toString();
	}

}
