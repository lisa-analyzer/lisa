package it.unive.lisa.analysis.nonrelational.value;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.SemanticDomain.Satisfiability;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.BinaryOperator;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.NullConstant;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.TernaryOperator;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.UnaryOperator;
import it.unive.lisa.symbolic.value.ValueExpression;

/**
 * Base implementation for {@link NonRelationalValueDomain}s. This class extends
 * {@link BaseLattice} and implements
 * {@link NonRelationalValueDomain#eval(it.unive.lisa.symbolic.SymbolicExpression, it.unive.lisa.analysis.FunctionalLattice, ProgramPoint)}
 * by taking care of the recursive computation of inner expressions evaluation.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <T> the concrete type of this domain
 */
public abstract class BaseNonRelationalValueDomain<T extends BaseNonRelationalValueDomain<T>> extends BaseLattice<T>
		implements NonRelationalValueDomain<T> {

	@Override
	public final Satisfiability satisfies(ValueExpression expression, ValueEnvironment<T> environment,
			ProgramPoint pp) {
		if (expression instanceof Identifier)
			return satisfiesAbstractValue(environment.getState((Identifier) expression), pp);

		if (expression instanceof NullConstant)
			return satisfiesNullConstant(pp);

		if (expression instanceof Constant)
			return satisfiesNonNullConstant((Constant) expression, pp);

		if (expression instanceof Skip)
			return Satisfiability.UNKNOWN;

		if (expression instanceof PushAny)
			return Satisfiability.UNKNOWN;

		if (expression instanceof UnaryExpression) {
			UnaryExpression unary = (UnaryExpression) expression;

			if (unary.getOperator() == UnaryOperator.LOGICAL_NOT)
				return satisfies((ValueExpression) unary.getExpression(), environment, pp).negate();
			else {
				T arg = eval((ValueExpression) unary.getExpression(), environment, pp);
				if (arg.isBottom())
					return Satisfiability.BOTTOM;
				return satisfiesUnaryExpression(unary.getOperator(), arg, pp);
			}
		}

		if (expression instanceof BinaryExpression) {
			BinaryExpression binary = (BinaryExpression) expression;

			if (binary.getOperator() == BinaryOperator.LOGICAL_AND)
				return satisfies((ValueExpression) binary.getLeft(), environment, pp)
						.and(satisfies((ValueExpression) binary.getRight(), environment, pp));
			else if (binary.getOperator() == BinaryOperator.LOGICAL_OR)
				return satisfies((ValueExpression) binary.getLeft(), environment, pp)
						.or(satisfies((ValueExpression) binary.getRight(), environment, pp));
			else {
				T left = eval((ValueExpression) binary.getLeft(), environment, pp);
				T right = eval((ValueExpression) binary.getRight(), environment, pp);
				if (left.isBottom() || right.isBottom())
					return Satisfiability.BOTTOM;
				return satisfiesBinaryExpression(binary.getOperator(), left, right, pp);
			}
		}

		if (expression instanceof TernaryExpression) {
			TernaryExpression ternary = (TernaryExpression) expression;

			T left = eval((ValueExpression) ternary.getLeft(), environment, pp);
			T middle = eval((ValueExpression) ternary.getMiddle(), environment, pp);
			T right = eval((ValueExpression) ternary.getRight(), environment, pp);

			if (left.isBottom() || middle.isBottom() || right.isBottom())
				return Satisfiability.BOTTOM;

			return satisfiesTernaryExpression(ternary.getOperator(), left, middle, right, pp);
		}

		return Satisfiability.UNKNOWN;
	}

	@Override
	public final T eval(ValueExpression expression, ValueEnvironment<T> environment, ProgramPoint pp) {
		if (expression instanceof Identifier)
			return environment.getState((Identifier) expression);

		if (expression instanceof NullConstant)
			return evalNullConstant(pp);

		if (expression instanceof Constant)
			return evalNonNullConstant((Constant) expression, pp);

		if (expression instanceof Skip)
			return bottom();

		if (expression instanceof PushAny)
			return top();

		if (expression instanceof UnaryExpression) {
			UnaryExpression unary = (UnaryExpression) expression;

			T arg = eval((ValueExpression) unary.getExpression(), environment, pp);
			if (arg.isTop() || arg.isBottom())
				return arg;

			return evalUnaryExpression(unary.getOperator(), arg, pp);
		}

		if (expression instanceof BinaryExpression) {
			BinaryExpression binary = (BinaryExpression) expression;

			T left = eval((ValueExpression) binary.getLeft(), environment, pp);
			if (left.isTop() || left.isBottom())
				return left;

			T right = eval((ValueExpression) binary.getRight(), environment, pp);
			if (right.isTop() || right.isBottom())
				return right;

			return evalBinaryExpression(binary.getOperator(), left, right, pp);
		}

		if (expression instanceof TernaryExpression) {
			TernaryExpression ternary = (TernaryExpression) expression;

			T left = eval((ValueExpression) ternary.getLeft(), environment, pp);
			if (left.isTop() || left.isBottom())
				return left;

			T middle = eval((ValueExpression) ternary.getMiddle(), environment, pp);
			if (middle.isTop() || middle.isBottom())
				return middle;

			T right = eval((ValueExpression) ternary.getRight(), environment, pp);
			if (right.isTop() || right.isBottom())
				return right;

			return evalTernaryExpression(ternary.getOperator(), left, middle, right, pp);
		}

		return bottom();
	}

	/**
	 * Yields the evaluation of the null constant {@link NullConstant}.
	 * 
	 * @param pp the program point that where this operation is being evaluated
	 * 
	 * @return the evaluation of the constant
	 */
	protected abstract T evalNullConstant(ProgramPoint pp);

	/**
	 * Yields the evaluation of the given non-null constant.
	 * 
	 * @param constant the constant to evaluate
	 * @param pp       the program point that where this operation is being
	 *                     evaluated
	 * 
	 * @return the evaluation of the constant
	 */
	protected abstract T evalNonNullConstant(Constant constant, ProgramPoint pp);

	/**
	 * Yields the evaluation of a {@link UnaryExpression} applying
	 * {@code operator} to an expression whose abstract value is {@code arg}. It
	 * is guaranteed that {@code arg} is neither {@link #top()} or
	 * {@link #bottom()}.
	 * 
	 * @param operator the operator applied by the expression
	 * @param arg      the instance of this domain representing the abstract
	 *                     value of the expresion's argument
	 * @param pp       the program point that where this operation is being
	 *                     evaluated
	 * 
	 * @return the evaluation of the expression
	 */
	protected abstract T evalUnaryExpression(UnaryOperator operator, T arg, ProgramPoint pp);

	/**
	 * Yields the evaluation of a {@link BinaryExpression} applying
	 * {@code operator} to two expressions whose abstract value are {@code left}
	 * and {@code right}, respectively. It is guaranteed that both {@code left}
	 * and {@code right} are neither {@link #top()} or {@link #bottom()}.
	 * 
	 * @param operator the operator applied by the expression
	 * @param left     the instance of this domain representing the abstract
	 *                     value of the left-hand side argument
	 * @param right    the instance of this domain representing the abstract
	 *                     value of the right-hand side argument
	 * @param pp       the program point that where this operation is being
	 *                     evaluated
	 * 
	 * @return the evaluation of the expression
	 */
	protected abstract T evalBinaryExpression(BinaryOperator operator, T left, T right, ProgramPoint pp);

	/**
	 * Yields the evaluation of a {@link TernaryExpression} applying
	 * {@code operator} to two expressions whose abstract value are
	 * {@code left}, {@code middle} and {@code right}, respectively. It is
	 * guaranteed that both {@code left} and {@code right} are neither
	 * {@link #top()} or {@link #bottom()}.
	 * 
	 * @param operator the operator applied by the expression
	 * @param left     the instance of this domain representing the abstract
	 *                     value of the left-hand side argument
	 * @param middle   the instance of this domain representing the abstract
	 *                     value of the middle argument
	 * @param right    the instance of this domain representing the abstract
	 *                     value of the right-hand side argument
	 * @param pp       the program point that where this operation is being
	 *                     evaluated
	 * 
	 * @return the evaluation of the expression
	 */
	protected abstract T evalTernaryExpression(TernaryOperator operator, T left, T middle, T right, ProgramPoint pp);

	/**
	 * Yields the satisfiability of an abstract value of type {@code <T>}.
	 * 
	 * @param value the abstract value whose satisfiability is to be evaluated
	 * @param pp    the program point that where this operation is being
	 *                  evaluated
	 * 
	 * @return {@link Satisfiability#SATISFIED} if the expression is satisfied
	 *             by this domain, {@link Satisfiability#NOT_SATISFIED} if it is
	 *             not satisfied, or {@link Satisfiability#UNKNOWN} if it is
	 *             either impossible to determine if it satisfied, or if it is
	 *             satisfied by some values and not by some others (this is
	 *             equivalent to a TOP boolean value)
	 */
	protected abstract Satisfiability satisfiesAbstractValue(T value, ProgramPoint pp);

	/**
	 * Yields the satisfiability of the null constant {@link NullConstant} on
	 * this abstract domain.
	 * 
	 * @param pp the program point that where this operation is being evaluated
	 * 
	 * @return {@link Satisfiability#SATISFIED} if the expression is satisfied
	 *             by this domain, {@link Satisfiability#NOT_SATISFIED} if it is
	 *             not satisfied, or {@link Satisfiability#UNKNOWN} if it is
	 *             either impossible to determine if it satisfied, or if it is
	 *             satisfied by some values and not by some others (this is
	 *             equivalent to a TOP boolean value)
	 */
	protected abstract Satisfiability satisfiesNullConstant(ProgramPoint pp);

	/**
	 * Yields the satisfiability of the given non-null constant on this abstract
	 * domain.
	 * 
	 * @param constant the constant to satisfied
	 * @param pp       the program point that where this operation is being
	 *                     evaluated
	 * 
	 * @return {@link Satisfiability#SATISFIED} is the constant is satisfied by
	 *             this domain, {@link Satisfiability#NOT_SATISFIED} if it is
	 *             not satisfied, or {@link Satisfiability#UNKNOWN} if it is
	 *             either impossible to determine if it satisfied, or if it is
	 *             satisfied by some values and not by some others (this is
	 *             equivalent to a TOP boolean value)
	 */
	protected abstract Satisfiability satisfiesNonNullConstant(Constant constant, ProgramPoint pp);

	/**
	 * Yields the satisfiability of a {@link UnaryExpression} applying
	 * {@code operator} to an expression whose abstract value is {@code arg},
	 * returning an instance of {@link Satisfiability}. It is guaranteed that
	 * {@code operator} is not {@link UnaryOperator#LOGICAL_NOT} and {@code arg}
	 * is not {@link #bottom()}.
	 * 
	 * @param operator the unary operator applied by the expression
	 * @param arg      an instance of this abstract domain representing the
	 *                     argument of the unary expression
	 * @param pp       the program point that where this operation is being
	 *                     evaluated
	 * 
	 * @return {@link Satisfiability#SATISFIED} if the expression is satisfied
	 *             by this domain, {@link Satisfiability#NOT_SATISFIED} if it is
	 *             not satisfied, or {@link Satisfiability#UNKNOWN} if it is
	 *             either impossible to determine if it satisfied, or if it is
	 *             satisfied by some values and not by some others (this is
	 *             equivalent to a TOP boolean value)
	 */
	protected abstract Satisfiability satisfiesUnaryExpression(UnaryOperator operator, T arg, ProgramPoint pp);

	/**
	 * Yields the satisfiability of a {@link BinaryExpression} applying
	 * {@code operator} to two expressions whose abstract values are
	 * {@code left}, and {@code right}. This method returns an instance of
	 * {@link Satisfiability}. It is guaranteed that {@code operator} is neither
	 * {@link BinaryOperator#LOGICAL_AND} nor {@link BinaryOperator#LOGICAL_OR},
	 * and that both {@code left} and {@code right} are not {@link #bottom()}.
	 * 
	 * @param operator the binary operator applied by the expression
	 * @param left     an instance of this abstract domain representing the
	 *                     argument of the left-hand side of the binary
	 *                     expression
	 * @param right    an instance of this abstract domain representing the
	 *                     argument of the right-hand side of the binary
	 *                     expression
	 * @param pp       the program point that where this operation is being
	 *                     evaluated
	 * 
	 * @return {@link Satisfiability#SATISFIED} if the expression is satisfied
	 *             by this domain, {@link Satisfiability#NOT_SATISFIED} if it is
	 *             not satisfied, or {@link Satisfiability#UNKNOWN} if it is
	 *             either impossible to determine if it satisfied, or if it is
	 *             satisfied by some values and not by some others (this is
	 *             equivalent to a TOP boolean value)
	 */
	protected abstract Satisfiability satisfiesBinaryExpression(BinaryOperator operator, T left, T right,
			ProgramPoint pp);

	/**
	 * Yields the satisfiability of a {@link TernaryExpression} applying
	 * {@code operator} to three expressions whose abstract values are
	 * {@code left}, {@code middle} and {@code right}. This method returns an
	 * instance of {@link Satisfiability}. It is guaranteed that {@code left},
	 * {@code middle} and {@code right} are not {@link #bottom()}.
	 * 
	 * @param operator the ternary operator applied by the expression
	 * @param left     an instance of this abstract domain representing the
	 *                     argument of the left-most side of the ternary
	 *                     expression
	 * @param middle   an instance of this abstract domain representing the
	 *                     argument in the middle of the ternary expression
	 * @param right    an instance of this abstract domain representing the
	 *                     argument of the right-most side of the ternary
	 *                     expression
	 * @param pp       the program point that where this operation is being
	 *                     evaluated
	 * 
	 * @return {@link Satisfiability#SATISFIED} if the expression is satisfied
	 *             by this domain, {@link Satisfiability#NOT_SATISFIED} if it is
	 *             not satisfied, or {@link Satisfiability#UNKNOWN} if it is
	 *             either impossible to determine if it satisfied, or if it is
	 *             satisfied by some values and not by some others (this is
	 *             equivalent to a TOP boolean value)
	 */
	protected abstract Satisfiability satisfiesTernaryExpression(TernaryOperator operator, T left, T middle, T right,
			ProgramPoint pp);

	@Override
	public final String toString() {
		return representation();
	}
}
