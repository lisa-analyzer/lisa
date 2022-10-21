package it.unive.lisa.program;

import it.unive.lisa.program.cfg.CodeLocation;

/**
 * A compilation unit of the program to analyze. A compilation unit is a
 * {@link Unit} that also defines instance members, that can be inherited by
 * subunits.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class AbstractClassUnit extends ClassUnit {

	/**
	 * Builds a concrete compilation unit, defined at the given program point.
	 * 
	 * @param location the location where the unit is define within the source
	 *                     file
	 * @param name     the name of the unit
	 * @param sealed   whether or not this unit is sealed, meaning that it
	 *                     cannot be used as super unit of other compilation
	 *                     units
	 */
	public AbstractClassUnit(CodeLocation location, String name, boolean sealed) {
		super(location, name, sealed);
	}

	@Override
	public boolean canBeInstantiated() {
		return false;
	}

	@Override
	public void validateAndFinalize() throws ProgramValidationException {
		if (hierarchyComputed)
			return;

		if (isSealed())
			throw new ProgramValidationException(this + " is an abstract class and cannot be sealed.");

		super.validateAndFinalize();
	}
}
