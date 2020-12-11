package it.unive.lisa.analysis.nonrelational;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticDomain.Satisfiability;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;

/**
 * A non-relational value domain, that is able to compute the value of a
 * {@link ValueExpression} by knowing the values of all program variables.
 * Instances of this class can be wrapped inside an {@link ValueEnvironment} to
 * represent abstract values of individual {@link Identifier}s.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <T> the concrete type of the domain
 */
public interface NonRelationalValueDomain<T extends NonRelationalValueDomain<T>>
		extends Lattice<T>, NonRelationalDomain<T, ValueExpression, ValueEnvironment<T>> {

	/**
	 * Checks if the given expression is satisfied by a given value environment
	 * tracking this abstract values, returning an instance of
	 * {@link Satisfiability}.
	 * 
	 * @param expression  the expression whose satisfiability is to be evaluated
	 * @param environment the environment where the expressions must be
	 *                        evaluated
	 * 
	 * @return {@link Satisfiability#SATISFIED} is the expression is satisfied
	 *             by the given environment,
	 *             {@link Satisfiability#NOT_SATISFIED} if it is not satisfied,
	 *             or {@link Satisfiability#UNKNOWN} if it is either impossible
	 *             to determine if it satisfied, or if it is satisfied by some
	 *             values and not by some others (this is equivalent to a TOP
	 *             boolean value)
	 */
	Satisfiability satisfies(SymbolicExpression expression, ValueEnvironment<T> environment);
}
