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
}
