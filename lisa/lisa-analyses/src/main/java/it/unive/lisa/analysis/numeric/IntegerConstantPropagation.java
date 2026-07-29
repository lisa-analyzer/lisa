package it.unive.lisa.analysis.numeric;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.combination.smash.SmashedSumIntDomain;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.lattices.numeric.IntegerConstant;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.PushFromConstraints;
import it.unive.lisa.symbolic.value.PushInv;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.DivisionOperator;
import it.unive.lisa.symbolic.value.operator.ModuloOperator;
import it.unive.lisa.symbolic.value.operator.MultiplicationOperator;
import it.unive.lisa.symbolic.value.operator.RemainderOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.BitwiseAnd;
import it.unive.lisa.symbolic.value.operator.binary.BitwiseOr;
import it.unive.lisa.symbolic.value.operator.binary.BitwiseShiftLeft;
import it.unive.lisa.symbolic.value.operator.binary.BitwiseShiftRight;
import it.unive.lisa.symbolic.value.operator.binary.BitwiseUnsignedShiftRight;
import it.unive.lisa.symbolic.value.operator.binary.BitwiseXor;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
import it.unive.lisa.symbolic.value.operator.binary.LogicalAnd;
import it.unive.lisa.symbolic.value.operator.binary.LogicalOr;
import it.unive.lisa.symbolic.value.operator.binary.NumericAtan2;
import it.unive.lisa.symbolic.value.operator.binary.NumericMax;
import it.unive.lisa.symbolic.value.operator.binary.NumericMin;
import it.unive.lisa.symbolic.value.operator.binary.NumericPow;
import it.unive.lisa.symbolic.value.operator.binary.StringIndexOf;
import it.unive.lisa.symbolic.value.operator.binary.StringIndexOfChar;
import it.unive.lisa.symbolic.value.operator.binary.StringLastIndexOf;
import it.unive.lisa.symbolic.value.operator.binary.StringLastIndexOfChar;
import it.unive.lisa.symbolic.value.operator.binary.ValueComparison;
import it.unive.lisa.symbolic.value.operator.ternary.StringIndexOfCharFromIndex;
import it.unive.lisa.symbolic.value.operator.ternary.StringLastIndexOfCharFromIndex;
import it.unive.lisa.symbolic.value.operator.ternary.StringLastIndexOfFromIndex;
import it.unive.lisa.symbolic.value.operator.ternary.TernaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.BitwiseNegation;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.symbolic.value.operator.unary.NumericAbs;
import it.unive.lisa.symbolic.value.operator.unary.NumericAcos;
import it.unive.lisa.symbolic.value.operator.unary.NumericAsin;
import it.unive.lisa.symbolic.value.operator.unary.NumericAtan;
import it.unive.lisa.symbolic.value.operator.unary.NumericCeil;
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
import it.unive.lisa.symbolic.value.operator.unary.NumericToString;
import it.unive.lisa.symbolic.value.operator.unary.StringLength;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumberConversionException;
import java.util.Collections;
import java.util.Set;

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
	public IntegerConstant evalConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (constant.getValue() instanceof Byte)
			return new IntegerConstant((int) (Byte) constant.getValue());
		if (constant.getValue() instanceof Short)
			return new IntegerConstant((int) (Short) constant.getValue());
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
			return generate(((PushFromConstraints) pushAny).getConstraints(), pp, oracle);
		return SmashedSumIntDomain.super.evalPushAny(pushAny, pp, oracle);
	}

	@Override
	public IntegerConstant evalUnaryExpression(
			UnaryExpression expression,
			IntegerConstant arg,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		UnaryOperator operator = expression.getOperator();

		if (oracle.hasWholeValueAnlysis() && operator == StringLength.INSTANCE) {
			Set<BinaryExpression> constraints = oracle.constraints(this, expression, pp);
			return generate(constraints, pp, oracle);
		}

		if (arg.isTop())
			return IntegerConstant.TOP;

		if (operator == BitwiseNegation.INSTANCE)
			return new IntegerConstant(~arg.value);
		if (operator == NumericAbs.INSTANCE)
			return new IntegerConstant(Math.abs(arg.value));
		if (operator == NumericAcos.INSTANCE)
			return new IntegerConstant((int) Math.acos(arg.value));
		if (operator == NumericAsin.INSTANCE)
			return new IntegerConstant((int) Math.asin(arg.value));
		if (operator == NumericAtan.INSTANCE)
			return new IntegerConstant((int) Math.atan(arg.value));
		if (operator == NumericCeil.INSTANCE)
			return new IntegerConstant((int) Math.ceil(arg.value));
		if (operator == NumericCos.INSTANCE)
			return new IntegerConstant((int) Math.cos(arg.value));
		if (operator == NumericExp.INSTANCE)
			return new IntegerConstant((int) Math.exp(arg.value));
		if (operator == NumericFloor.INSTANCE)
			return new IntegerConstant((int) Math.floor(arg.value));
		if (operator == NumericLog.INSTANCE)
			return new IntegerConstant((int) Math.log(arg.value));
		if (operator == NumericLog10.INSTANCE)
			return new IntegerConstant((int) Math.log10(arg.value));
		if (operator == NumericNegation.INSTANCE)
			return new IntegerConstant(-arg.value);
		if (operator == NumericRound.INSTANCE)
			return new IntegerConstant((int) Math.round(arg.value));
		if (operator == NumericSin.INSTANCE)
			return new IntegerConstant((int) Math.sin(arg.value));
		if (operator == NumericSqrt.INSTANCE)
			return new IntegerConstant((int) Math.sqrt(arg.value));
		if (operator == NumericTan.INSTANCE)
			return new IntegerConstant((int) Math.tan(arg.value));
		if (operator == NumericToRadians.INSTANCE)
			return new IntegerConstant((int) Math.toRadians(arg.value));

		return IntegerConstant.TOP;
	}

	@Override
	public IntegerConstant evalBinaryExpression(
			BinaryExpression expression,
			IntegerConstant left,
			IntegerConstant right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		BinaryOperator operator = expression.getOperator();

		if (oracle.hasWholeValueAnlysis() && (operator == StringIndexOfChar.INSTANCE
				|| operator == StringLastIndexOfChar.INSTANCE
				|| operator == StringIndexOf.INSTANCE
				|| operator == StringLastIndexOf.INSTANCE
				// for value comparison we ask for constraints only if we do not
				// know anything about both operands as otherwise this might be
				// a numerical comparison
				|| (operator == ValueComparison.INSTANCE && left.isTop() && right.isTop()))) {
			Set<BinaryExpression> constraints = oracle.constraints(this, expression, pp);
			return generate(constraints, pp, oracle);
		}

		if (operator instanceof DivisionOperator)
			if (!left.isTop() && left.value == 0)
				return new IntegerConstant(0);
			else if (!right.isTop() && right.value == 0)
				return IntegerConstant.BOTTOM;
			else if (left.isTop() || right.isTop() || left.value % right.value != 0)
				return IntegerConstant.TOP;
			else
				return new IntegerConstant(left.value / right.value);
		if (operator instanceof MultiplicationOperator)
			if (!left.isTop() && left.value == 0)
				return new IntegerConstant(0);
			else if (!right.isTop() && right.value == 0)
				return new IntegerConstant(0);
			else if (left.isTop() || right.isTop())
				return IntegerConstant.TOP;
			else
				return new IntegerConstant(left.value * right.value);

		if (left.isTop() || right.isTop())
			return IntegerConstant.TOP;

		if (operator == BitwiseAnd.INSTANCE)
			return new IntegerConstant(left.value & right.value);
		if (operator == BitwiseOr.INSTANCE)
			return new IntegerConstant(left.value | right.value);
		if (operator == BitwiseShiftLeft.INSTANCE)
			return new IntegerConstant(left.value << right.value);
		if (operator == BitwiseShiftRight.INSTANCE)
			return new IntegerConstant(left.value >> right.value);
		if (operator == BitwiseUnsignedShiftRight.INSTANCE)
			return new IntegerConstant(left.value >>> right.value);
		if (operator == BitwiseXor.INSTANCE)
			return new IntegerConstant(left.value ^ right.value);

		if (operator instanceof AdditionOperator)
			return new IntegerConstant(left.value + right.value);
		if (operator instanceof ModuloOperator)
			return new IntegerConstant(Math.floorMod(left.value, right.value));
		if (operator instanceof RemainderOperator)
			return new IntegerConstant(left.value % right.value);
		if (operator instanceof SubtractionOperator)
			return new IntegerConstant(left.value - right.value);

		if (operator == NumericAtan2.INSTANCE)
			return new IntegerConstant((int) Math.atan2(left.value, right.value));
		if (operator == NumericMax.INSTANCE)
			return new IntegerConstant(Math.max(left.value, right.value));
		if (operator == NumericMin.INSTANCE)
			return new IntegerConstant(Math.min(left.value, right.value));
		if (operator == NumericPow.INSTANCE)
			return new IntegerConstant((int) Math.pow(left.value, right.value));

		if (operator == ValueComparison.INSTANCE)
			return new IntegerConstant(Integer.compare(left.value, right.value));

		return IntegerConstant.TOP;
	}

	@Override
	public IntegerConstant evalTernaryExpression(
			TernaryExpression expression,
			IntegerConstant left,
			IntegerConstant middle,
			IntegerConstant right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		TernaryOperator operator = expression.getOperator();
		if (oracle.hasWholeValueAnlysis() && (operator == StringIndexOfCharFromIndex.INSTANCE
				|| operator == StringLastIndexOfCharFromIndex.INSTANCE
				|| operator == StringLastIndexOfFromIndex.INSTANCE)) {
			Set<BinaryExpression> constraints = oracle.constraints(this, expression, pp);
			return generate(constraints, pp, oracle);
		}
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
			return left.value.intValue() == right.value.intValue() ? Satisfiability.SATISFIED
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
			return left.value.intValue() != right.value.intValue() ? Satisfiability.SATISFIED
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
				if (!canProcess(right, src, oracle))
					// the expression does not have a numerical value, we do not
					// assume anything on it
					return environment;
				IntegerConstant eval = eval(environment, right, src, oracle);
				if (eval.isBottom())
					return environment.bottom();
				if (eval.isTop())
					// we do not know anything about the expression, so we
					// cannot assume anything on the identifier
					return environment;
				return environment.putState((Identifier) left, eval);
			} else if (right instanceof Identifier) {
				if (!canProcess(left, src, oracle))
					// the expression does not have a numerical value, we do not
					// assume anything on it
					return environment;
				IntegerConstant eval = eval(environment, left, src, oracle);
				if (eval.isBottom())
					return environment.bottom();
				if (eval.isTop())
					// we do not know anything about the expression, so we
					// cannot assume anything on the identifier
					return environment;
				return environment.putState((Identifier) right, eval);
			}
		return environment;
	}

	@Override
	public IntegerConstant fromInterval(
			IntInterval intv)
			throws SemanticException {
		if (intv.isSingleton())
			try {
				return new IntegerConstant(intv.getLow().toInt());
			} catch (MathNumberConversionException e) {
				throw new SemanticException("Cannot convert " + intv + " to an integer constant", e);
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

	@Override
	public Set<BinaryExpression> constraints(
			ValueDomain<?> requesting,
			ValueEnvironment<IntegerConstant> state,
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
				IntegerConstant value = eval(state, arg, pp, oracle);
				if (value.isTop())
					return Collections.emptySet();
				if (value.isBottom())
					return null;
				return ValueDomain.makeEqConstraint(
						pp.getProgram().getTypes().getStringType(),
						value.value.toString(),
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

		IntegerConstant value = eval(state, e, pp, oracle);
		if (value.isTop())
			return Collections.emptySet();
		if (value.isBottom())
			return null;
		return ValueDomain.makeEqConstraint(
				pp.getProgram().getTypes().getIntegerType(),
				value.value,
				e,
				pp);
	}

	/**
	 * Generates an integer constant from a set of constraints. If a constraint
	 * of the form {@code c == expr} is found, the constant {@code c} is
	 * returned. Otherwise, the constant is generated if two constraints
	 * {@code c >= expr} and {@code c <= expr} are found the same constant
	 * {@code c}, that is returned.
	 * 
	 * @param constraints the set of constraints
	 * @param pp          the program point
	 * @param oracle      the semantic oracle
	 * 
	 * @return the generated constant, or {@link IntegerConstant#TOP} if no
	 *             constraints are provided, or {@link IntegerConstant#BOTTOM}
	 *             if the constraint set is {@code null}
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	public IntegerConstant generate(
			Set<BinaryExpression> constraints,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (constraints == null)
			return IntegerConstant.BOTTOM;

		Integer ge = null, le = null;
		for (BinaryExpression expr : constraints)
			if (expr.getLeft() instanceof Constant && ((Constant) expr.getLeft()).getValue() instanceof Integer) {
				Integer val = (Integer) ((Constant) expr.getLeft()).getValue();
				if (expr.getOperator() instanceof ComparisonEq)
					return new IntegerConstant(val);
				else if (expr.getOperator() instanceof ComparisonGe)
					ge = val;
				else if (expr.getOperator() instanceof ComparisonLe)
					le = val;
			}

		if (ge != null && ge.equals(le))
			return new IntegerConstant(ge);

		return IntegerConstant.TOP;
	}

	@Override
	public boolean canProcess(
			ValueExpression e,
			ProgramPoint pp,
			SemanticOracle oracle) {
		boolean whole = oracle.hasWholeValueAnlysis();
		if (e instanceof PushInv)
			// the type approximation of a pushinv is bottom, so the below check
			// will always fail regardless of the kind of value we are tracking
			return whole
					? e.getStaticType().isNumericType() && e.getStaticType().asNumericType().isIntegral()
					: e.getStaticType().isValueType();

		Set<Type> rts = null;
		try {
			rts = oracle.getRuntimeTypesOf(e, pp);
		} catch (SemanticException ex) {
			return false;
		}

		if (rts == null || rts.isEmpty())
			// if we have no runtime types, either the type domain has no type
			// information for the given expression (thus it can be anything,
			// also something that we can track) or the computation returned
			// bottom (and the whole state is likely going to go to bottom
			// anyway).
			return true;

		if (whole)
			return rts.stream().anyMatch(t -> t.isNumericType() && t.asNumericType().isIntegral());
		else
			return rts.stream().anyMatch(Type::isValueType);
	}

}
