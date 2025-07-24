package it.unive.lisa.analysis.numeric;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.combination.smash.SmashedSumIntDomain;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.lattices.numeric.IntegerConstant;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.PushFromConstraints;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.DivisionOperator;
import it.unive.lisa.symbolic.value.operator.ModuloOperator;
import it.unive.lisa.symbolic.value.operator.MultiplicationOperator;
import it.unive.lisa.symbolic.value.operator.RemainderOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumberConversionException;

/**
 * The overflow-insensitive basic integer constant propagation analysis,
 * tracking if a certain integer value has constant value or not, implemented as
 * a {@link BaseNonRelationalValueDomain}. The lattice structure used by this
 * domain is {@link IntegerConstant}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class IntegerConstantPropagation
		implements
		SmashedSumIntDomain<IntegerConstant> {

	@Override
	public IntegerConstant evalNullConstant(
			ProgramPoint pp,
			SemanticOracle oracle) {
		return IntegerConstant.TOP;
	}

	@Override
	public IntegerConstant evalNonNullConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (constant.getValue() instanceof Integer)
			return new IntegerConstant((Integer) constant.getValue());
		return IntegerConstant.TOP;
	}

	@Override
	public IntegerConstant evalPushAny(
			PushAny pushAny,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (pushAny instanceof PushFromConstraints)
			return new IntegerConstant().generate(((PushFromConstraints) pushAny).getConstraints(), pp);
		return SmashedSumIntDomain.super.evalPushAny(pushAny, pp, oracle);
	}

	@Override
	public IntegerConstant evalUnaryExpression(
			UnaryExpression expression,
			IntegerConstant arg,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (arg.isTop())
			return IntegerConstant.TOP;

		if (expression.getOperator() == NumericNegation.INSTANCE)
			return new IntegerConstant(-arg.value);

		return IntegerConstant.TOP;
	}

	@Override
	public IntegerConstant evalBinaryExpression(
			BinaryExpression expression,
			IntegerConstant left,
			IntegerConstant right,
			ProgramPoint pp,
			SemanticOracle oracle) {
		BinaryOperator operator = expression.getOperator();
		if (operator instanceof AdditionOperator)
			return left.isTop() || right.isTop() ? IntegerConstant.TOP : new IntegerConstant(left.value + right.value);
		else if (operator instanceof DivisionOperator)
			if (!left.isTop() && left.value == 0)
				return new IntegerConstant(0);
			else if (!right.isTop() && right.value == 0)
				return IntegerConstant.BOTTOM;
			else if (left.isTop() || right.isTop() || left.value % right.value != 0)
				return IntegerConstant.TOP;
			else
				return new IntegerConstant(left.value / right.value);
		else if (operator instanceof ModuloOperator)
			// this is different from the semantics of java
			return left.isTop() || right.isTop()
					? IntegerConstant.TOP
					: new IntegerConstant(
							right.value < 0 ? -Math.abs(left.value % right.value)
									: -Math.abs(left.value % right.value));
		else if (operator instanceof RemainderOperator)
			// this matches the semantics of java
			return left.isTop() || right.isTop() ? IntegerConstant.TOP : new IntegerConstant(left.value % right.value);
		else if (operator instanceof MultiplicationOperator)
			return left.isTop() || right.isTop() ? IntegerConstant.TOP : new IntegerConstant(left.value * right.value);
		else if (operator instanceof SubtractionOperator)
			return left.isTop() || right.isTop() ? IntegerConstant.TOP : new IntegerConstant(left.value - right.value);
		else
			return IntegerConstant.TOP;
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(
			BinaryExpression expression,
			IntegerConstant left,
			IntegerConstant right,
			ProgramPoint pp,
			SemanticOracle oracle) {

		if (left.isTop() || right.isTop())
			return Satisfiability.UNKNOWN;

		BinaryOperator operator = expression.getOperator();
		if (operator == ComparisonEq.INSTANCE)
			return left.value.intValue() == right.value.intValue()
					? Satisfiability.SATISFIED
					: Satisfiability.NOT_SATISFIED;
		else if (operator == ComparisonGe.INSTANCE)
			return left.value >= right.value ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
		else if (operator == ComparisonGt.INSTANCE)
			return left.value > right.value ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
		else if (operator == ComparisonLe.INSTANCE)
			return left.value <= right.value ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
		else if (operator == ComparisonLt.INSTANCE)
			return left.value < right.value ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
		else if (operator == ComparisonNe.INSTANCE)
			return left.value.intValue() != right.value.intValue()
					? Satisfiability.SATISFIED
					: Satisfiability.NOT_SATISFIED;
		else
			return Satisfiability.UNKNOWN;
	}

	@Override
	public ValueEnvironment<IntegerConstant> assumeBinaryExpression(
			ValueEnvironment<IntegerConstant> environment,
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

		BinaryOperator operator = expression.getOperator();
		ValueExpression left = (ValueExpression) expression.getLeft();
		ValueExpression right = (ValueExpression) expression.getRight();
		if (operator == ComparisonEq.INSTANCE)
			if (left instanceof Identifier) {
				IntegerConstant eval = eval(environment, right, src, oracle);
				if (eval.isBottom())
					return environment.bottom();
				return environment.putState((Identifier) left, eval);
			} else if (right instanceof Identifier) {
				IntegerConstant eval = eval(environment, left, src, oracle);
				if (eval.isBottom())
					return environment.bottom();
				return environment.putState((Identifier) right, eval);
			}
		return environment;
	}

	@Override
	public IntegerConstant fromInterval(
			IntInterval intv)
			throws SemanticException {
		if (intv.isFinite() && intv.getHigh().equals(intv.getLow()))
			try {
				return new IntegerConstant(intv.getLow().toInt());
			} catch (MathNumberConversionException e) {
				throw new SemanticException(
						"Cannot convert " + intv + " to an integer constant",
						e);
			}
		return IntegerConstant.TOP;
	}

	@Override
	public IntInterval toInterval(
			IntegerConstant con)
			throws SemanticException {
		if (con.isBottom())
			return null;
		if (con.isTop())
			return IntInterval.INFINITY;
		return new IntInterval(con.value, con.value);
	}

	@Override
	public IntegerConstant top() {
		return IntegerConstant.TOP;
	}

	@Override
	public IntegerConstant bottom() {
		return IntegerConstant.BOTTOM;
	}

}
