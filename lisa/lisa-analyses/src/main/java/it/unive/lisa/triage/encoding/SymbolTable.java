package it.unive.lisa.triage.encoding;

import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.triage.smt.Sort;
import it.unive.lisa.triage.smt.Term;

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
}
