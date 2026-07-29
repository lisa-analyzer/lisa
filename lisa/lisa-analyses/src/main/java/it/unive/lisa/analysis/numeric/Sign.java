package it.unive.lisa.analysis.numeric;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.combination.smash.SmashedSumIntDomain;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.value.NumericAbstraction;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.lattices.numeric.SignLattice;
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
import it.unive.lisa.symbolic.value.operator.binary.LogicalAnd;
import it.unive.lisa.symbolic.value.operator.binary.LogicalOr;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.symbolic.value.operator.unary.NumericToString;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import java.util.Collections;
import java.util.Set;

/**
 * The basic overflow-insensitive Sign abstract domain, tracking zero, strictly
 * positive and strictly negative integer values, implemented as a
 * {@link BaseNonRelationalValueDomain}.
 * 
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
public class Sign
		implements
		NumericAbstraction<ValueEnvironment<SignLattice>>,
		SmashedSumIntDomain<SignLattice> {

	@Override
	public SignLattice evalConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (constant.getValue() instanceof Byte) {
			Byte i = (Byte) constant.getValue();
			return i == 0 ? SignLattice.ZERO : i > 0 ? SignLattice.POS : SignLattice.NEG;
		}
		if (constant.getValue() instanceof Short) {
			Short i = (Short) constant.getValue();
			return i == 0 ? SignLattice.ZERO : i > 0 ? SignLattice.POS : SignLattice.NEG;
		}
		if (constant.getValue() instanceof Integer) {
			Integer i = (Integer) constant.getValue();
			return i == 0 ? SignLattice.ZERO : i > 0 ? SignLattice.POS : SignLattice.NEG;
		}
		if (constant.getValue() instanceof Long) {
			Long i = (Long) constant.getValue();
			return i == 0 ? SignLattice.ZERO : i > 0 ? SignLattice.POS : SignLattice.NEG;
		}
		if (constant.getValue() instanceof Float) {
			Float i = (Float) constant.getValue();
			return i == 0 ? SignLattice.ZERO : i > 0 ? SignLattice.POS : SignLattice.NEG;
		}
		if (constant.getValue() instanceof Double) {
			Double i = (Double) constant.getValue();
			return i == 0 ? SignLattice.ZERO : i > 0 ? SignLattice.POS : SignLattice.NEG;
		}

		return SignLattice.TOP;
	}

	@Override
	public SignLattice evalPushAny(
			PushAny pushAny,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (pushAny instanceof PushFromConstraints)
			return generate(((PushFromConstraints) pushAny).getConstraints(), pp, oracle);
		return SmashedSumIntDomain.super.evalPushAny(pushAny, pp, oracle);
	}

	@Override
	public SignLattice evalUnaryExpression(
			UnaryExpression expression,
			SignLattice arg,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (expression.getOperator() == NumericNegation.INSTANCE)
			if (arg.isPositive())
				return SignLattice.NEG;
			else if (arg.isNegative())
				return SignLattice.POS;
			else if (arg.isZero())
				return SignLattice.ZERO;
			else
				return SignLattice.TOP;
		return SignLattice.TOP;
	}

	@Override
	public SignLattice evalBinaryExpression(
			BinaryExpression expression,
			SignLattice left,
			SignLattice right,
			ProgramPoint pp,
			SemanticOracle oracle) {
		BinaryOperator operator = expression.getOperator();
		if (operator instanceof AdditionOperator)
			if (left.isZero())
				return right;
			else if (right.isZero())
				return left;
			else if (left.equals(right))
				return left;
			else
				return top();
		else if (operator instanceof SubtractionOperator)
			if (left.isZero())
				return right.opposite();
			else if (right.isZero())
				return left;
			else if (left.equals(right))
				return top();
			else
				return left;
		else if (operator instanceof DivisionOperator)
			if (right.isZero())
				return bottom();
			else if (left.isZero())
				return SignLattice.ZERO;
			else if (left.equals(right))
				// top/top = top
				// +/+ = +
				// -/- = +
				return left.isTop() ? left : SignLattice.POS;
			else if (!left.isTop() && left.equals(right.opposite()))
				// +/- = -
				// -/+ = -
				return SignLattice.NEG;
			else
				return top();
		else if (operator instanceof ModuloOperator)
			return right;
		else if (operator instanceof RemainderOperator)
			return left;
		else if (operator instanceof MultiplicationOperator)
			if (left.isZero() || right.isZero())
				return SignLattice.ZERO;
			else if (left.isTop() || right.isTop())
				return top();
			else if (left.equals(right))
				return SignLattice.POS;
			else
				return SignLattice.NEG;
		else
			return SignLattice.TOP;
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(
			BinaryExpression expression,
			SignLattice left,
			SignLattice right,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (left.isTop() || right.isTop())
			return Satisfiability.UNKNOWN;

		BinaryOperator operator = expression.getOperator();
		if (operator == ComparisonEq.INSTANCE)
			return left.eq(right);
		else if (operator == ComparisonGe.INSTANCE)
			return left.eq(right).or(left.gt(right));
		else if (operator == ComparisonGt.INSTANCE)
			return left.gt(right);
		else if (operator == ComparisonLe.INSTANCE)
			// e1 <= e2 same as !(e1 > e2)
			return left.gt(right).negate();
		else if (operator == ComparisonLt.INSTANCE)
			// e1 < e2 -> !(e1 >= e2) && !(e1 == e2)
			return left.gt(right).negate().and(left.eq(right).negate());
		else if (operator == ComparisonNe.INSTANCE)
			return left.eq(right).negate();
		else
			return Satisfiability.UNKNOWN;
	}

	@Override
	public ValueEnvironment<SignLattice> assumeBinaryExpression(
			ValueEnvironment<SignLattice> environment,
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
		SignLattice eval;
		boolean rightIsExpr;
		BinaryOperator operator = expression.getOperator();
		ValueExpression left = (ValueExpression) expression.getLeft();
		ValueExpression right = (ValueExpression) expression.getRight();
		if (left instanceof Identifier) {
			if (!canProcess(right, src, oracle))
				// the expression does not have a numerical value, we do not
				// assume anything on it
				return environment;
			eval = eval(environment, right, src, oracle);
			id = (Identifier) left;
			rightIsExpr = true;
		} else if (right instanceof Identifier) {
			if (!canProcess(left, src, oracle))
				// the expression does not have a numerical value, we do not
				// assume anything on it
				return environment;
			eval = eval(environment, left, src, oracle);
			id = (Identifier) right;
			rightIsExpr = false;
		} else
			return environment;

		SignLattice starting = environment.getState(id);
		if (eval.isBottom() || starting.isBottom())
			return environment.bottom();
		if (eval.isTop())
			// we do not know anything about the expression, so we cannot assume
			// anything on the identifier
			return environment;

		SignLattice update = null;
		if (operator == ComparisonEq.INSTANCE)
			update = starting.glb(eval);
		else {
			// the rule for an operator op is:
			// - if `start op eval`, `update = U { start n v | v op eval, v in {
			// +, 0, -} }`
			// - if `eval op start`, `update = U { start n v | eval op v, v in {
			// +, 0, -} }`

			SignLattice[] all = new SignLattice[] { SignLattice.NEG, SignLattice.ZERO, SignLattice.POS };
			if (operator == ComparisonGe.INSTANCE)
				if (rightIsExpr) {
					for (SignLattice s : all)
						if (s.gt(eval).or(s.eq(eval)).mightBeTrue())
							update = update == null ? starting.glb(s) : update.lub(starting.glb(s));
				} else {
					for (SignLattice s : all)
						if (eval.gt(s).or(eval.eq(s)).mightBeTrue())
							update = update == null ? starting.glb(s) : update.lub(starting.glb(s));
				}
			else if (operator == ComparisonLe.INSTANCE)
				if (rightIsExpr) {
					for (SignLattice s : all)
						// we invert <= to > and look at the failing ones
						if (s.gt(eval).mightBeFalse())
							update = update == null ? starting.glb(s) : update.lub(starting.glb(s));
				} else {
					for (SignLattice s : all)
						// we invert <= to > and look at the failing ones
						if (eval.gt(s).mightBeFalse())
							update = update == null ? starting.glb(s) : update.lub(starting.glb(s));
				}
			else if (operator == ComparisonLt.INSTANCE)
				if (rightIsExpr) {
					for (SignLattice s : all)
						// we invert < to >= and look at the failing ones
						if (s.gt(eval).or(s.eq(eval)).mightBeFalse())
							update = update == null ? starting.glb(s) : update.lub(starting.glb(s));
				} else {
					for (SignLattice s : all)
						// we invert < to >= and look at the failing ones
						if (eval.gt(s).or(eval.eq(s)).mightBeFalse())
							update = update == null ? starting.glb(s) : update.lub(starting.glb(s));
				}
			else if (operator == ComparisonGt.INSTANCE)
				if (rightIsExpr) {
					for (SignLattice s : all)
						if (s.gt(eval).mightBeTrue())
							update = update == null ? starting.glb(s) : update.lub(starting.glb(s));
				} else {
					for (SignLattice s : all)
						if (eval.gt(s).mightBeTrue())
							update = update == null ? starting.glb(s) : update.lub(starting.glb(s));
				}
		}

		if (update == null)
			return environment;
		else if (update.isBottom())
			return environment.bottom();
		else
			return environment.putState(id, update);
	}

	@Override
	public SignLattice fromInterval(
			IntInterval intv)
			throws SemanticException {
		if (intv.is(0))
			return SignLattice.ZERO;
		if (!intv.lowIsMinusInfinity() && intv.getLow().compareTo(MathNumber.ZERO) > 0)
			return SignLattice.POS;
		if (!intv.highIsPlusInfinity() && intv.getHigh().compareTo(MathNumber.ZERO) < 0)
			return SignLattice.NEG;
		return SignLattice.TOP;
	}

	@Override
	public IntInterval toInterval(
			SignLattice sign)
			throws SemanticException {
		if (sign.isBottom())
			return null;
		if (sign.isTop())
			return IntInterval.INFINITY;
		if (sign.isZero())
			return IntInterval.ZERO;
		// in the cases below we use 0 even if it should not be included since
		// eg a positive sign can represent 0.1, but the interval has integer
		// bounds
		if (sign.isPositive())
			return new IntInterval(0, null);
		return new IntInterval(null, 0);
	}

	@Override
	public SignLattice top() {
		return SignLattice.TOP;
	}

	@Override
	public SignLattice bottom() {
		return SignLattice.BOTTOM;
	}

	@Override
	public Set<BinaryExpression> constraints(
			ValueDomain<?> requesting,
			ValueEnvironment<SignLattice> state,
			ValueExpression e,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (state.isTop())
			return Collections.emptySet();
		if (state.isBottom())
			return null;

		if ((e instanceof UnaryExpression && ((UnaryExpression) e).getOperator() == LogicalNegation.INSTANCE)
				|| (e instanceof BinaryExpression && ((BinaryExpression) e).getOperator() == LogicalAnd.INSTANCE)
				|| (e instanceof BinaryExpression && ((BinaryExpression) e).getOperator() == LogicalOr.INSTANCE)) {
			Satisfiability sat = satisfies(state, e, pp, oracle);
			if (sat == Satisfiability.SATISFIED)
				return ValueDomain.makeEqConstraint(pp.getProgram().getTypes().getBooleanType(), true, e, pp);
			else if (sat == Satisfiability.NOT_SATISFIED)
				return ValueDomain.makeEqConstraint(pp.getProgram().getTypes().getBooleanType(), false, e, pp);
			else if (sat == Satisfiability.UNKNOWN)
				return Collections.emptySet();
			else
				return null;
		}

		if (e instanceof UnaryExpression) {
			UnaryOperator operator = ((UnaryExpression) e).getOperator();
			if (operator == NumericToString.INSTANCE) {
				ValueExpression arg = (ValueExpression) ((UnaryExpression) e).getExpression();
				SignLattice value = eval(state, arg, pp, oracle);
				if (value.isTop())
					return Collections.emptySet();
				if (value.isBottom())
					return null;
				if (value.isZero())
					return ValueDomain.makeEqConstraint(
							pp.getProgram().getTypes().getStringType(),
							"0",
							e,
							pp);
			}
		}

		if (e instanceof BinaryExpression) {
			BinaryOperator operator = ((BinaryExpression) e).getOperator();
			if (operator == ComparisonEq.INSTANCE
					|| operator == ComparisonNe.INSTANCE
					|| operator == ComparisonLe.INSTANCE
					|| operator == ComparisonLt.INSTANCE
					|| operator == ComparisonGe.INSTANCE
					|| operator == ComparisonGt.INSTANCE) {
				Satisfiability sat = satisfies(state, e, pp, oracle);
				if (sat == Satisfiability.SATISFIED)
					return ValueDomain.makeEqConstraint(pp.getProgram().getTypes().getBooleanType(), true, e, pp);
				else if (sat == Satisfiability.NOT_SATISFIED)
					return ValueDomain.makeEqConstraint(pp.getProgram().getTypes().getBooleanType(), false, e, pp);
				else if (sat == Satisfiability.UNKNOWN)
					return Collections.emptySet();
				else
					return null;
			}
		}

		SignLattice value = eval(state, e, pp, oracle);
		if (value.isBottom())
			return null;
		if (value.isTop())
			return Collections.emptySet();
		if (value == SignLattice.ZERO)
			return ValueDomain.makeEqConstraint(
					pp.getProgram().getTypes().getIntegerType(),
					0,
					e,
					pp);
		else if (value == SignLattice.POS)
			return ValueDomain.makeRangeConstraints(pp.getProgram().getTypes().getIntegerType(), 0, null, e, pp);
		else
			return ValueDomain.makeRangeConstraints(pp.getProgram().getTypes().getIntegerType(), null, 0, e, pp);
	}

	private SignLattice generate(
			Set<BinaryExpression> constraints,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (constraints == null)
			return SignLattice.BOTTOM;

		Integer ge = null, le = null;
		for (BinaryExpression expr : constraints)
			if (expr.getLeft() instanceof Constant && ((Constant) expr.getLeft()).getValue() instanceof Integer) {
				Integer val = (Integer) ((Constant) expr.getLeft()).getValue();
				if (expr.getOperator() instanceof ComparisonEq)
					return val == 0 ? SignLattice.ZERO : val > 0 ? SignLattice.POS : SignLattice.NEG;
				else if (expr.getOperator() instanceof ComparisonGe)
					ge = val;
				else if (expr.getOperator() instanceof ComparisonLe)
					le = val;
			}

		if (ge != null && ge.equals(le))
			return ge == 0 ? SignLattice.ZERO : ge > 0 ? SignLattice.POS : SignLattice.NEG;
		else if (ge != null)
			if (ge < 0)
				return SignLattice.NEG;
			else if (le != null && le > 0)
				return SignLattice.POS;
			else
				return SignLattice.TOP;
		else if (le != null)
			if (le > 0)
				return SignLattice.POS;

		return SignLattice.TOP;
	}

}
