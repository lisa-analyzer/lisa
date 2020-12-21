package it.unive.lisa.analysis;

import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;

public class ValueCartesianProduct<T1 extends ValueDomain<T1>, T2 extends ValueDomain<T2>>
		extends CartesianProduct<T1, T2, ValueExpression, Identifier>
		implements ValueDomain<ValueCartesianProduct<T1, T2>> {

	public ValueCartesianProduct(T1 left, T2 right) {
		super(left, right);
	}

	@Override
	public ValueCartesianProduct<T1, T2> assign(Identifier id, ValueExpression expression) throws SemanticException {
		T1 newLeft = left.assign(id, expression);
		T2 newRight = right.assign(id, expression);
		return new ValueCartesianProduct<T1, T2>(newLeft, newRight);
	}

	@Override
	public ValueCartesianProduct<T1, T2> smallStepSemantics(ValueExpression expression) throws SemanticException {
		T1 newLeft = left.smallStepSemantics(expression);
		T2 newRight = right.smallStepSemantics(expression);
		return new ValueCartesianProduct<T1, T2>(newLeft, newRight);
	}

	@Override
	public ValueCartesianProduct<T1, T2> assume(ValueExpression expression) throws SemanticException {
		T1 newLeft = left.assume(expression);
		T2 newRight = right.assume(expression);
		return new ValueCartesianProduct<T1, T2>(newLeft, newRight);
	}

	@Override
	public ValueCartesianProduct<T1, T2> forgetIdentifier(Identifier id) throws SemanticException {
		T1 newLeft = left.forgetIdentifier(id);
		T2 newRight = right.forgetIdentifier(id);
		return new ValueCartesianProduct<T1, T2>(newLeft, newRight);
	}

	@Override
	public Satisfiability satisfies(ValueExpression expression) throws SemanticException {
		return left.satisfies(expression).and(right.satisfies(expression));
	}

	@Override
	public String representation() {
		return left.representation() + ", " + right.representation();
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
