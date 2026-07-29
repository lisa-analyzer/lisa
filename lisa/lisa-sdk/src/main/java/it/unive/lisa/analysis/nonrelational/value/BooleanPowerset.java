package it.unive.lisa.analysis.nonrelational.value;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.value.BooleanAbstraction;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.PushFromConstraints;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
import it.unive.lisa.symbolic.value.operator.binary.LogicalAnd;
import it.unive.lisa.symbolic.value.operator.binary.LogicalOr;
import it.unive.lisa.symbolic.value.operator.binary.StringContains;
import it.unive.lisa.symbolic.value.operator.binary.StringEndsWith;
import it.unive.lisa.symbolic.value.operator.binary.StringEquals;
import it.unive.lisa.symbolic.value.operator.binary.StringEqualsIgnoreCase;
import it.unive.lisa.symbolic.value.operator.binary.StringIsPrefixOf;
import it.unive.lisa.symbolic.value.operator.binary.StringIsSuffixOf;
import it.unive.lisa.symbolic.value.operator.binary.StringMatches;
import it.unive.lisa.symbolic.value.operator.binary.StringStartsWith;
import it.unive.lisa.symbolic.value.operator.binary.TypeCheck;
import it.unive.lisa.symbolic.value.operator.ternary.StringStartsWithFromIndex;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.type.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;

/**
 * A {@link NonRelationalValueDomain} that tracks sets of boolean values in the
 * environments it produces. Sets are are represented as {@link Satisfiability}
 * values.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class BooleanPowerset
		implements
		BooleanAbstraction<ValueEnvironment<Satisfiability>>,
		BaseNonRelationalValueDomain<Satisfiability> {

	@Override
	public Satisfiability evalPushAny(
			PushAny pushAny,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (pushAny instanceof PushFromConstraints)
			return generate(((PushFromConstraints) pushAny).getConstraints(), pp, oracle);
		return BaseNonRelationalValueDomain.super.evalPushAny(pushAny, pp, oracle);
	}

	@Override
	public Satisfiability evalConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (constant.getValue() instanceof Boolean)
			return Satisfiability.fromBoolean((Boolean) constant.getValue());
		return Satisfiability.UNKNOWN;
	}

	@Override
	public Satisfiability evalUnaryExpression(
			UnaryExpression expression,
			Satisfiability arg,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (expression.getOperator() == LogicalNegation.INSTANCE)
			return arg.negate();
		return Satisfiability.UNKNOWN;
	}

	@Override
	public Satisfiability evalBinaryExpression(
			BinaryExpression expression,
			Satisfiability left,
			Satisfiability right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		BinaryOperator operator = expression.getOperator();

		if (oracle.hasWholeValueAnlysis() &&
				(operator == ComparisonLe.INSTANCE
						|| operator == ComparisonLt.INSTANCE
						|| operator == ComparisonGe.INSTANCE
						|| operator == ComparisonGt.INSTANCE
						|| operator == StringContains.INSTANCE
						|| operator == StringEndsWith.INSTANCE
						|| operator == StringEquals.INSTANCE
						|| operator == StringEqualsIgnoreCase.INSTANCE
						|| operator == StringMatches.INSTANCE
						|| operator == StringStartsWith.INSTANCE
						|| operator == StringIsPrefixOf.INSTANCE
						|| operator == StringIsSuffixOf.INSTANCE)) {
			Set<BinaryExpression> constraints = oracle.constraints(this, expression, pp);
			return generate(constraints, pp, oracle);
		}

		if (operator == TypeCheck.INSTANCE) {
			Set<Type> types = oracle.getRuntimeTypesOf(expression.getLeft(), pp);
			Set<Type> target = oracle.getRuntimeTypesOf(expression.getRight(), pp);
			if (target.equals(types) || target.containsAll(types))
				// all expression types are allowed
				return Satisfiability.SATISFIED;
			Collection<Type> intersection = CollectionUtils.intersection(types, target);
			if (!intersection.isEmpty())
				// some expression types are allowed
				return Satisfiability.UNKNOWN;
			return Satisfiability.NOT_SATISFIED;
		}

		if (operator == LogicalAnd.INSTANCE)
			return left.and(right);
		if (operator == LogicalOr.INSTANCE)
			return left.or(right);
		if (operator == ComparisonEq.INSTANCE)
			if (left == Satisfiability.UNKNOWN || right == Satisfiability.UNKNOWN)
				return Satisfiability.UNKNOWN;
			else
				return Satisfiability.fromBoolean(left.equals(right));
		if (operator == ComparisonNe.INSTANCE)
			if (left == Satisfiability.UNKNOWN || right == Satisfiability.UNKNOWN)
				return Satisfiability.UNKNOWN;
			else
				return Satisfiability.fromBoolean(!left.equals(right));
		return Satisfiability.UNKNOWN;
	}

	@Override
	public Satisfiability evalTernaryExpression(
			TernaryExpression expression,
			Satisfiability left,
			Satisfiability middle,
			Satisfiability right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (oracle.hasWholeValueAnlysis() && expression.getOperator() == StringStartsWithFromIndex.INSTANCE) {
			Set<BinaryExpression> constraints = oracle.constraints(this, expression, pp);
			return generate(constraints, pp, oracle);
		}
		return Satisfiability.UNKNOWN;
	}

	@Override
	public ValueEnvironment<Satisfiability> assumeConstant(
			ValueEnvironment<Satisfiability> environment,
			Constant expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		if (evalConstant(expression, src, oracle) == Satisfiability.NOT_SATISFIED)
			return environment.bottom();
		return environment;
	}

	@Override
	public ValueEnvironment<Satisfiability> assumeIdentifier(
			ValueEnvironment<Satisfiability> environment,
			Identifier expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		// we assume that the identifier holds, and thus its value becomes true
		return environment.putState(expression, Satisfiability.SATISFIED);
	}

	@Override
	public ValueEnvironment<Satisfiability> assumeUnaryExpression(
			ValueEnvironment<Satisfiability> environment,
			UnaryExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		Satisfiability sat = satisfies(environment, expression, src, oracle);
		if (sat == Satisfiability.NOT_SATISFIED)
			return environment.bottom();
		if (sat == Satisfiability.SATISFIED)
			return environment;

		if (expression.getOperator() == LogicalNegation.INSTANCE && expression.getExpression() instanceof Identifier) {
			Identifier id = (Identifier) expression.getExpression();
			Satisfiability eval = environment.getState(id);
			if (eval.isBottom())
				return environment.bottom();
			else if (eval == Satisfiability.NOT_SATISFIED)
				return environment;
			else
				return environment.putState(id, Satisfiability.NOT_SATISFIED);
		}

		return environment;
	}

	@Override
	public ValueEnvironment<Satisfiability> assumeBinaryExpression(
			ValueEnvironment<Satisfiability> environment,
			BinaryExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		Satisfiability sat = satisfies(environment, expression, src, oracle);
		if (sat == Satisfiability.NOT_SATISFIED)
			return environment.bottom();
		if (sat == Satisfiability.SATISFIED)
			return environment;

		Identifier id;
		Satisfiability eval;
		ValueExpression left = (ValueExpression) expression.getLeft();
		ValueExpression right = (ValueExpression) expression.getRight();
		if (left instanceof Identifier) {
			if (!canProcess(right, src, oracle))
				// the expression does not have a boolean value, we do not
				// assume anything on it
				return environment;
			eval = eval(environment, right, src, oracle);
			id = (Identifier) left;
		} else if (right instanceof Identifier) {
			if (!canProcess(left, src, oracle))
				// the expression does not have a boolean value, we do not
				// assume anything on it
				return environment;
			eval = eval(environment, left, src, oracle);
			id = (Identifier) right;
		} else
			return environment;

		Satisfiability starting = environment.getState(id);
		if (eval.isBottom() || starting.isBottom())
			return environment.bottom();

		Satisfiability update = null;
		if (expression.getOperator() == ComparisonEq.INSTANCE)
			update = eval;
		else if (expression.getOperator() == ComparisonNe.INSTANCE)
			update = eval.negate();

		if (update == null)
			return environment;
		else
			return environment.putState(id, update);
	}

	@Override
	public Satisfiability satisfiesAbstractValue(
			Satisfiability value,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return value;
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(
			BinaryExpression expression,
			Satisfiability left,
			Satisfiability right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return evalBinaryExpression(expression, left, right, pp, oracle);
	}

	@Override
	public Satisfiability satisfiesConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return evalConstant(constant, pp, oracle);
	}

	@Override
	public Satisfiability satisfiesUnaryExpression(
			UnaryExpression expression,
			Satisfiability arg,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return evalUnaryExpression(expression, arg, pp, oracle);
	}

	@Override
	public Satisfiability top() {
		return Satisfiability.UNKNOWN;
	}

	@Override
	public Satisfiability bottom() {
		return Satisfiability.BOTTOM;
	}

	@Override
	public Set<BinaryExpression> constraints(
			ValueDomain<?> requesting,
			ValueEnvironment<Satisfiability> state,
			ValueExpression e,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (state.isTop())
			return Collections.emptySet();
		if (state.isBottom())
			return null;

		Satisfiability value = eval(state, e, pp, oracle);
		if (value == Satisfiability.UNKNOWN)
			return Collections.emptySet();
		return ValueDomain.makeEqConstraint(
				pp.getProgram().getTypes().getBooleanType(),
				value == Satisfiability.SATISFIED ? true : false,
				e,
				pp);
	}

	private Satisfiability generate(
			Set<BinaryExpression> constraints,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (constraints == null)
			return Satisfiability.BOTTOM;

		for (BinaryExpression expr : constraints)
			if (expr.getOperator() instanceof ComparisonEq
					&& expr.getLeft() instanceof Constant
					&& ((Constant) expr.getLeft()).getValue() instanceof Boolean) {
				Boolean val = (Boolean) ((Constant) expr.getLeft()).getValue();
				if (val.booleanValue())
					return Satisfiability.SATISFIED;
				else
					return Satisfiability.NOT_SATISFIED;
			}

		return Satisfiability.UNKNOWN;
	}
}
