package it.unive.lisa.analysis.combination.smash;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.BooleanPowerset;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
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

/**
 * The smashed-sum abstract domain between {@link BooleanPowerset}, a
 * non-relational numeric abstract domain, and a non-relational string abstract
 * domain. This domains tracks environments of smashed values, which are values
 * of one of the types produced by the client domains.
 * 
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 *
 * @param <I> the non-relational integer abstract domain
 * @param <S> the non-relational string abstract domain
 */
public class SmashedSum<I extends Lattice<I>,
		S extends Lattice<S>>
		implements
		BaseNonRelationalValueDomain<SmashedValue<I, S>> {

	/**
	 * The integer domain.
	 */
	public final SmashedSumIntDomain<I> intDom;

	/**
	 * The string domain.
	 */
	public final SmashedSumStringDomain<S> strDom;

	/**
	 * The boolean domain.
	 */
	public final BooleanPowerset boolDom;

	/**
	 * Builds a smashed-sum abstract domain.
	 * 
	 * @param intDom the integer domain
	 * @param strDom the string domain
	 */
	public SmashedSum(
			SmashedSumIntDomain<I> intDom,
			SmashedSumStringDomain<S> strDom) {
		this.intDom = intDom;
		this.strDom = strDom;
		this.boolDom = new BooleanPowerset();
	}

	private SmashedValue<I, S> mkInt(
			I intDom) {
		return new SmashedValue<>(intDom, strDom.bottom(), boolDom.bottom());
	}

	private SmashedValue<I, S> mkString(
			S strDom) {
		return new SmashedValue<>(intDom.bottom(), strDom, boolDom.bottom());
	}

	private SmashedValue<I, S> mkBool(
			Satisfiability boolDom) {
		return new SmashedValue<>(intDom.bottom(), strDom.bottom(), boolDom);
	}

	@Override
	public SmashedValue<I, S> evalConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (constant.getValue() instanceof Integer)
			return mkInt(intDom.evalConstant(constant, pp, oracle));
		else if (constant.getValue() instanceof String)
			return mkString(strDom.evalConstant(constant, pp, oracle));
		else if (constant.getValue() instanceof Boolean)
			return mkBool(boolDom.evalConstant(constant, pp, oracle));
		return top();
	}

	@Override
	public SmashedValue<I, S> evalUnaryExpression(
			UnaryExpression expression,
			SmashedValue<I, S> arg,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		UnaryOperator operator = expression.getOperator();
		if (operator == StringLength.INSTANCE && arg.isString())
			return mkInt(intDom.fromInterval(strDom.length(arg.getStringValue())));
		if (operator == NumericNegation.INSTANCE && arg.isNumber())
			return mkInt(intDom.evalUnaryExpression(expression, arg.getIntValue(), pp, oracle));
		if (operator == LogicalNegation.INSTANCE && arg.isBool())
			return mkBool(boolDom.evalUnaryExpression(expression, arg.getBoolValue(), pp, oracle));
		return top();
	}

	@Override
	public SmashedValue<I, S> evalBinaryExpression(
			BinaryExpression expression,
			SmashedValue<I, S> left,
			SmashedValue<I, S> right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		BinaryOperator operator = expression.getOperator();
		if (operator instanceof NumericOperation && left.isNumber() && right.isNumber())
			return mkInt(intDom.evalBinaryExpression(expression, left.getIntValue(), right.getIntValue(), pp, oracle));
		if (operator instanceof LogicalOperation && left.isBool() && right.isBool())
			return mkBool(
					boolDom.evalBinaryExpression(expression, left.getBoolValue(), right.getBoolValue(), pp, oracle));
		if (operator instanceof NumericComparison && left.isNumber() && right.isNumber())
			return mkBool(
					intDom.satisfiesBinaryExpression(expression, left.getIntValue(), right.getIntValue(), pp, oracle));
		if (operator == ComparisonEq.INSTANCE || operator == ComparisonNe.INSTANCE)
			return mkBool(satisfiesBinaryExpression(expression, left, right, pp, oracle));
		if (operator == StringIndexOf.INSTANCE && left.isString() && right.isString())
			return mkInt(intDom.fromInterval(strDom.indexOf(left.getStringValue(), right.getStringValue())));
		if (operator == StringConcat.INSTANCE && left.isString() && right.isString())
			return mkString(
					strDom.evalBinaryExpression(expression, left.getStringValue(), right.getStringValue(), pp, oracle));
		if (operator instanceof StringOperation && left.isString() && right.isString())
			return mkBool(
					strDom
							.satisfiesBinaryExpression(expression, left.getStringValue(), right.getStringValue(), pp,
									oracle));
		return top();
	}

	@Override
	public SmashedValue<I, S> evalTernaryExpression(
			TernaryExpression expression,
			SmashedValue<I, S> left,
			SmashedValue<I, S> middle,
			SmashedValue<I, S> right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		TernaryOperator operator = expression.getOperator();
		if (operator == StringSubstring.INSTANCE && left.isString() && middle.isNumber() && right.isNumber()) {
			IntInterval begin = intDom.toInterval(middle.getIntValue());
			IntInterval end = intDom.toInterval(right.getIntValue());

			if (!begin.isFinite() || !end.isFinite())
				return mkString(strDom.top());

			S partial = strDom.bottom();
			S temp;

			outer: for (long b : begin)
				if (b >= 0)
					for (long e : end) {
						if (b < e)
							temp = partial.lub(strDom.substring(left.getStringValue(), b, e));
						else if (b == e)
							temp = partial.lub(
									this.strDom.evalConstant(
											new Constant(Untyped.INSTANCE, "", SyntheticLocation.INSTANCE),
											null,
											oracle));
						else
							temp = strDom.bottom();

						if (temp.equals(partial))
							break outer;

						partial = temp;
						if (partial.isTop())
							break outer;
					}

			return mkString(partial);
		} else if (operator == StringReplace.INSTANCE && left.isString() && middle.isString() && right.isString())
			return mkString(
					strDom.evalTernaryExpression(
							expression,
							left.getStringValue(),
							middle.getStringValue(),
							right.getStringValue(),
							pp,
							oracle));

		return top();
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(
			BinaryExpression expression,
			SmashedValue<I, S> left,
			SmashedValue<I, S> right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		BinaryOperator operator = expression.getOperator();
		if (operator instanceof StringOperation && left.isString() && right.isString())
			return strDom
					.satisfiesBinaryExpression(expression, left.getStringValue(), right.getStringValue(), pp, oracle);
		if (operator instanceof NumericComparison && left.isNumber() && right.isNumber())
			return intDom.satisfiesBinaryExpression(expression, left.getIntValue(), right.getIntValue(), pp, oracle);
		if (operator instanceof LogicalOperation && left.isBool() && right.isBool())
			return boolDom.satisfiesBinaryExpression(expression, left.getBoolValue(), right.getBoolValue(), pp, oracle);
		if (operator == ComparisonEq.INSTANCE) {
			if (!left.sameKind(right))
				return Satisfiability.NOT_SATISFIED;
			if (left.isString())
				return strDom
						.satisfiesBinaryExpression(expression, left.getStringValue(), right.getStringValue(), pp,
								oracle);
			if (left.isNumber())
				return intDom
						.satisfiesBinaryExpression(expression, left.getIntValue(), right.getIntValue(), pp, oracle);
			if (left.isBool())
				return boolDom
						.satisfiesBinaryExpression(expression, left.getBoolValue(), right.getBoolValue(), pp, oracle);
		}
		if (operator == ComparisonNe.INSTANCE) {
			if (!left.sameKind(right))
				return Satisfiability.SATISFIED;
			if (left.isString())
				return strDom
						.satisfiesBinaryExpression(expression, left.getStringValue(), right.getStringValue(), pp,
								oracle);
			if (left.isNumber())
				return intDom
						.satisfiesBinaryExpression(expression, left.getIntValue(), right.getIntValue(), pp, oracle);
			if (left.isBool())
				return boolDom
						.satisfiesBinaryExpression(expression, left.getBoolValue(), right.getBoolValue(), pp, oracle);
		}
		return Satisfiability.UNKNOWN;
	}

	@Override
	public ValueEnvironment<SmashedValue<I, S>> assume(
			ValueEnvironment<SmashedValue<I, S>> environment,
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
		return BaseNonRelationalValueDomain.super.assume(environment, expression, src, dest, oracle);
	}

	@Override
	public SmashedValue<I, S> top() {
		return new SmashedValue<>(intDom.top(), strDom.top(), boolDom.top());
	}

	@Override
	public SmashedValue<I, S> bottom() {
		return new SmashedValue<>(intDom.bottom(), strDom.bottom(), boolDom.bottom());
	}

	@Override
	public ValueEnvironment<SmashedValue<I, S>> makeLattice() {
		return new ValueEnvironment<>(top());
	}

}
