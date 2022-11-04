package it.unive.lisa.program;

import it.unive.lisa.program.cfg.CodeLocation;

/**
 * A unit that is part of a LiSA {@link Program}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class ProgramUnit extends Unit implements CodeElement {

	/**
	 * The location in the program of this unit
	 */
	private final CodeLocation location;

	/**
	 * The program where this unit is defined
	 */
	private final Program program;

	/**
	 * Builds a unit, defined at the given location.
	 * 
	 * @param location the location where the unit is define within the source
	 *                     file
	 * @param program  the program where this unit is defined
	 * @param name     the name of the unit
	 */
	public ProgramUnit(CodeLocation location, Program program, String name) {
		super(name);
		this.program = program;
		this.location = location;
	}

	@Override
	public CodeLocation getLocation() {
		return location;
	}

	@Override
	public Program getProgram() {
		return program;
	}
}
