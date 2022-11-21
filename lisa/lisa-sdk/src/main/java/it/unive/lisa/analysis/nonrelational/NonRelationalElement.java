package it.unive.lisa.analysis.nonrelational;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticDomain.Satisfiability;
import it.unive.lisa.analysis.SemanticEvaluator;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.FunctionalLattice;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;

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
public interface NonRelationalElement<T extends NonRelationalElement<T, E, F>,
		E extends SymbolicExpression,
		F extends FunctionalLattice<F, Identifier, T>>
		extends Lattice<T>, SemanticEvaluator {

	/**
	 * Checks whether {@code expression} is satisfied in {@code environment},
	 * assuming that the values of program variables are the ones stored in
	 * {@code environment} and returning an instance of {@link Satisfiability}.
	 * 
	 * @param expression  the expression whose satisfiability is to be evaluated
	 * @param environment the environment containing the values of program
	 *                        variables for the satisfiability
	 * @param pp          the program point that where this operation is being
	 *                        evaluated
	 * 
	 * @return {@link Satisfiability#SATISFIED} if the expression is satisfied
	 *             by the environment, {@link Satisfiability#NOT_SATISFIED} if
	 *             it is not satisfied, or {@link Satisfiability#UNKNOWN} if it
	 *             is either impossible to determine if it satisfied, or if it
	 *             is satisfied by some values and not by some others (this is
	 *             equivalent to a TOP boolean value)
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	Satisfiability satisfies(E expression, F environment, ProgramPoint pp) throws SemanticException;

	/**
	 * Yields the environment {@code environment} on which the expression
	 * {@code expression} is assumed to hold by this domain. The returned
	 * environment must be an updated version of the given one, where the
	 * relevant abstractions have been (optionally) updated. Returning the given
	 * environment as-is is always a sound implementation.
	 * 
	 * @param environment the environment
	 * @param expression  the expression to be assumed
	 * @param pp          the program point where {@code expression} occurs.
	 * 
	 * @return the environment {@code environment} where {@code expression} is
	 *             assumed to hold
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	F assume(F environment, E expression, ProgramPoint pp) throws SemanticException;

	/**
	 * Yields a fixed abstraction of the given variable. The abstraction does
	 * not depend on the abstract values that get assigned to the variable, but
	 * is instead fixed among all possible execution paths. If this method does
	 * not return the bottom element (as the default implementation does), then
	 * {@link Environment#assign(Identifier, SymbolicExpression, ProgramPoint)}
	 * will store that abstract element instead of the one computed starting
	 * from the expression.
	 * 
	 * @param id The identifier representing the variable being assigned
	 * @param pp the program point that where this operation is being evaluated
	 * 
	 * @return the fixed abstraction of the variable
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	default T variable(Identifier id, ProgramPoint pp) throws SemanticException {
		return bottom();
	}

	/**
	 * Yields a {@link DomainRepresentation} of the information contained in
	 * this domain's instance.
	 * 
	 * @return the representation
	 */
	DomainRepresentation representation();
}
