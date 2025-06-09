package it.unive.lisa.analysis.combination;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.util.numeric.IntInterval;

/**
 * Interface for a string analysis that exposes utility methods to compute
 * semantics operations.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 *
 * @param <S> the concrete type of the instances of this domain
 */
public interface SmashedSumStringDomain<S extends SmashedSumStringDomain<S>>
		extends
		BaseNonRelationalValueDomain<S> {

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
	Satisfiability containsChar(
			char c)
			throws SemanticException;

	/**
	 * Yields the {@link IntInterval} containing the minimum and maximum length
	 * of this abstract value.
	 * 
	 * @return the minimum and maximum length of this abstract value
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	IntInterval length()
			throws SemanticException;

	/**
	 * Yields the {@link IntInterval} containing the minimum and maximum index
	 * of {@code s} in {@code this}.
	 *
	 * @param s the string to be searched
	 * 
	 * @return the minimum and maximum index of {@code s} in {@code this}
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	IntInterval indexOf(
			S s)
			throws SemanticException;

	/**
	 * Yields the the substring of this abstract value between two indexes.
	 * 
	 * @param begin where the substring starts
	 * @param end   where the substring ends
	 * 
	 * @return the substring of this abstract value between two indexes
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	S substring(
			long begin,
			long end)
			throws SemanticException;
}
