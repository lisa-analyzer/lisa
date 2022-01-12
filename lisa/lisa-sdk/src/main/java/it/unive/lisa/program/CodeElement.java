package it.unive.lisa.program;

import it.unive.lisa.program.cfg.CodeLocation;

/**
 * Interface for code elements that have to provide information about the
 * location where they appear.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
@FunctionalInterface
public interface CodeElement {

	/**
	 * Yields the location where this code element appears in the program.
	 * 
	 * @return the location where this code element apperars in the program
	 */
	CodeLocation getLocation();
}
