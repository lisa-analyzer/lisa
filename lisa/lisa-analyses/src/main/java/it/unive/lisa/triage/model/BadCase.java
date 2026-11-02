package it.unive.lisa.triage.model;

import it.unive.lisa.symbolic.SymbolicExpression;
import java.util.Collections;
import java.util.List;

/**
 * One way of violating a property at the bad location of an {@link AlarmModel}:
 * the violation happens in a state at the beginning of the bad location if both
 * the path condition (leading from the beginning of the location to the point
 * where the property is checked) and the violation condition hold. Both are
 * expressed over the values of identifiers at the beginning of the bad
 * location, and over fresh values.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class BadCase {

	private final List<SymbolicExpression> pathCondition;

	private final SymbolicExpression violation;

	/**
	 * Builds the case.
	 *
	 * @param pathCondition the conjuncts of the path condition
	 * @param violation     the violation condition, or {@code null} if the
	 *                          violation happens as soon as the path condition
	 *                          holds
	 */
	public BadCase(
			List<SymbolicExpression> pathCondition,
			SymbolicExpression violation) {
		this.pathCondition = Collections.unmodifiableList(pathCondition);
		this.violation = violation;
	}

	/**
	 * Yields the conjuncts of the path condition.
	 *
	 * @return the conjuncts
	 */
	public List<SymbolicExpression> getPathCondition() {
		return pathCondition;
	}

	/**
	 * Yields the violation condition.
	 *
	 * @return the condition, or {@code null} if the violation happens as soon
	 *             as the path condition holds
	 */
	public SymbolicExpression getViolation() {
		return violation;
	}

	@Override
	public String toString() {
		return (pathCondition.isEmpty() ? "true" : pathCondition.toString())
				+ " && "
				+ (violation == null ? "true" : violation.toString());
	}
}
