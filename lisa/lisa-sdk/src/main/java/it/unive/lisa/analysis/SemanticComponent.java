package it.unive.lisa.analysis;

import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;

/**
 * A domain able to determine how abstract information evolves thanks to the
 * semantics of {@link SymbolicExpression}s. Transformers of a semantic domain
 * receive instances of a lattice structure and transform them into new
 * instances according to the semantics of the expression being evaluated.
 * Instances of this class differ from {@link SemanticDomain} in that they are
 * responsible for abstracting part of the program memory, and should rely on
 * the {@link SemanticOracle} to communicate with other components handling
 * different parts of the program memory.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <L> the type of {@link DomainLattice} that this domain works with
 * @param <T> the type of values returned by the transformers of this domain
 * @param <E> the type of {@link SymbolicExpression} that this domain can
 *                process
 * @param <I> the type of variable {@link Identifier} that this domain can
 *                handle
 */
public interface SemanticComponent<L extends DomainLattice<L, T>,
		T,
		E extends SymbolicExpression,
		I extends Identifier> {

	/**
	 * Yields a copy of this domain, where {@code id} has been assigned to
	 * {@code value}.
	 * 
	 * @param state      the current state of the domain, that will be modified
	 *                       by this operation
	 * @param id         the identifier to assign the value to
	 * @param expression the expression to assign
	 * @param pp         the program point that where this operation is being
	 *                       evaluated
	 * @param oracle     the oracle for inter-domain communication
	 * 
	 * @return a copy of this domain, modified by the assignment
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	T assign(
			L state,
			I id,
			E expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException;

	/**
	 * Yields a copy of this domain, that has been modified accordingly to the
	 * semantics of the given {@code expression}.
	 * 
	 * @param state      the current state of the domain, that will be modified
	 *                       by this operation
	 * @param expression the expression whose semantics need to be computed
	 * @param pp         the program point that where this operation is being
	 *                       evaluated
	 * @param oracle     the oracle for inter-domain communication
	 * 
	 * @return a copy of this domain, modified accordingly to the semantics of
	 *             {@code expression}
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	T smallStepSemantics(
			L state,
			E expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException;

	/**
	 * Yields a copy of this domain, modified by assuming that the given
	 * expression holds. It is required that the returned domain is in relation
	 * with this one. A safe (but imprecise) implementation of this method can
	 * always return {@code this}.
	 * 
	 * @param state      the current state of the domain, that will be modified
	 *                       by this operation
	 * @param expression the expression to assume to hold.
	 * @param src        the program point that where this operation is being
	 *                       evaluated, corresponding to the one that generated
	 *                       the given expression
	 * @param dest       the program point where the execution will move after
	 *                       the expression has been assumed
	 * @param oracle     the oracle for inter-domain communication
	 * 
	 * @return the (optionally) modified copy of this domain
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	T assume(
			L state,
			E expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException;

	/**
	 * Checks if the given expression is satisfied by the abstract values of
	 * this domain, returning an instance of {@link Satisfiability}. The default
	 * implementation of this method always returns
	 * {@link Satisfiability#UNKNOWN}.
	 * 
	 * @param state      the current state of the domain
	 * @param expression the expression whose satisfiability is to be evaluated
	 * @param pp         the program point that where this operation is being
	 *                       evaluated
	 * @param oracle     the oracle for inter-domain communication
	 * 
	 * @return {@link Satisfiability#SATISFIED} is the expression is satisfied
	 *             by the values of this domain,
	 *             {@link Satisfiability#NOT_SATISFIED} if it is not satisfied,
	 *             or {@link Satisfiability#UNKNOWN} if it is either impossible
	 *             to determine if it satisfied, or if it is satisfied by some
	 *             values and not by some others (this is equivalent to a TOP
	 *             boolean value)
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	default Satisfiability satisfies(
			L state,
			E expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return Satisfiability.UNKNOWN;
	}

	/**
	 * Builds an instance of the lattice that this domain works with. The
	 * returned value can be <i>any</i> instance of the lattice, including the
	 * top or bottom element.
	 * 
	 * @return an instance of the lattice that this domain works with
	 */
	L makeLattice();

}
