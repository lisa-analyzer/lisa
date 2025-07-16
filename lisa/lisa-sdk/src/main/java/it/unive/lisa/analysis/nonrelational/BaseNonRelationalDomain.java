package it.unive.lisa.analysis.nonrelational;

import it.unive.lisa.analysis.DomainLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.FunctionalLattice;
import it.unive.lisa.analysis.lattices.Satisfiability;
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
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.Type;
import java.util.Map;
import java.util.Set;

/**
 * Base implementation for {@link NonRelationalDomain}s that can evaluate
 * {@link ValueExpression}s, and whose transformers simply return functional
 * lattices. This class implements
 * {@link #eval(SymbolicExpression, Environment, ProgramPoint, SemanticOracle)}
 * by taking care of the recursive computation of inner expressions evaluation.
 * It also defines
 * {@link #assign(FunctionalLattice, Identifier, ValueExpression, ProgramPoint, SemanticOracle)},
 * {@link #smallStepSemantics(FunctionalLattice, ValueExpression, ProgramPoint, SemanticOracle)},
 * {@link #assume(FunctionalLattice, ValueExpression, ProgramPoint, SemanticOracle)}
 * and
 * {@link #satisfies(FunctionalLattice, ValueExpression, ProgramPoint, SemanticOracle)}
 * by exploiting eval. Callbacks for customizing the evaluation, assumption, and
 * satisfiability of specific expressions are also provided.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <L> the lattice structure that appears values in the functional
 *                lattices
 * @param <M> the type of environments to use with this domain
 */
public interface BaseNonRelationalDomain<L extends Lattice<L>,
		M extends FunctionalLattice<M, Identifier, L> & DomainLattice<M, M>>
		extends
		NonRelationalDomain<L, M, M, ValueExpression>,
		ExpressionVisitor<L> {

	@Override
	default M assign(
			M state,
			Identifier id,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (state.isBottom())
			return state;

		if (!canProcess(expression, pp, oracle))
			return state;

		Map<Identifier, L> func = state.mkNewFunction(state.function, false);
		L value = eval(state, expression, pp, oracle);
		L v = fixedVariable(id, pp, oracle);
		if (!v.isBottom())
			// some domains might provide fixed representations
			// for some variables
			value = v;
		else if (id.isWeak() && state.function != null && state.function.containsKey(id))
			// if we have a weak identifier for which we already have
			// information, we we perform a weak assignment
			value = value.lub(state.getState(id));
		func.put(id, value);
		return state.mk(state.lattice, func);
	}

	@Override
	default M smallStepSemantics(
			M state,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		// environments do not change without assignments
		return state;
	}

	@Override
	default L fixedVariable(
			Identifier id,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return bottom();
	}

	@Override
	default L unknownValue(
			Identifier id) {
		return top();
	}

	/**
	 * The error message thrown when a heap expression is encountered while
	 * traversing an expression.
	 */
	static final String CANNOT_PROCESS_ERROR = "Cannot process a heap expression with a non-relational value domain";

	@Override
	default L eval(
			M environment,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return expression.accept(this, environment, pp, oracle);
	}

	@Override
	default L visit(
			HeapExpression expression,
			L[] subExpressions,
			Object... params)
			throws SemanticException {
		throw new SemanticException(CANNOT_PROCESS_ERROR);
	}

	@Override
	default L visit(
			AccessChild expression,
			L receiver,
			L child,
			Object... params)
			throws SemanticException {
		throw new SemanticException(CANNOT_PROCESS_ERROR);
	}

	@Override
	default L visit(
			MemoryAllocation expression,
			Object... params)
			throws SemanticException {
		throw new SemanticException(CANNOT_PROCESS_ERROR);
	}

	@Override
	default L visit(
			HeapReference expression,
			L arg,
			Object... params)
			throws SemanticException {
		throw new SemanticException(CANNOT_PROCESS_ERROR);
	}

	@Override
	default L visit(
			HeapDereference expression,
			L arg,
			Object... params)
			throws SemanticException {
		throw new SemanticException(CANNOT_PROCESS_ERROR);
	}

	@Override
	default L visit(
			UnaryExpression expression,
			L arg,
			Object... params)
			throws SemanticException {
		if (arg.isBottom())
			return arg;

		ProgramPoint pp = (ProgramPoint) params[1];
		SemanticOracle oracle = (SemanticOracle) params[2];
		return evalUnaryExpression(expression, arg, pp, oracle);
	}

	/**
	 * Yields the evaluation of a {@link UnaryExpression} applying its operator
	 * to an expression whose abstract value is {@code arg}. It is guaranteed
	 * that {@code arg} is not {@link #bottom()}.
	 * 
	 * @param expression the expression to evaluate
	 * @param arg        the instance of this domain representing the abstract
	 *                       value of the expresion's argument
	 * @param pp         the program point that where this operation is being
	 *                       evaluated
	 * @param oracle     the oracle for inter-domain communication
	 * 
	 * @return the evaluation of the expression
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	default L evalUnaryExpression(
			UnaryExpression expression,
			L arg,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return arg.top();
	}

	@Override
	default L visit(
			BinaryExpression expression,
			L left,
			L right,
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

		return evalBinaryExpression(expression, left, right, pp, oracle);
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
	default L evalTypeCast(
			BinaryExpression cast,
			L left,
			L right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return oracle.getRuntimeTypesOf(cast, pp).isEmpty() ? left.bottom() : left;
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
	default L evalTypeConv(
			BinaryExpression conv,
			L left,
			L right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return oracle.getRuntimeTypesOf(conv, pp).isEmpty() ? left.bottom() : left;
	}

	/**
	 * Yields the evaluation of a {@link BinaryExpression} applying its operator
	 * to two expressions whose abstract values are {@code left} and
	 * {@code right}, respectively. It is guaranteed that both {@code left} and
	 * {@code right} are not {@link #bottom()} and that {@code operator} is
	 * neither {@link TypeCast} nor {@link TypeConv}.
	 * 
	 * @param expression the expression to evaluate
	 * @param left       the instance of this domain representing the abstract
	 *                       value of the left-hand side argument
	 * @param right      the instance of this domain representing the abstract
	 *                       value of the right-hand side argument
	 * @param pp         the program point that where this operation is being
	 *                       evaluated
	 * @param oracle     the oracle for inter-domain communication
	 * 
	 * @return the evaluation of the expression
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	default L evalBinaryExpression(
			BinaryExpression expression,
			L left,
			L right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return left.top();
	}

	@Override
	default L visit(
			TernaryExpression expression,
			L left,
			L middle,
			L right,
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
		return evalTernaryExpression(expression, left, middle, right, pp, oracle);
	}

	/**
	 * Yields the evaluation of a {@link TernaryExpression} applying its
	 * operator to three expressions whose abstract values are {@code left},
	 * {@code middle} and {@code right}, respectively. It is guaranteed that
	 * both {@code left} and {@code right} are not {@link #bottom()}.
	 * 
	 * @param expression the expression to evaluate
	 * @param left       the instance of this domain representing the abstract
	 *                       value of the left-hand side argument
	 * @param middle     the instance of this domain representing the abstract
	 *                       value of the middle argument
	 * @param right      the instance of this domain representing the abstract
	 *                       value of the right-hand side argument
	 * @param pp         the program point that where this operation is being
	 *                       evaluated
	 * @param oracle     the oracle for inter-domain communication
	 * 
	 * @return the evaluation of the expression
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	default L evalTernaryExpression(
			TernaryExpression expression,
			L left,
			L middle,
			L right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return left.top();
	}

	@Override
	default L visit(
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
	default L evalSkip(
			Skip skip,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return bottom();
	}

	@Override
	default L visit(
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
	default L evalPushAny(
			PushAny pushAny,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return top();
	}

	@Override
	default L visit(
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
	default L evalPushInv(
			PushInv pushInv,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return bottom();
	}

	@Override
	default L visit(
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
	default L evalNullConstant(
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
	default L evalNonNullConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return top();
	}

	@Override
	default L visit(
			Identifier expression,
			Object... params)
			throws SemanticException {
		@SuppressWarnings("unchecked")
		M environment = (M) params[0];
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
	default L evalIdentifier(
			Identifier id,
			M environment,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return environment.getState(id);
	}

	@Override
	default L visit(
			ValueExpression expression,
			L[] subExpressions,
			Object... params)
			throws SemanticException {
		ProgramPoint pp = (ProgramPoint) params[1];
		SemanticOracle oracle = (SemanticOracle) params[2];

		if (subExpressions != null)
			for (L sub : subExpressions)
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
	default L evalValueExpression(
			ValueExpression expression,
			L[] subExpressions,
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
			rts = oracle.getRuntimeTypesOf(expression, pp);
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
			M state,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (state.isBottom())
			return Satisfiability.BOTTOM;

		if (expression instanceof Identifier)
			return satisfiesAbstractValue(state.getState((Identifier) expression), pp, oracle);

		if (expression instanceof NullConstant)
			return satisfiesNullConstant(pp, oracle);

		if (expression instanceof Constant)
			return satisfiesNonNullConstant((Constant) expression, pp, oracle);

		if (expression instanceof UnaryExpression) {
			UnaryExpression unary = (UnaryExpression) expression;
			ValueExpression e = (ValueExpression) unary.getExpression();
			if (unary.getOperator() == LogicalNegation.INSTANCE)
				return satisfies(state, e, pp, oracle).negate();
			else {
				L arg = eval(state, e, pp, oracle);
				if (arg.isBottom())
					return Satisfiability.BOTTOM;

				return satisfiesUnaryExpression(unary, arg, pp, oracle);
			}
		}

		if (expression instanceof BinaryExpression) {
			BinaryExpression binary = (BinaryExpression) expression;
			ValueExpression eleft = (ValueExpression) binary.getLeft();
			ValueExpression eright = (ValueExpression) binary.getRight();

			if (binary.getOperator() == LogicalAnd.INSTANCE)
				return satisfies(state, eleft, pp, oracle).and(satisfies(state, eright, pp, oracle));
			else if (binary.getOperator() == LogicalOr.INSTANCE)
				return satisfies(state, eleft, pp, oracle).or(satisfies(state, eright, pp, oracle));
			else {
				L left = eval(state, eleft, pp, oracle);
				if (left.isBottom())
					return Satisfiability.BOTTOM;

				L right = eval(state, eright, pp, oracle);
				if (right.isBottom())
					return Satisfiability.BOTTOM;

				return satisfiesBinaryExpression(binary, left, right, pp, oracle);
			}
		}

		if (expression instanceof TernaryExpression) {
			TernaryExpression ternary = (TernaryExpression) expression;
			ValueExpression eleft = (ValueExpression) ternary.getLeft();
			L left = eval(state, eleft, pp, oracle);
			if (left.isBottom())
				return Satisfiability.BOTTOM;

			ValueExpression emiddle = (ValueExpression) ternary.getMiddle();
			L middle = eval(state, emiddle, pp, oracle);
			if (middle.isBottom())
				return Satisfiability.BOTTOM;

			ValueExpression eright = (ValueExpression) ternary.getRight();
			L right = eval(state, eright, pp, oracle);
			if (right.isBottom())
				return Satisfiability.BOTTOM;

			return satisfiesTernaryExpression(ternary, left, middle, right, pp, oracle);
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
			L value,
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
	 * Yields the satisfiability of a {@link UnaryExpression} applying its
	 * operator to an expression whose abstract value is {@code arg}, returning
	 * an instance of {@link Satisfiability}. It is guaranteed that
	 * {@code operator} is not {@link LogicalNegation} and {@code arg} is not
	 * {@link #bottom()}.
	 * 
	 * @param expression the expression whose satisfiability is to be assessed
	 * @param arg        an instance of this abstract domain representing the
	 *                       argument of the unary expression
	 * @param pp         the program point that where this operation is being
	 *                       evaluated
	 * @param oracle     the oracle for inter-domain communication
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
			UnaryExpression expression,
			L arg,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return Satisfiability.UNKNOWN;
	}

	/**
	 * Yields the satisfiability of a {@link BinaryExpression} applying its
	 * operator to two expressions whose abstract values are {@code left}, and
	 * {@code right}. This method returns an instance of {@link Satisfiability}.
	 * It is guaranteed that {@code operator} is neither {@link LogicalAnd} nor
	 * {@link LogicalOr}, and that both {@code left} and {@code right} are not
	 * {@link #bottom()}.
	 * 
	 * @param expression the expression whose satisfiability is to be assessed
	 * @param left       an instance of this abstract domain representing the
	 *                       argument of the left-hand side of the binary
	 *                       expression
	 * @param right      an instance of this abstract domain representing the
	 *                       argument of the right-hand side of the binary
	 *                       expression
	 * @param pp         the program point that where this operation is being
	 *                       evaluated
	 * @param oracle     the oracle for inter-domain communication
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
			BinaryExpression expression,
			L left,
			L right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return Satisfiability.UNKNOWN;
	}

	/**
	 * Yields the satisfiability of a {@link TernaryExpression} applying its
	 * operator to three expressions whose abstract values are {@code left},
	 * {@code middle} and {@code right}. This method returns an instance of
	 * {@link Satisfiability}. It is guaranteed that {@code left},
	 * {@code middle} and {@code right} are not {@link #bottom()}.
	 * 
	 * @param expression the expression whose satisfiability is to be assessed
	 * @param left       an instance of this abstract domain representing the
	 *                       argument of the left-most side of the ternary
	 *                       expression
	 * @param middle     an instance of this abstract domain representing the
	 *                       argument in the middle of the ternary expression
	 * @param right      an instance of this abstract domain representing the
	 *                       argument of the right-most side of the ternary
	 *                       expression
	 * @param pp         the program point that where this operation is being
	 *                       evaluated
	 * @param oracle     the oracle for inter-domain communication
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
			TernaryExpression expression,
			L left,
			L middle,
			L right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return Satisfiability.UNKNOWN;
	}

	@Override
	default M assume(
			M environment,
			ValueExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		if (environment.isBottom())
			return environment;

		if (expression instanceof Identifier)
			return assumeIdentifier(environment, (Identifier) expression, src, dest, oracle);

		if (expression instanceof Constant)
			return assumeConstant(environment, (Constant) expression, src, dest, oracle);

		if (expression instanceof UnaryExpression) {
			UnaryExpression unary = (UnaryExpression) expression;
			UnaryOperator op = unary.getOperator();
			if (op == LogicalNegation.INSTANCE) {
				ValueExpression rewritten = (ValueExpression) unary.removeNegations();
				// It is possible that the expression cannot be rewritten (e.g.,
				// !true) hence we recursively call assume iff something changed
				if (rewritten != unary)
					return assume(environment, rewritten, src, dest, oracle);
			}

			return assumeUnaryExpression(environment, unary, src, dest, oracle);
		}

		if (expression instanceof BinaryExpression) {
			BinaryExpression binary = (BinaryExpression) expression;
			ValueExpression eleft = (ValueExpression) binary.getLeft();
			ValueExpression eright = (ValueExpression) binary.getRight();
			BinaryOperator op = binary.getOperator();
			if (op == LogicalAnd.INSTANCE)
				return assume(environment, eleft, src, dest, oracle)
						.glb(assume(environment, eright, src, dest, oracle));
			else if (op == LogicalOr.INSTANCE)
				return assume(environment, eleft, src, dest, oracle)
						.lub(assume(environment, eright, src, dest, oracle));
			else
				return assumeBinaryExpression(environment, binary, src, dest, oracle);
		}

		if (expression instanceof TernaryExpression)
			return assumeTernaryExpression(environment, (TernaryExpression) expression, src, dest, oracle);

		if (expression instanceof ValueExpression)
			return assumeValueExpression(environment, (ValueExpression) expression, src, dest, oracle);

		// other simple expressions (e.g., skip, push-any) don't really need
		// their separate callbacks
		return environment;
	}

	/**
	 * Yields the environment {@code environment} assuming that a constant
	 * holds.
	 * 
	 * @param environment the environment on which the expression must be
	 *                        assumed
	 * @param expression  the expression to assume
	 * @param src         the program point that where this operation is being
	 *                        evaluated, corresponding to the one that generated
	 *                        the given expression
	 * @param dest        the program point where the execution will move after
	 *                        the expression has been assumed
	 * @param oracle      the oracle for inter-domain communication
	 * 
	 * @return the environment {@code environment} assuming that the given
	 *             expression holds
	 * 
	 * @throws SemanticException if something goes wrong during the assumption
	 */
	default M assumeConstant(
			M environment,
			Constant expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		return environment;
	}

	/**
	 * Yields the environment {@code environment} assuming that an identifier
	 * holds.
	 * 
	 * @param environment the environment on which the expression must be
	 *                        assumed
	 * @param expression  the expression to assume
	 * @param src         the program point that where this operation is being
	 *                        evaluated, corresponding to the one that generated
	 *                        the given expression
	 * @param dest        the program point where the execution will move after
	 *                        the expression has been assumed
	 * @param oracle      the oracle for inter-domain communication
	 * 
	 * @return the environment {@code environment} assuming that the given
	 *             expression holds
	 * 
	 * @throws SemanticException if something goes wrong during the assumption
	 */
	default M assumeIdentifier(
			M environment,
			Identifier expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		return environment;
	}

	/**
	 * Yields the environment {@code environment} assuming that an unary
	 * expression holds.
	 * 
	 * @param environment the environment on which the expression must be
	 *                        assumed
	 * @param expression  the expression to assume
	 * @param src         the program point that where this operation is being
	 *                        evaluated, corresponding to the one that generated
	 *                        the given expression
	 * @param dest        the program point where the execution will move after
	 *                        the expression has been assumed
	 * @param oracle      the oracle for inter-domain communication
	 * 
	 * @return the environment {@code environment} assuming that the given
	 *             expression holds
	 * 
	 * @throws SemanticException if something goes wrong during the assumption
	 */
	default M assumeUnaryExpression(
			M environment,
			UnaryExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		return environment;
	}

	/**
	 * Yields the environment {@code environment} assuming that a binary
	 * expression holds. The binary expression with binary operator
	 * {@link LogicalAnd} and {@link LogicalOr} are already handled by
	 * {@link BaseNonRelationalDomain#assume}.
	 * 
	 * @param environment the environment on which the expression must be
	 *                        assumed
	 * @param expression  the expression to assume
	 * @param src         the program point that where this operation is being
	 *                        evaluated, corresponding to the one that generated
	 *                        the given expression
	 * @param dest        the program point where the execution will move after
	 *                        the expression has been assumed
	 * @param oracle      the oracle for inter-domain communication
	 * 
	 * @return the environment {@code environment} assuming that the given
	 *             expression holds
	 * 
	 * @throws SemanticException if something goes wrong during the assumption
	 */
	default M assumeBinaryExpression(
			M environment,
			BinaryExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		return environment;
	}

	/**
	 * Yields the environment {@code environment} assuming that a ternary
	 * expression holds.
	 * 
	 * @param environment the environment on which the expression must be
	 *                        assumed
	 * @param expression  the expression to assume
	 * @param src         the program point that where this operation is being
	 *                        evaluated, corresponding to the one that generated
	 *                        the given expression
	 * @param dest        the program point where the execution will move after
	 *                        the expression has been assumed
	 * @param oracle      the oracle for inter-domain communication
	 * 
	 * @return the environment {@code environment} assuming that the given
	 *             expression holds
	 * 
	 * @throws SemanticException if something goes wrong during the assumption
	 */
	default M assumeTernaryExpression(
			M environment,
			TernaryExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		return environment;
	}

	/**
	 * Yields the environment {@code environment} assuming that a value
	 * expression holds.
	 * 
	 * @param environment the environment on which the expression must be
	 *                        assumed
	 * @param expression  the expression to assume
	 * @param src         the program point that where this operation is being
	 *                        evaluated, corresponding to the one that generated
	 *                        the given expression
	 * @param dest        the program point where the execution will move after
	 *                        the expression has been assumed
	 * @param oracle      the oracle for inter-domain communication
	 * 
	 * @return the environment {@code environment} assuming that the given
	 *             expression holds
	 * 
	 * @throws SemanticException if something goes wrong during the assumption
	 */
	default M assumeValueExpression(
			M environment,
			ValueExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		return environment;
	}

	/**
	 * Yields the top element of the environment's values.
	 * 
	 * @return the top element
	 */
	L top();

	/**
	 * Yields the bottom element of the environment's values.
	 * 
	 * @return the bottom element
	 */
	L bottom();

}
