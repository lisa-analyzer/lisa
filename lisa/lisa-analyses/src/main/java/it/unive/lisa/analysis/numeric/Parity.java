package it.unive.lisa.analysis.numeric;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.combination.smash.SmashedSumIntDomain;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.lattices.numeric.ParityLattice;
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
import it.unive.lisa.symbolic.value.operator.unary.StringLength;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumberConversionException;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The overflow-insensitive Parity abstract domain, tracking if a numeric value
 * is even or odd, implemented as a {@link BaseNonRelationalValueDomain}.
 * 
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
public class Parity
		implements
		SmashedSumIntDomain<ParityLattice> {

	@Override
	public ParityLattice evalConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (constant.getValue() instanceof Byte) {
			Byte i = (Byte) constant.getValue();
			return i % 2 == 0 ? ParityLattice.EVEN : ParityLattice.ODD;
		}
		if (constant.getValue() instanceof Short) {
			Short i = (Short) constant.getValue();
			return i % 2 == 0 ? ParityLattice.EVEN : ParityLattice.ODD;
		}
		if (constant.getValue() instanceof Integer) {
			Integer i = (Integer) constant.getValue();
			return i % 2 == 0 ? ParityLattice.EVEN : ParityLattice.ODD;
		}
		if (constant.getValue() instanceof Long) {
			Long i = (Long) constant.getValue();
			return i % 2 == 0 ? ParityLattice.EVEN : ParityLattice.ODD;
		}

		return ParityLattice.TOP;
	}

	@Override
	public ParityLattice evalPushAny(
			PushAny pushAny,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (pushAny instanceof PushFromConstraints)
			return generate(((PushFromConstraints) pushAny).getConstraints(), pp, oracle);
		return SmashedSumIntDomain.super.evalPushAny(pushAny, pp, oracle);
	}

	@Override
	public ParityLattice evalUnaryExpression(
			UnaryExpression expression,
			ParityLattice arg,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		UnaryOperator operator = expression.getOperator();

		if (oracle.hasWholeValueAnlysis() && operator == StringLength.INSTANCE) {
			Set<BinaryExpression> constraints = oracle.constraints(
					this,
					(ValueExpression) expression.getExpression(),
					pp);
			if (constraints == null)
				return ParityLattice.BOTTOM;
			return generate(
					constraints.stream()
							.filter(c -> c.getRight() instanceof UnaryExpression
									&& ((UnaryExpression) c.getRight()).getOperator() == StringLength.INSTANCE)
							.collect(Collectors.toSet()),
					pp,
					oracle);
		}

		if (arg.isTop())
			return ParityLattice.TOP;

		if (operator == BitwiseNegation.INSTANCE)
			// negation flips the parity
			return arg.isEven() ? ParityLattice.ODD : ParityLattice.EVEN;
		if (operator == NumericAbs.INSTANCE
				|| operator == NumericCeil.INSTANCE
				|| operator == NumericFloor.INSTANCE
				|| operator == NumericNegation.INSTANCE
				|| operator == NumericRound.INSTANCE)
			return arg;
		if (operator == NumericAcos.INSTANCE
				|| operator == NumericAsin.INSTANCE
				|| operator == NumericAtan.INSTANCE
				|| operator == NumericCos.INSTANCE
				|| operator == NumericExp.INSTANCE
				|| operator == NumericLog.INSTANCE
				|| operator == NumericLog10.INSTANCE
				|| operator == NumericSin.INSTANCE
				|| operator == NumericSqrt.INSTANCE
				|| operator == NumericTan.INSTANCE
				|| operator == NumericToRadians.INSTANCE)
			return ParityLattice.TOP;

		return ParityLattice.TOP;
	}

	@Override
	public ParityLattice evalBinaryExpression(
			BinaryExpression expression,
			ParityLattice left,
			ParityLattice right,
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

		if (left.isTop() || right.isTop())
			return ParityLattice.TOP;

		if (operator instanceof AdditionOperator || operator instanceof SubtractionOperator)
			if (right.equals(left))
				return ParityLattice.EVEN;
			else
				return ParityLattice.ODD;
		else if (operator instanceof MultiplicationOperator)
			if (left.isEven() || right.isEven())
				return ParityLattice.EVEN;
			else
				return ParityLattice.ODD;
		else if (operator instanceof DivisionOperator)
			if (left.isOdd())
				return right.isOdd() ? ParityLattice.ODD : ParityLattice.EVEN;
			else
				return right.isOdd() ? ParityLattice.EVEN : ParityLattice.TOP;
		else if (operator instanceof ModuloOperator || operator instanceof RemainderOperator)
			return ParityLattice.TOP;

		if (operator == BitwiseAnd.INSTANCE)
			// we only look at the last bit
			return left.isEven() || right.isEven() ? ParityLattice.EVEN : ParityLattice.ODD;
		if (operator == BitwiseOr.INSTANCE)
			// we only look at the last bit
			return left.isOdd() || right.isOdd() ? ParityLattice.ODD : ParityLattice.EVEN;
		if (operator == BitwiseShiftLeft.INSTANCE)
			// we only look at the last bit
			return ParityLattice.EVEN;
		if (operator == BitwiseXor.INSTANCE)
			// we only look at the last bit
			return left != right ? ParityLattice.ODD : ParityLattice.EVEN;

		if (operator == NumericMax.INSTANCE)
			return left == right ? left : ParityLattice.TOP;
		if (operator == NumericMin.INSTANCE)
			return left == right ? left : ParityLattice.TOP;

		if (operator == NumericAtan2.INSTANCE
				|| operator == NumericPow.INSTANCE
				|| operator == BitwiseShiftRight.INSTANCE
				|| operator == BitwiseUnsignedShiftRight.INSTANCE
				|| operator == ValueComparison.INSTANCE)
			return ParityLattice.TOP;

		return ParityLattice.TOP;
	}

	@Override
	public ParityLattice evalTernaryExpression(
			TernaryExpression expression,
			ParityLattice left,
			ParityLattice middle,
			ParityLattice right,
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
		return ParityLattice.TOP;
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(
			BinaryExpression expression,
			ParityLattice left,
			ParityLattice right,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (left.isTop() || right.isTop())
			return Satisfiability.UNKNOWN;

		BinaryOperator operator = expression.getOperator();
		if (operator == ComparisonEq.INSTANCE)
			return left == right ? Satisfiability.UNKNOWN : Satisfiability.NOT_SATISFIED;
		else if (operator == ComparisonNe.INSTANCE)
			// same parity: might be equal or not (e.g., 2 and 4 are both even,
			// but
			// not equal); different parities: can never be equal (even ≠ odd
			// always)
			return left == right ? Satisfiability.UNKNOWN : Satisfiability.SATISFIED;
		else
			return Satisfiability.UNKNOWN;
	}

	@Override
	public ValueEnvironment<ParityLattice> assumeBinaryExpression(
			ValueEnvironment<ParityLattice> environment,
			BinaryExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		BinaryOperator operator = expression.getOperator();
		ValueExpression left = (ValueExpression) expression.getLeft();
		ValueExpression right = (ValueExpression) expression.getRight();
		if (operator == ComparisonEq.INSTANCE)
			if (left instanceof Identifier) {
				if (!canProcess(right, src, oracle))
					// the expression does not have a numerical value, we do not
					// assume anything on it
					return environment;
				ParityLattice eval = eval(environment, right, src, oracle);
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
				ParityLattice eval = eval(environment, left, src, oracle);
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
	public ParityLattice fromInterval(
			IntInterval intv)
			throws SemanticException {
		if (intv.isSingleton())
			try {
				return intv.getLow().toLong() % 2 == 0 ? ParityLattice.EVEN : ParityLattice.ODD;
			} catch (MathNumberConversionException e) {
				throw new SemanticException("Cannot convert " + intv + " to an integer constant", e);
			}
		return ParityLattice.TOP;
	}

	@Override
	public IntInterval toInterval(
			ParityLattice con)
			throws SemanticException {
		if (con.isBottom())
			return null;
		return IntInterval.INFINITY;
	}

	@Override
	public ParityLattice top() {
		return ParityLattice.TOP;
	}

	@Override
	public ParityLattice bottom() {
		return ParityLattice.BOTTOM;
	}

	@Override
	public Set<BinaryExpression> constraints(
			ValueDomain<?> requesting,
			ValueEnvironment<ParityLattice> state,
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

		if (e instanceof BinaryExpression) {
			BinaryOperator operator = ((BinaryExpression) e).getOperator();
			if (operator == ComparisonEq.INSTANCE || operator == ComparisonNe.INSTANCE) {
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

		// right now the constraints do not allow for arbitrary expressions on
		// the right-hand side, so we cannot express the parity in terms of the
		// remainder
		return Collections.emptySet();
	}

	private ParityLattice generate(
			Set<BinaryExpression> constraints,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (constraints == null)
			return ParityLattice.BOTTOM;

		for (BinaryExpression expr : constraints)
			if (expr.getLeft() instanceof Constant && ((Constant) expr.getLeft()).getValue() instanceof Integer) {
				Integer val = (Integer) ((Constant) expr.getLeft()).getValue();
				if (expr.getOperator() instanceof ComparisonEq)
					return val % 2 == 0 ? ParityLattice.EVEN : ParityLattice.ODD;
			}

		return ParityLattice.TOP;
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
