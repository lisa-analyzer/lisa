package it.unive.lisa.analysis;

import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;

/**
 * A Cartesian product between two non-communicating {@link ValueDomain}s (i.e.,
 * no exchange of information between the abstract domains) assigning
 * {@link Identifier}s and handling {@link ValueExpression}s.
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 *
 * @param <T1> the concrete instance of the left-hand side value abstract domain
 *                 of the Cartesian product
 * @param <T2> the concrete instance of the right-hand side value abstract
 *                 domain of the Cartesian product
 */
public class ValueCartesianProduct<T1 extends ValueDomain<T1>, T2 extends ValueDomain<T2>>
		extends CartesianProduct<T1, T2, ValueExpression, Identifier>
		implements ValueDomain<ValueCartesianProduct<T1, T2>> {

	/**
	 * Builds the value Cartesian product.
	 * 
	 * @param left  the left-hand side of the value Cartesian product
	 * @param right the right-hand side of the value Cartesian product
	 */
	public ValueCartesianProduct(T1 left, T2 right) {
		super(left, right);
	}

	@Override
	public ValueCartesianProduct<T1, T2> assign(Identifier id, ValueExpression expression, ProgramPoint pp)
			throws SemanticException {
		T1 newLeft = left.assign(id, expression, pp);
		T2 newRight = right.assign(id, expression, pp);
		return new ValueCartesianProduct<T1, T2>(newLeft, newRight);
	}

	@Override
	public ValueCartesianProduct<T1, T2> smallStepSemantics(ValueExpression expression, ProgramPoint pp)
			throws SemanticException {
		T1 newLeft = left.smallStepSemantics(expression, pp);
		T2 newRight = right.smallStepSemantics(expression, pp);
		return new ValueCartesianProduct<T1, T2>(newLeft, newRight);
	}

	@Override
	public ValueCartesianProduct<T1, T2> assume(ValueExpression expression, ProgramPoint pp) throws SemanticException {
		T1 newLeft = left.assume(expression, pp);
		T2 newRight = right.assume(expression, pp);
		return new ValueCartesianProduct<T1, T2>(newLeft, newRight);
	}

	@Override
	public ValueCartesianProduct<T1, T2> forgetIdentifier(Identifier id) throws SemanticException {
		T1 newLeft = left.forgetIdentifier(id);
		T2 newRight = right.forgetIdentifier(id);
		return new ValueCartesianProduct<T1, T2>(newLeft, newRight);
	}

	@Override
	public Satisfiability satisfies(ValueExpression expression, ProgramPoint pp) throws SemanticException {
		return left.satisfies(expression, pp).and(right.satisfies(expression, pp));
	}

	@Override
	public String toString() {
		return representation();
	}

	@Override
	public String representation() {
		if (left instanceof ValueEnvironment && right instanceof ValueEnvironment) {
			ValueEnvironment<?> leftEnv = (ValueEnvironment<?>) left;
			ValueEnvironment<?> rightEnv = (ValueEnvironment<?>) right;
			String result = "";
			if (!leftEnv.isTop() && !leftEnv.isBottom()) {
				for (Identifier x : leftEnv.getKeys())
					result += x + ": (" + leftEnv.getState(x).representation() + ", "
							+ rightEnv.getState(x).representation() + ")\n";
				return result;
			} else if (!rightEnv.isTop() && !rightEnv.isBottom()) {
				for (Identifier x : rightEnv.getKeys())
					result += x + ": (" + leftEnv.getState(x).representation() + ", "
							+ rightEnv.getState(x).representation() + ")\n";
				return result;
			}
		}

		return "(" + left.representation() + ", " + right.representation() + ")";
	}

	@Override
	public ValueCartesianProduct<T1, T2> lub(ValueCartesianProduct<T1, T2> other) throws SemanticException {
		return new ValueCartesianProduct<T1, T2>(left.lub(other.left), right.lub(other.right));
	}

	@Override
	public ValueCartesianProduct<T1, T2> widening(ValueCartesianProduct<T1, T2> other) throws SemanticException {
		return new ValueCartesianProduct<T1, T2>(left.widening(other.left), right.widening(other.right));
	}

	@Override
	public boolean lessOrEqual(ValueCartesianProduct<T1, T2> other) throws SemanticException {
		return left.lessOrEqual(other.left) && right.lessOrEqual(other.right);
	}

	@Override
	public ValueCartesianProduct<T1, T2> top() {
		return new ValueCartesianProduct<T1, T2>(left.top(), right.top());
	}

	@Override
	public ValueCartesianProduct<T1, T2> bottom() {
		return new ValueCartesianProduct<T1, T2>(left.bottom(), right.bottom());
	}
}
