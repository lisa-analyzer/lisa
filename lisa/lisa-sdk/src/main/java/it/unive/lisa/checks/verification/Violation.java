package it.unive.lisa.checks.verification;

import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;

/**
 * A description of when a candidate warning issued on a {@link Statement}
 * corresponds to an actual violation, to be verified by a
 * {@link WarningVerifier} before the warning is issued. Instances of this class
 * are built through the static factories of this class:
 * <ul>
 * <li>{@link #reach()}: reaching the statement is the violation;</li>
 * <li>{@link #condition(Statement, SymbolicExpression)}: the violation happens
 * if a condition holds after a given statement;</li>
 * <li>{@link #error()} and {@link #error(Type)}: the violation happens if the
 * statement moves the execution to an error continuation.</li>
 * </ul>
 * {@link WarningVerifier}s are not required to support all kinds of
 * violations, and should yield {@link VerificationOutcome#POSSIBLE} for the
 * ones they do not support.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class Violation {

	/**
	 * Builds a {@link Violation} that happens whenever the statement on which
	 * the warning is issued is reached.
	 *
	 * @return the violation
	 */
	public static ReachViolation reach() {
		return ReachViolation.INSTANCE;
	}

	/**
	 * Builds a {@link Violation} that happens whenever the given condition
	 * holds in the state after {@code where} (e.g., the negation of the
	 * condition of an assertion, evaluated after the assertion's
	 * sub-expression).
	 *
	 * @param where     the statement whose post-state the condition must be
	 *                      evaluated in
	 * @param condition the condition that, if it holds, causes the violation
	 *
	 * @return the violation
	 */
	public static ConditionViolation condition(
			Statement where,
			SymbolicExpression condition) {
		return new ConditionViolation(where, condition);
	}

	/**
	 * Builds a {@link Violation} that happens whenever the statement on which
	 * the warning is issued moves the execution to an error continuation,
	 * regardless of the type of the error.
	 *
	 * @return the violation
	 */
	public static ErrorViolation error() {
		return ErrorViolation.ANY;
	}

	/**
	 * Builds a {@link Violation} that happens whenever the statement on which
	 * the warning is issued moves the execution to an error continuation for
	 * an error whose type is a subtype of the given one.
	 *
	 * @param type the type of the errors causing the violation
	 *
	 * @return the violation
	 */
	public static ErrorViolation error(
			Type type) {
		return new ErrorViolation(type);
	}
}
