package it.unive.lisa.analysis.nonrelational.heap;

import it.unive.lisa.analysis.HeapSemanticOperation;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.nonrelational.NonRelationalDomain;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;

/**
 * A non-relational heap domain, that is able to compute the value of a
 * {@link SymbolicExpression} by knowing the values of all program variables.
 * Instances of this class can be wrapped inside a {@link HeapEnvironment} to
 * represent abstract values of individual {@link Identifier}s.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <T> the concrete type of the domain
 */
public interface NonRelationalHeapDomain<T extends NonRelationalHeapDomain<T>>
		extends Lattice<T>, NonRelationalDomain<T, SymbolicExpression, HeapEnvironment<T>>, HeapSemanticOperation {
}
