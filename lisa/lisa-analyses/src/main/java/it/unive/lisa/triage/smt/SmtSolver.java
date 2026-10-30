package it.unive.lisa.triage.smt;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * An incremental SMT solver. Implementations might rely on external tools that
 * are not available in every environment: in that case, their creation fails
 * with an {@link SmtSolverUnavailableException}. All methods throw an
 * {@link SmtException} if the solver cannot complete the request. Solvers hold
 * external resources, and must be closed once they are no longer needed.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface SmtSolver
		extends
		AutoCloseable {

	/**
	 * Declares a new uninterpreted sort with the given name.
	 *
	 * @param name the name of the sort
	 *
	 * @return the declared sort
	 */
	Sort declareSort(
			String name);

	/**
	 * Declares the given algebraic datatype, so that its terms can be used.
	 *
	 * @param datatype the datatype to declare
	 *
	 * @return the sort of the terms of the datatype
	 */
	Sort declareDatatype(
			Datatype datatype);

	/**
	 * Declares a new constant with the given name and sort.
	 *
	 * @param name the name of the constant
	 * @param sort the sort of the constant
	 *
	 * @return the term representing the constant
	 */
	Term declare(
			String name,
			Sort sort);

	/**
	 * Adds the given boolean term to the assertions of the current scope.
	 *
	 * @param term the term to assert
	 */
	void assertTerm(
			Term term);

	/**
	 * Adds the given boolean term to the assertions of the current scope,
	 * naming it so that it can appear in unsat cores (see
	 * {@link #getUnsatCore()}).
	 *
	 * @param name the name of the assertion
	 * @param term the term to assert
	 */
	void assertNamed(
			String name,
			Term term);

	/**
	 * Opens a new scope. Declarations and assertions made in the scope are
	 * discarded when the scope is closed through {@link #pop()}.
	 */
	void push();

	/**
	 * Closes the innermost scope opened through {@link #push()}.
	 */
	void pop();

	/**
	 * Checks the satisfiability of the current assertions.
	 *
	 * @return the result of the check
	 */
	SmtResult check();

	/**
	 * Yields the values of the given terms in the model computed by the last
	 * {@link #check()}, that must have returned {@link SmtResult#SAT}. Values
	 * are given in their SMT-LIB representation (e.g., {@code 4},
	 * {@code (- 3)}, {@code "abc"}, {@code #x0f}).
	 *
	 * @param terms the terms whose values should be retrieved
	 *
	 * @return a mapping from each term to its value
	 */
	Map<Term, String> getValues(
			Collection<Term> terms);

	/**
	 * Yields the names of the named assertions (see
	 * {@link #assertNamed(String, Term)}) forming an unsatisfiable core of the
	 * current assertions. The last {@link #check()} must have returned
	 * {@link SmtResult#UNSAT}.
	 *
	 * @return the names of the assertions in the core
	 */
	List<String> getUnsatCore();

	/**
	 * Yields the reason why the last {@link #check()} returned
	 * {@link SmtResult#UNKNOWN}, as reported by the solver.
	 *
	 * @return the reason
	 */
	String getReasonUnknown();

	/**
	 * Releases the resources held by this solver. Further requests to the
	 * solver will fail.
	 */
	@Override
	void close();
}
