package it.unive.lisa.analysis;

import it.unive.lisa.analysis.AnalysisState.Error;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.cfg.protection.ProtectedBlock;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import org.apache.commons.lang3.tuple.Pair;

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
	 * top or bottom element, and it should be what this analysis uses as a
	 * start state at the beginning of the analysis (e.g., as an entry state to
	 * the main function).
	 * 
	 * @return an instance of the lattice that this domain works with
	 */
	L makeLattice();

	/**
	 * Yields the state of this domain after a call has been performed. This
	 * method allows the domain to properly merge the execution back into the
	 * caller, possibly restoring portions of the execution state. This might be
	 * useful, for instance, to restore some local security context that is not
	 * dependent on the callee execution (e.g., if the callee's return is in
	 * some user-controlled code, returning to the caller also goes back to non
	 * user-controlled execution).
	 * 
	 * @param entryState the state before the call
	 * @param callres    the state returned by the callee
	 * @param call       the program point corresponding to the call
	 * 
	 * @return the state after the call
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	default L onCallReturn(
			L entryState,
			L callres,
			ProgramPoint call)
			throws SemanticException {
		return callres;
	}

	/**
	 * Yields the states of this domain after (part of) the normal execution has
	 * been moved to an error continuation, that is, when an error is raised.
	 * The move is <i>total</i> if all executions raise the error, and in this
	 * case {@code remaining} is bottom. Otherwise, the move is <i>partial</i>:
	 * some executions raise the error ({@code moved}) while others continue
	 * normally ({@code remaining}). This also happens when returning from a
	 * call whose callee raised errors, with {@code pp} being the call. This
	 * method allows the domain to adjust the information that is stored for
	 * both the normal execution and the error continuation. By default, this
	 * method returns the given states unchanged.
	 * 
	 * @param remaining the state of the executions that continue normally
	 *                      (bottom if the move is total)
	 * @param moved     the state of the executions that raise the error
	 * @param error     the error continuation that {@code moved} is moved to
	 * @param pp        the program point where the move happens
	 * 
	 * @return the pair containing the updated {@code remaining} (left) and
	 *             {@code moved} (right) states
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	default Pair<L, L> onExecutionToError(
			L remaining,
			L moved,
			Error error,
			ProgramPoint pp)
			throws SemanticException {
		return Pair.of(remaining, moved);
	}

	/**
	 * Yields the state of this domain after it has been moved from one or more
	 * error continuations back to the normal execution, that is, when errors
	 * are caught. The given state is the one resulting from the merge of all
	 * the caught errors' states, after the caught error has been (possibly)
	 * assigned to the variable of the handler. This method allows the domain to
	 * adjust the information that is stored for the normal execution. By
	 * default, this method returns {@code state} unchanged.
	 * 
	 * @param state          the state of the caught error continuations being
	 *                           moved
	 * @param protectedBlock the block whose errors are being caught
	 * @param pp             the program point where the move happens
	 * 
	 * @return the state to use for the normal execution
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	default L onErrorToExecution(
			L state,
			ProtectedBlock protectedBlock,
			ProgramPoint pp)
			throws SemanticException {
		return state;
	}

	/**
	 * Yields the states of this domain after (part of) the normal execution has
	 * been moved to the halting continuation, that is, when the program
	 * terminates its execution. The move is <i>total</i> if all executions
	 * halt, and in this case {@code remaining} is bottom. Otherwise, the move
	 * is <i>partial</i>: some executions halt ({@code moved}) while others
	 * continue normally ({@code remaining}). This also happens when returning
	 * from a call whose callee halted, with {@code pp} being the call. This
	 * method allows the domain to adjust the information that is stored for
	 * both the normal execution and the halting continuation. By default, this
	 * method returns the given states unchanged.
	 * 
	 * @param remaining the state of the executions that continue normally
	 *                      (bottom if the move is total)
	 * @param moved     the state of the executions that halt
	 * @param pp        the program point where the move happens
	 * 
	 * @return the pair containing the updated {@code remaining} (left) and
	 *             {@code moved} (right) states
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	default Pair<L, L> onExecutionToHalting(
			L remaining,
			L moved,
			ProgramPoint pp)
			throws SemanticException {
		return Pair.of(remaining, moved);
	}
}
