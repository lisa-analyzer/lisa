package it.unive.lisa.type;

import it.unive.lisa.program.CompilationUnit;

/**
 * Interface for types that are introduced by a {@link CompilationUnit}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface UnitType extends Type {

	/**
	 * Yields the {@link CompilationUnit} that induces this type.
	 * 
	 * @return the unit
	 */
	CompilationUnit getUnit();
}
