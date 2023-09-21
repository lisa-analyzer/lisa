package it.unive.lisa.analysis.nonrelational;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.FunctionalLattice;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;

/**
 * A {@link NonRelationalElement} that models the standard concept of
 * non-relational abstract domain, and that is able to compute the value of a
 * {@link SymbolicExpression}s of type {@code E} by knowing the values of all
 * program variables. Instances of this class can be wrapped inside an
 * {@link FunctionalLattice} to represent abstract values of individual
 * {@link Identifier}s.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <T> the concrete type of the domain
 * @param <E> the type of expressions that this domain can evaluate
 * @param <F> the type of functional lattice that is used in conjunction with
 *                this domain
 */
public interface NonRelationalDomain<T extends NonRelationalDomain<T, E, F>,
		E extends SymbolicExpression,
		F extends Environment<F, E, T>>
		extends NonRelationalElement<T, E, F> {

	/**
	 * Evaluates a {@link SymbolicExpression}, assuming that the values of
	 * program variables are the ones stored in {@code environment}.
	 * 
	 * @param expression  the expression to evaluate
	 * @param environment the environment containing the values of program
	 *                        variables for the evaluation
	 * @param pp          the program point that where this operation is being
	 *                        evaluated
	 * @param oracle      the oracle for inter-domain communication
	 * 
	 * @return an new instance of this domain, representing the abstract result
	 *             of {@code expression} when evaluated on {@code environment}
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	T eval(E expression, F environment, ProgramPoint pp, SemanticOracle oracle) throws SemanticException;
}
