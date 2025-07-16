package it.unive.lisa.analysis.nonrelational;

import it.unive.lisa.analysis.DomainLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticComponent;
import it.unive.lisa.analysis.SemanticEvaluator;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.FunctionalLattice;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;

/**
 * A non-relational domain, that is able to compute the value of a
 * {@link SymbolicExpression}s of type {@code E} by knowing the values of all
 * program variables. States managed by this domain are instances of
 * {@link FunctionalLattice}, containing a mapping from {@link Identifier}s to
 * instances of the type {@code L}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <L> the type of {@link DomainLattice} that this domain works with
 * @param <T> the type of values returned by the transformers of this domain
 * @param <M> the type of {@link FunctionalLattice} that this domain works with
 * @param <E> the type of {@link SymbolicExpression}s that this domain can
 *                evaluate
 */
public interface NonRelationalDomain<L extends Lattice<L>,
		T,
		M extends FunctionalLattice<M, Identifier, L> & DomainLattice<M, T>,
		E extends SymbolicExpression>
		extends
		SemanticComponent<M, T, E, Identifier>,
		SemanticEvaluator {

	/**
	 * Evaluates a {@link SymbolicExpression}, assuming that the values of
	 * program variables are the ones stored in {@code environment}.
	 * 
	 * @param environment the environment containing the values of program
	 *                        variables for the evaluation
	 * @param expression  the expression to evaluate
	 * @param pp          the program point that where this operation is being
	 *                        evaluated
	 * @param oracle      the oracle for inter-domain communication
	 * 
	 * @return an new instance of this domain, representing the abstract result
	 *             of {@code expression} when evaluated on {@code environment}
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	L eval(
			M environment,
			E expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException;

	/**
	 * Yields a fixed abstraction of the given variable. The abstraction does
	 * not depend on the abstract values that get assigned to the variable, but
	 * is instead fixed among all possible execution paths. If this method does
	 * not return the bottom element (as the default implementation does), then
	 * assignments to the identifier {@code id} should store that abstract
	 * element instead of the one computed starting from the expression.
	 * 
	 * @param id     The identifier representing the variable being assigned
	 * @param pp     the program point that where this operation is being
	 *                   evaluated
	 * @param oracle the oracle for inter-domain communication
	 * 
	 * @return the fixed abstraction of the variable
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	L fixedVariable(
			Identifier id,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException;

	/**
	 * Yields the default abstraction returned whenever a functional lattice
	 * using this element as values is queried for the state of a variable not
	 * currently part of its mapping. Abstraction for such a variable might have
	 * been lost, for instance, due to a call to {@link Lattice#top()} on the
	 * function itself.
	 * 
	 * @param id the variable that is missing from the mapping
	 * 
	 * @return a default abstraction for the variable
	 */
	L unknownValue(
			Identifier id);

}
