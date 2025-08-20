package it.unive.lisa.analysis.nonrelational.value;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.NullConstant;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
import it.unive.lisa.symbolic.value.operator.binary.LogicalAnd;
import it.unive.lisa.symbolic.value.operator.binary.LogicalOr;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;

/**
 * A {@link NonRelationalValueDomain} that tracks sets of boolean values in the
 * environments it produces. Sets are are represented as {@link Satisfiability}
 * values.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class BooleanPowerset
		implements
		BaseNonRelationalValueDomain<Satisfiability> {

	@Override
	public Satisfiability evalNonNullConstant(
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
	public ValueEnvironment<Satisfiability> assumeConstant(
			ValueEnvironment<Satisfiability> environment,
			Constant expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		if (expression instanceof NullConstant)
			return environment;
		if (evalNonNullConstant(expression, src, oracle) == Satisfiability.NOT_SATISFIED)
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
			eval = eval(environment, right, src, oracle);
			id = (Identifier) left;
		} else if (right instanceof Identifier) {
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
	public Satisfiability satisfiesNonNullConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return evalNonNullConstant(constant, pp, oracle);
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

}
