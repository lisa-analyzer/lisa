package it.unive.lisa.lattices.informationFlow;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.SemanticException;

/**
 * An interface for lattices used in taint analyses. This interface extends
 * {@link BaseLattice} and provides methods to access the domain elements that
 * represent tainted and clean values, as well as methods to check whether a
 * value is always tainted, possibly tainted, always clean, or possibly clean.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <L> the concrete type of the taint lattice
 */
public interface TaintLattice<L extends TaintLattice<L>>
		extends
		BaseLattice<L> {

	/**
	 * Yields the domain element that represents tainted values.
	 * 
	 * @return the tainted domain element
	 */
	L tainted();

	/**
	 * Yields the domain element that represents clean values.
	 * 
	 * @return the clean domain element
	 */
	L clean();

	/**
	 * Combines two taint lattices using the logical OR operation.
	 * 
	 * @param other the other lattice to combine with
	 * 
	 * @return the combined lattice
	 * 
	 * @throws SemanticException if an error occurs during the combination
	 */
	L or(
			L other)
			throws SemanticException;

	/**
	 * Yields {@code true} if this instance represents information that is
	 * definitely tainted across all execution paths.
	 * 
	 * @return {@code true} if that condition holds
	 */
	boolean isAlwaysTainted();

	/**
	 * Yields {@code true} if this instance represents information that is
	 * definitely tainted in at least one execution path.
	 * 
	 * @return {@code true} if that condition holds
	 */
	boolean isPossiblyTainted();

	/**
	 * Yields {@code true} if this instance represents information that is
	 * definitely clean across all execution paths. By default, this method
	 * returns {@code true} if this is not the bottom instance and
	 * {@link #isPossiblyTainted()} returns {@code false}.
	 * 
	 * @return {@code true} if that condition holds
	 */
	default boolean isAlwaysClean() {
		return !isPossiblyTainted() && !isBottom();
	}

	/**
	 * Yields {@code true} if this instance represents information that is
	 * definitely clean in at least one execution path. By default, this method
	 * returns {@code true} if {@link #isAlwaysTainted()} returns {@code false}.
	 * 
	 * @return {@code true} if that condition holds
	 */
	default boolean isPossiblyClean() {
		return !isAlwaysTainted();
	}

}
