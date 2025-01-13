package it.unive.lisa.program.cfg;

import it.unive.lisa.program.CodeElement;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.Unit;

/**
 * A program point, representing an instruction that is happening in one of the
 * {@link CFG} under analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface ProgramPoint extends CodeElement {

	/**
	 * Yields the {@link CFG} that this program point belongs to.
	 * 
	 * @return the containing cfg
	 */
	CFG getCFG();

	/**
	 * Yields the {@link Unit} where this program point is defined. By
	 * default, this is obtained through the {@link CFG} returned by
	 * {@link #getCFG()}.
	 * 
	 * @return the containing unit
	 */
	default Unit getUnit() {
		return getCFG().getDescriptor().getUnit();
	}

	/**
	 * Yields the {@link Program} where this program point is defined. By
	 * default, this is obtained through the {@link CFG} returned by
	 * {@link #getCFG()}.
	 * 
	 * @return the containing program
	 */
	default Program getProgram() {
		return getCFG().getDescriptor().getUnit().getProgram();
	}
}
