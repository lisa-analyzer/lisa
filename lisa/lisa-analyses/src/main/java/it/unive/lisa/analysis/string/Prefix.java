package it.unive.lisa.analysis.string;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.combination.smash.SmashedSumStringDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.numeric.Interval;
import it.unive.lisa.analysis.value.StringAbstraction;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.lattices.string.StrPrefix;
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
import it.unive.lisa.symbolic.value.operator.binary.StringCharAt;
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
import it.unive.lisa.symbolic.value.operator.binary.StringSubstringToEnd;
import it.unive.lisa.symbolic.value.operator.binary.ValueComparison;
import it.unive.lisa.symbolic.value.operator.ternary.StringIndexOfCharFromIndex;
import it.unive.lisa.symbolic.value.operator.ternary.StringIndexOfFromIndex;
import it.unive.lisa.symbolic.value.operator.ternary.StringLastIndexOfCharFromIndex;
import it.unive.lisa.symbolic.value.operator.ternary.StringLastIndexOfFromIndex;
import it.unive.lisa.symbolic.value.operator.ternary.StringReplace;
import it.unive.lisa.symbolic.value.operator.ternary.StringReplaceAll;
import it.unive.lisa.symbolic.value.operator.ternary.StringReplaceFirst;
import it.unive.lisa.symbolic.value.operator.ternary.StringStartsWithFromIndex;
import it.unive.lisa.symbolic.value.operator.ternary.StringSubstring;
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
import it.unive.lisa.util.numeric.MathNumberConversionException;
import java.util.Collections;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

/**
 * The prefix string abstract domain.
 *
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 * @author <a href="mailto:sergiosalvatore.evola@studenti.unipr.it">Sergio
 *             Salvatore Evola</a>
 * 
 * @see <a href=
 *          "https://link.springer.com/chapter/10.1007/978-3-642-24559-6_34">
 *          https://link.springer.com/chapter/10.1007/978-3-642-24559-6_34</a>
 */
public class Prefix
		implements
		StringAbstraction<ValueEnvironment<StrPrefix>>,
		SmashedSumStringDomain<StrPrefix> {

	/**
	 * The interval domain that we use to handle numerical constraints.
	 */
	private final Interval intDomain = new Interval();

	@Override
	public StrPrefix evalConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (constant.getValue() instanceof String) {
			String str = (String) constant.getValue();
			if (!str.isEmpty())
				return new StrPrefix(str);

		}

		return StrPrefix.TOP;
	}

	@Override
	public StrPrefix evalPushAny(
			PushAny pushAny,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (pushAny instanceof PushFromConstraints)
			return generate(((PushFromConstraints) pushAny).getConstraints(), pp, oracle);
		return SmashedSumStringDomain.super.evalPushAny(pushAny, pp, oracle);
	}

	@Override
	public StrPrefix evalUnaryExpression(
			UnaryExpression expression,
			StrPrefix arg,
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
			// reversing a prefix does not give any information
			return StrPrefix.TOP;
		else if (operator == StringToLowerCase.INSTANCE)
			return new StrPrefix(arg.prefix.toLowerCase());
		else if (operator == StringToUpperCase.INSTANCE)
			return new StrPrefix(arg.prefix.toUpperCase());
		else if (operator == StringTrim.INSTANCE)
			return new StrPrefix(StringUtils.stripStart(arg.prefix, null));

		return StrPrefix.TOP;
	}

	@Override
	public StrPrefix evalBinaryExpression(
			BinaryExpression expression,
			StrPrefix left,
			StrPrefix right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		BinaryOperator operator = expression.getOperator();

		if (left.isTop())
			return StrPrefix.TOP;

		if (oracle.hasWholeValueAnlysis()
				&& (operator == StringCharAt.INSTANCE
						|| operator == StringSubstringToEnd.INSTANCE)) {
			Set<BinaryExpression> constraints = oracle.constraints(this, (ValueExpression) expression.getRight(), pp);
			IntInterval val = intDomain.generate(constraints, pp, oracle);
			if (val.isBottom())
				return StrPrefix.BOTTOM;
			if (val.isTop())
				return StrPrefix.TOP;

			MathNumber len = new MathNumber(left.prefix.length());
			if (val.getHigh().compareTo(MathNumber.ZERO) < 0)
				// invalid range/position
				return StrPrefix.BOTTOM;
			else if (val.getLow().compareTo(len) > 0)
				// the start of the substring/char is after the prefix, so we
				// know nothing
				return StrPrefix.TOP;
			else {
				// there is some overlap between the prefix length and the
				// interval
				int low, high;
				if (val.getLow().compareTo(MathNumber.ZERO) < 0)
					// we ignore negative start values
					low = 0;
				else
					try {
						low = val.getLow().toInt();
					} catch (MathNumberConversionException e) {
						// this should never happen as it is greater than 0
						throw new SemanticException("Cannot convert string index to int", e);
					}
				if (val.getHigh().compareTo(len) > 0)
					// we ignore end values greater than the prefix length
					// as we do not have any information on those
					high = left.prefix.length();
				else
					try {
						high = val.getHigh().toInt();
					} catch (MathNumberConversionException e) {
						// this should never happen as it is less than the
						// prefix length
						throw new SemanticException("Cannot convert string index to int", e);
					}

				String common = null;
				for (int i = low; i <= high; i++) {
					String element;
					if (operator == StringCharAt.INSTANCE)
						element = String.valueOf(left.prefix.charAt(i));
					else // if (operator == StringSubstringToEnd.INSTANCE)
						element = left.prefix.substring(i);

					if (common == null)
						common = element;
					else
						common = StringUtils.getCommonPrefix(common, element);

					if (common.isEmpty())
						return StrPrefix.TOP;
				}

				if (common == null)
					return StrPrefix.TOP;
				return new StrPrefix(common);
			}
		}

		if (operator instanceof StringConcat)
			return left;

		return StrPrefix.TOP;

	}

	@Override
	public StrPrefix evalTernaryExpression(
			TernaryExpression expression,
			StrPrefix left,
			StrPrefix middle,
			StrPrefix right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		TernaryOperator operator = expression.getOperator();

		if (left.isTop())
			return StrPrefix.TOP;

		if (oracle.hasWholeValueAnlysis() && operator == StringSubstring.INSTANCE) {
			Set<BinaryExpression> cM = oracle.constraints(this, (ValueExpression) expression.getMiddle(), pp);
			IntInterval mid = intDomain.generate(cM, pp, oracle);
			Set<BinaryExpression> cR = oracle.constraints(this, (ValueExpression) expression.getRight(), pp);
			IntInterval rig = intDomain.generate(cR, pp, oracle);
			if (mid.isBottom() || rig.isBottom())
				return StrPrefix.BOTTOM;
			if (mid.isTop() || rig.isTop())
				return StrPrefix.TOP;

			MathNumber len = new MathNumber(left.prefix.length());
			if (mid.getLow().compareTo(rig.getHigh()) > 0
					|| mid.getHigh().compareTo(MathNumber.ZERO) < 0
					|| rig.getHigh().compareTo(MathNumber.ZERO) < 0)
				// invalid range/position
				return StrPrefix.BOTTOM;
			else if (mid.getLow().compareTo(len) > 0)
				// the start of the substring is after the prefix, so we
				// know nothing
				return StrPrefix.TOP;
			else {
				// there is some overlap between the prefix length and the
				// interval
				int mlow, mhigh;
				if (mid.getLow().compareTo(MathNumber.ZERO) < 0)
					// we ignore negative start values
					mlow = 0;
				else
					try {
						mlow = mid.getLow().toInt();
					} catch (MathNumberConversionException e) {
						// this should never happen as it is greater than 0
						throw new SemanticException("Cannot convert string index to int", e);
					}
				if (mid.getHigh().compareTo(len) > 0)
					// we ignore end values greater than the prefix length
					// as we do not have any information on those
					mhigh = left.prefix.length();
				else
					try {
						mhigh = mid.getHigh().toInt();
					} catch (MathNumberConversionException e) {
						// this should never happen as it is less than the
						// prefix length
						throw new SemanticException("Cannot convert string index to int", e);
					}

				int rlow, rhigh;
				if (rig.getLow().compareTo(MathNumber.ZERO) < 0)
					// we ignore negative start values
					rlow = 0;
				else
					try {
						rlow = rig.getLow().toInt();
					} catch (MathNumberConversionException e) {
						// this should never happen as it is greater than 0
						throw new SemanticException("Cannot convert string index to int", e);
					}
				if (rig.getHigh().compareTo(len) > 0)
					// we ignore end values greater than the prefix length
					// as we do not have any information on those
					rhigh = left.prefix.length();
				else
					try {
						rhigh = rig.getHigh().toInt();
					} catch (MathNumberConversionException e) {
						// this should never happen as it is less than the
						// prefix length
						throw new SemanticException("Cannot convert string index to int", e);
					}

				// by cutting the intervals short we might have made them empty
				if (mlow > mhigh)
					// low is the one we cut
					mhigh = mlow;
				if (rlow > rhigh)
					// high is the one we cut
					rlow = rhigh;

				String common = null;
				for (int i = mlow; i <= mhigh; i++)
					for (int j = rlow; j <= rhigh; j++)
						if (i <= j) {
							String element = left.prefix.substring(i, j);

							if (common == null)
								common = element;
							else
								common = StringUtils.getCommonPrefix(common, element);

							if (common.isEmpty())
								return StrPrefix.TOP;
						}

				if (common == null)
					return StrPrefix.TOP;
				return new StrPrefix(common);
			}
		}

		if (right.isTop() || middle.isTop())
			return StrPrefix.TOP;

		if (operator instanceof StringReplace
				|| operator == StringReplaceAll.INSTANCE
				|| operator == StringReplaceFirst.INSTANCE) {
			// we should keep the prefix of first from the beginning to the
			// first occurence of the second prefix, as the rest might be
			// replaced by the operation
			String pref = left.prefix.replaceFirst(middle.prefix + ".*", "");
			if (pref.isEmpty())
				return StrPrefix.TOP;
			else if (pref.equals(left.prefix))
				return left;
			else
				return new StrPrefix(pref);
		}

		return StrPrefix.TOP;
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(
			BinaryExpression expression,
			StrPrefix left,
			StrPrefix right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (left.isTop() || right.isTop())
			return Satisfiability.UNKNOWN;

		BinaryOperator operator = expression.getOperator();
		// most queries can be solved by checking the equality of the common
		// prefix with the shortest prefix: since the extra characters in the
		// longest one can be contained in remainder of the string with the
		// shortest prefix, what we must know is whether the beginning of
		// the two strings is the same
		String ll, rr;
		if (left.prefix.length() == right.prefix.length()) {
			ll = left.prefix;
			rr = right.prefix;
		} else if (left.prefix.length() > right.prefix.length()) {
			ll = left.prefix.substring(0, right.prefix.length());
			rr = right.prefix;
		} else {
			ll = left.prefix;
			rr = right.prefix.substring(0, left.prefix.length());
		}

		Boolean b;
		if (operator == ComparisonEq.INSTANCE)
			b = ll.equals(rr) ? null : false;
		else if (operator == ComparisonNe.INSTANCE)
			b = !ll.equals(rr) ? true : null;
		else if (operator == StringContains.INSTANCE)
			b = null; // we cannot be sure about containment
		else if (operator == StringEndsWith.INSTANCE)
			b = null; // we cannot be sure about suffixes
		else if (operator == StringEquals.INSTANCE)
			b = ll.equals(rr) ? null : false;
		else if (operator == StringEqualsIgnoreCase.INSTANCE)
			b = ll.toLowerCase().equals(rr.toLowerCase()) ? null : false;
		else if (operator == StringMatches.INSTANCE)
			b = null; // we cannot be sure about regexes
		else if (operator == StringStartsWith.INSTANCE)
			b = ll.equals(rr) ? null : false;
		else if (operator == StringIsPrefixOf.INSTANCE)
			b = rr.startsWith(ll) ? null : false;
		else if (operator == StringIsSuffixOf.INSTANCE)
			b = null; // we cannot be sure about suffixes
		else
			return Satisfiability.UNKNOWN;

		if (b == null)
			return Satisfiability.UNKNOWN;

		return b ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
	}

	@Override
	public Satisfiability satisfiesTernaryExpression(
			TernaryExpression expression,
			StrPrefix left,
			StrPrefix middle,
			StrPrefix right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (left.isTop() || middle.isTop())
			return Satisfiability.UNKNOWN;

		if (oracle.hasWholeValueAnlysis() && expression.getOperator() == StringStartsWithFromIndex.INSTANCE) {
			Set<BinaryExpression> constraints = oracle.constraints(this, (ValueExpression) expression.getRight(), pp);
			IntInterval val = intDomain.generate(constraints, pp, oracle);
			if (val.isBottom())
				return Satisfiability.BOTTOM;
			if (val.isTop())
				return Satisfiability.UNKNOWN;

			MathNumber len = new MathNumber(left.prefix.length());
			if (val.getHigh().compareTo(MathNumber.ZERO) < 0)
				// invalid range/position
				return Satisfiability.BOTTOM;
			else if (val.getLow().compareTo(len) > 0)
				// the start of the substring/char is after the prefix, so we
				// do not know anything
				return Satisfiability.UNKNOWN;
			else {
				// there is some overlap between the prefix length and the
				// interval
				int low, high;
				if (val.getLow().compareTo(MathNumber.ZERO) < 0)
					// we ignore negative start values
					low = 0;
				else
					try {
						low = val.getLow().toInt();
					} catch (MathNumberConversionException e) {
						// this should never happen as it is greater than 0
						throw new SemanticException("Cannot convert string index to int", e);
					}
				if (val.getHigh().compareTo(len) > 0)
					// we ignore end values greater than the prefix length
					// as we do not have any information on those
					high = left.prefix.length();
				else
					try {
						high = val.getHigh().toInt();
					} catch (MathNumberConversionException e) {
						// this should never happen as it is less than the
						// prefix length
						throw new SemanticException("Cannot convert string index to int", e);
					}

				boolean allTrue = true, allFalse = true;
				for (int i = low; i <= high; i++) {
					// we apply the same reasoning of startsWith while sliding
					// the start index
					String ll, rr;
					String pref = left.prefix.substring(i);
					if (pref.length() == right.prefix.length()) {
						ll = pref;
						rr = right.prefix;
					} else if (left.prefix.length() > right.prefix.length()) {
						ll = pref.substring(0, right.prefix.length());
						rr = right.prefix;
					} else {
						ll = pref;
						rr = right.prefix.substring(0, pref.length());
					}

					if (ll.equals(rr))
						allFalse = false;
					else
						allTrue = false;
				}

				if (allTrue)
					return Satisfiability.UNKNOWN;
				else if (allFalse)
					return Satisfiability.NOT_SATISFIED;
				else
					return Satisfiability.UNKNOWN;
			}
		}

		return Satisfiability.UNKNOWN;
	}

	@Override
	public ValueEnvironment<StrPrefix> assumeBinaryExpression(
			ValueEnvironment<StrPrefix> environment,
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
	public StrPrefix substring(
			StrPrefix s,
			long begin,
			long end) {
		if (s.isTop() || s.isBottom())
			return s;

		if (end <= s.prefix.length())
			return new StrPrefix(s.prefix.substring((int) begin, (int) end));
		else if (begin < s.prefix.length())
			return new StrPrefix(s.prefix.substring((int) begin));

		return StrPrefix.TOP;
	}

	@Override
	public IntInterval length(
			StrPrefix s) {
		return new IntInterval(new MathNumber(s.prefix.length()), MathNumber.PLUS_INFINITY);
	}

	@Override
	public IntInterval indexOf(
			StrPrefix current,
			StrPrefix other) {
		return new IntInterval(MathNumber.MINUS_ONE, MathNumber.PLUS_INFINITY);
	}

	@Override
	public Satisfiability containsChar(
			StrPrefix current,
			char c) {
		if (current.isTop())
			return Satisfiability.UNKNOWN;
		if (current.isBottom())
			return Satisfiability.BOTTOM;
		return current.prefix.contains(String.valueOf(c)) ? Satisfiability.SATISFIED : Satisfiability.UNKNOWN;
	}

	@Override
	public StrPrefix top() {
		return StrPrefix.TOP;
	}

	@Override
	public StrPrefix bottom() {
		return StrPrefix.BOTTOM;
	}

	@Override
	public Set<BinaryExpression> constraints(
			ValueDomain<?> requesting,
			ValueEnvironment<StrPrefix> state,
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
				StrPrefix value = eval(state, arg, pp, oracle);
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
						value.prefix.length(),
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
				StrPrefix left = eval(state, (ValueExpression) ((BinaryExpression) e).getLeft(), pp, oracle);
				StrPrefix right = eval(state, (ValueExpression) ((BinaryExpression) e).getRight(), pp, oracle);
				if (left.isTop() || right.isTop())
					return ValueDomain.makeRangeConstraints(
							pp.getProgram().getTypes().getIntegerType(),
							-1,
							1,
							e,
							pp);
				if (left.isBottom() || right.isBottom())
					return null;
				int i = left.prefix.compareTo(right.prefix);
				if (i != 0)
					return ValueDomain.makeEqConstraint(
							pp.getProgram().getTypes().getIntegerType(),
							i,
							e,
							pp);
				else
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

		StrPrefix value = eval(state, e, pp, oracle);
		if (value.isTop())
			return Collections.emptySet();
		if (value.isBottom())
			return null;
		return ValueDomain.makeConstraint(
				pp.getProgram().getTypes().getStringType(),
				value.prefix,
				StringIsPrefixOf.INSTANCE,
				e,
				pp);
	}

	private StrPrefix generate(
			Set<BinaryExpression> constraints,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (constraints == null)
			return StrPrefix.BOTTOM;

		for (BinaryExpression expr : constraints)
			if (expr.getLeft() instanceof Constant) {
				String val = ((Constant) expr.getLeft()).getValue().toString();
				if (expr.getOperator() instanceof ComparisonEq)
					return new StrPrefix(val);
			}

		return StrPrefix.TOP;
	}

}
