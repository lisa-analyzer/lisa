package it.unive.lisa.analysis.nonrelational.value;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.SemanticDomain.Satisfiability;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.Environment;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.ExpressionVisitor;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.heap.MemoryAllocation;
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
 * Base implementation for {@link NonRelationalValueDomain}s. This class extends
 * {@link BaseLattice} and implements
 * {@link NonRelationalValueDomain#eval(SymbolicExpression, Environment, ProgramPoint)}
 * by taking care of the recursive computation of inner expressions evaluation.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <T> the concrete type of this domain
 */
public interface BaseNonRelationalValueDomain<T extends BaseNonRelationalValueDomain<T>>
		extends BaseLattice<T>, NonRelationalValueDomain<T> {

	/**
	 * A {@link ExpressionVisitor} for {@link BaseNonRelationalValueDomain}
	 * instances.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 * 
	 * @param <T> the concrete type of the domain using this visitor
	 */
	@SuppressWarnings("unchecked")
	public class EvaluationVisitor<T extends BaseNonRelationalValueDomain<T>> implements ExpressionVisitor<T> {

		private static final String CANNOT_PROCESS_ERROR = "Cannot process a heap expression with a non-relational value domain";

		private final T singleton;

		/**
		 * Builds the visitor.
		 * 
		 * @param singleton an instance of the domain using this visitor
		 */
		public EvaluationVisitor(T singleton) {
			this.singleton = singleton;
		}

		@Override
		public T visit(AccessChild expression, T receiver, T child, Object... params) throws SemanticException {
			throw new SemanticException(CANNOT_PROCESS_ERROR);
		}

		@Override
		public T visit(MemoryAllocation expression, Object... params) throws SemanticException {
			throw new SemanticException(CANNOT_PROCESS_ERROR);
		}

		@Override
		public T visit(HeapReference expression, T arg, Object... params) throws SemanticException {
			throw new SemanticException(CANNOT_PROCESS_ERROR);
		}

		@Override
		public T visit(HeapDereference expression, T arg, Object... params) throws SemanticException {
			throw new SemanticException(CANNOT_PROCESS_ERROR);
		}

		@Override
		public T visit(UnaryExpression expression, T arg, Object... params) throws SemanticException {
			if (arg.isBottom())
				return arg;

			return singleton.evalUnaryExpression(expression.getOperator(), arg, (ProgramPoint) params[1]);
		}

		@Override
		public T visit(BinaryExpression expression, T left, T right, Object... params) throws SemanticException {
			if (left.isBottom())
				return left;
			if (right.isBottom())
				return right;

			if (expression.getOperator() == TypeCast.INSTANCE)
				return singleton.evalTypeCast(expression, left, right, (ProgramPoint) params[1]);

			if (expression.getOperator() == TypeConv.INSTANCE)
				return singleton.evalTypeConv(expression, left, right, (ProgramPoint) params[1]);

			return singleton.evalBinaryExpression(expression.getOperator(), left, right, (ProgramPoint) params[1]);
		}

		@Override
		public T visit(TernaryExpression expression, T left, T middle, T right, Object... params)
				throws SemanticException {
			if (left.isBottom())
				return left;
			if (middle.isBottom())
				return middle;
			if (right.isBottom())
				return right;

			return singleton.evalTernaryExpression(expression.getOperator(), left, middle, right,
					(ProgramPoint) params[1]);
		}

		@Override
		public T visit(Skip expression, Object... params) throws SemanticException {
			return singleton.evalSkip(expression, (ProgramPoint) params[1]);
		}

		@Override
		public T visit(PushAny expression, Object... params) throws SemanticException {
			return singleton.evalPushAny(expression, (ProgramPoint) params[1]);
		}

		@Override
		public T visit(Constant expression, Object... params) throws SemanticException {
			if (expression instanceof NullConstant)
				return singleton.evalNullConstant((ProgramPoint) params[1]);
			return singleton.evalNonNullConstant(expression, (ProgramPoint) params[1]);
		}

		@Override
		public T visit(Identifier expression, Object... params) throws SemanticException {
			return singleton.evalIdentifier(expression, (ValueEnvironment<T>) params[0], (ProgramPoint) params[1]);
		}
	}

	@Override
	default Satisfiability satisfies(ValueExpression expression, ValueEnvironment<T> environment,
			ProgramPoint pp) throws SemanticException {
		if (expression instanceof Identifier)
			return satisfiesAbstractValue(environment.getState((Identifier) expression), pp);

		if (expression instanceof NullConstant)
			return satisfiesNullConstant(pp);

		if (expression instanceof Constant)
			return satisfiesNonNullConstant((Constant) expression, pp);

		if (expression instanceof UnaryExpression) {
			UnaryExpression unary = (UnaryExpression) expression;

			if (unary.getOperator() == LogicalNegation.INSTANCE)
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

			if (binary.getOperator() == LogicalAnd.INSTANCE)
				return satisfies((ValueExpression) binary.getLeft(), environment, pp)
						.and(satisfies((ValueExpression) binary.getRight(), environment, pp));
			else if (binary.getOperator() == LogicalOr.INSTANCE)
				return satisfies((ValueExpression) binary.getLeft(), environment, pp)
						.or(satisfies((ValueExpression) binary.getRight(), environment, pp));
			else {
				T left = eval((ValueExpression) binary.getLeft(), environment, pp);
				if (left.isBottom())
					return Satisfiability.BOTTOM;

				T right = eval((ValueExpression) binary.getRight(), environment, pp);
				if (right.isBottom())
					return Satisfiability.BOTTOM;

				return satisfiesBinaryExpression(binary.getOperator(), left, right, pp);
			}
		}

		if (expression instanceof TernaryExpression) {
			TernaryExpression ternary = (TernaryExpression) expression;

			T left = eval((ValueExpression) ternary.getLeft(), environment, pp);
			if (left.isBottom())
				return Satisfiability.BOTTOM;

			T middle = eval((ValueExpression) ternary.getMiddle(), environment, pp);
			if (middle.isBottom())
				return Satisfiability.BOTTOM;

			T right = eval((ValueExpression) ternary.getRight(), environment, pp);
			if (right.isBottom())
				return Satisfiability.BOTTOM;

			return satisfiesTernaryExpression(ternary.getOperator(), left, middle, right, pp);
		}

		return Satisfiability.UNKNOWN;
	}

	@Override
	@SuppressWarnings("unchecked")
	default T eval(ValueExpression expression, ValueEnvironment<T> environment, ProgramPoint pp)
			throws SemanticException {
		return expression.accept(new EvaluationVisitor<>((T) this), environment, pp, this);
	}

	@Override
	default boolean tracksIdentifiers(Identifier id) {
		// As default, base non relational values domains
		// tracks only non-pointer identifier
		return canProcess(id);
	}

	@Override
	default boolean canProcess(SymbolicExpression expression) {
		if (expression.hasRuntimeTypes())
			return expression.getRuntimeTypes(null).stream().anyMatch(t -> !t.isPointerType() && !t.isInMemoryType());
		return !expression.getStaticType().isPointerType() && !expression.getStaticType().isInMemoryType();
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
	default T evalIdentifier(Identifier id, ValueEnvironment<T> environment, ProgramPoint pp)
			throws SemanticException {
		return environment.getState(id);
	}

	/**
	 * Yields the evaluation of a skip expression.
	 * 
	 * @param skip the skip expression to be evaluated
	 * @param pp   the program point that where this operation is being
	 *                 evaluated
	 * 
	 * @return the evaluation of the skip expression
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	default T evalSkip(Skip skip, ProgramPoint pp) throws SemanticException {
		return bottom();
	}

	/**
	 * Yields the evaluation of a push-any expression.
	 * 
	 * @param pushAny the push-any expression to be evaluated
	 * @param pp      the program point that where this operation is being
	 *                    evaluated
	 * 
	 * @return the evaluation of the push-any expression
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	default T evalPushAny(PushAny pushAny, ProgramPoint pp) throws SemanticException {
		return top();
	}

	/**
	 * Yields the evaluation of a type conversion expression.
	 * 
	 * @param conv  the type conversion expression
	 * @param left  the left expression, namely the expression to be converted
	 * @param right the right expression, namely the types to which left should
	 *                  be converted
	 * @param pp    the program point that where this operation is being
	 *                  evaluated
	 * 
	 * @return the evaluation of the type conversion expression
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	default T evalTypeConv(BinaryExpression conv, T left, T right, ProgramPoint pp) throws SemanticException {
		return conv.getRuntimeTypes(pp.getProgram().getTypes()).isEmpty() ? bottom() : left;
	}

	/**
	 * Yields the evaluation of a type cast expression.
	 * 
	 * @param cast  the type casted expression
	 * @param left  the left expression, namely the expression to be casted
	 * @param right the right expression, namely the types to which left should
	 *                  be casted
	 * @param pp    the program point that where this operation is being
	 *                  evaluated
	 * 
	 * @return the evaluation of the type cast expression
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	default T evalTypeCast(BinaryExpression cast, T left, T right, ProgramPoint pp) throws SemanticException {
		return cast.getRuntimeTypes(pp.getProgram().getTypes()).isEmpty() ? bottom() : left;
	}

	/**
	 * Yields the evaluation of the null constant {@link NullConstant}.
	 * 
	 * @param pp the program point that where this operation is being evaluated
	 * 
	 * @return the evaluation of the constant
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	default T evalNullConstant(ProgramPoint pp) throws SemanticException {
		return top();
	}

	/**
	 * Yields the evaluation of the given non-null constant.
	 * 
	 * @param constant the constant to evaluate
	 * @param pp       the program point that where this operation is being
	 *                     evaluated
	 * 
	 * @return the evaluation of the constant
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	default T evalNonNullConstant(Constant constant, ProgramPoint pp) throws SemanticException {
		return top();
	}

	/**
	 * Yields the evaluation of a {@link UnaryExpression} applying
	 * {@code operator} to an expression whose abstract value is {@code arg}. It
	 * is guaranteed that {@code arg} is not {@link #bottom()}.
	 * 
	 * @param operator the operator applied by the expression
	 * @param arg      the instance of this domain representing the abstract
	 *                     value of the expresion's argument
	 * @param pp       the program point that where this operation is being
	 *                     evaluated
	 * 
	 * @return the evaluation of the expression
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	default T evalUnaryExpression(UnaryOperator operator, T arg, ProgramPoint pp) throws SemanticException {
		return top();
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
	 * @param pp       the program point that where this operation is being
	 *                     evaluated
	 * 
	 * @return the evaluation of the expression
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	default T evalBinaryExpression(BinaryOperator operator, T left, T right, ProgramPoint pp)
			throws SemanticException {
		return top();
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
	 * @param pp       the program point that where this operation is being
	 *                     evaluated
	 * 
	 * @return the evaluation of the expression
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	default T evalTernaryExpression(TernaryOperator operator, T left, T middle, T right, ProgramPoint pp)
			throws SemanticException {
		return top();
	}

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
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	default Satisfiability satisfiesAbstractValue(T value, ProgramPoint pp) throws SemanticException {
		return Satisfiability.UNKNOWN;
	}

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
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	default Satisfiability satisfiesNullConstant(ProgramPoint pp) throws SemanticException {
		return Satisfiability.UNKNOWN;
	}

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
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	default Satisfiability satisfiesNonNullConstant(Constant constant, ProgramPoint pp) throws SemanticException {
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
	default Satisfiability satisfiesUnaryExpression(UnaryOperator operator, T arg, ProgramPoint pp)
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
	default Satisfiability satisfiesBinaryExpression(BinaryOperator operator, T left, T right,
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
	default Satisfiability satisfiesTernaryExpression(TernaryOperator operator, T left, T middle, T right,
			ProgramPoint pp) throws SemanticException {
		return Satisfiability.UNKNOWN;
	}

	@Override
	default ValueEnvironment<T> assume(ValueEnvironment<T> environment, ValueExpression expression,
			ProgramPoint src, ProgramPoint dest) throws SemanticException {
		Satisfiability sat = satisfies(expression, environment, src);
		if (sat == Satisfiability.NOT_SATISFIED)
			return environment.bottom();
		if (sat == Satisfiability.SATISFIED)
			return environment;

		if (expression instanceof UnaryExpression) {
			UnaryExpression unary = (UnaryExpression) expression;

			if (unary.getOperator() == LogicalNegation.INSTANCE) {
				ValueExpression rewritten = unary.removeNegations();
				// It is possible that the expression cannot be rewritten (e.g.,
				// !true) hence we recursively call assume iff something changed
				if (rewritten != unary)
					return assume(environment, rewritten, src, dest);
			}

			return assumeUnaryExpression(environment, unary.getOperator(), (ValueExpression) unary.getExpression(), src,
					dest);
		}

		if (expression instanceof BinaryExpression) {
			BinaryExpression binary = (BinaryExpression) expression;

			if (binary.getOperator() == LogicalAnd.INSTANCE)
				return assume(environment, (ValueExpression) binary.getLeft(), src, dest)
						.glb(assume(environment, (ValueExpression) binary.getRight(), src, dest));
			else if (binary.getOperator() == LogicalOr.INSTANCE)
				return assume(environment, (ValueExpression) binary.getLeft(), src, dest)
						.lub(assume(environment, (ValueExpression) binary.getRight(), src, dest));
			else
				return assumeBinaryExpression(environment, binary.getOperator(), (ValueExpression) binary.getLeft(),
						(ValueExpression) binary.getRight(), src, dest);
		}

		if (expression instanceof TernaryExpression) {
			TernaryExpression ternary = (TernaryExpression) expression;

			return assumeTernaryExpression(environment, ternary.getOperator(), (ValueExpression) ternary.getLeft(),
					(ValueExpression) ternary.getMiddle(), (ValueExpression) ternary.getRight(), src, dest);
		}

		return environment;
	}

	/**
	 * Yields the environment {@code environment} assuming that a ternary
	 * expression with operator {@code operator}, left argument {@code left},
	 * middle argument {@code middle},and right argument {@code right} holds.
	 * 
	 * @param environment the environment on which the expression must be
	 *                        assumed
	 * @param operator    the operator of the ternary expression
	 * @param left        the left-hand side argument of the ternary expression
	 * @param middle      the middle-hand side argument of the ternary
	 *                        expression
	 * @param right       the right-hand side argument of the ternary expression
	 * @param src         the program point that where this operation is being
	 *                        evaluated, corresponding to the one that generated
	 *                        the given expression
	 * @param dest        the program point where the execution will move after
	 *                        the expression has been assumed
	 * 
	 * @return the environment {@code environment} assuming that a ternary
	 *             expression with operator {@code operator}, left argument
	 *             {@code left}, middle argument {@code middle},and right
	 *             argument {@code right} holds
	 * 
	 * @throws SemanticException if something goes wrong during the assumption
	 */
	default ValueEnvironment<T> assumeTernaryExpression(ValueEnvironment<T> environment,
			TernaryOperator operator, ValueExpression left, ValueExpression middle, ValueExpression right,
			ProgramPoint src, ProgramPoint dest) throws SemanticException {
		return environment;
	}

	/**
	 * Yields the environment {@code environment} assuming that a binary
	 * expression with operator {@code operator}, left argument {@code left},
	 * and right argument {@code right} holds. The binary expression with binary
	 * operator {@link LogicalAnd} and {@link LogicalOr} are already handled by
	 * {@link BaseNonRelationalValueDomain#assume}.
	 * 
	 * @param environment the environment on which the expression must be
	 *                        assumed
	 * @param operator    the operator of the binary expression
	 * @param left        the left-hand side argument of the binary expression
	 * @param right       the right-hand side argument of the binary expression
	 * @param src         the program point that where this operation is being
	 *                        evaluated, corresponding to the one that generated
	 *                        the given expression
	 * @param dest        the program point where the execution will move after
	 *                        the expression has been assumed
	 * 
	 * @return the environment {@code environment} assuming that a binary
	 *             expression with operator {@code operator}, left argument
	 *             {@code left}, and right argument {@code right} holds
	 * 
	 * @throws SemanticException if something goes wrong during the assumption
	 */
	default ValueEnvironment<T> assumeBinaryExpression(ValueEnvironment<T> environment,
			BinaryOperator operator, ValueExpression left, ValueExpression right, ProgramPoint src,
			ProgramPoint dest) throws SemanticException {
		return environment;
	}

	/**
	 * Yields the environment {@code environment} assuming that an unary
	 * expression with operator {@code operator} and argument {@code expression}
	 * holds.
	 * 
	 * @param environment the environment on which the expression must be
	 *                        assumed
	 * @param operator    the operator of the unary expression
	 * @param expression  the argument of the unary expression
	 * @param src         the program point that where this operation is being
	 *                        evaluated, corresponding to the one that generated
	 *                        the given expression
	 * @param dest        the program point where the execution will move after
	 *                        the expression has been assumed
	 * 
	 * @return the environment {@code environment} assuming that an unary
	 *             expression with operator {@code operator} and argument
	 *             {@code expression} holds.
	 * 
	 * @throws SemanticException if something goes wrong during the assumption
	 */
	default ValueEnvironment<T> assumeUnaryExpression(ValueEnvironment<T> environment,
			UnaryOperator operator, ValueExpression expression, ProgramPoint src, ProgramPoint dest)
			throws SemanticException {
		return environment;
	}
}
