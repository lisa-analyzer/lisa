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

/**
 * Base implementation for {@link NonRelationalValueDomain}s. This class extends
 * {@link BaseLattice} and implements
 * {@link NonRelationalValueDomain#eval(it.unive.lisa.symbolic.SymbolicExpression, it.unive.lisa.analysis.FunctionalLattice)}
 * by taking care of the recursive computation of inner expressions evaluation.
 * 
 * @param <T> the concrete type of this domain
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class BaseNonRelationalValueDomain<T extends BaseNonRelationalValueDomain<T>> extends BaseLattice<T>
		implements NonRelationalValueDomain<T> {

	@Override
	public final T eval(ValueExpression expression, ValueEnvironment<T> environment) {
		if (expression instanceof Identifier)
			return environment.getState((Identifier) expression);

		if (expression instanceof NullConstant)
			return evalNullConstant();

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

	/**
	 * Yields the evaluation of the null constant {@link NullConstant}.
	 * 
	 * @return the evaluation of the constant
	 */
	protected abstract T evalNullConstant();

	/**
	 * Yields the evaluation of the given non-null constant.
	 * 
	 * @param constant the constant to evaluate
	 * @return the evaluation of the constant
	 */
	protected abstract T evalNonNullConstant(Constant constant);

	/**
	 * Yields the evaluation of a {@link UnaryExpression} applying {@code operator}
	 * to an expression whose abstract value is {@code arg}. It is guaranteed that
	 * {@code arg} is neither {@link #top()} or {@link #bottom()}.
	 * 
	 * @param operator the operator applied by the expression
	 * @param arg      the instance of this domain representing the abstract value
	 *                 of the expresion's argument
	 * @return the evaluation of the expression
	 */
	protected abstract T evalUnaryExpression(UnaryOperator operator, T arg);

	/**
	 * Yields the evaluation of a {@link BinaryExpression} applying {@code operator}
	 * to two expressions whose abstract value are {@code left} and {@code right},
	 * respectively. It is guaranteed that both {@code left} and {@code right} are
	 * neither {@link #top()} or {@link #bottom()}.
	 * 
	 * @param operator the operator applied by the expression
	 * @param left     the instance of this domain representing the abstract value
	 *                 of the left-hand side argument
	 * @param right    the instance of this domain representing the abstract value
	 *                 of the right-hand side argument
	 * @return the evaluation of the expression
	 */
	protected abstract T evalBinaryExpression(BinaryOperator operator, T left, T right);
}
