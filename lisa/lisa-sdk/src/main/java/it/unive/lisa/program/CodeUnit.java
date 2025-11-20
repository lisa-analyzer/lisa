package it.unive.lisa.program;

import it.unive.lisa.program.cfg.CodeLocation;

/**
 * A file-based unit of the program to analyze, that logically groups code in
 * files or modules.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class CodeUnit
		extends
		ProgramUnit {

	/**
	 * Builds a unit, defined at the given location.
	 * 
	 * @param location the location where the unit is define within the source
	 *                     file
	 * @param program  the program where this unit is defined
	 * @param name     the name of the unit
	 */
	public CodeUnit(
			CodeLocation location,
			Program program,
			String name) {
		super(location, program, name);
	}

	@Override
	public boolean canBeInstantiated() {
		return false;
	}

}
