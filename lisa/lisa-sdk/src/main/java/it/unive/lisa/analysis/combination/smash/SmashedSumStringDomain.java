package it.unive.lisa.analysis.combination.smash;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.util.numeric.IntInterval;

/**
 * Interface for a {@link BaseNonRelationalValueDomain} for string values that
 * can be used in the smashed-sum abstract domain.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 *
 * @param <L> the type of lattice produced by this domain
 */
public interface SmashedSumStringDomain<L extends Lattice<L>>
		extends
		BaseNonRelationalValueDomain<L> {

	/**
	 * Simplified semantics of the string contains operator, checking if a
	 * single character is part of the string.
	 * 
	 * @param current the lattice instance to check
	 * @param c       the character to check
	 * 
	 * @return whether or not the character is part of the string
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	Satisfiability containsChar(
			L current,
			char c)
			throws SemanticException;

	/**
	 * Yields the {@link IntInterval} containing the minimum and maximum length
	 * of the given abstract value.
	 * 
	 * @param current the lattice instance
	 * 
	 * @return the minimum and maximum length of the given abstract value
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	IntInterval length(
			L current)
			throws SemanticException;

	/**
	 * Yields the {@link IntInterval} containing the minimum and maximum index
	 * of {@code s} in {@code current}.
	 *
	 * @param current the lattice instance
	 * @param s       the string to be searched
	 * 
	 * @return the minimum and maximum index of {@code s} in {@code current}
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	IntInterval indexOf(
			L current,
			L s)
			throws SemanticException;

	/**
	 * Yields the the substring of the given abstract value between two indexes.
	 * 
	 * @param current the lattice instance
	 * @param begin   where the substring starts
	 * @param end     where the substring ends
	 * 
	 * @return the substring of the given abstract value between two indexes
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	L substring(
			L current,
			long begin,
			long end)
			throws SemanticException;

}
