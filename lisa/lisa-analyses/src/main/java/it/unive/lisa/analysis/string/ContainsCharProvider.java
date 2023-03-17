package it.unive.lisa.analysis.string;

import it.unive.lisa.analysis.SemanticDomain.Satisfiability;
import it.unive.lisa.analysis.SemanticException;

/**
 * Interface for a string analysis that exposes the {@link #containsChar(char)}
 * method.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface ContainsCharProvider {

	/**
	 * Simplified semantics of the string contains operator, checking a single
	 * character is part of the string.
	 * 
	 * @param c the character to check
	 * 
	 * @return whether or not the character is part of the string
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	Satisfiability containsChar(char c) throws SemanticException;
}
