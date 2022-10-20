package it.unive.lisa.program.cfg;

import it.unive.lisa.program.ProgramValidationException;

/**
 * A CFG interface, implemented by {@link AbstractCFG} and
 * {@link CFG}.
 * 
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
public interface ICFG extends CodeMember {

	/**
	 * Validates this cfg, ensuring that the code contained in it is well
	 * formed.
	 * 
	 * @throws ProgramValidationException if one of the aforementioned checks
	 *                                        fail
	 */
	public void validate() throws ProgramValidationException;
}
