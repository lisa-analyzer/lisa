package it.unive.lisa.analysis.nonrelational;

import it.unive.lisa.analysis.HeapSemanticOperation;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticDomain.Satisfiability;
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

	/**
	 * Checks if the given expression is satisfied by a given heap environment 
	 * tracking this abstract values, returning an instance of {@link Satisfiability}.
	 * 
	 * @param expression		the expression whose satisfiability is to be evaluated
	 * @param heapEnvironment	the heap environment where the expressions must be evaluated 
	 * @return {@link Satisfiability#SATISFIED} if the expression is satisfied by
	 *         the heap environment, {@link Satisfiability#NOT_SATISFIED} if it
	 *         is not satisfied, or {@link Satisfiability#UNKNOWN} if it is either
	 *         impossible to determine if it satisfied, or if it is satisfied by
	 *         some values and not by some others (this is equivalent to a TOP
	 *         boolean value)
	 */
	Satisfiability satisfies(SymbolicExpression expression, HeapEnvironment<T> heapEnvironment);
}
