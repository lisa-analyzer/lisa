package it.unive.lisa.triage.smt;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * An incremental SMT solver. Implementations might rely on external tools that
 * are not available in every environment: in that case, their creation fails
 * with an {@link SmtSolverUnavailableException}. All methods throw an
 * {@link SmtException} if the solver cannot complete the request: requests that
 * answer nothing (declarations, definitions, assertions and scopes) might be
 * sent to the solver later, together with the next request answering something
 * (e.g., {@link #check()}), and their failures reported by it. Solvers hold
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
	 * Declares a new uninterpreted function with the given name and signature.
	 * Applications of the function are built with
	 * {@link Term#app(String, Sort, Term...)}, using the returned symbol.
	 *
	 * @param name      the name of the function
	 * @param arguments the sorts of the arguments
	 * @param result    the sort of the result
	 *
	 * @return the symbol of the function
	 */
	String declareFunction(
			String name,
			List<Sort> arguments,
			Sort result);

	/**
	 * Defines a constant equal to the given term, that can be used in place of
	 * the term in later requests (e.g., to send a term that is used by many
	 * queries only once). Definitions, as declarations, are global, and survive
	 * {@link #pop()}.
	 *
	 * @param name the name of the constant, that must not be already declared
	 *                 or defined
	 * @param body the term
	 *
	 * @return the constant
	 */
	Term define(
			String name,
			Term body);

	/**
	 * Discards all declarations, definitions, assertions and scopes, so that
	 * the solver can be reused for unrelated queries as if it was new. Solvers
	 * that cannot be reset are left unchanged.
	 *
	 * @return {@code true} if the solver has been reset, {@code false} if it
	 *             cannot be reset
	 */
	default boolean reset() {
		return false;
	}

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
	 * Opens a new scope. Assertions made in the scope are discarded when the
	 * scope is closed through {@link #pop()}, while declarations (of sorts,
	 * datatypes, and constants) are global, and survive it.
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
	 * Signals that the given declared function stands for a function over
	 * floating point numbers that can be computed on concrete values (e.g., the
	 * sine), so that solvers can use it to look for models where the function
	 * has its actual values (e.g., when they cannot decide a query otherwise).
	 * The function is still uninterpreted in the queries. By default, this does
	 * nothing.
	 *
	 * @param function  the function, as returned by
	 *                      {@link #declareFunction(String, List, Sort)}
	 * @param arguments the sorts of its arguments
	 * @param result    the sort of its result
	 * @param evaluator the function computing the result from the values of the
	 *                      arguments ({@link Double}s for {@link Sort#FLOAT64},
	 *                      {@link Float}s for {@link Sort#FLOAT32})
	 */
	default void evaluable(
			String function,
			List<Sort> arguments,
			Sort result,
			Function<List<Number>, Number> evaluator) {
	}

	/**
	 * Yields whether the solver can still be used: a solver that has been
	 * closed (e.g., because it did not answer in time) cannot. By default, it
	 * always can.
	 *
	 * @return whether the solver can still be used
	 */
	default boolean isAlive() {
		return true;
	}

	/**
	 * Releases the resources held by this solver. Further requests to the
	 * solver will fail.
	 */
	@Override
	void close();
}
