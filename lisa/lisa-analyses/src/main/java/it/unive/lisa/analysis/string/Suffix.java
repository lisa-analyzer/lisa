package it.unive.lisa.analysis.string;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.combination.smash.SmashedSumStringDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.value.StringAbstraction;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.lattices.string.StrSuffix;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.PushFromConstraints;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
import it.unive.lisa.symbolic.value.operator.binary.LogicalAnd;
import it.unive.lisa.symbolic.value.operator.binary.LogicalOr;
import it.unive.lisa.symbolic.value.operator.binary.StringConcat;
import it.unive.lisa.symbolic.value.operator.binary.StringContains;
import it.unive.lisa.symbolic.value.operator.binary.StringEndsWith;
import it.unive.lisa.symbolic.value.operator.binary.StringEquals;
import it.unive.lisa.symbolic.value.operator.binary.StringEqualsIgnoreCase;
import it.unive.lisa.symbolic.value.operator.binary.StringIndexOf;
import it.unive.lisa.symbolic.value.operator.binary.StringIndexOfChar;
import it.unive.lisa.symbolic.value.operator.binary.StringIsPrefixOf;
import it.unive.lisa.symbolic.value.operator.binary.StringIsSuffixOf;
import it.unive.lisa.symbolic.value.operator.binary.StringLastIndexOf;
import it.unive.lisa.symbolic.value.operator.binary.StringLastIndexOfChar;
import it.unive.lisa.symbolic.value.operator.binary.StringMatches;
import it.unive.lisa.symbolic.value.operator.binary.StringStartsWith;
import it.unive.lisa.symbolic.value.operator.binary.ValueComparison;
import it.unive.lisa.symbolic.value.operator.ternary.StringIndexOfCharFromIndex;
import it.unive.lisa.symbolic.value.operator.ternary.StringIndexOfFromIndex;
import it.unive.lisa.symbolic.value.operator.ternary.StringLastIndexOfCharFromIndex;
import it.unive.lisa.symbolic.value.operator.ternary.StringLastIndexOfFromIndex;
import it.unive.lisa.symbolic.value.operator.ternary.StringStartsWithFromIndex;
import it.unive.lisa.symbolic.value.operator.ternary.TernaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.symbolic.value.operator.unary.NumericToString;
import it.unive.lisa.symbolic.value.operator.unary.StringLength;
import it.unive.lisa.symbolic.value.operator.unary.StringReverse;
import it.unive.lisa.symbolic.value.operator.unary.StringToLowerCase;
import it.unive.lisa.symbolic.value.operator.unary.StringToUpperCase;
import it.unive.lisa.symbolic.value.operator.unary.StringTrim;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import java.util.Collections;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

/**
 * The suffix string abstract domain.
 *
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 * @author <a href="mailto:sergiosalvatore.evola@studenti.unipr.it">Sergio
 *             Salvatore Evola</a>
 * 
 * @see <a href=
 *          "https://link.springer.com/chapter/10.1007/978-3-642-24559-6_34">
 *          https://link.springer.com/chapter/10.1007/978-3-642-24559-6_34</a>
 */
public class Suffix
		implements
		StringAbstraction<ValueEnvironment<StrSuffix>>,
		SmashedSumStringDomain<StrSuffix> {

	@Override
	public StrSuffix evalConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (constant.getValue() instanceof String) {
			String str = (String) constant.getValue();
			if (!str.isEmpty())
				return new StrSuffix(str);
		}

		return StrSuffix.TOP;
	}

	@Override
	public StrSuffix evalPushAny(
			PushAny pushAny,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (pushAny instanceof PushFromConstraints)
			return generate(((PushFromConstraints) pushAny).getConstraints(), pp, oracle);
		return SmashedSumStringDomain.super.evalPushAny(pushAny, pp, oracle);
	}

	@Override
	public StrSuffix evalUnaryExpression(
			UnaryExpression expression,
			StrSuffix arg,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		UnaryOperator operator = expression.getOperator();

		if (oracle.hasWholeValueAnlysis() && operator == NumericToString.INSTANCE) {
			Set<BinaryExpression> constraints = oracle.constraints(this, expression, pp);
			return generate(constraints, pp, oracle);
		}

		if (arg.isTop())
			return top();

		if (operator == StringReverse.INSTANCE)
			return StrSuffix.TOP; // we would need prefix information for this
		else if (operator == StringToLowerCase.INSTANCE)
			return new StrSuffix(arg.suffix.toLowerCase());
		else if (operator == StringToUpperCase.INSTANCE)
			return new StrSuffix(arg.suffix.toUpperCase());
		else if (operator == StringTrim.INSTANCE)
			return new StrSuffix(StringUtils.stripEnd(arg.suffix, null));

		return StrSuffix.TOP;
	}

	@Override
	public StrSuffix evalBinaryExpression(
			BinaryExpression expression,
			StrSuffix left,
			StrSuffix right,
			ProgramPoint pp,
			SemanticOracle oracle) {
		// we do not exploit the whole value analysis here, as not knowing how
		// many characters preceed the suffix prevents any meaningful reasoning
		// and would always make us go to top

		BinaryOperator operator = expression.getOperator();
		if (operator == StringConcat.INSTANCE)
			return right;

		return StrSuffix.TOP;
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(
			BinaryExpression expression,
			StrSuffix left,
			StrSuffix right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (left.isTop() || right.isTop())
			return Satisfiability.UNKNOWN;

		BinaryOperator operator = expression.getOperator();
		// most queries can be solved by checking the equality of the common
		// suffix with the shortest suffix: since the extra characters in the
		// longest one can be contained in remainder of the string with the
		// shortest suffix, what we must know is whether the end of
		// the two strings is the same
		String ll, rr;
		if (left.suffix.length() == right.suffix.length()) {
			ll = left.suffix;
			rr = right.suffix;
		} else if (left.suffix.length() > right.suffix.length()) {
			ll = left.suffix.substring(left.suffix.length() - right.suffix.length());
			rr = right.suffix;
		} else {
			ll = left.suffix;
			rr = right.suffix.substring(right.suffix.length() - left.suffix.length());
		}

		Boolean b;
		if (operator == ComparisonEq.INSTANCE)
			b = ll.equals(rr) ? null : false;
		else if (operator == ComparisonNe.INSTANCE)
			b = !ll.equals(rr) ? true : null;
		else if (operator == StringContains.INSTANCE)
			b = null; // we cannot be sure about containment
		else if (operator == StringEndsWith.INSTANCE)
			b = ll.endsWith(rr) ? null : false;
		else if (operator == StringEquals.INSTANCE)
			b = ll.equals(rr) ? null : false;
		else if (operator == StringEqualsIgnoreCase.INSTANCE)
			b = ll.toLowerCase().equals(rr.toLowerCase()) ? null : false;
		else if (operator == StringMatches.INSTANCE)
			b = null; // we cannot be sure about regexes
		else if (operator == StringStartsWith.INSTANCE)
			b = null; // we cannot be sure about prefixes
		else if (operator == StringIsPrefixOf.INSTANCE)
			b = null; // we cannot be sure about prefixes
		else if (operator == StringIsSuffixOf.INSTANCE)
			b = ll.equals(rr) ? null : false;
		else
			return Satisfiability.UNKNOWN;

		if (b == null)
			return Satisfiability.UNKNOWN;

		return b ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
	}

	@Override
	public ValueEnvironment<StrSuffix> assumeBinaryExpression(
			ValueEnvironment<StrSuffix> environment,
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

		// we keep it simple: we only go to bottom if we cannot prove that the
		// condition is not satisfied, otherwise we do not change the
		// environment
		return environment;
	}

	@Override
	public StrSuffix substring(
			StrSuffix current,
			long begin,
			long end) {
		return StrSuffix.TOP;
	}

	@Override
	public IntInterval length(
			StrSuffix current) {
		return new IntInterval(new MathNumber(current.suffix.length()), MathNumber.PLUS_INFINITY);
	}

	@Override
	public IntInterval indexOf(
			StrSuffix current,
			StrSuffix other) {
		return new IntInterval(MathNumber.MINUS_ONE, MathNumber.PLUS_INFINITY);
	}

	@Override
	public Satisfiability containsChar(
			StrSuffix current,
			char c) {
		if (current.isTop())
			return Satisfiability.UNKNOWN;
		if (current.isBottom())
			return Satisfiability.BOTTOM;
		return current.suffix.contains(String.valueOf(c)) ? Satisfiability.SATISFIED : Satisfiability.UNKNOWN;
	}

	@Override
	public StrSuffix top() {
		return StrSuffix.TOP;
	}

	@Override
	public StrSuffix bottom() {
		return StrSuffix.BOTTOM;
	}

	@Override
	public Set<BinaryExpression> constraints(
			ValueDomain<?> requesting,
			ValueEnvironment<StrSuffix> state,
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
			if (operator == StringLength.INSTANCE) {
				ValueExpression arg = (ValueExpression) ((UnaryExpression) e).getExpression();
				StrSuffix value = eval(state, arg, pp, oracle);
				if (value.isTop())
					return ValueDomain.makeRangeConstraints(
							pp.getProgram().getTypes().getIntegerType(),
							0,
							null,
							e,
							pp);
				if (value.isBottom())
					return null;
				return ValueDomain.makeRangeConstraints(
						pp.getProgram().getTypes().getIntegerType(),
						value.suffix.length(),
						null,
						e,
						pp);
			}
		}

		if (e instanceof BinaryExpression) {
			BinaryOperator operator = ((BinaryExpression) e).getOperator();
			if (operator == ComparisonEq.INSTANCE
					|| operator == ComparisonNe.INSTANCE
					|| operator == StringContains.INSTANCE
					|| operator == StringEndsWith.INSTANCE
					|| operator == StringEquals.INSTANCE
					|| operator == StringEqualsIgnoreCase.INSTANCE
					|| operator == StringMatches.INSTANCE
					|| operator == StringStartsWith.INSTANCE
					|| operator == StringIsPrefixOf.INSTANCE
					|| operator == StringIsSuffixOf.INSTANCE) {
				Satisfiability sat = satisfies(state, e, pp, oracle);
				if (sat == Satisfiability.SATISFIED)
					return ValueDomain.makeEqConstraint(pp.getProgram().getTypes().getBooleanType(), true, e, pp);
				else if (sat == Satisfiability.NOT_SATISFIED)
					return ValueDomain.makeEqConstraint(pp.getProgram().getTypes().getBooleanType(), false, e, pp);
				else if (sat == Satisfiability.UNKNOWN)
					return Collections.emptySet();
				else
					return null;
			} else if (operator == StringIndexOfChar.INSTANCE
					|| operator == StringLastIndexOfChar.INSTANCE
					|| operator == StringIndexOf.INSTANCE
					|| operator == StringLastIndexOf.INSTANCE)
				return ValueDomain.makeRangeConstraints(
						pp.getProgram().getTypes().getIntegerType(),
						-1,
						null,
						e,
						pp);
			else if (operator == ValueComparison.INSTANCE) {
				StrSuffix left = eval(state, (ValueExpression) ((BinaryExpression) e).getLeft(), pp, oracle);
				StrSuffix right = eval(state, (ValueExpression) ((BinaryExpression) e).getRight(), pp, oracle);
				if (left.isBottom() || right.isBottom())
					return null;
				return ValueDomain.makeRangeConstraints(
						pp.getProgram().getTypes().getIntegerType(),
						-1,
						1,
						e,
						pp);
			}
		}

		if (e instanceof TernaryExpression) {
			TernaryOperator operator = ((TernaryExpression) e).getOperator();
			if (operator == StringIndexOfCharFromIndex.INSTANCE
					|| operator == StringIndexOfFromIndex.INSTANCE
					|| operator == StringLastIndexOfCharFromIndex.INSTANCE
					|| operator == StringLastIndexOfFromIndex.INSTANCE)
				return ValueDomain.makeRangeConstraints(
						pp.getProgram().getTypes().getIntegerType(),
						-1,
						null,
						e,
						pp);
			else if (operator == StringStartsWithFromIndex.INSTANCE) {
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

		StrSuffix value = eval(state, e, pp, oracle);
		if (value.isTop())
			return Collections.emptySet();
		if (value.isBottom())
			return null;
		return ValueDomain.makeConstraint(
				pp.getProgram().getTypes().getStringType(),
				value.suffix,
				StringIsSuffixOf.INSTANCE,
				e,
				pp);
	}

	private StrSuffix generate(
			Set<BinaryExpression> constraints,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (constraints == null)
			return bottom();

		for (BinaryExpression expr : constraints)
			if ((expr.getOperator() instanceof ComparisonEq || expr.getOperator() instanceof StringEndsWith)
					&& expr.getLeft() instanceof Constant
					&& ((Constant) expr.getLeft()).getValue() instanceof String)
				return new StrSuffix(((Constant) expr.getLeft()).getValue().toString());

		return StrSuffix.TOP;
	}

}
