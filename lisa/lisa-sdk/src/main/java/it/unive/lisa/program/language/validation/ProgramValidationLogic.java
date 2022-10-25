package it.unive.lisa.program.language.validation;

import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.ProgramValidationException;

/**
 * A logic for validating {@link Program}s. Depending on the language, different
 * requirements on inheritance hierarchies or code members' signatures can be
 * required: those are to be implemented inside the
 * {@link #validateAndFinalize(Program)} method, that should throw a
 * {@link ProgramValidationException} whenever such requirements are not met.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface ProgramValidationLogic {

	/**
	 * Validates the given {@link Program}, ensuring its consistency. This
	 * identifies erroneous situations (e.g., code members with the same
	 * signature) that can crash the analysis. During validation, the program is
	 * also <i>finalized</i> by populating additional data structures (e.g.,
	 * computing the instances of all {@link CompilationUnit}s defined in the
	 * program).
	 * 
	 * @param program the program to validate
	 * 
	 * @throws ProgramValidationException if the program has an invalid
	 *                                        structure
	 */
	void validateAndFinalize(Program program) throws ProgramValidationException;
}
