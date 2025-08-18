package it.unive.lisa.analysis.combination.constraints;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
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
import java.util.Set;

/**
 * The constraint-based whole-value analysis between a non-relational Boolean
 * abstract domain, a non-relational numeric abstract domain, and a
 * non-relational string abstract domain. This domains tracks environments of
 * whole-value elements, which are values of one of the types produced by the
 * client domains.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 *
 * @param <N> the non-relational integer abstract domain
 * @param <S> the non-relational string abstract domain
 * @param <B> the non-relational Boolean abstract domain
 */
public class WholeValueAnalysis<N extends WholeValueElement<N>,
		S extends WholeValueElement<S>,
		B extends WholeValueElement<B>> implements BaseNonRelationalValueDomain<WholeValue<N, S, B>> {

	/**
	 * The non-relational integer abstract domain.
	 */
	public final BaseNonRelationalValueDomain<N> intDom;

	/**
	 * The non-relational string abstract domain.
	 */
	public final WholeValueStringDomain<S> strDom;

	/**
	 * The non-relational Boolean abstract domain.
	 */
	public final BaseNonRelationalValueDomain<B> boolDom;

	/**
	 * Builds a whole-value analysis.
	 * 
	 * @param intDom  the non-relational integer abstract domain
	 * @param strDom  the non-relational string abstract domain
	 * @param boolDom the non-relational Boolean abstract domain
	 */
	public WholeValueAnalysis(
			BaseNonRelationalValueDomain<N> intDom,
			WholeValueStringDomain<S> strDom,
			BaseNonRelationalValueDomain<B> boolDom) {
		this.intDom = intDom;
		this.strDom = strDom;
		this.boolDom = boolDom;
	}

	private WholeValue<N, S, B> mkInt(
			N intValue) {
		return new WholeValue<>(intValue, strDom.bottom(), boolDom.bottom());
	}

	private WholeValue<N, S, B> mkString(
			S stringValue) {
		return new WholeValue<>(intDom.bottom(), stringValue, boolDom.bottom());
	}

	private WholeValue<N, S, B> mkBool(
			B boolValue) {
		return new WholeValue<>(intDom.bottom(), strDom.bottom(), boolValue);
	}

	@Override
	public WholeValue<N, S, B> evalNonNullConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (constant.getValue() instanceof Integer)
			return mkInt(intDom.evalNonNullConstant(constant, pp, oracle));
		else if (constant.getValue() instanceof String)
			return mkString(strDom.evalNonNullConstant(constant, pp, oracle));
		else if (constant.getValue() instanceof Boolean)
			return mkBool(boolDom.evalNonNullConstant(constant, pp, oracle));
		return top();
	}

	@Override
	public WholeValue<N, S, B> evalUnaryExpression(
			UnaryExpression expression,
			WholeValue<N, S, B> arg,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		UnaryOperator operator = expression.getOperator();
		if (operator == StringLength.INSTANCE && arg.isString())
			return mkInt(
				arg.getIntValue()
					.generate(arg.getStringValue().constraints((ValueExpression) expression.getExpression(), pp), pp));
		if (operator == NumericNegation.INSTANCE && arg.isNumber())
			return mkInt(intDom.evalUnaryExpression(expression, arg.getIntValue(), pp, oracle));
		if (operator == LogicalNegation.INSTANCE && arg.isBool())
			return mkBool(boolDom.evalUnaryExpression(expression, arg.getBoolValue(), pp, oracle));
		return top();
	}

	@Override
	public WholeValue<N, S, B> evalBinaryExpression(
			BinaryExpression expression,
			WholeValue<N, S, B> left,
			WholeValue<N, S, B> right,
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
				left.getBoolValue()
					.generate(
						intDom
							.satisfiesBinaryExpression(expression, left.getIntValue(), right.getIntValue(), pp, oracle)
							.constraints(expression, pp),
						pp));
		if (operator == ComparisonEq.INSTANCE || operator == ComparisonNe.INSTANCE)
			return mkBool(
				left.getBoolValue()
					.generate(
						satisfiesBinaryExpression(expression, left, right, pp, oracle).constraints(expression, pp),
						pp));
		if (operator == StringIndexOf.INSTANCE && left.isString() && right.isString())
			return mkInt(
				left.getIntValue()
					.generate(
						strDom.indexOf_constr(expression, left.getStringValue(), right.getStringValue(), pp),
						pp));
		if (operator == StringConcat.INSTANCE && left.isString() && right.isString())
			return mkString(
				strDom.evalBinaryExpression(expression, left.getStringValue(), right.getStringValue(), pp, oracle));
		if (operator instanceof StringOperation && left.isString() && right.isString())
			return mkBool(
				left.getBoolValue()
					.generate(
						strDom
							.satisfiesBinaryExpression(
								expression,
								left.getStringValue(),
								right.getStringValue(),
								pp,
								oracle)
							.constraints(expression, pp),
						pp));
		return top();
	}

	@Override
	public WholeValue<N, S, B> evalTernaryExpression(
			TernaryExpression expression,
			WholeValue<N, S, B> left,
			WholeValue<N, S, B> middle,
			WholeValue<N, S, B> right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		TernaryOperator operator = expression.getOperator();
		if (operator == StringSubstring.INSTANCE && left.isString() && middle.isNumber() && right.isNumber()) {
			Set<BinaryExpression> begin = middle.getIntValue().constraints((ValueExpression) expression.getLeft(), pp);
			Set<BinaryExpression> end = right.getIntValue().constraints((ValueExpression) expression.getRight(), pp);
			return mkString(strDom.substring(left.getStringValue(), begin, end, pp));
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
			WholeValue<N, S, B> left,
			WholeValue<N, S, B> right,
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
					.satisfiesBinaryExpression(expression, left.getStringValue(), right.getStringValue(), pp, oracle);
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
					.satisfiesBinaryExpression(expression, left.getStringValue(), right.getStringValue(), pp, oracle);
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
	public ValueEnvironment<WholeValue<N, S, B>> assume(
			ValueEnvironment<WholeValue<N, S, B>> environment,
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
	public WholeValue<N, S, B> top() {
		return new WholeValue<>(intDom.top(), strDom.top(), boolDom.top());
	}

	@Override
	public WholeValue<N, S, B> bottom() {
		return new WholeValue<>(intDom.bottom(), strDom.bottom(), boolDom.bottom());
	}

	@Override
	public ValueEnvironment<WholeValue<N, S, B>> makeLattice() {
		return new ValueEnvironment<>(top());
	}

}
