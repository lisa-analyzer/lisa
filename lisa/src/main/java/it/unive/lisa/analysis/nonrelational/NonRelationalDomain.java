package it.unive.lisa.analysis.nonrelational;

import it.unive.lisa.analysis.FunctionalLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;

/**
 * A non-relational domain, that is able to compute the value of a
 * {@link SymbolicExpression}s of type {@code E} by knowing the values of all
 * program variables. Instances of this class can be wrapped inside an
 * {@link FunctionalLattice} to represent abstract values of individual
 * {@link Identifier}s.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <T> the concrete type of the domain
 * @param <E> the type of expressions that this domain can evaluate
 * @param <F> the type of functional lattice that is used in conjuntion with
 *                this domain
 */
public interface NonRelationalDomain<T extends NonRelationalDomain<T, E, F>, E extends SymbolicExpression, F extends FunctionalLattice<F, Identifier, T>>
		extends Lattice<T> {

	/**
	 * Evaluates a {@link ValueExpression}, assuming that the values of program
	 * variables are the ones stored in {@code environment}.
	 * 
	 * @param expression  the expression to evaluate
	 * @param environment the environment containing the values of program
	 *                        variables for the evaluation
	 * 
	 * @return an new instance of this domain, representing the abstract result
	 *             of {@code expression} when evaluated on {@code environment}
	 */
	public T eval(E expression, F environment);

	/**
	 * Yields a textual representation of the content of this domain's instance.
	 * 
	 * @return the textual representation
	 */
	String representation();
}
