package it.unive.lisa.type;

import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Unit;

/**
 * Interface for types that are introduced by a {@link CompilationUnit}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface UnitType extends Type {

	/**
	 * Yields the {@link Unit} that induces this type.
	 * 
	 * @return the unit
	 */
	Unit getUnit();
}
