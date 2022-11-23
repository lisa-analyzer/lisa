package it.unive.lisa.analysis.nonrelational.inference;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.SemanticDomain.Satisfiability;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.ExpressionVisitor;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapAllocation;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.NullConstant;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.LogicalAnd;
import it.unive.lisa.symbolic.value.operator.binary.LogicalOr;
import it.unive.lisa.symbolic.value.operator.binary.TypeCast;
import it.unive.lisa.symbolic.value.operator.binary.TypeConv;
import it.unive.lisa.symbolic.value.operator.ternary.TernaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;

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

	/**
	 * A {@link ExpressionVisitor} for {@link BaseInferredValue} instances.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	@SuppressWarnings("unchecked")
	public class EvaluationVisitor implements ExpressionVisitor<InferredPair<T>> {

		private static final String CANNOT_PROCESS_ERROR = "Cannot process a heap expression with an inferred value domain";

		@Override
		public InferredPair<T> visit(AccessChild expression, InferredPair<T> receiver, InferredPair<T> child,
				Object... params) throws SemanticException {
			throw new SemanticException(CANNOT_PROCESS_ERROR);
		}

		@Override
		public InferredPair<T> visit(HeapAllocation expression, Object... params) throws SemanticException {
			throw new SemanticException(CANNOT_PROCESS_ERROR);
		}

		@Override
		public InferredPair<T> visit(HeapReference expression, InferredPair<T> arg, Object... params)
				throws SemanticException {
			throw new SemanticException(CANNOT_PROCESS_ERROR);
		}

		@Override
		public InferredPair<T> visit(HeapDereference expression, InferredPair<T> arg, Object... params)
				throws SemanticException {
			throw new SemanticException(CANNOT_PROCESS_ERROR);
		}

		@Override
		public InferredPair<T> visit(UnaryExpression expression, InferredPair<T> arg, Object... params)
				throws SemanticException {
			if (arg.getInferred().isBottom())
				return arg;

			return evalUnaryExpression(expression.getOperator(), arg.getInferred(),
					((InferenceSystem<T>) params[0]).getExecutionState(), (ProgramPoint) params[1]);
		}

		@Override
		public InferredPair<T> visit(BinaryExpression expression, InferredPair<T> left, InferredPair<T> right,
				Object... params) throws SemanticException {
			if (left.getInferred().isBottom())
				return left;
			if (right.getInferred().isBottom())
				return right;

			if (expression.getOperator() == TypeCast.INSTANCE)
				return evalTypeCast(expression, left.getInferred(), right.getInferred(),
						((InferenceSystem<T>) params[0]).getExecutionState(), (ProgramPoint) params[1]);

			if (expression.getOperator() == TypeConv.INSTANCE)
				return evalTypeConv(expression, left.getInferred(), right.getInferred(),
						((InferenceSystem<T>) params[0]).getExecutionState(), (ProgramPoint) params[1]);

			return evalBinaryExpression(expression.getOperator(), left.getInferred(), right.getInferred(),
					((InferenceSystem<T>) params[0]).getExecutionState(), (ProgramPoint) params[1]);
		}

		@Override
		public InferredPair<T> visit(TernaryExpression expression, InferredPair<T> left, InferredPair<T> middle,
				InferredPair<T> right, Object... params)
				throws SemanticException {
			if (left.getInferred().isBottom())
				return left;
			if (middle.getInferred().isBottom())
				return middle;
			if (right.getInferred().isBottom())
				return right;

			return evalTernaryExpression(expression.getOperator(), left.getInferred(), middle.getInferred(),
					right.getInferred(), ((InferenceSystem<T>) params[0]).getExecutionState(),
					(ProgramPoint) params[1]);
		}

		@Override
		public InferredPair<T> visit(Skip expression, Object... params) throws SemanticException {
			T bot = bottom();
			return new InferredPair<>(bot, bot, bot);
		}

		@Override
		public InferredPair<T> visit(PushAny expression, Object... params) throws SemanticException {
			return evalPushAny(expression, ((InferenceSystem<T>) params[0]).getExecutionState(),
					(ProgramPoint) params[1]);
		}

		@Override
		public InferredPair<T> visit(Constant expression, Object... params) throws SemanticException {
			if (expression instanceof NullConstant)
				return evalNullConstant(((InferenceSystem<T>) params[0]).getExecutionState(), (ProgramPoint) params[1]);
			return evalNonNullConstant(expression, ((InferenceSystem<T>) params[0]).getExecutionState(),
					(ProgramPoint) params[1]);
		}

		@Override
		public InferredPair<T> visit(Identifier expression, Object... params) throws SemanticException {
			return evalIdentifier(expression, (InferenceSystem<T>) params[0], (ProgramPoint) params[1]);
		}

	}

	@Override
	public Satisfiability satisfies(ValueExpression expression, InferenceSystem<T> environment, ProgramPoint pp)
			throws SemanticException {
		if (expression instanceof Identifier) {
			InferredPair<T> eval = evalIdentifier((Identifier) expression, environment, pp);
			return satisfiesAbstractValue(eval.getInferred(), eval.getState(), pp);
		}

		if (expression instanceof NullConstant)
			return satisfiesNullConstant(environment.getExecutionState(), pp);

		if (expression instanceof Constant)
			return satisfiesNonNullConstant((Constant) expression, environment.getExecutionState(), pp);

		if (expression instanceof PushAny)
			return satisfiesPushAny((PushAny) expression, environment.getExecutionState());

		if (expression instanceof UnaryExpression) {
			UnaryExpression unary = (UnaryExpression) expression;

			if (unary.getOperator() == LogicalNegation.INSTANCE)
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

			if (binary.getOperator() == LogicalAnd.INSTANCE)
				return satisfies((ValueExpression) binary.getLeft(), environment, pp)
						.and(satisfies((ValueExpression) binary.getRight(), environment, pp));
			else if (binary.getOperator() == LogicalOr.INSTANCE)
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
	public InferredPair<T> eval(ValueExpression expression, InferenceSystem<T> environment, ProgramPoint pp)
			throws SemanticException {
		return expression.accept(new EvaluationVisitor(), environment, pp);
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
	public InferredPair<T> evalIdentifier(Identifier id, InferenceSystem<T> environment, ProgramPoint pp)
			throws SemanticException {
		return new InferredPair<>((T) this, environment.getState(id), environment.getExecutionState());
	}

	/**
	 * Yields the evaluation of a push-any expression.
	 * 
	 * @param pushAny the push-any expression to be evaluated
	 * @param state   the current execution state
	 * @param pp      the program point that where this operation is being
	 *                    evaluated
	 * 
	 * @return the evaluation of the push-any expression
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	public InferredPair<T> evalPushAny(PushAny pushAny, T state, ProgramPoint pp) throws SemanticException {
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
	public InferredPair<T> evalNullConstant(T state, ProgramPoint pp) throws SemanticException {
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
	public InferredPair<T> evalNonNullConstant(Constant constant, T state, ProgramPoint pp)
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
	public InferredPair<T> evalUnaryExpression(UnaryOperator operator, T arg, T state, ProgramPoint pp)
			throws SemanticException {
		T top = top();
		return new InferredPair<>(top, top, top);
	}

	/**
	 * Yields the evaluation of a {@link BinaryExpression} applying
	 * {@code operator} to two expressions whose abstract value are {@code left}
	 * and {@code right}, respectively. It is guaranteed that both {@code left}
	 * and {@code right} are not {@link #bottom()} and that {@code operator} is
	 * neither {@link TypeCast} nor {@link TypeConv}.
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
	public InferredPair<T> evalBinaryExpression(BinaryOperator operator, T left, T right, T state, ProgramPoint pp)
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
	 * @param pp    the program point that where this operation is being
	 *                  evaluated
	 * 
	 * @return the evaluation of the type conversion expression
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	@SuppressWarnings("unchecked")
	public InferredPair<T> evalTypeConv(BinaryExpression conv, T left, T right, T state, ProgramPoint pp)
			throws SemanticException {
		T bot = bottom();
		return conv.getRuntimeTypes(pp.getProgram().getTypes()).isEmpty() ? new InferredPair<>(bot, bot, bot)
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
	 * @param pp    the program point that where this operation is being
	 *                  evaluated
	 * 
	 * @return the evaluation of the type cast expression
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	@SuppressWarnings("unchecked")
	public InferredPair<T> evalTypeCast(BinaryExpression cast, T left, T right, T state, ProgramPoint pp)
			throws SemanticException {
		T bot = bottom();
		return cast.getRuntimeTypes(pp.getProgram().getTypes()).isEmpty() ? new InferredPair<>(bot, bot, bot)
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
	public InferredPair<T> evalTernaryExpression(TernaryOperator operator, T left, T middle, T right, T state,
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
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	public Satisfiability satisfiesAbstractValue(T value, T state, ProgramPoint pp) throws SemanticException {
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
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	public Satisfiability satisfiesPushAny(PushAny pushAny, T state) throws SemanticException {
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
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	public Satisfiability satisfiesNullConstant(T state, ProgramPoint pp) throws SemanticException {
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
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	public Satisfiability satisfiesNonNullConstant(Constant constant, T state, ProgramPoint pp)
			throws SemanticException {
		return Satisfiability.UNKNOWN;
	}

	/**
	 * Yields the satisfiability of a {@link UnaryExpression} applying
	 * {@code operator} to an expression whose abstract value is {@code arg},
	 * returning an instance of {@link Satisfiability}. It is guaranteed that
	 * {@code operator} is not {@link LogicalNegation} and {@code arg} is not
	 * {@link #bottom()}.
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
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	public Satisfiability satisfiesUnaryExpression(UnaryOperator operator, T arg, T state, ProgramPoint pp)
			throws SemanticException {
		return Satisfiability.UNKNOWN;
	}

	/**
	 * Yields the satisfiability of a {@link BinaryExpression} applying
	 * {@code operator} to two expressions whose abstract values are
	 * {@code left}, and {@code right}. This method returns an instance of
	 * {@link Satisfiability}. It is guaranteed that {@code operator} is neither
	 * {@link LogicalAnd} nor {@link LogicalOr}, and that both {@code left} and
	 * {@code right} are not {@link #bottom()}.
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
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	public Satisfiability satisfiesBinaryExpression(BinaryOperator operator, T left, T right, T state,
			ProgramPoint pp) throws SemanticException {
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
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	public Satisfiability satisfiesTernaryExpression(TernaryOperator operator, T left, T middle, T right, T state,
			ProgramPoint pp) throws SemanticException {
		return Satisfiability.UNKNOWN;
	}

	@Override
	public final String toString() {
		return representation().toString();
	}

	@Override
	public boolean tracksIdentifiers(Identifier id) {
		// As default, base inferred values tracks only non-pointer identifier
		return canProcess(id);
	}

	@Override
	public boolean canProcess(SymbolicExpression expression) {
		if (expression.hasRuntimeTypes())
			return expression.getRuntimeTypes(null).stream().anyMatch(t -> !t.isPointerType() && !t.isInMemoryType());
		return !expression.getStaticType().isPointerType() && !expression.getStaticType().isInMemoryType();
	}

	@Override
	public InferenceSystem<T> assume(InferenceSystem<T> environment, ValueExpression expression, ProgramPoint pp)
			throws SemanticException {
		return environment;
	}
}
