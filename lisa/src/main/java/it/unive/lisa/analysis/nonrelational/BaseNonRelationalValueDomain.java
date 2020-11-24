package it.unive.lisa.analysis.nonrelational;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.BinaryOperator;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.NullConstant;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.UnaryOperator;
import it.unive.lisa.symbolic.value.ValueExpression;

public abstract class BaseNonRelationalValueDomain<T extends BaseNonRelationalValueDomain<T>> extends BaseLattice<T>
		implements NonRelationalValueDomain<T> {

	@Override
	public final T eval(ValueExpression expression, ValueEnvironment<T> environment) {
		if (expression instanceof Identifier)
			return environment.getState((Identifier) expression);

		if (expression instanceof NullConstant)
			return evalNullConstant((NullConstant) expression);

		if (expression instanceof Constant)
			return evalNonNullConstant((Constant) expression);

		if (expression instanceof Skip)
			return bottom();

		if (expression instanceof UnaryExpression) {
			UnaryExpression unary = (UnaryExpression) expression;

			T arg = eval((ValueExpression) unary.getExpression(), environment);
			if (arg.isTop() || arg.isBottom())
				return arg;

			return evalUnaryExpression(unary.getOperator(), arg);
		}

		if (expression instanceof BinaryExpression) {
			BinaryExpression binary = (BinaryExpression) expression;

			T left = eval((ValueExpression) binary.getLeft(), environment);
			if (left.isTop() || left.isBottom())
				return left;

			T right = eval((ValueExpression) binary.getRight(), environment);
			if (right.isTop() || right.isBottom())
				return right;

			return evalBinaryExpression(binary.getOperator(), left, right);
		}

		return bottom();
	}

	protected abstract T evalNullConstant(NullConstant constant);

	protected abstract T evalNonNullConstant(Constant constant);

	protected abstract T evalUnaryExpression(UnaryOperator operator, T arg);

	protected abstract T evalBinaryExpression(BinaryOperator operator, T left, T right);
}
