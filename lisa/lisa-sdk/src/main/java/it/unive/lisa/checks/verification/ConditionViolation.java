package it.unive.lisa.checks.verification;

import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import java.util.Objects;

/**
 * A {@link Violation} that happens whenever a condition holds in the state
 * after a given statement. Instances of this class can be obtained through
 * {@link Violation#condition(Statement, SymbolicExpression)}.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public final class ConditionViolation
		extends
		Violation {

	private final Statement where;

	private final SymbolicExpression condition;

	/**
	 * Builds the violation.
	 *
	 * @param where     the statement whose post-state the condition must be
	 *                      evaluated in
	 * @param condition the condition that, if it holds, causes the violation
	 */
	ConditionViolation(
			Statement where,
			SymbolicExpression condition) {
		Objects.requireNonNull(where);
		Objects.requireNonNull(condition);
		this.where = where;
		this.condition = condition;
	}

	/**
	 * Yields the statement whose post-state the condition must be evaluated in.
	 *
	 * @return the statement
	 */
	public Statement getWhere() {
		return where;
	}

	/**
	 * Yields the condition that, if it holds, causes the violation.
	 *
	 * @return the condition
	 */
	public SymbolicExpression getCondition() {
		return condition;
	}

	@Override
	public int hashCode() {
		return Objects.hash(condition, where);
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ConditionViolation other = (ConditionViolation) obj;
		return Objects.equals(condition, other.condition) && Objects.equals(where, other.where);
	}

	@Override
	public String toString() {
		return condition + " after " + where;
	}
}
