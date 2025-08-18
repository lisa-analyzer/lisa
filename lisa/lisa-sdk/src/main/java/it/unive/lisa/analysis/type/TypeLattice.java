package it.unive.lisa.analysis.type;

import it.unive.lisa.analysis.DomainLattice;
import it.unive.lisa.analysis.value.LatticeWithReplacement;

/**
 * A {@link DomainLattice} that tracks information about the types of variables
 * and heap locations in a program. This interface extends
 * {@link LatticeWithReplacement} to allow the application of substitutions that
 * modify the types of variables and heap locations.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <L> the concrete type of {@link TypeLattice}
 */
public interface TypeLattice<L extends TypeLattice<L>> extends LatticeWithReplacement<L> {
}
