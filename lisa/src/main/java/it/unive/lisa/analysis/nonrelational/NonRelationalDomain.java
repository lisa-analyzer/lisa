package it.unive.lisa.analysis.nonrelational;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;

/**
 * A non-relational domain, that is able to compute the value of an expression
 * by knowing the values of all program variables. Instances of this class can
 * be wrapped inside an {@link Environment} to represent abstract values of
 * individual {@link Identifier}s.
 * 
 * @param <T> the concrete type of the domain
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface NonRelationalDomain<T extends NonRelationalDomain<T>> extends Lattice<T> {

	/**
	 * Evaluates an expression, assuming that the values of program variables are
	 * the ones stored in {@code environment}.
	 * 
	 * @param expression  the expression to evaluate
	 * @param environment the environment containing the values of program variables
	 *                    for the evaluation
	 * @return an new instance of this domain, representing the abstract result of
	 *         {@code expression} when evaluated on {@code environment}
	 */
	public T eval(SymbolicExpression expression, Environment<T> environment);
}
