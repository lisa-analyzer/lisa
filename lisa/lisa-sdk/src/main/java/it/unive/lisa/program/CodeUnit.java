package it.unive.lisa.program;

import it.unive.lisa.program.cfg.CodeLocation;

/**
 * A file-based unit of the program to analyze, that logically groups code in
 * files or modules.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class CodeUnit extends Unit implements CodeElement {

	/**
	 * The location in the program of this unit
	 */
	private final CodeLocation location;

	/**
	 * Builds a unit, defined at the given location.
	 * 
	 * @param location the location where the unit is define within the source
	 *                     file
	 * @param name     the name of the unit
	 */
	public CodeUnit(CodeLocation location, String name) {
		super(name);
		this.location = location;
	}

	@Override
	public CodeLocation getLocation() {
		return location;
	}

	@Override
	public boolean canBeInstantiated() {
		return false;
	}
}
