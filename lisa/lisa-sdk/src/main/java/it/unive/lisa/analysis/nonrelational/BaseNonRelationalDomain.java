package it.unive.lisa.analysis.nonrelational;

import java.util.Set;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.value.NonRelationalValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.ExpressionVisitor;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.heap.HeapExpression;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.heap.MemoryAllocation;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.NullConstant;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.PushInv;
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
import it.unive.lisa.type.Type;

/**
 * Base implementation for {@link NonRelationalValueDomain}s. This class extends
 * {@link BaseLattice} and implements
 * {@link NonRelationalValueDomain#eval(SymbolicExpression, Environment, ProgramPoint, SemanticOracle)}
 * by taking care of the recursive computation of inner expressions evaluation.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <T> the concrete type of this domain
 */
public interface BaseNonRelationalDomain<T extends BaseNonRelationalDomain<T, E, F>,
		E extends SymbolicExpression,
		F extends Environment<F, E, T>>
		extends
		BaseLattice<T>,
		NonRelationalDomain<T, E, F>,
		ExpressionVisitor<T> {

	static final String CANNOT_PROCESS_ERROR = "Cannot process a heap expression with a non-relational value domain";

	@Override
	default T eval(
			E expression,
			F environment,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return expression.accept(this, environment, pp, oracle);
	}

	@Override
	default T visit(
			HeapExpression expression,
			T[] subExpressions,
			Object... params)
			throws SemanticException {
		throw new SemanticException(CANNOT_PROCESS_ERROR);
	}

	@Override
	default T visit(
			AccessChild expression,
			T receiver,
			T child,
			Object... params)
			throws SemanticException {
		throw new SemanticException(CANNOT_PROCESS_ERROR);
	}

	@Override
	default T visit(
			MemoryAllocation expression,
			Object... params)
			throws SemanticException {
		throw new SemanticException(CANNOT_PROCESS_ERROR);
	}

	@Override
	default T visit(
			HeapReference expression,
			T arg,
			Object... params)
			throws SemanticException {
		throw new SemanticException(CANNOT_PROCESS_ERROR);
	}

	@Override
	default T visit(
			HeapDereference expression,
			T arg,
			Object... params)
			throws SemanticException {
		throw new SemanticException(CANNOT_PROCESS_ERROR);
	}

	@Override
	default T visit(
			UnaryExpression expression,
			T arg,
			Object... params)
			throws SemanticException {
		if (arg.isBottom())
			return arg;

		ProgramPoint pp = (ProgramPoint) params[1];
		SemanticOracle oracle = (SemanticOracle) params[2];
		return evalUnaryExpression(expression.getOperator(), arg, pp, oracle);
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
	 * @param oracle   the oracle for inter-domain communication
	 * 
	 * @return the evaluation of the expression
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	default T evalUnaryExpression(
			UnaryOperator operator,
			T arg,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return top();
	}

	@Override
	default T visit(
			BinaryExpression expression,
			T left,
			T right,
			Object... params)
			throws SemanticException {
		if (left.isBottom())
			return left;
		if (right.isBottom())
			return right;

		ProgramPoint pp = (ProgramPoint) params[1];
		SemanticOracle oracle = (SemanticOracle) params[2];

		if (expression.getOperator() == TypeCast.INSTANCE)
			return evalTypeCast(expression, left, right, pp, oracle);

		if (expression.getOperator() == TypeConv.INSTANCE)
			return evalTypeConv(expression, left, right, pp, oracle);

		return evalBinaryExpression(expression.getOperator(), left, right, pp, oracle);
	}

	/**
	 * Yields the evaluation of a type cast expression.
	 * 
	 * @param cast   the type casted expression
	 * @param left   the left expression, namely the expression to be casted
	 * @param right  the right expression, namely the types to which left should
	 *                   be casted
	 * @param pp     the program point that where this operation is being
	 *                   evaluated
	 * @param oracle the oracle for inter-domain communication
	 * 
	 * @return the evaluation of the type cast expression
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	default T evalTypeCast(
			BinaryExpression cast,
			T left,
			T right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return oracle.getRuntimeTypesOf(cast, pp, oracle).isEmpty() ? bottom() : left;
	}

	/**
	 * Yields the evaluation of a type conversion expression.
	 * 
	 * @param conv   the type conversion expression
	 * @param left   the left expression, namely the expression to be converted
	 * @param right  the right expression, namely the types to which left should
	 *                   be converted
	 * @param pp     the program point that where this operation is being
	 *                   evaluated
	 * @param oracle the oracle for inter-domain communication
	 * 
	 * @return the evaluation of the type conversion expression
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	default T evalTypeConv(
			BinaryExpression conv,
			T left,
			T right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return oracle.getRuntimeTypesOf(conv, pp, oracle).isEmpty() ? bottom() : left;
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
	 * @param oracle   the oracle for inter-domain communication
	 * 
	 * @return the evaluation of the expression
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	default T evalBinaryExpression(
			BinaryOperator operator,
			T left,
			T right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return top();
	}

	@Override
	default T visit(
			TernaryExpression expression,
			T left,
			T middle,
			T right,
			Object... params)
			throws SemanticException {
		if (left.isBottom())
			return left;
		if (middle.isBottom())
			return middle;
		if (right.isBottom())
			return right;

		ProgramPoint pp = (ProgramPoint) params[1];
		SemanticOracle oracle = (SemanticOracle) params[2];
		return evalTernaryExpression(expression.getOperator(), left, middle, right, pp, oracle);
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
	 * @param oracle   the oracle for inter-domain communication
	 * 
	 * @return the evaluation of the expression
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	default T evalTernaryExpression(
			TernaryOperator operator,
			T left,
			T middle,
			T right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return top();
	}

	@Override
	default T visit(
			Skip expression,
			Object... params)
			throws SemanticException {
		ProgramPoint pp = (ProgramPoint) params[1];
		SemanticOracle oracle = (SemanticOracle) params[2];
		return evalSkip(expression, pp, oracle);
	}

	/**
	 * Yields the evaluation of a skip expression.
	 * 
	 * @param skip   the skip expression to be evaluated
	 * @param pp     the program point that where this operation is being
	 *                   evaluated
	 * @param oracle the oracle for inter-domain communication
	 * 
	 * @return the evaluation of the skip expression
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	default T evalSkip(
			Skip skip,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return bottom();
	}

	@Override
	default T visit(
			PushAny expression,
			Object... params)
			throws SemanticException {
		ProgramPoint pp = (ProgramPoint) params[1];
		SemanticOracle oracle = (SemanticOracle) params[2];
		return evalPushAny(expression, pp, oracle);
	}

	/**
	 * Yields the evaluation of a push-any expression.
	 * 
	 * @param pushAny the push-any expression to be evaluated
	 * @param pp      the program point that where this operation is being
	 *                    evaluated
	 * @param oracle  the oracle for inter-domain communication
	 * 
	 * @return the evaluation of the push-any expression
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	default T evalPushAny(
			PushAny pushAny,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return top();
	}

	@Override
	default T visit(
			PushInv expression,
			Object... params)
			throws SemanticException {
		ProgramPoint pp = (ProgramPoint) params[1];
		SemanticOracle oracle = (SemanticOracle) params[2];
		return evalPushInv(expression, pp, oracle);
	}

	/**
	 * Yields the evaluation of a push-inv expression.
	 * 
	 * @param pushInv the push-inv expression to be evaluated
	 * @param pp      the program point that where this operation is being
	 *                    evaluated
	 * @param oracle  the oracle for inter-domain communication
	 * 
	 * @return the evaluation of the push-inv expression
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	default T evalPushInv(
			PushInv pushInv,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return bottom();
	}

	@Override
	default T visit(
			Constant expression,
			Object... params)
			throws SemanticException {
		ProgramPoint pp = (ProgramPoint) params[1];
		SemanticOracle oracle = (SemanticOracle) params[2];
		if (expression instanceof NullConstant)
			return evalNullConstant(pp, oracle);
		return evalNonNullConstant(expression, pp, oracle);
	}

	/**
	 * Yields the evaluation of the null constant {@link NullConstant}.
	 * 
	 * @param pp     the program point that where this operation is being
	 *                   evaluated
	 * @param oracle the oracle for inter-domain communication
	 * 
	 * @return the evaluation of the constant
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	default T evalNullConstant(
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return top();
	}

	/**
	 * Yields the evaluation of the given non-null constant.
	 * 
	 * @param constant the constant to evaluate
	 * @param pp       the program point that where this operation is being
	 *                     evaluated
	 * @param oracle   the oracle for inter-domain communication
	 * 
	 * @return the evaluation of the constant
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	default T evalNonNullConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return top();
	}

	@Override
	default T visit(
			Identifier expression,
			Object... params)
			throws SemanticException {
		@SuppressWarnings("unchecked")
		F environment = (F) params[0];
		ProgramPoint pp = (ProgramPoint) params[1];
		SemanticOracle oracle = (SemanticOracle) params[2];
		return evalIdentifier(expression, environment, pp, oracle);
	}

	/**
	 * Yields the evaluation of an identifier in a given environment.
	 * 
	 * @param id          the identifier to be evaluated
	 * @param environment the environment where the identifier must be evaluated
	 * @param pp          the program point that where this operation is being
	 *                        evaluated
	 * @param oracle      the oracle for inter-domain communication
	 * 
	 * @return the evaluation of the identifier
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	default T evalIdentifier(
			Identifier id,
			F environment,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return environment.getState(id);
	}

	@Override
	default T visit(
			ValueExpression expression,
			T[] subExpressions,
			Object... params)
			throws SemanticException {
		ProgramPoint pp = (ProgramPoint) params[1];
		SemanticOracle oracle = (SemanticOracle) params[2];

		if (subExpressions != null)
			for (T sub : subExpressions)
				if (sub.isBottom())
					return sub;

		return evalValueExpression(expression, subExpressions, pp, oracle);
	}

	/**
	 * Yields the evaluation of a generic {@link ValueExpression}, where the
	 * recursive evaluation of its sub-expressions, if any, has already happened
	 * and is passed in the {@code subExpressions} parameters. It is guaranteed
	 * that no element of {@code subExpressions} is {@link #bottom()}.<br>
	 * <br>
	 * This method allows evaluating frontend-defined expressions. For all
	 * standard expressions defined within LiSA, the corresponding evaluation
	 * method will be invoked instead.
	 * 
	 * @param expression     the expression to evaluate
	 * @param subExpressions the instances of this domain representing the
	 *                           abstract values of all its sub-expressions, if
	 *                           any; if there are no sub-expressions, this
	 *                           parameter can be {@code null} or empty
	 * @param pp             the program point that where this operation is
	 *                           being evaluated
	 * @param oracle         the oracle for inter-domain communication
	 * 
	 * @return the evaluation of the expression
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	default T evalValueExpression(
			ValueExpression expression,
			T[] subExpressions,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return top();
	}

	@Override
	default boolean canProcess(
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (expression instanceof PushInv)
			// the type approximation of a pushinv is bottom, so the below check
			// will always fail regardless of the kind of value we are tracking
			return expression.getStaticType().isValueType();

		Set<Type> rts = null;
		try {
			rts = oracle.getRuntimeTypesOf(expression, pp, oracle);
		} catch (SemanticException e) {
			return false;
		}

		if (rts == null || rts.isEmpty())
			// if we have no runtime types, either the type domain has no type
			// information for the given expression (thus it can be anything,
			// also something that we can track) or the computation returned
			// bottom (and the whole state is likely going to go to bottom
			// anyway).
			return true;

		return rts.stream().anyMatch(Type::isValueType);
	}

	@Override
	default Satisfiability satisfies(
			E expression,
			F environment,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (expression instanceof Identifier)
			return satisfiesAbstractValue(environment.getState((Identifier) expression), pp, oracle);

		if (expression instanceof NullConstant)
			return satisfiesNullConstant(pp, oracle);

		if (expression instanceof Constant)
			return satisfiesNonNullConstant((Constant) expression, pp, oracle);

		if (expression instanceof UnaryExpression) {
			UnaryExpression unary = (UnaryExpression) expression;

			@SuppressWarnings("unchecked")
			E e = (E) unary.getExpression();
			if (unary.getOperator() == LogicalNegation.INSTANCE)
				return satisfies(e, environment, pp, oracle).negate();
			else {
				T arg = eval(e, environment, pp, oracle);
				if (arg.isBottom())
					return Satisfiability.BOTTOM;

				return satisfiesUnaryExpression(unary.getOperator(), arg, pp, oracle);
			}
		}

		if (expression instanceof BinaryExpression) {
			BinaryExpression binary = (BinaryExpression) expression;

			@SuppressWarnings("unchecked")
			E eleft = (E) binary.getLeft();
			@SuppressWarnings("unchecked")
			E eright = (E) binary.getRight();

			if (binary.getOperator() == LogicalAnd.INSTANCE)
				return satisfies(eleft, environment, pp, oracle).and(satisfies(eright, environment, pp, oracle));
			else if (binary.getOperator() == LogicalOr.INSTANCE)
				return satisfies(eleft, environment, pp, oracle).or(satisfies(eright, environment, pp, oracle));
			else {
				T left = eval(eleft, environment, pp, oracle);
				if (left.isBottom())
					return Satisfiability.BOTTOM;

				T right = eval(eright, environment, pp, oracle);
				if (right.isBottom())
					return Satisfiability.BOTTOM;

				return satisfiesBinaryExpression(binary.getOperator(), left, right, pp, oracle);
			}
		}

		if (expression instanceof TernaryExpression) {
			TernaryExpression ternary = (TernaryExpression) expression;

			@SuppressWarnings("unchecked")
			E eleft = (E) ternary.getLeft();
			T left = eval(eleft, environment, pp, oracle);
			if (left.isBottom())
				return Satisfiability.BOTTOM;

			@SuppressWarnings("unchecked")
			E emiddle = (E) ternary.getMiddle();
			T middle = eval(emiddle, environment, pp, oracle);
			if (middle.isBottom())
				return Satisfiability.BOTTOM;

			@SuppressWarnings("unchecked")
			E eright = (E) ternary.getRight();
			T right = eval(eright, environment, pp, oracle);
			if (right.isBottom())
				return Satisfiability.BOTTOM;

			return satisfiesTernaryExpression(ternary.getOperator(), left, middle, right, pp, oracle);
		}

		return Satisfiability.UNKNOWN;
	}

	/**
	 * Yields the satisfiability of an abstract value of type {@code <T>}.
	 * 
	 * @param value  the abstract value whose satisfiability is to be evaluated
	 * @param pp     the program point that where this operation is being
	 *                   evaluated
	 * @param oracle the oracle for inter-domain communication
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
	default Satisfiability satisfiesAbstractValue(
			T value,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return Satisfiability.UNKNOWN;
	}

	/**
	 * Yields the satisfiability of the null constant {@link NullConstant} on
	 * this abstract domain.
	 * 
	 * @param pp     the program point that where this operation is being
	 *                   evaluated
	 * @param oracle the oracle for inter-domain communication
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
	default Satisfiability satisfiesNullConstant(
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return Satisfiability.UNKNOWN;
	}

	/**
	 * Yields the satisfiability of the given non-null constant on this abstract
	 * domain.
	 * 
	 * @param constant the constant to satisfied
	 * @param pp       the program point that where this operation is being
	 *                     evaluated
	 * @param oracle   the oracle for inter-domain communication
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
	default Satisfiability satisfiesNonNullConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle)
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
	 * @param pp       the program point that where this operation is being
	 *                     evaluated
	 * @param oracle   the oracle for inter-domain communication
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
	default Satisfiability satisfiesUnaryExpression(
			UnaryOperator operator,
			T arg,
			ProgramPoint pp,
			SemanticOracle oracle)
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
	 * @param oracle   the oracle for inter-domain communication
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
	default Satisfiability satisfiesBinaryExpression(
			BinaryOperator operator,
			T left,
			T right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
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
	 * @param oracle   the oracle for inter-domain communication
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
	default Satisfiability satisfiesTernaryExpression(
			TernaryOperator operator,
			T left,
			T middle,
			T right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return Satisfiability.UNKNOWN;
	}

	@Override
	default F assume(
			F environment,
			E expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		if (expression instanceof UnaryExpression) {
			UnaryExpression unary = (UnaryExpression) expression;

			UnaryOperator op = unary.getOperator();
			if (op == LogicalNegation.INSTANCE) {
				@SuppressWarnings("unchecked")
				E rewritten = (E) unary.removeNegations();
				// It is possible that the expression cannot be rewritten (e.g.,
				// !true) hence we recursively call assume iff something changed
				if (rewritten != unary)
					return assume(environment, rewritten, src, dest, oracle);
			}

			@SuppressWarnings("unchecked")
			E earg = (E) unary.getExpression();
			return assumeUnaryExpression(environment, op, earg, src, dest, oracle);
		}

		if (expression instanceof BinaryExpression) {
			BinaryExpression binary = (BinaryExpression) expression;

			@SuppressWarnings("unchecked")
			E eleft = (E) binary.getLeft();
			@SuppressWarnings("unchecked")
			E eright = (E) binary.getRight();
			BinaryOperator op = binary.getOperator();
			if (op == LogicalAnd.INSTANCE)
				return assume(environment, eleft, src, dest, oracle)
						.glb(assume(environment, eright, src, dest, oracle));
			else if (op == LogicalOr.INSTANCE)
				return assume(environment, eleft, src, dest, oracle)
						.lub(assume(environment, eright, src, dest, oracle));
			else
				return assumeBinaryExpression(environment, op, eleft, eright, src, dest, oracle);
		}

		if (expression instanceof TernaryExpression) {
			TernaryExpression ternary = (TernaryExpression) expression;

			@SuppressWarnings("unchecked")
			E eleft = (E) ternary.getLeft();
			@SuppressWarnings("unchecked")
			E emiddle = (E) ternary.getMiddle();
			@SuppressWarnings("unchecked")
			E eright = (E) ternary.getRight();
			TernaryOperator op = ternary.getOperator();
			return assumeTernaryExpression(environment, op, eleft, emiddle, eright, src, dest, oracle);
		}

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
	 * @param oracle      the oracle for inter-domain communication
	 * 
	 * @return the environment {@code environment} assuming that an unary
	 *             expression with operator {@code operator} and argument
	 *             {@code expression} holds.
	 * 
	 * @throws SemanticException if something goes wrong during the assumption
	 */
	default F assumeUnaryExpression(
			F environment,
			UnaryOperator operator,
			E expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		return environment;
	}

	/**
	 * Yields the environment {@code environment} assuming that a binary
	 * expression with operator {@code operator}, left argument {@code left},
	 * and right argument {@code right} holds. The binary expression with binary
	 * operator {@link LogicalAnd} and {@link LogicalOr} are already handled by
	 * {@link BaseNonRelationalDomain#assume}.
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
	 * @param oracle      the oracle for inter-domain communication
	 * 
	 * @return the environment {@code environment} assuming that a binary
	 *             expression with operator {@code operator}, left argument
	 *             {@code left}, and right argument {@code right} holds
	 * 
	 * @throws SemanticException if something goes wrong during the assumption
	 */
	default F assumeBinaryExpression(
			F environment,
			BinaryOperator operator,
			E left,
			E right,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
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
	 * @param oracle      the oracle for inter-domain communication
	 * 
	 * @return the environment {@code environment} assuming that a ternary
	 *             expression with operator {@code operator}, left argument
	 *             {@code left}, middle argument {@code middle},and right
	 *             argument {@code right} holds
	 * 
	 * @throws SemanticException if something goes wrong during the assumption
	 */
	default F assumeTernaryExpression(
			F environment,
			TernaryOperator operator,
			E left,
			E middle,
			E right,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		return environment;
	}
}
