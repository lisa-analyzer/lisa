package it.unive.lisa.symbolic.value;

import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.type.Type;
import java.util.Set;

/**
 * An expression that pushes any possible value on the stack, but only those
 * that satisfy a set of constraints. This is useful to represent top values
 * that are constrained by some conditions, e.g., random numbers between given
 * bounds.<br>
 * <br>
 * Constraints are expressed as a set of {@link BinaryExpression}s, which are
 * used to define the conditions that the pushed values must satisfy. The
 * constraints must be definite, as in with each constraint the set of allowed
 * values shrinks. An empty set of constraints thus represents any possible
 * concrete value. Each constraint is given as a {@link BinaryExpression}, where
 * the left operand is a constant and the right operand can be an arbitrary
 * expression, which should be ignored during the evaluation of the constraint.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class PushFromConstraints extends PushAny {

	private final Set<BinaryExpression> constraints;

	/**
	 * Builds the push from constraints expression.
	 * 
	 * @param staticType  the static type of this expression
	 * @param location    the code location of the statement that has generated
	 *                        this expression
	 * @param constraints the constraints that the pushed values must satisfy;
	 *                        each constraint is a {@link BinaryExpression}
	 *                        where the left operand is a constant and the right
	 *                        operand can be an arbitrary expression, which
	 *                        should be ignored during the evaluation of the
	 *                        constraint
	 */
	public PushFromConstraints(
			Type staticType,
			CodeLocation location,
			BinaryExpression... constraints) {
		this(staticType, location, Set.of(constraints));
	}

	/**
	 * Builds the push from constraints expression.
	 * 
	 * @param staticType  the static type of this expression
	 * @param location    the code location of the statement that has generated
	 *                        this expression
	 * @param constraints the set of constraints that the pushed values must
	 *                        satisfy; each constraint is a
	 *                        {@link BinaryExpression} where the left operand is
	 *                        a constant and the right operand can be an
	 *                        arbitrary expression, which should be ignored
	 *                        during the evaluation of the constraint
	 */
	public PushFromConstraints(
			Type staticType,
			CodeLocation location,
			Set<BinaryExpression> constraints) {
		super(staticType, location);
		this.constraints = constraints;
	}

	/**
	 * Returns the set of constraints that the pushed values must satisfy.
	 * 
	 * @return the set of constraints
	 */
	public Set<BinaryExpression> getConstraints() {
		return constraints;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((constraints == null) ? 0 : constraints.hashCode());
		return result;
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		PushFromConstraints other = (PushFromConstraints) obj;
		if (constraints == null) {
			if (other.constraints != null)
				return false;
		} else if (!constraints.equals(other.constraints))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return super.toString()
			+ " ["
			+ String.join(", ", constraints.stream().map(Object::toString).sorted().toArray(String[]::new))
			+ "]";
	}

}
