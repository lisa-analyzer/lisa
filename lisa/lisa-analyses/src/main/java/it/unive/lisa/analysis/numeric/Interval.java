package it.unive.lisa.analysis.numeric;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.combination.smash.SmashedSumIntDomain;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
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
import it.unive.lisa.symbolic.value.operator.unary.StringLength;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;

/**
 * The overflow-insensitive interval abstract domain, approximating integer
 * values as the minimum integer interval containing them. It is implemented as
 * a {@link BaseNonRelationalValueDomain}. The lattice structure of this domain
 * is {@link IntInterval}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Interval
		implements
		SmashedSumIntDomain<IntInterval> {

	@Override
	public IntInterval evalConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (constant.getValue() instanceof Integer) {
			Integer i = (Integer) constant.getValue();
			return new IntInterval(new MathNumber(i), new MathNumber(i));
		}

		return IntInterval.TOP;
	}

	@Override
	public IntInterval evalPushAny(
			PushAny pushAny,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (pushAny instanceof PushFromConstraints)
			return IntInterval.TOP.generate(((PushFromConstraints) pushAny).getConstraints(), pp);
		return SmashedSumIntDomain.super.evalPushAny(pushAny, pp, oracle);
	}

	@Override
	public IntInterval evalUnaryExpression(
			UnaryExpression expression,
			IntInterval arg,
			ProgramPoint pp,
			SemanticOracle oracle) {
		UnaryOperator operator = expression.getOperator();
		if (operator == NumericNegation.INSTANCE)
			if (arg.isTop())
				return IntInterval.TOP;
			else
				return arg.mul(IntInterval.MINUS_ONE);
		else if (operator == StringLength.INSTANCE)
			return new IntInterval(MathNumber.ZERO, MathNumber.PLUS_INFINITY);
		else
			return IntInterval.TOP;
	}

	@Override
	public IntInterval evalBinaryExpression(
			BinaryExpression expression,
			IntInterval left,
			IntInterval right,
			ProgramPoint pp,
			SemanticOracle oracle) {
		BinaryOperator operator = expression.getOperator();
		if (!(operator instanceof DivisionOperator) && (left.isTop() || right.isTop()))
			// with div, we can return zero or bottom even if one of the
			// operands is top
			return IntInterval.TOP;

		if (operator instanceof AdditionOperator)
			return left.plus(right);
		else if (operator instanceof SubtractionOperator)
			return left.diff(right);
		else if (operator instanceof MultiplicationOperator)
			if (left.is(0) || right.is(0))
				return IntInterval.ZERO;
			else
				return left.mul(right);
		else if (operator instanceof DivisionOperator)
			if (right.is(0))
				return IntInterval.BOTTOM;
			else if (left.is(0))
				return IntInterval.ZERO;
			else if (left.isTop() || right.isTop())
				return IntInterval.TOP;
			else
				return left.div(right, false, false);
		else if (operator instanceof ModuloOperator)
			if (right.is(0))
				return IntInterval.BOTTOM;
			else if (left.is(0))
				return IntInterval.ZERO;
			else if (left.isTop() || right.isTop())
				return IntInterval.TOP;
			else {
				// the result takes the sign of the divisor - l%r is:
				// - [r.low+1,0] if r.high < 0 (fully negative)
				// - [0,r.high-1] if r.low > 0 (fully positive)
				// - [r.low+1,r.high-1] otherwise
				if (right.getHigh().compareTo(MathNumber.ZERO) < 0)
					return new IntInterval(right.getLow().add(MathNumber.ONE), MathNumber.ZERO);
				else if (right.getLow().compareTo(MathNumber.ZERO) > 0)
					return new IntInterval(MathNumber.ZERO, right.getHigh().subtract(MathNumber.ONE));
				else
					return new IntInterval(
							right.getLow().add(MathNumber.ONE),
							right.getHigh().subtract(MathNumber.ONE));
			}
		else if (operator instanceof RemainderOperator)
			if (right.is(0))
				return IntInterval.BOTTOM;
			else if (left.is(0))
				return IntInterval.ZERO;
			else if (left.isTop() || right.isTop())
				return IntInterval.TOP;
			else {
				// the result takes the sign of the dividend - l%r is:
				// - [-M+1,0] if l.high < 0 (fully negative)
				// - [0,M-1] if l.low > 0 (fully positive)
				// - [-M+1,M-1] otherwise
				// where M is
				// - -r.low if r.high < 0 (fully negative)
				// - r.high if r.low > 0 (fully positive)
				// - max(abs(r.low),abs(r.right)) otherwise
				MathNumber M;
				if (right.getHigh().compareTo(MathNumber.ZERO) < 0)
					M = right.getLow().multiply(MathNumber.MINUS_ONE);
				else if (right.getLow().compareTo(MathNumber.ZERO) > 0)
					M = right.getHigh();
				else
					M = right.getLow().abs().max(right.getHigh().abs());

				if (left.getHigh().compareTo(MathNumber.ZERO) < 0)
					return new IntInterval(M.multiply(MathNumber.MINUS_ONE).add(MathNumber.ONE), MathNumber.ZERO);
				else if (left.getLow().compareTo(MathNumber.ZERO) > 0)
					return new IntInterval(MathNumber.ZERO, M.subtract(MathNumber.ONE));
				else
					return new IntInterval(
							M.multiply(MathNumber.MINUS_ONE).add(MathNumber.ONE),
							M.subtract(MathNumber.ONE));
			}
		return IntInterval.TOP;
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(
			BinaryExpression expression,
			IntInterval left,
			IntInterval right,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (left.isTop() || right.isTop())
			return Satisfiability.UNKNOWN;

		BinaryOperator operator = expression.getOperator();
		if (operator == ComparisonEq.INSTANCE) {
			IntInterval glb = null;
			try {
				glb = left.glb(right);
			} catch (SemanticException e) {
				return Satisfiability.UNKNOWN;
			}

			if (glb.isBottom())
				return Satisfiability.NOT_SATISFIED;
			else if (left.isSingleton() && left.equals(right))
				return Satisfiability.SATISFIED;
			return Satisfiability.UNKNOWN;
		} else if (operator == ComparisonGe.INSTANCE)
			return satisfiesBinaryExpression(expression.withOperator(ComparisonLe.INSTANCE), right, left, pp, oracle);
		else if (operator == ComparisonGt.INSTANCE)
			return satisfiesBinaryExpression(expression.withOperator(ComparisonLt.INSTANCE), right, left, pp, oracle);
		else if (operator == ComparisonLe.INSTANCE) {
			IntInterval glb = null;
			try {
				glb = left.glb(right);
			} catch (SemanticException e) {
				return Satisfiability.UNKNOWN;
			}

			if (glb.isBottom())
				return Satisfiability.fromBoolean(left.getHigh().compareTo(right.getLow()) <= 0);
			// we might have a singleton as glb if the two intervals share a
			// bound
			if (glb.isSingleton() && left.getHigh().compareTo(right.getLow()) == 0)
				return Satisfiability.SATISFIED;
			return Satisfiability.UNKNOWN;
		} else if (operator == ComparisonLt.INSTANCE) {
			IntInterval glb = null;
			try {
				glb = left.glb(right);
			} catch (SemanticException e) {
				return Satisfiability.UNKNOWN;
			}

			if (glb.isBottom())
				return Satisfiability.fromBoolean(left.getHigh().compareTo(right.getLow()) < 0);
			return Satisfiability.UNKNOWN;
		} else if (operator == ComparisonNe.INSTANCE) {
			IntInterval glb = null;
			try {
				glb = left.glb(right);
			} catch (SemanticException e) {
				return Satisfiability.UNKNOWN;
			}
			if (glb.isBottom())
				return Satisfiability.SATISFIED;
			return Satisfiability.UNKNOWN;
		}
		return Satisfiability.UNKNOWN;
	}

	@Override
	public ValueEnvironment<IntInterval> assumeBinaryExpression(
			ValueEnvironment<IntInterval> environment,
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
		IntInterval eval;
		boolean rightIsExpr;
		ValueExpression left = (ValueExpression) expression.getLeft();
		ValueExpression right = (ValueExpression) expression.getRight();
		if (left instanceof Identifier) {
			eval = eval(environment, right, src, oracle);
			id = (Identifier) left;
			rightIsExpr = true;
		} else if (right instanceof Identifier) {
			eval = eval(environment, left, src, oracle);
			id = (Identifier) right;
			rightIsExpr = false;
		} else
			return environment;

		IntInterval starting = environment.getState(id);
		if (eval.isBottom() || starting.isBottom())
			return environment.bottom();

		IntInterval update = updateValue(expression.getOperator(), rightIsExpr, starting, eval);

		if (update == null)
			return environment;
		else if (update.isBottom())
			return environment.bottom();
		else
			return environment.putState(id, update);
	}

	/**
	 * Auxiliary method to assume that a condition holds, optionally changing
	 * the value of an identifier. This method returns {@code null} if no update
	 * is necessary, {@link IntInterval#BOTTOM} if the condition cannot hold
	 * with the current value of the identifier, or a new {@link IntInterval} if
	 * the identifier needs to be updated.
	 * 
	 * @param operator    the operator of the condition
	 * @param rightIsExpr if {@code true}, the condition is of the form
	 *                        {@code id op expr}, otherwise it is of the form
	 *                        {@code expr op id}
	 * @param idValue     the current value of the identifier
	 * @param exprValue   the value of the expression
	 * 
	 * @return {@code null} if no update is necessary,
	 *             {@link IntInterval#BOTTOM} if the condition cannot hold, or a
	 *             new {@link IntInterval} if the identifier needs to be updated
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	public static IntInterval updateValue(
			BinaryOperator operator,
			boolean rightIsExpr,
			IntInterval idValue,
			IntInterval exprValue)
			throws SemanticException {
		boolean exprLowIsMinInf = exprValue.lowIsMinusInfinity();
		IntInterval low_inf = new IntInterval(exprValue.getLow(), MathNumber.PLUS_INFINITY);
		IntInterval lowp1_inf = new IntInterval(exprValue.getLow().add(MathNumber.ONE), MathNumber.PLUS_INFINITY);
		IntInterval inf_high = new IntInterval(MathNumber.MINUS_INFINITY, exprValue.getHigh());
		IntInterval inf_highm1 = new IntInterval(
				MathNumber.MINUS_INFINITY,
				exprValue.getHigh().subtract(MathNumber.ONE));

		IntInterval update = null;
		if (operator == ComparisonEq.INSTANCE)
			// if eval is not a possible value, we go to bottom
			update = idValue.glb(exprValue);
		else if (operator == ComparisonGe.INSTANCE)
			if (rightIsExpr)
				update = exprLowIsMinInf ? null : idValue.glb(low_inf);
			else
				update = idValue.glb(inf_high);
		else if (operator == ComparisonGt.INSTANCE)
			if (rightIsExpr)
				update = exprLowIsMinInf ? null : idValue.glb(lowp1_inf);
			else
				update = exprLowIsMinInf ? exprValue : idValue.glb(inf_highm1);
		else if (operator == ComparisonLe.INSTANCE)
			if (rightIsExpr)
				update = idValue.glb(inf_high);
			else
				update = exprLowIsMinInf ? null : idValue.glb(low_inf);
		else if (operator == ComparisonLt.INSTANCE)
			if (rightIsExpr)
				update = exprLowIsMinInf ? exprValue : idValue.glb(inf_highm1);
			else
				update = exprLowIsMinInf ? null : idValue.glb(lowp1_inf);
		return update;
	}

	@Override
	public IntInterval fromInterval(
			IntInterval intv)
			throws SemanticException {
		return intv;
	}

	@Override
	public IntInterval toInterval(
			IntInterval value)
			throws SemanticException {
		return value;
	}

	@Override
	public IntInterval top() {
		return IntInterval.TOP;
	}

	@Override
	public IntInterval bottom() {
		return IntInterval.BOTTOM;
	}

}
