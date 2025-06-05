package it.unive.lisa.analysis.numeric;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
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
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

/**
 * The overflow-insensitive interval abstract domain, approximating integer
 * values as the minimum integer interval containing them. It is implemented as
 * a {@link BaseNonRelationalValueDomain}, handling top and bottom values for
 * the expression evaluation and bottom values for the expression
 * satisfiability. Top and bottom cases for least upper bounds, widening and
 * less or equals operations are handled by {@link BaseLattice} in
 * {@link BaseLattice#lub}, {@link BaseLattice#widening} and
 * {@link BaseLattice#lessOrEqual} methods, respectively.
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 */
public class Interval implements BaseNonRelationalValueDomain<Interval>, Comparable<Interval> {

	/**
	 * The abstract zero ({@code [0, 0]}) element.
	 */
	public static final Interval ZERO = new Interval(IntInterval.ZERO);

	/**
	 * The abstract top ({@code [-Inf, +Inf]}) element.
	 */
	public static final Interval TOP = new Interval(IntInterval.INFINITY);

	/**
	 * The abstract bottom element.
	 */
	public static final Interval BOTTOM = new Interval(null);

	/**
	 * The interval represented by this domain element.
	 */
	public final IntInterval interval;

	/**
	 * Builds the interval.
	 * 
	 * @param interval the underlying {@link IntInterval}
	 */
	public Interval(
			IntInterval interval) {
		this.interval = interval;
	}

	/**
	 * Builds the interval.
	 * 
	 * @param low  the lower bound
	 * @param high the higher bound
	 */
	public Interval(
			MathNumber low,
			MathNumber high) {
		this(new IntInterval(low, high));
	}

	/**
	 * Builds the interval.
	 * 
	 * @param low  the lower bound
	 * @param high the higher bound
	 */
	public Interval(
			int low,
			int high) {
		this(new IntInterval(low, high));
	}

	/**
	 * Builds the top interval.
	 */
	public Interval() {
		this(IntInterval.INFINITY);
	}

	@Override
	public Interval top() {
		return TOP;
	}

	@Override
	public boolean isTop() {
		return interval != null && interval.isInfinity();
	}

	@Override
	public Interval bottom() {
		return BOTTOM;
	}

	@Override
	public boolean isBottom() {
		return interval == null;
	}

	@Override
	public StructuredRepresentation representation() {
		if (isBottom())
			return Lattice.bottomRepresentation();

		return new StringRepresentation(interval.toString());
	}

	@Override
	public String toString() {
		return representation().toString();
	}

	@Override
	public Interval evalNonNullConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (constant.getValue() instanceof Integer) {
			Integer i = (Integer) constant.getValue();
			return new Interval(new MathNumber(i), new MathNumber(i));
		}

		return top();
	}

	@Override
	public Interval evalUnaryExpression(
			UnaryOperator operator,
			Interval arg,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (operator == NumericNegation.INSTANCE)
			if (arg.isTop())
				return top();
			else
				return new Interval(arg.interval.mul(IntInterval.MINUS_ONE));
		else if (operator == StringLength.INSTANCE)
			return new Interval(MathNumber.ZERO, MathNumber.PLUS_INFINITY);
		else
			return top();
	}

	/**
	 * Tests whether this interval instance corresponds (i.e., concretizes)
	 * exactly to the given integer. The tests is performed through
	 * {@link IntInterval#is(int)}.
	 * 
	 * @param n the integer value
	 * 
	 * @return {@code true} if that condition holds
	 */
	public boolean is(
			int n) {
		return !isBottom() && interval.is(n);
	}

	@Override
	public Interval evalBinaryExpression(
			BinaryOperator operator,
			Interval left,
			Interval right,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (!(operator instanceof DivisionOperator) && (left.isTop() || right.isTop()))
			// with div, we can return zero or bottom even if one of the
			// operands is top
			return top();

		if (operator instanceof AdditionOperator)
			return new Interval(left.interval.plus(right.interval));
		else if (operator instanceof SubtractionOperator)
			return new Interval(left.interval.diff(right.interval));
		else if (operator instanceof MultiplicationOperator)
			if (left.is(0) || right.is(0))
				return ZERO;
			else
				return new Interval(left.interval.mul(right.interval));
		else if (operator instanceof DivisionOperator)
			if (right.is(0))
				return bottom();
			else if (left.is(0))
				return ZERO;
			else if (left.isTop() || right.isTop())
				return top();
			else
				return new Interval(left.interval.div(right.interval, false, false));
		else if (operator instanceof ModuloOperator)
			if (right.is(0))
				return bottom();
			else if (left.is(0))
				return ZERO;
			else if (left.isTop() || right.isTop())
				return top();
			else {
				// the result takes the sign of the divisor - l%r is:
				// - [r.low+1,0] if r.high < 0 (fully negative)
				// - [0,r.high-1] if r.low > 0 (fully positive)
				// - [r.low+1,r.high-1] otherwise
				if (right.interval.getHigh().compareTo(MathNumber.ZERO) < 0)
					return new Interval(right.interval.getLow().add(MathNumber.ONE), MathNumber.ZERO);
				else if (right.interval.getLow().compareTo(MathNumber.ZERO) > 0)
					return new Interval(MathNumber.ZERO, right.interval.getHigh().subtract(MathNumber.ONE));
				else
					return new Interval(right.interval.getLow().add(MathNumber.ONE),
							right.interval.getHigh().subtract(MathNumber.ONE));
			}
		else if (operator instanceof RemainderOperator)
			if (right.is(0))
				return bottom();
			else if (left.is(0))
				return ZERO;
			else if (left.isTop() || right.isTop())
				return top();
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
				if (right.interval.getHigh().compareTo(MathNumber.ZERO) < 0)
					M = right.interval.getLow().multiply(MathNumber.MINUS_ONE);
				else if (right.interval.getLow().compareTo(MathNumber.ZERO) > 0)
					M = right.interval.getHigh();
				else
					M = right.interval.getLow().abs().max(right.interval.getHigh().abs());

				if (left.interval.getHigh().compareTo(MathNumber.ZERO) < 0)
					return new Interval(M.multiply(MathNumber.MINUS_ONE).add(MathNumber.ONE), MathNumber.ZERO);
				else if (left.interval.getLow().compareTo(MathNumber.ZERO) > 0)
					return new Interval(MathNumber.ZERO, M.subtract(MathNumber.ONE));
				else
					return new Interval(M.multiply(MathNumber.MINUS_ONE).add(MathNumber.ONE),
							M.subtract(MathNumber.ONE));
			}
		return top();
	}

	@Override
	public Interval lubAux(
			Interval other)
			throws SemanticException {
		MathNumber newLow = interval.getLow().min(other.interval.getLow());
		MathNumber newHigh = interval.getHigh().max(other.interval.getHigh());
		return newLow.isMinusInfinity() && newHigh.isPlusInfinity() ? top() : new Interval(newLow, newHigh);
	}

	@Override
	public Interval glbAux(
			Interval other) {
		MathNumber newLow = interval.getLow().max(other.interval.getLow());
		MathNumber newHigh = interval.getHigh().min(other.interval.getHigh());

		if (newLow.compareTo(newHigh) > 0)
			return bottom();
		return newLow.isMinusInfinity() && newHigh.isPlusInfinity() ? top() : new Interval(newLow, newHigh);
	}

	@Override
	public Interval wideningAux(
			Interval other)
			throws SemanticException {
		MathNumber newLow, newHigh;
		if (other.interval.getHigh().compareTo(interval.getHigh()) > 0)
			newHigh = MathNumber.PLUS_INFINITY;
		else
			newHigh = interval.getHigh();

		if (other.interval.getLow().compareTo(interval.getLow()) < 0)
			newLow = MathNumber.MINUS_INFINITY;
		else
			newLow = interval.getLow();

		return newLow.isMinusInfinity() && newHigh.isPlusInfinity() ? top() : new Interval(newLow, newHigh);
	}

	@Override
	public Interval narrowingAux(
			Interval other)
			throws SemanticException {
		MathNumber newLow, newHigh;
		newHigh = interval.getHigh().isInfinite() ? other.interval.getHigh() : interval.getHigh();
		newLow = interval.getLow().isInfinite() ? other.interval.getLow() : interval.getLow();
		return new Interval(newLow, newHigh);
	}

	@Override
	public boolean lessOrEqualAux(
			Interval other)
			throws SemanticException {
		return other.interval.includes(interval);
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(
			BinaryOperator operator,
			Interval left,
			Interval right,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (left.isTop() || right.isTop())
			return Satisfiability.UNKNOWN;

		if (operator == ComparisonEq.INSTANCE) {
			Interval glb = null;
			try {
				glb = left.glb(right);
			} catch (SemanticException e) {
				return Satisfiability.UNKNOWN;
			}

			if (glb.isBottom())
				return Satisfiability.NOT_SATISFIED;
			else if (left.interval.isSingleton() && left.equals(right))
				return Satisfiability.SATISFIED;
			return Satisfiability.UNKNOWN;
		} else if (operator == ComparisonGe.INSTANCE)
			return satisfiesBinaryExpression(ComparisonLe.INSTANCE, right, left, pp, oracle);
		else if (operator == ComparisonGt.INSTANCE)
			return satisfiesBinaryExpression(ComparisonLt.INSTANCE, right, left, pp, oracle);
		else if (operator == ComparisonLe.INSTANCE) {
			Interval glb = null;
			try {
				glb = left.glb(right);
			} catch (SemanticException e) {
				return Satisfiability.UNKNOWN;
			}

			if (glb.isBottom())
				return Satisfiability.fromBoolean(left.interval.getHigh().compareTo(right.interval.getLow()) <= 0);
			// we might have a singleton as glb if the two intervals share a
			// bound
			if (glb.interval.isSingleton() && left.interval.getHigh().compareTo(right.interval.getLow()) == 0)
				return Satisfiability.SATISFIED;
			return Satisfiability.UNKNOWN;
		} else if (operator == ComparisonLt.INSTANCE) {
			Interval glb = null;
			try {
				glb = left.glb(right);
			} catch (SemanticException e) {
				return Satisfiability.UNKNOWN;
			}

			if (glb.isBottom())
				return Satisfiability.fromBoolean(left.interval.getHigh().compareTo(right.interval.getLow()) < 0);
			return Satisfiability.UNKNOWN;
		} else if (operator == ComparisonNe.INSTANCE) {
			Interval glb = null;
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
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((interval == null) ? 0 : interval.hashCode());
		return result;
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Interval other = (Interval) obj;
		if (interval == null) {
			if (other.interval != null)
				return false;
		} else if (!interval.equals(other.interval))
			return false;
		return true;
	}

	@Override
	public ValueEnvironment<Interval> assumeBinaryExpression(
			ValueEnvironment<Interval> environment,
			BinaryOperator operator,
			ValueExpression left,
			ValueExpression right,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		Identifier id;
		Interval eval;
		boolean rightIsExpr;
		if (left instanceof Identifier) {
			eval = eval(right, environment, src, oracle);
			id = (Identifier) left;
			rightIsExpr = true;
		} else if (right instanceof Identifier) {
			eval = eval(left, environment, src, oracle);
			id = (Identifier) right;
			rightIsExpr = false;
		} else
			return environment;

		Interval starting = environment.getState(id);
		if (eval.isBottom() || starting.isBottom())
			return environment.bottom();

		boolean lowIsMinusInfinity = eval.interval.lowIsMinusInfinity();
		Interval low_inf = new Interval(eval.interval.getLow(), MathNumber.PLUS_INFINITY);
		Interval lowp1_inf = new Interval(eval.interval.getLow().add(MathNumber.ONE), MathNumber.PLUS_INFINITY);
		Interval inf_high = new Interval(MathNumber.MINUS_INFINITY, eval.interval.getHigh());
		Interval inf_highm1 = new Interval(MathNumber.MINUS_INFINITY, eval.interval.getHigh().subtract(MathNumber.ONE));

		Interval update = null;
		if (operator == ComparisonEq.INSTANCE)
			// if eval is not a possible value, we go to bottom
			update = starting.glb(eval);
		else if (operator == ComparisonGe.INSTANCE)
			if (rightIsExpr)
				update = lowIsMinusInfinity ? null : starting.glb(low_inf);
			else
				update = starting.glb(inf_high);
		else if (operator == ComparisonGt.INSTANCE)
			if (rightIsExpr)
				update = lowIsMinusInfinity ? null : starting.glb(lowp1_inf);
			else
				update = lowIsMinusInfinity ? eval : starting.glb(inf_highm1);
		else if (operator == ComparisonLe.INSTANCE)
			if (rightIsExpr)
				update = starting.glb(inf_high);
			else
				update = lowIsMinusInfinity ? null : starting.glb(low_inf);
		else if (operator == ComparisonLt.INSTANCE)
			if (rightIsExpr)
				update = lowIsMinusInfinity ? eval : starting.glb(inf_highm1);
			else
				update = lowIsMinusInfinity ? null : starting.glb(lowp1_inf);

		if (update == null)
			return environment;
		else if (update.isBottom())
			return environment.bottom();
		else
			return environment.putState(id, update);
	}

	@Override
	public int compareTo(
			Interval o) {
		if (isBottom())
			return o.isBottom() ? 0 : -1;
		if (isTop())
			return o.isTop() ? 0 : 1;
		if (o.isBottom())
			return 1;
		if (o.isTop())
			return -1;
		return interval.compareTo(o.interval);
	}
}
