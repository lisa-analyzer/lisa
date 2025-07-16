package it.unive.lisa.analysis.value;

import it.unive.lisa.analysis.DomainLattice;

/**
 * A {@link DomainLattice} that tracks information about the values of variables
 * and heap locations in a program. This interface extends
 * {@link LatticeWithReplacement} to allow the application of substitutions that
 * modify the values of variables and heap locations.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <L> the concrete type of {@link ValueLattice}
 */
public interface ValueLattice<L extends ValueLattice<L>>
		extends
		LatticeWithReplacement<L> {
}
