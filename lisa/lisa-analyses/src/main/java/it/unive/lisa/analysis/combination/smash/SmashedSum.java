package it.unive.lisa.analysis.combination.smash;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
import it.unive.lisa.symbolic.value.operator.binary.LogicalOperation;
import it.unive.lisa.symbolic.value.operator.binary.NumericComparison;
import it.unive.lisa.symbolic.value.operator.binary.NumericOperation;
import it.unive.lisa.symbolic.value.operator.binary.StringConcat;
import it.unive.lisa.symbolic.value.operator.binary.StringIndexOf;
import it.unive.lisa.symbolic.value.operator.binary.StringOperation;
import it.unive.lisa.symbolic.value.operator.ternary.StringReplace;
import it.unive.lisa.symbolic.value.operator.ternary.StringSubstring;
import it.unive.lisa.symbolic.value.operator.ternary.TernaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.symbolic.value.operator.unary.StringLength;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

/**
 * The smashed-sum abstract domain between interval, satisfiability and a
 * non-relational string abstract domain.
 * 
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 *
 * @param <I> the non-relational integer abstract domain
 * @param <S> the non-relational string abstract domain
 */
public class SmashedSum<
		I extends SmashedSumIntDomain<I>,
		S extends SmashedSumStringDomain<S>>
		implements
		BaseNonRelationalValueDomain<SmashedSum<I, S>> {

	private final I intValue;
	private final S stringValue;
	private final Satisfiability boolValue;

	/**
	 * Builds an abstract element of this domain.
	 * 
	 * @param intValue    the abstract value for intergers
	 * @param stringValue the abstract value for strings
	 * @param boolValue   the abstract value for booleans
	 */
	public SmashedSum(
			I intValue,
			S stringValue,
			Satisfiability boolValue) {
		this.intValue = intValue;
		this.stringValue = stringValue;
		this.boolValue = boolValue;
	}

	/**
	 * Yields the integer abstract value.
	 * 
	 * @return the integer abstract value
	 */
	public I getIntValue() {
		return intValue;
	}

	/**
	 * Yields the string abstract value.
	 * 
	 * @return the string abstract value
	 */
	public S getStringValue() {
		return stringValue;
	}

	/**
	 * Yields the boolean abstract value.
	 *
	 * @return the boolean abstract value
	 */
	public Satisfiability getBoolValue() {
		return boolValue;
	}

	@Override
	public SmashedSum<I, S> evalNonNullConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (constant.getValue() instanceof Integer)
			return mkSmashedValue(intValue.evalNonNullConstant(constant, pp, oracle));
		else if (constant.getValue() instanceof String)
			return mkSmashedValue(stringValue.evalNonNullConstant(constant, pp, oracle));
		else if (constant.getValue() instanceof Boolean)
			return mkSmashedValue(boolValue.evalNonNullConstant(constant, pp, oracle));
		return top();
	}

	@Override
	public SmashedSum<I, S> lubAux(
			SmashedSum<I, S> other)
			throws SemanticException {
		return new SmashedSum<I, S>(intValue.lub(other.intValue), stringValue.lub(other.stringValue),
				boolValue.lub(other.boolValue));
	}

	@Override
	public SmashedSum<I, S> wideningAux(
			SmashedSum<I, S> other)
			throws SemanticException {
		return new SmashedSum<I, S>(intValue.widening(other.intValue), stringValue.widening(other.stringValue),
				boolValue.widening(other.boolValue));

	}

	@Override
	public boolean lessOrEqualAux(
			SmashedSum<I, S> other)
			throws SemanticException {
		return intValue.lessOrEqual(other.intValue) && stringValue.lessOrEqual(other.stringValue)
				&& boolValue.lessOrEqual(other.boolValue);
	}

	@Override
	public boolean isTop() {
		return intValue.isTop() && stringValue.isTop() && boolValue.isTop();
	}

	@Override
	public SmashedSum<I, S> top() {
		return new SmashedSum<I, S>(intValue.top(), stringValue.top(), boolValue.top());
	}

	@Override
	public boolean isBottom() {
		return intValue.isBottom() && stringValue.isBottom() && boolValue.isBottom();
	}

	@Override
	public SmashedSum<I, S> bottom() {
		return new SmashedSum<I, S>(intValue.bottom(), stringValue.bottom(), boolValue.bottom());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((intValue == null) ? 0 : intValue.hashCode());
		result = prime * result + ((stringValue == null) ? 0 : stringValue.hashCode());
		result = prime * result + ((boolValue == null) ? 0 : boolValue.hashCode());
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
		SmashedSum<?, ?> other = (SmashedSum<?, ?>) obj;
		if (intValue == null) {
			if (other.intValue != null)
				return false;
		} else if (!intValue.equals(other.intValue))
			return false;
		if (stringValue == null) {
			if (other.stringValue != null)
				return false;
		} else if (!stringValue.equals(other.stringValue))
			return false;
		if (boolValue != other.boolValue)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return representation().toString();
	}

	@Override
	public SmashedSum<I, S> evalUnaryExpression(
			UnaryOperator operator,
			SmashedSum<I, S> arg,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (operator == StringLength.INSTANCE && arg.isString())
			return mkSmashedValue(intValue.fromInterval(arg.stringValue.length()));
		if (operator == NumericNegation.INSTANCE && arg.isNumber())
			return mkSmashedValue(intValue.evalUnaryExpression(operator, arg.intValue, pp, oracle));
		if (operator == LogicalNegation.INSTANCE && arg.isBool())
			return mkSmashedValue(boolValue.evalUnaryExpression(operator, arg.boolValue, pp, oracle));
		return top();
	}

	@Override
	public SmashedSum<I, S> evalBinaryExpression(
			BinaryOperator operator,
			SmashedSum<I, S> left,
			SmashedSum<I, S> right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (operator instanceof NumericOperation && left.isNumber() && right.isNumber())
			return mkSmashedValue(
					intValue.evalBinaryExpression(operator, left.intValue, right.intValue, pp, oracle));
		if (operator instanceof LogicalOperation && left.isBool() && right.isBool())
			return mkSmashedValue(
					boolValue.evalBinaryExpression(operator, left.boolValue, right.boolValue, pp, oracle));
		if (operator instanceof NumericComparison && left.isNumber() && right.isNumber())
			return mkSmashedValue(
					intValue.satisfiesBinaryExpression(operator, left.intValue, right.intValue, pp, oracle));
		if (operator == ComparisonEq.INSTANCE || operator == ComparisonNe.INSTANCE)
			return mkSmashedValue(satisfiesBinaryExpression(operator, left, right, pp, oracle));
		if (operator == StringIndexOf.INSTANCE && left.isString() && right.isString())
			return mkSmashedValue(intValue.fromInterval(left.stringValue.indexOf(right.stringValue)));
		if (operator == StringConcat.INSTANCE && left.isString() && right.isString())
			return mkSmashedValue(
					stringValue.evalBinaryExpression(operator, left.stringValue, right.stringValue, pp, oracle));
		if (operator instanceof StringOperation && left.isString() && right.isString())
			return mkSmashedValue(
					stringValue.satisfiesBinaryExpression(operator, left.stringValue, right.stringValue, pp, oracle));
		return top();
	}

	@Override
	public SmashedSum<I, S> evalTernaryExpression(
			TernaryOperator operator,
			SmashedSum<I, S> left,
			SmashedSum<I, S> middle,
			SmashedSum<I, S> right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (operator == StringSubstring.INSTANCE && left.isString() && middle.isNumber() && right.isNumber()) {
			IntInterval begin = middle.intValue.toInterval();
			IntInterval end = right.intValue.toInterval();

			if (!begin.isFinite() || !end.isFinite())
				return mkSmashedValue(stringValue.top());

			S partial = stringValue.bottom();
			S temp;

			outer: for (long b : begin)
				if (b >= 0)
					for (long e : end) {
						if (b < e)
							temp = partial.lub(left.stringValue.substring(b, e));
						else if (b == e)
							temp = partial.lub(this.stringValue.evalNonNullConstant(
									new Constant(Untyped.INSTANCE, "", SyntheticLocation.INSTANCE),
									null,
									oracle));
						else
							temp = stringValue.bottom();

						if (temp.equals(partial))
							break outer;

						partial = temp;
						if (partial.isTop())
							break outer;
					}

			return mkSmashedValue(partial);
		} else if (operator == StringReplace.INSTANCE && left.isString() && middle.isString() && right.isString())
			return mkSmashedValue(stringValue.evalTernaryExpression(operator, left.stringValue, middle.stringValue,
					right.stringValue, pp, oracle));

		return top();
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(
			BinaryOperator operator,
			SmashedSum<I, S> left,
			SmashedSum<I, S> right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (operator instanceof StringOperation && left.isString() && right.isString())
			return stringValue.satisfiesBinaryExpression(operator, left.stringValue, right.stringValue, pp, oracle);
		if (operator instanceof NumericComparison && left.isNumber() && right.isNumber())
			return intValue.satisfiesBinaryExpression(operator, left.intValue, right.intValue, pp, oracle);
		if (operator instanceof LogicalOperation && left.isBool() && right.isBool())
			return boolValue.satisfiesBinaryExpression(operator, left.boolValue, right.boolValue, pp, oracle);
		if (operator == ComparisonEq.INSTANCE) {
			if (!left.sameKind(right))
				return Satisfiability.NOT_SATISFIED;
			if (left.isString())
				return stringValue.satisfiesBinaryExpression(operator, left.stringValue, right.stringValue, pp, oracle);
			if (left.isNumber())
				return intValue.satisfiesBinaryExpression(operator, left.intValue, right.intValue, pp, oracle);
			if (left.isBool())
				return boolValue.satisfiesBinaryExpression(operator, left.boolValue, right.boolValue, pp, oracle);
		}
		if (operator == ComparisonNe.INSTANCE) {
			if (!left.sameKind(right))
				return Satisfiability.SATISFIED;
			if (left.isString())
				return stringValue.satisfiesBinaryExpression(operator, left.stringValue, right.stringValue, pp, oracle);
			if (left.isNumber())
				return intValue.satisfiesBinaryExpression(operator, left.intValue, right.intValue, pp, oracle);
			if (left.isBool())
				return boolValue.satisfiesBinaryExpression(operator, left.boolValue, right.boolValue, pp, oracle);
		}
		return Satisfiability.UNKNOWN;
	}

	@Override
	public ValueEnvironment<SmashedSum<I, S>> assume(
			ValueEnvironment<SmashedSum<I, S>> environment,
			ValueExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		Satisfiability sat = satisfies(expression, environment, src, oracle);
		if (sat == Satisfiability.NOT_SATISFIED)
			return environment.bottom();
		if (sat == Satisfiability.SATISFIED)
			return environment;
		return BaseNonRelationalValueDomain.super.assume(environment, expression, src, dest, oracle);
	}

	@Override
	public StructuredRepresentation representation() {
		if (isBottom())
			return Lattice.bottomRepresentation();
		if (isTop())
			return Lattice.topRepresentation();
		if (isString())
			return stringValue.representation();
		if (isNumber())
			return intValue.representation();
		if (isBool())
			return boolValue.representation();
		return new StringRepresentation(
				"(" + intValue.representation().toString() + ", " + stringValue.representation().toString() + ", "
						+ boolValue.representation().toString() + ")");
	}

	private boolean sameKind(
			SmashedSum<I, S> other) {
		return (intValue.isBottom() == other.intValue.isBottom())
				&& (stringValue.isBottom() == other.stringValue.isBottom())
				&& (boolValue.isBottom() == other.boolValue.isBottom());
	}

	private boolean isNumber() {
		return isTop() || (!isBottom() && stringValue.isBottom() && boolValue.isBottom());
	}

	private boolean isString() {
		return isTop() || (!isBottom() && intValue.isBottom() && boolValue.isBottom());
	}

	private boolean isBool() {
		return isTop() || (!isBottom() && stringValue.isBottom() && intValue.isBottom());
	}

	private SmashedSum<I, S> mkSmashedValue(
			S stringValue) {
		return new SmashedSum<>(intValue.bottom(), stringValue, boolValue.bottom());
	}

	private SmashedSum<I, S> mkSmashedValue(
			I intValue) {
		return new SmashedSum<>(intValue, stringValue.bottom(), boolValue.bottom());
	}

	private SmashedSum<I, S> mkSmashedValue(
			Satisfiability boolValue) {
		return new SmashedSum<>(intValue.bottom(), stringValue.bottom(), boolValue);
	}
}