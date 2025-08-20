package it.unive.lisa.analysis.combination.smash;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.util.numeric.IntInterval;

/**
 * Interface for a {@link BaseNonRelationalValueDomain} for numeric values that
 * can be used in the smashed-sum abstract domain.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 *
 * @param <L> the type of lattice produced by this domain
 */
public interface SmashedSumIntDomain<L extends Lattice<L>>
		extends
		BaseNonRelationalValueDomain<L> {

	/**
	 * Creates a new instance of the lattice produced by this domain from the
	 * given interval.
	 * 
	 * @param intv the {@link IntInterval} to convert into an instance of the
	 *                 lattice
	 * 
	 * @return an {@link IntInterval} representing the range of values
	 * 
	 * @throws SemanticException if an error occurs during the conversion
	 */
	L fromInterval(
			IntInterval intv)
			throws SemanticException;

	/**
	 * Converts a lattice produced by this domain to an interval.
	 * 
	 * @param current the lattice instance to convert
	 * 
	 * @return an {@link IntInterval} representing the range of values
	 * 
	 * @throws SemanticException if an error occurs during the conversion
	 */
	IntInterval toInterval(
			L current)
			throws SemanticException;

}
