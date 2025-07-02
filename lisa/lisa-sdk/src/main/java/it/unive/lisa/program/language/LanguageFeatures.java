package it.unive.lisa.program.language;

import it.unive.lisa.program.language.hierarchytraversal.HierarcyTraversalStrategy;
import it.unive.lisa.program.language.parameterassignment.ParameterAssigningStrategy;
import it.unive.lisa.program.language.resolution.ParameterMatchingStrategy;
import it.unive.lisa.program.language.scoping.DefaultScopingStrategy;
import it.unive.lisa.program.language.scoping.ScopingStrategy;
import it.unive.lisa.program.language.validation.BaseValidationLogic;
import it.unive.lisa.program.language.validation.ProgramValidationLogic;

/**
 * Logical grouping of all language-specific features, such as strategies for
 * matching call parameters or traversing type hierarchies.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class LanguageFeatures {

	/**
	 * Yields the {@link ParameterMatchingStrategy} encoding how actual
	 * parameters of a call are matched against the formal ones of its targets.
	 * 
	 * @return the matching strategy
	 */
	public abstract ParameterMatchingStrategy getMatchingStrategy();

	/**
	 * Yields the {@link HierarcyTraversalStrategy} that expresses how a class
	 * hierarchy is traversed, starting from a leaf and visiting all of its
	 * ancestors recursively, when searching for program members in it.
	 * 
	 * @return the traversal strategy
	 */
	public abstract HierarcyTraversalStrategy getTraversalStrategy();

	/**
	 * Yields the {@link ParameterAssigningStrategy} defining how actual
	 * parameters of a call are matched against the formal ones of its targets.
	 * 
	 * @return the assigning strategy
	 */
	public abstract ParameterAssigningStrategy getAssigningStrategy();

	/**
	 * Yields the {@link ScopingStrategy} that defines how scopes are pushed and
	 * popped during the analysis, e.g., when entering and exiting a CFG.
	 * 
	 * @return the scoping strategy
	 */
	public ScopingStrategy getScopingStrategy() {
		return new DefaultScopingStrategy();
	}

	/**
	 * Yields the {@link ProgramValidationLogic} that validates the structure of
	 * a program, identifying erroneous situations (e.g., code members with the
	 * same signature) that can crash the analysis.
	 * 
	 * @return the validation logic
	 */
	public ProgramValidationLogic getProgramValidationLogic() {
		return new BaseValidationLogic();
	}
}
