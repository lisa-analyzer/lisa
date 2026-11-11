package it.unive.lisa.triage.encoding;

import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.triage.smt.Sort;
import it.unive.lisa.triage.smt.Term;
import java.util.List;
import java.util.function.Function;

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
	 * Yields a fresh constant of the given sort, standing for the value of the
	 * given expression that is not encoded precisely only when the given guard
	 * does not hold: the caller must use the constant only in that case (e.g.,
	 * as {@code ite(guard, precise, fresh)}). Implementations can use the guard
	 * to decide, given a model, whether the imprecise value has been used. By
	 * default, this is the same as {@link #fresh(Sort, SymbolicExpression)}.
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
	 * condition to decide, given a model, whether the encoding is precise. By
	 * default, this does nothing.
	 *
	 * @param guard the condition
	 */
	default void exactOnlyIf(
			Term guard) {
	}

	/**
	 * Signals that the encoding being built is precise only if the given
	 * condition holds, as {@link #exactOnlyIf(Term)}, because of the encoding
	 * of the given expression. By default, this calls
	 * {@link #exactOnlyIf(Term)}.
	 *
	 * @param guard  the condition
	 * @param origin the expression whose encoding is precise only if the
	 *                   condition holds (can be {@code null} if unknown)
	 */
	default void exactOnlyIf(
			Term guard,
			SymbolicExpression origin) {
		exactOnlyIf(guard);
	}

	/**
	 * Signals that the encoding being built is precise only if the given
	 * condition holds, as {@link #exactOnlyIf(Term, SymbolicExpression)},
	 * because the given expression reads a location of the heap whose value is
	 * computed precisely only if the location has been written by the model
	 * (see {@link it.unive.lisa.triage.model.HeapModel}). By default, this
	 * calls {@link #exactOnlyIf(Term, SymbolicExpression)}.
	 *
	 * @param written the condition stating that the location has been written
	 * @param origin  the expression reading the heap
	 */
	default void exactOnlyIfWritten(
			Term written,
			SymbolicExpression origin) {
		exactOnlyIf(written, origin);
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
	 * not encoded precisely only when the given guard does not hold (the caller
	 * must use the application only in that case). Unlike
	 * {@link #fresh(Sort, SymbolicExpression, Term)}, the same function applied
	 * to the same arguments yields the same value, in every encoding that uses
	 * it (e.g., the result of an arithmetic operation over floating point
	 * numbers is unknown, but always the same for the same operands). By
	 * default, this is the same as
	 * {@link #fresh(Sort, SymbolicExpression, Term)}.
	 *
	 * @param function  the name of the function
	 * @param result    the sort of the result
	 * @param arguments the arguments
	 * @param origin    the expression whose value is not encoded precisely
	 * @param guard     the condition under which the application is not used
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

	/**
	 * Yields a term equivalent to the given one, possibly a shorter one (e.g.,
	 * a constant defined as the given term): encodings that use an operand more
	 * than once (e.g., in the branches of a conditional term) use this to avoid
	 * their size growing exponentially with the nesting of the expressions. By
	 * default, this is the given term itself.
	 *
	 * @param term the term
	 *
	 * @return the equivalent term
	 */
	default Term share(
			Term term) {
		return term;
	}

	/**
	 * Yields the application of an unspecified, deterministic function to the
	 * given arguments, standing for the value of the given expression, that is
	 * never encoded precisely (e.g., the result of an operator the encoder does
	 * not know). As for
	 * {@link #apply(String, Sort, List, SymbolicExpression, Term)}, the same
	 * function applied to the same arguments yields the same value, but, as for
	 * {@link #fresh(Sort, SymbolicExpression)}, the value is never precise. By
	 * default, this is the same as {@link #fresh(Sort, SymbolicExpression)}.
	 *
	 * @param function  the name of the function
	 * @param result    the sort of the result
	 * @param arguments the arguments
	 * @param origin    the expression whose value is not encoded precisely
	 *
	 * @return the application
	 */
	default Term unknown(
			String function,
			Sort result,
			List<Term> arguments,
			SymbolicExpression origin) {
		return fresh(result, origin);
	}

	/**
	 * Yields the application of a deterministic function to the given
	 * arguments, standing for the value of the given expression, that the
	 * encoder cannot encode but can compute on concrete values (e.g., a
	 * mathematical function of a library, such as the sine of a floating point
	 * number): the arguments and the result are floating point numbers, and
	 * the given evaluator computes the result from the values of the arguments
	 * ({@link Double}s for {@link Sort#FLOAT64}, {@link Float}s for
	 * {@link Sort#FLOAT32}). Implementations can use the evaluator to decide,
	 * given a model, whether the value of the application in the model is the
	 * one of the function, and thus precise. By default, this is the same as
	 * {@link #unknown(String, Sort, List, SymbolicExpression)}.
	 *
	 * @param function  the name of the function
	 * @param result    the sort of the result
	 * @param arguments the arguments
	 * @param evaluator the function computing the result from the values of
	 *                      the arguments
	 * @param origin    the expression whose value is the application
	 *
	 * @return the application
	 */
	default Term evaluable(
			String function,
			Sort result,
			List<Term> arguments,
			Function<List<Number>, Number> evaluator,
			SymbolicExpression origin) {
		return unknown(function, result, arguments, origin);
	}

	/**
	 * Yields the application of a function of one argument that the encoder
	 * can compute on concrete values, as
	 * {@link #evaluable(String, Sort, List, Function, SymbolicExpression)},
	 * that is known to be monotone: non-decreasing if {@code increasing}
	 * holds, non-increasing otherwise (e.g., the exponential function).
	 * Implementations can use the values of the function they know to bound
	 * its other values. By default, this is the same as
	 * {@link #evaluable(String, Sort, List, Function, SymbolicExpression)}.
	 *
	 * @param function   the name of the function
	 * @param result     the sort of the result
	 * @param argument   the argument
	 * @param evaluator  the function computing the result from the value of
	 *                       the argument
	 * @param increasing whether the function is non-decreasing (or
	 *                       non-increasing)
	 * @param origin     the expression whose value is the application
	 *
	 * @return the application
	 */
	default Term monotone(
			String function,
			Sort result,
			Term argument,
			Function<List<Number>, Number> evaluator,
			boolean increasing,
			SymbolicExpression origin) {
		return evaluable(function, result, List.of(argument), evaluator, origin);
	}
}
