package it.unive.lisa.program.cfg;

/**
 * A program point, representing an instruction that is happening in one of the
 * {@link CFG} under analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface ProgramPoint {

	/**
	 * Yields the {@link CFG} that this program point belongs to.
	 * 
	 * @return the containing cfg
	 */
	CFG getCFG();
}
