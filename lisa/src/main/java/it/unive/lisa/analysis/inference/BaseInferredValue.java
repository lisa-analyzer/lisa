package it.unive.lisa.analysis.inference;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.SemanticDomain.Satisfiability;
import it.unive.lisa.analysis.SemanticException;
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
 * Base implementation for {@link InferredValue}s. This class extends
 * {@link BaseLattice} and implements
 * {@link InferredValue#eval(ValueExpression, InferenceSystem, ProgramPoint)} by
 * taking care of the recursive computation of inner expressions evaluation.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <T> the concrete type of this domain
 */
public abstract class BaseInferredValue<T extends BaseInferredValue<T>> extends BaseLattice<T>
		implements InferredValue<T> {

	@Override
	public final Satisfiability satisfies(ValueExpression expression, InferenceSystem<T> environment, ProgramPoint pp)
			throws SemanticException {
		if (expression instanceof Identifier) {
			InferredPair<T> eval = evalIdentifier((Identifier) expression, environment, pp);
			return satisfiesAbstractValue(eval.getInferred(), eval.getState(), pp);
		}

		if (expression instanceof NullConstant)
			return satisfiesNullConstant(environment.getExecutionState(), pp);

		if (expression instanceof Constant)
			return satisfiesNonNullConstant((Constant) expression, environment.getExecutionState(), pp);

		if (expression instanceof Skip)
			return Satisfiability.UNKNOWN;

		if (expression instanceof PushAny)
			return satisfiesPushAny((PushAny) expression, environment.getExecutionState());

		if (expression instanceof UnaryExpression) {
			UnaryExpression unary = (UnaryExpression) expression;

			if (unary.getOperator() == UnaryOperator.LOGICAL_NOT)
				return satisfies((ValueExpression) unary.getExpression(), environment, pp).negate();
			else {
				InferredPair<T> arg = eval((ValueExpression) unary.getExpression(), environment, pp);
				if (arg.isBottom())
					return Satisfiability.BOTTOM;

				return satisfiesUnaryExpression(unary.getOperator(), arg.getInferred(), environment.getExecutionState(),
						pp);
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
				InferredPair<T> left = eval((ValueExpression) binary.getLeft(), environment, pp);
				if (left.isBottom())
					return Satisfiability.BOTTOM;

				InferredPair<T> right = eval((ValueExpression) binary.getRight(), environment, pp);
				if (right.isBottom())
					return Satisfiability.BOTTOM;

				return satisfiesBinaryExpression(binary.getOperator(), left.getInferred(), right.getInferred(),
						environment.getExecutionState(), pp);
			}
		}

		if (expression instanceof TernaryExpression) {
			TernaryExpression ternary = (TernaryExpression) expression;

			InferredPair<T> left = eval((ValueExpression) ternary.getLeft(), environment, pp);
			if (left.isBottom())
				return Satisfiability.BOTTOM;

			InferredPair<T> middle = eval((ValueExpression) ternary.getMiddle(), environment, pp);
			if (middle.isBottom())
				return Satisfiability.BOTTOM;

			InferredPair<T> right = eval((ValueExpression) ternary.getRight(), environment, pp);
			if (right.isBottom())
				return Satisfiability.BOTTOM;

			return satisfiesTernaryExpression(ternary.getOperator(), left.getInferred(), middle.getInferred(),
					right.getInferred(), environment.getExecutionState(), pp);
		}

		return Satisfiability.UNKNOWN;
	}

	@Override
	public final InferredPair<T> eval(ValueExpression expression, InferenceSystem<T> environment, ProgramPoint pp)
			throws SemanticException {
		if (expression instanceof Identifier)
			return evalIdentifier((Identifier) expression, environment, pp);

		if (expression instanceof NullConstant)
			return evalNullConstant(environment.getExecutionState(), pp);

		if (expression instanceof Constant)
			return evalNonNullConstant((Constant) expression, environment.getExecutionState(), pp);

		if (expression instanceof Skip) {
			T bot = bottom();
			return new InferredPair<>(bot, bot, bot);
		}

		if (expression instanceof PushAny)
			return evalPushAny((PushAny) expression, environment.getExecutionState());

		if (expression instanceof UnaryExpression) {
			UnaryExpression unary = (UnaryExpression) expression;

			InferredPair<T> arg = eval((ValueExpression) unary.getExpression(), environment, pp);
			if (arg.isBottom())
				return arg;

			return evalUnaryExpression(unary.getOperator(), arg.getInferred(), environment.getExecutionState(), pp);
		}

		if (expression instanceof BinaryExpression) {
			BinaryExpression binary = (BinaryExpression) expression;

			InferredPair<T> left = eval((ValueExpression) binary.getLeft(), environment, pp);
			if (left.isBottom())
				return left;

			InferredPair<T> right = eval((ValueExpression) binary.getRight(), environment, pp);
			if (right.isBottom())
				return right;

			if (binary.getOperator() == BinaryOperator.TYPE_CAST)
				return evalTypeCast(binary, left.getInferred(), right.getInferred(), environment.getExecutionState());

			if (binary.getOperator() == BinaryOperator.TYPE_CONV)
				return evalTypeConv(binary, left.getInferred(), right.getInferred(), environment.getExecutionState());

			return evalBinaryExpression(binary.getOperator(), left.getInferred(), right.getInferred(),
					environment.getExecutionState(), pp);
		}

		if (expression instanceof TernaryExpression) {
			TernaryExpression ternary = (TernaryExpression) expression;

			InferredPair<T> left = eval((ValueExpression) ternary.getLeft(), environment, pp);
			if (left.isBottom())
				return left;

			InferredPair<T> middle = eval((ValueExpression) ternary.getMiddle(), environment, pp);
			if (middle.isBottom())
				return middle;

			InferredPair<T> right = eval((ValueExpression) ternary.getRight(), environment, pp);
			if (right.isBottom())
				return right;

			return evalTernaryExpression(ternary.getOperator(), left.getInferred(), middle.getInferred(),
					right.getInferred(), environment.getExecutionState(), pp);
		}

		T top = top();
		return new InferredPair<>(top, top, top);
	}

	/**
	 * Yields the evaluation of an identifier in a given environment.
	 * 
	 * @param id          the identifier to be evaluated
	 * @param environment the environment where the identifier must be evaluated
	 * @param pp          the program point that where this operation is being
	 *                        evaluated
	 * 
	 * @return the evaluation of the identifier
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	@SuppressWarnings("unchecked")
	protected InferredPair<T> evalIdentifier(Identifier id, InferenceSystem<T> environment, ProgramPoint pp)
			throws SemanticException {
		return new InferredPair<>((T) this, environment.getState(id), environment.getExecutionState());
	}

	/**
	 * Yields the evaluation of a push-any expression.
	 * 
	 * @param pushAny the push-any expression to be evaluated
	 * @param state   the current execution state
	 * 
	 * @return the evaluation of the push-any expression
	 */
	protected InferredPair<T> evalPushAny(PushAny pushAny, T state) {
		T top = top();
		return new InferredPair<>(top, top, top);
	}

	/**
	 * Yields the evaluation of the null constant {@link NullConstant}.
	 * 
	 * @param state the current execution state
	 * @param pp    the program point that where this operation is being
	 *                  evaluated
	 * 
	 * @return the evaluation of the constant
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	protected InferredPair<T> evalNullConstant(T state, ProgramPoint pp) throws SemanticException {
		T top = top();
		return new InferredPair<>(top, top, top);
	}

	/**
	 * Yields the evaluation of the given non-null constant.
	 * 
	 * @param constant the constant to evaluate
	 * @param state    the current execution state
	 * @param pp       the program point that where this operation is being
	 *                     evaluated
	 * 
	 * @return the evaluation of the constant
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	protected InferredPair<T> evalNonNullConstant(Constant constant, T state, ProgramPoint pp)
			throws SemanticException {
		T top = top();
		return new InferredPair<>(top, top, top);
	}

	/**
	 * Yields the evaluation of a {@link UnaryExpression} applying
	 * {@code operator} to an expression whose abstract value is {@code arg}. It
	 * is guaranteed that {@code arg} is not {@link #bottom()}.
	 * 
	 * @param operator the operator applied by the expression
	 * @param arg      the instance of this domain representing the abstract
	 *                     value of the expresion's argument
	 * @param state    the current execution state
	 * @param pp       the program point that where this operation is being
	 *                     evaluated
	 * 
	 * @return the evaluation of the expression
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	protected InferredPair<T> evalUnaryExpression(UnaryOperator operator, T arg, T state, ProgramPoint pp)
			throws SemanticException {
		T top = top();
		return new InferredPair<>(top, top, top);
	}

	/**
	 * Yields the evaluation of a {@link BinaryExpression} applying
	 * {@code operator} to two expressions whose abstract value are {@code left}
	 * and {@code right}, respectively. It is guaranteed that both {@code left}
	 * and {@code right} are not {@link #bottom()} and that {@code operator} is
	 * neither {@link BinaryOperator#TYPE_CAST} nor
	 * {@link BinaryOperator#TYPE_CONV}.
	 * 
	 * @param operator the operator applied by the expression
	 * @param left     the instance of this domain representing the abstract
	 *                     value of the left-hand side argument
	 * @param right    the instance of this domain representing the abstract
	 *                     value of the right-hand side argument
	 * @param state    the current execution state
	 * @param pp       the program point that where this operation is being
	 *                     evaluated
	 * 
	 * @return the evaluation of the expression
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	protected InferredPair<T> evalBinaryExpression(BinaryOperator operator, T left, T right, T state, ProgramPoint pp)
			throws SemanticException {
		T top = top();
		return new InferredPair<>(top, top, top);
	}

	/**
	 * Yields the evaluation of a type conversion expression.
	 * 
	 * @param conv  the type conversion expression
	 * @param left  the left expression, namely the expression to be converted
	 * @param right the right expression, namely the types to which left should
	 *                  be converted
	 * @param state the current execution state
	 * 
	 * @return the evaluation of the type conversion expression
	 */
	@SuppressWarnings("unchecked")
	protected InferredPair<T> evalTypeConv(BinaryExpression conv, T left, T right, T state) {
		T bot = bottom();
		return conv.getTypes().isEmpty() ? new InferredPair<>(bot, bot, bot)
				: new InferredPair<>((T) this, left, state);
	}

	/**
	 * Yields the evaluation of a type cast expression.
	 * 
	 * @param cast  the type casted expression
	 * @param left  the left expression, namely the expression to be casted
	 * @param right the right expression, namely the types to which left should
	 *                  be casted
	 * @param state the current execution state
	 * 
	 * @return the evaluation of the type cast expression
	 */
	@SuppressWarnings("unchecked")
	protected InferredPair<T> evalTypeCast(BinaryExpression cast, T left, T right, T state) {
		T bot = bottom();
		return cast.getTypes().isEmpty() ? new InferredPair<>(bot, bot, bot)
				: new InferredPair<>((T) this, left, state);
	}

	/**
	 * Yields the evaluation of a {@link TernaryExpression} applying
	 * {@code operator} to two expressions whose abstract value are
	 * {@code left}, {@code middle} and {@code right}, respectively. It is
	 * guaranteed that both {@code left} and {@code right} are not
	 * {@link #bottom()}.
	 * 
	 * @param operator the operator applied by the expression
	 * @param left     the instance of this domain representing the abstract
	 *                     value of the left-hand side argument
	 * @param middle   the instance of this domain representing the abstract
	 *                     value of the middle argument
	 * @param right    the instance of this domain representing the abstract
	 *                     value of the right-hand side argument
	 * @param state    the current execution state
	 * @param pp       the program point that where this operation is being
	 *                     evaluated
	 * 
	 * @return the evaluation of the expression
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	protected InferredPair<T> evalTernaryExpression(TernaryOperator operator, T left, T middle, T right, T state,
			ProgramPoint pp) throws SemanticException {
		T top = top();
		return new InferredPair<>(top, top, top);
	}

	/**
	 * Yields the satisfiability of an abstract value of type {@code <T>}.
	 * 
	 * @param value the abstract value whose satisfiability is to be evaluated
	 * @param state the current execution state
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
	protected Satisfiability satisfiesAbstractValue(T value, T state, ProgramPoint pp) {
		return Satisfiability.UNKNOWN;
	}

	/**
	 * Yields the satisfiability of the push any expression.
	 * 
	 * @param pushAny the push any expression to satisfy
	 * @param state   the current execution state
	 * 
	 * @return {@link Satisfiability#SATISFIED} if the expression is satisfied
	 *             by this domain, {@link Satisfiability#NOT_SATISFIED} if it is
	 *             not satisfied, or {@link Satisfiability#UNKNOWN} if it is
	 *             either impossible to determine if it satisfied, or if it is
	 *             satisfied by some values and not by some others (this is
	 *             equivalent to a TOP boolean value)
	 */
	protected Satisfiability satisfiesPushAny(PushAny pushAny, T state) {
		return Satisfiability.UNKNOWN;
	}

	/**
	 * Yields the satisfiability of the null constant {@link NullConstant} on
	 * this abstract domain.
	 * 
	 * @param state the current execution state
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
	protected Satisfiability satisfiesNullConstant(T state, ProgramPoint pp) {
		return Satisfiability.UNKNOWN;
	}

	/**
	 * Yields the satisfiability of the given non-null constant on this abstract
	 * domain.
	 * 
	 * @param constant the constant to satisfied
	 * @param state    the current execution state
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
	protected Satisfiability satisfiesNonNullConstant(Constant constant, T state, ProgramPoint pp) {
		return Satisfiability.UNKNOWN;
	}

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
	 * @param state    the current execution state
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
	protected Satisfiability satisfiesUnaryExpression(UnaryOperator operator, T arg, T state, ProgramPoint pp) {
		return Satisfiability.UNKNOWN;
	}

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
	 * @param state    the current execution state
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
	protected Satisfiability satisfiesBinaryExpression(BinaryOperator operator, T left, T right, T state,
			ProgramPoint pp) {
		return Satisfiability.UNKNOWN;
	}

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
	 * @param state    the current execution state
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
	protected Satisfiability satisfiesTernaryExpression(TernaryOperator operator, T left, T middle, T right, T state,
			ProgramPoint pp) {
		return Satisfiability.UNKNOWN;
	}

	@Override
	public final String toString() {
		return representation().toString();
	}

	@Override
	public InferenceSystem<T> assume(InferenceSystem<T> environment, ValueExpression expression, ProgramPoint pp)
			throws SemanticException {
		return environment;
	}

	@Override
	@SuppressWarnings("unchecked")
	public final T glb(T other) throws SemanticException {
		if (other == null || this.isBottom() || other.isTop() || this == other || this.equals(other)
				|| this.lessOrEqual(other))
			return (T) this;

		if (other.isBottom() || this.isTop() || other.lessOrEqual((T) this))
			return (T) other;

		return glbAux(other);
	}

	/**
	 * Performs the greatest lower bound operation between this inferred value
	 * and {@code other}, assuming that base cases have already been handled. In
	 * particular, it is guaranteed that:
	 * <ul>
	 * <li>{@code other} is not {@code null}</li>
	 * <li>{@code other} is neither <i>top</i> nor <i>bottom</i></li>
	 * <li>{@code this} is neither <i>top</i> nor <i>bottom</i></li>
	 * <li>{@code this} and {@code other} are not the same object (according
	 * both to {@code ==} and to {@link Object#equals(Object)})</li>
	 * <li>{@code this} and {@code other} are not comparable (according to
	 * {@link BaseLattice#lessOrEqual(BaseLattice)})</li>
	 * </ul>
	 * The default implementation returns {@link BaseLattice#bottom()}
	 * 
	 * @param other the other inferred value
	 * 
	 * @return the greatest lower bound between this and other
	 */
	protected T glbAux(T other) {
		return bottom();
	}
}
