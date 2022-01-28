package it.unive.lisa.program.cfg;

import it.unive.lisa.program.CodeElement;

/**
 * A program point, representing an instruction that is happening in one of the
 * {@link ImplementedCFG} under analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface ProgramPoint extends CodeElement {

	/**
	 * Yields the {@link ImplementedCFG} that this program point belongs to.
	 * 
	 * @return the containing cfg
	 */
	ImplementedCFG getCFG();
}
