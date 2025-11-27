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
import it.unive.lisa.symbolic.value.operator.binary.NumericMax;
import it.unive.lisa.symbolic.value.operator.binary.NumericMin;
import it.unive.lisa.symbolic.value.operator.binary.ValueComparison;
import it.unive.lisa.symbolic.value.operator.unary.NumericAbs;
import it.unive.lisa.symbolic.value.operator.unary.NumericAcos;
import it.unive.lisa.symbolic.value.operator.unary.NumericAsin;
import it.unive.lisa.symbolic.value.operator.unary.NumericAtan;
import it.unive.lisa.symbolic.value.operator.unary.NumericCos;
import it.unive.lisa.symbolic.value.operator.unary.NumericExp;
import it.unive.lisa.symbolic.value.operator.unary.NumericFloor;
import it.unive.lisa.symbolic.value.operator.unary.NumericLog;
import it.unive.lisa.symbolic.value.operator.unary.NumericLog10;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.symbolic.value.operator.unary.NumericRound;
import it.unive.lisa.symbolic.value.operator.unary.NumericSin;
import it.unive.lisa.symbolic.value.operator.unary.NumericSqrt;
import it.unive.lisa.symbolic.value.operator.unary.NumericTan;
import it.unive.lisa.symbolic.value.operator.unary.NumericToRadians;
import it.unive.lisa.symbolic.value.operator.unary.StringLength;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.lisa.util.numeric.MathNumberConversionException;
import java.util.function.Function;

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

		if (operator == StringLength.INSTANCE)
			return new IntInterval(MathNumber.ZERO, MathNumber.PLUS_INFINITY);

		if (operator instanceof NumericSin)
			return trigonometric(arg, Math::sin, 4 * Math.PI);
		if (operator instanceof NumericCos)
			return trigonometric(arg, Math::cos, 4 * Math.PI);
		if (operator instanceof NumericTan)
			return trigonometric(arg, Math::tan, Math.PI);

		Double l, h;
		try {
			l = arg.getLow().toDouble();
		} catch (MathNumberConversionException e) {
			l = null;
		}
		try {
			h = arg.getHigh().toDouble();
		} catch (MathNumberConversionException e) {
			h = null;
		}

		if (operator instanceof NumericAsin)
			if (arg.lowIsMinusInfinity() || arg.getLow().compareTo(MathNumber.MINUS_ONE) <= 0)
				if (arg.highIsPlusInfinity() || arg.getHigh().compareTo(MathNumber.ONE) >= 1)
					return new IntInterval(new MathNumber(Math.asin(-1)), new MathNumber(Math.asin(1)));
				else
					return new IntInterval(new MathNumber(Math.asin(-1)), new MathNumber(Math.asin(h)));
			else if (arg.highIsPlusInfinity() || arg.getHigh().compareTo(MathNumber.ONE) >= 1)
				return new IntInterval(new MathNumber(Math.asin(l)), new MathNumber(Math.asin(1)));
			else
				return new IntInterval(new MathNumber(Math.asin(l)), new MathNumber(Math.asin(h)));

		if (operator instanceof NumericAcos)
			if (arg.lowIsMinusInfinity() || arg.getLow().compareTo(MathNumber.MINUS_ONE) <= 0)
				if (arg.highIsPlusInfinity() || arg.getHigh().compareTo(MathNumber.ONE) >= 1)
					return new IntInterval(new MathNumber(Math.acos(1)), new MathNumber(Math.acos(-1)));
				else
					return new IntInterval(new MathNumber(Math.acos(h)), new MathNumber(Math.acos(-1)));
			else if (arg.highIsPlusInfinity() || arg.getHigh().compareTo(MathNumber.ONE) >= 1)
				return new IntInterval(new MathNumber(Math.acos(1)), new MathNumber(Math.acos(l)));
			else
				return new IntInterval(new MathNumber(Math.acos(h)), new MathNumber(Math.acos(l)));

		if (operator instanceof NumericAtan)
			if (arg.lowIsMinusInfinity())
				if (arg.highIsPlusInfinity())
					return IntInterval.TOP;
				else
					return new IntInterval(MathNumber.MINUS_INFINITY, new MathNumber(Math.atan(h)));
			else if (arg.highIsPlusInfinity())
				return new IntInterval(new MathNumber(Math.atan(l)), MathNumber.PLUS_INFINITY);
			else
				return new IntInterval(new MathNumber(Math.atan(l)), new MathNumber(Math.atan(h)));

		if (operator instanceof NumericToRadians)
			if (arg.lowIsMinusInfinity())
				if (arg.highIsPlusInfinity())
					return IntInterval.TOP;
				else
					return new IntInterval(MathNumber.MINUS_INFINITY, new MathNumber(Math.toRadians(h)));
			else if (arg.highIsPlusInfinity())
				return new IntInterval(new MathNumber(Math.toRadians(l)), MathNumber.PLUS_INFINITY);
			else
				return new IntInterval(new MathNumber(Math.toRadians(l)), new MathNumber(Math.toRadians(h)));

		if (operator instanceof NumericSqrt)
			if (arg.lowIsMinusInfinity() || arg.getLow().compareTo(MathNumber.ZERO) <= 0)
				if (arg.getHigh().compareTo(MathNumber.ZERO) <= 0)
					return IntInterval.BOTTOM;
				else if (arg.highIsPlusInfinity())
					return new IntInterval(MathNumber.ZERO, MathNumber.PLUS_INFINITY);
				else
					return new IntInterval(MathNumber.ZERO, new MathNumber(Math.sqrt(h)));
			else if (arg.highIsPlusInfinity())
				return new IntInterval(new MathNumber(Math.sqrt(l)), MathNumber.PLUS_INFINITY);
			else
				return new IntInterval(new MathNumber(Math.sqrt(l)), new MathNumber(Math.sqrt(h)));

		if (operator instanceof NumericLog)
			if (arg.lowIsMinusInfinity() || arg.getLow().compareTo(MathNumber.ZERO) <= 0)
				if (arg.getHigh().compareTo(MathNumber.ZERO) <= 0)
					return IntInterval.BOTTOM;
				else if (arg.highIsPlusInfinity())
					return IntInterval.TOP;
				else
					return new IntInterval(MathNumber.MINUS_INFINITY, new MathNumber(Math.log(h)));
			else if (arg.highIsPlusInfinity())
				return new IntInterval(new MathNumber(Math.log(l)), MathNumber.PLUS_INFINITY);
			else
				return new IntInterval(new MathNumber(Math.log(l)), new MathNumber(Math.log(h)));

		if (operator instanceof NumericLog10)
			if (arg.lowIsMinusInfinity() || arg.getLow().compareTo(MathNumber.ZERO) <= 0)
				if (arg.getHigh().compareTo(MathNumber.ZERO) <= 0)
					return IntInterval.BOTTOM;
				else if (arg.highIsPlusInfinity())
					return IntInterval.TOP;
				else
					return new IntInterval(MathNumber.MINUS_INFINITY, new MathNumber(Math.log10(h)));
			else if (arg.highIsPlusInfinity())
				return new IntInterval(new MathNumber(Math.log10(l)), MathNumber.PLUS_INFINITY);
			else
				return new IntInterval(new MathNumber(Math.log10(l)), new MathNumber(Math.log10(h)));

		if (operator instanceof NumericExp)
			if (arg.lowIsMinusInfinity())
				if (arg.highIsPlusInfinity())
					return IntInterval.TOP;
				else
					return new IntInterval(MathNumber.MINUS_INFINITY, new MathNumber(Math.exp(h)));
			else if (arg.highIsPlusInfinity())
				return new IntInterval(new MathNumber(Math.exp(l)), MathNumber.PLUS_INFINITY);
			else
				return new IntInterval(new MathNumber(Math.exp(l)), new MathNumber(Math.exp(h)));

		if (operator instanceof NumericFloor)
			return arg;
		if (operator instanceof NumericRound)
			return arg;

		if (operator instanceof NumericAbs)
			if (arg.getLow().compareTo(MathNumber.ZERO) >= 0)
				return arg;
			else if (arg.getHigh().compareTo(MathNumber.ZERO) <= 0)
				return new IntInterval(arg.getHigh().multiply(MathNumber.MINUS_ONE),
						arg.getLow().multiply(MathNumber.MINUS_ONE));
			else if (arg.getHigh().compareTo(arg.getLow().multiply(MathNumber.MINUS_ONE)) >= 0)
				return new IntInterval(MathNumber.ZERO, arg.getHigh());
			else
				return new IntInterval(MathNumber.ZERO, arg.getLow().multiply(MathNumber.MINUS_ONE));

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

		if (operator instanceof NumericMax)
			return new IntInterval(left.getLow().max(right.getLow()), left.getHigh().max(right.getHigh()));

		if (operator instanceof NumericMin)
			return new IntInterval(left.getLow().min(right.getLow()), left.getHigh().min(right.getHigh()));

		if (operator instanceof ValueComparison)
			return new IntInterval(-1, 1);

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

	public static IntInterval trigonometric(
			IntInterval i,
			Function<Double, Double> function,
			double period) {
		if (i.isBottom())
			return i;

		if (i.lowIsMinusInfinity() || i.highIsPlusInfinity())
			// unbounded -> all values
			return new IntInterval(-1, 1);

		double a, b;
		try {
			a = i.getLow().toDouble();
			b = i.getHigh().toDouble();
		} catch (MathNumberConversionException e) {
			// this should never happen as both bounds are finite
			return IntInterval.BOTTOM;
		}

		if (b - a >= period)
			// an interval wider than the period will include all values
			return new IntInterval(-1, 1);

		// these are the coefficients of the smaller and greater multiples of pi
		// that are included in the interval
		double pi = Math.PI;
		int kStart = (int) Math.ceil(a / pi);
		int kEnd = (int) Math.floor(b / pi);

		double trig_a = function.apply(a);
		double trig_b = function.apply(b);

		// the min/max are the ones of the bounds, unless a local
		// max/min exists inside the interval: this always correspond
		// to a multiple of pi
		double min = Math.min(trig_a, trig_b);
		double max = Math.max(trig_a, trig_b);

		// we iterate over the multiples of pi inside the interval
		// to scan for local min/max
		for (int k = kStart; k <= kEnd; ++k) {
			double x = function.apply(k * pi);
			min = Math.min(min, x);
			max = Math.max(max, x);
		}

		return new IntInterval((int) Math.floor(min), (int) Math.ceil(max));
	}

	@Override
	public ValueEnvironment<IntInterval> assume(
			ValueEnvironment<IntInterval> environment,
			ValueExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		Satisfiability sat = satisfies(environment, expression, src, oracle);
		if (sat == Satisfiability.NOT_SATISFIED)
			return environment.bottom();
		if (sat == Satisfiability.SATISFIED)
			return environment;
		return SmashedSumIntDomain.super.assume(environment, expression, src, dest, oracle);
	}

}
