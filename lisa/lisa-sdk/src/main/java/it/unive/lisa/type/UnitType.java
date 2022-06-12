package it.unive.lisa.type;

import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.UnitWithSuperUnits;

/**
 * Interface for types that are introduced by a {@link CompilationUnit}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface UnitType extends InMemoryType {

	/**
	 * Yields the {@link Unit} that induces this type.
	 * 
	 * @return the unit
	 */
	UnitWithSuperUnits getUnit();
}
