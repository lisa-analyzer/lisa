package it.unive.lisa.triage.encoding;

import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.triage.smt.Sort;
import it.unive.lisa.triage.smt.Term;
import java.util.List;

/**
 * The provider of the SMT constants used by an {@link ExpressionEncoder}:
 * implementations decide how program identifiers are named (e.g., with
 * different versions for pre- and post-states) and create fresh constants for
 * unknown values. Implementations are responsible for declaring the constants
 * they return.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface SymbolTable {

	/**
	 * Yields the constant representing the value of the given identifier.
	 *
	 * @param id   the identifier
	 * @param sort the sort of the identifier
	 *
	 * @return the constant
	 */
	Term constant(
			Identifier id,
			Sort sort);

	/**
	 * Yields a fresh constant of the given sort, representing an unknown value
	 * originating from the given expression (e.g., an unknown value introduced
	 * by the program, or the result of an operation that cannot be encoded
	 * precisely).
	 *
	 * @param sort   the sort of the constant
	 * @param origin the expression originating the unknown value
	 *
	 * @return the fresh constant
	 */
	Term fresh(
			Sort sort,
			SymbolicExpression origin);

	/**
	 * Yields a fresh constant of the given sort, standing for the value of
	 * the given expression that is not encoded precisely only when the given
	 * guard does not hold: the caller must use the constant only in that
	 * case (e.g., as {@code ite(guard, precise, fresh)}). Implementations can
	 * use the guard to decide, given a model, whether the imprecise value
	 * has been used. By default, this is the same as
	 * {@link #fresh(Sort, SymbolicExpression)}.
	 *
	 * @param sort   the sort of the constant
	 * @param origin the expression whose value is not encoded precisely
	 * @param guard  the condition under which the constant is not used
	 *
	 * @return the fresh constant
	 */
	default Term fresh(
			Sort sort,
			SymbolicExpression origin,
			Term guard) {
		return fresh(sort, origin);
	}

	/**
	 * Signals that the encoding being built is precise only if the given
	 * condition holds (e.g., a value is read as an integer, and its value is
	 * unspecified if it is not an integer). Implementations can use the
	 * condition to decide, given a model, whether the encoding is precise.
	 * By default, this does nothing.
	 *
	 * @param guard the condition
	 */
	default void exactOnlyIf(
			Term guard) {
	}

	/**
	 * Signals that the expression being encoded can be evaluated only if the
	 * given condition holds, since its operators are not defined otherwise
	 * (e.g., the operands of numeric operators must be numbers): executions
	 * where the condition does not hold do not evaluate the expression.
	 * Implementations can conjoin the condition to the encoding of the
	 * executions evaluating the expression. By default, this does nothing,
	 * which is sound but less precise.
	 *
	 * @param condition the condition
	 */
	default void require(
			Term condition) {
	}

	/**
	 * Yields the application of an unspecified, deterministic function to the
	 * given arguments, standing for the value of the given expression that is
	 * not encoded precisely only when the given guard does not hold (the
	 * caller must use the application only in that case). Unlike
	 * {@link #fresh(Sort, SymbolicExpression, Term)}, the same function
	 * applied to the same arguments yields the same value, in every encoding
	 * that uses it (e.g., the result of an arithmetic operation over
	 * floating point numbers is unknown, but always the same for the same
	 * operands). By default, this is the same as
	 * {@link #fresh(Sort, SymbolicExpression, Term)}.
	 *
	 * @param function  the name of the function
	 * @param result    the sort of the result
	 * @param arguments the arguments
	 * @param origin    the expression whose value is not encoded precisely
	 * @param guard     the condition under which the application is not
	 *                      used
	 *
	 * @return the application
	 */
	default Term apply(
			String function,
			Sort result,
			List<Term> arguments,
			SymbolicExpression origin,
			Term guard) {
		return fresh(result, origin, guard);
	}
}
