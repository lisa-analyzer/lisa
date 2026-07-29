package it.unive.lisa.analysis.string;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.combination.smash.SmashedSumStringDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.numeric.IntegerConstantPropagation;
import it.unive.lisa.analysis.value.StringAbstraction;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.lattices.numeric.IntegerConstant;
import it.unive.lisa.lattices.string.StringConstant;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
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
import java.util.Collections;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

/**
 * The string constant propagation abstract domain, tracking if a certain string
 * value has constant value or not. Top and bottom cases for least upper bounds,
 * widening and less or equals operations are handled by {@link BaseLattice} in
 * {@link BaseLattice#lub}, {@link BaseLattice#widening} and
 * {@link BaseLattice#lessOrEqual}, respectively.
 * 
 * @author <a href="mailto:michele.martelli1@studenti.unipr.it">Michele
 *             Martelli</a>
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
public class StringConstantPropagation
		implements
		StringAbstraction<ValueEnvironment<StringConstant>>,
		SmashedSumStringDomain<StringConstant> {

	/**
	 * The integer domain that we use to process numerical constraints.
	 */
	private final IntegerConstantPropagation intDomain = new IntegerConstantPropagation();

	@Override
	public StringConstant evalConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (constant.getValue() instanceof String)
			return new StringConstant((String) constant.getValue());

		return StringConstant.TOP;
	}

	@Override
	public StringConstant evalPushAny(
			PushAny pushAny,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (pushAny instanceof PushFromConstraints)
			return generate(((PushFromConstraints) pushAny).getConstraints(), pp, oracle);
		return SmashedSumStringDomain.super.evalPushAny(pushAny, pp, oracle);
	}

	@Override
	public StringConstant evalUnaryExpression(
			UnaryExpression expression,
			StringConstant arg,
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
			return new StringConstant(StringUtils.reverse(arg.value));
		else if (operator == StringToLowerCase.INSTANCE)
			return new StringConstant(arg.value.toLowerCase());
		else if (operator == StringToUpperCase.INSTANCE)
			return new StringConstant(arg.value.toUpperCase());
		else if (operator == StringTrim.INSTANCE)
			return new StringConstant(arg.value.trim());

		return StringConstant.TOP;
	}

	@Override
	public StringConstant evalBinaryExpression(
			BinaryExpression expression,
			StringConstant left,
			StringConstant right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		BinaryOperator operator = expression.getOperator();

		if (left.isTop())
			return StringConstant.TOP;

		if (oracle.hasWholeValueAnlysis()
				&& (operator == StringCharAt.INSTANCE
						|| operator == StringSubstringToEnd.INSTANCE)) {
			Set<BinaryExpression> constraints = oracle.constraints(this, (ValueExpression) expression.getRight(), pp);
			IntegerConstant val = intDomain.generate(constraints, pp, oracle);
			if (val.isBottom())
				return StringConstant.BOTTOM;
			if (val.isTop())
				return StringConstant.TOP;
			if (operator == StringCharAt.INSTANCE)
				return new StringConstant(String.valueOf(left.value.charAt(val.value)));
			if (operator == StringSubstringToEnd.INSTANCE)
				return new StringConstant(left.value.substring(val.value));
		}

		if (right.isTop())
			return StringConstant.TOP;

		if (operator instanceof StringConcat)
			return new StringConstant(left.value + right.value);

		return StringConstant.TOP;
	}

	@Override
	public StringConstant evalTernaryExpression(
			TernaryExpression expression,
			StringConstant left,
			StringConstant middle,
			StringConstant right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		TernaryOperator operator = expression.getOperator();

		if (left.isTop())
			return StringConstant.TOP;

		if (oracle.hasWholeValueAnlysis() && operator == StringSubstring.INSTANCE) {
			Set<BinaryExpression> cM = oracle.constraints(this, (ValueExpression) expression.getMiddle(), pp);
			IntegerConstant mid = intDomain.generate(cM, pp, oracle);
			Set<BinaryExpression> cR = oracle.constraints(this, (ValueExpression) expression.getRight(), pp);
			IntegerConstant rig = intDomain.generate(cR, pp, oracle);
			if (mid.isBottom() || rig.isBottom())
				return StringConstant.BOTTOM;
			if (mid.isTop() || rig.isTop())
				return StringConstant.TOP;
			return new StringConstant(left.value.substring(mid.value, rig.value));
		}

		if (right.isTop() || middle.isTop())
			return StringConstant.TOP;

		if (operator instanceof StringReplace)
			return new StringConstant(left.value.replace(middle.value, right.value));
		if (operator == StringReplaceAll.INSTANCE)
			return new StringConstant(left.value.replaceAll(middle.value, right.value));
		if (operator == StringReplaceFirst.INSTANCE)
			return new StringConstant(left.value.replaceFirst(middle.value, right.value));

		return StringConstant.TOP;
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(
			BinaryExpression expression,
			StringConstant left,
			StringConstant right,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (left.isTop() || right.isTop())
			return Satisfiability.UNKNOWN;

		BinaryOperator operator = expression.getOperator();
		boolean b;
		if (operator == ComparisonEq.INSTANCE)
			b = left.value.equals(right.value);
		else if (operator == ComparisonNe.INSTANCE)
			b = !left.value.equals(right.value);
		else if (operator == StringContains.INSTANCE)
			b = left.value.contains(right.value);
		else if (operator == StringEndsWith.INSTANCE)
			b = left.value.endsWith(right.value);
		else if (operator == StringEquals.INSTANCE)
			b = left.value.equals(right.value);
		else if (operator == StringEqualsIgnoreCase.INSTANCE)
			b = left.value.equalsIgnoreCase(right.value);
		else if (operator == StringMatches.INSTANCE)
			b = left.value.matches(right.value);
		else if (operator == StringStartsWith.INSTANCE)
			b = left.value.startsWith(right.value);
		else if (operator == StringIsPrefixOf.INSTANCE)
			b = right.value.startsWith(left.value);
		else if (operator == StringIsSuffixOf.INSTANCE)
			b = right.value.endsWith(left.value);
		else
			return Satisfiability.UNKNOWN;

		return Satisfiability.fromBoolean(b);
	}

	@Override
	public Satisfiability satisfiesTernaryExpression(
			TernaryExpression expression,
			StringConstant left,
			StringConstant middle,
			StringConstant right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (left.isTop() || middle.isTop())
			return Satisfiability.UNKNOWN;

		if (oracle.hasWholeValueAnlysis() && expression.getOperator() == StringStartsWithFromIndex.INSTANCE) {
			Set<BinaryExpression> constraints = oracle.constraints(this, (ValueExpression) expression.getRight(), pp);
			IntegerConstant val = intDomain.generate(constraints, pp, oracle);
			if (val.isBottom())
				return Satisfiability.BOTTOM;
			if (val.isTop())
				return Satisfiability.UNKNOWN;
			boolean b = left.value.startsWith(middle.value, val.value);
			return b ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
		}

		return Satisfiability.UNKNOWN;
	}

	@Override
	public ValueEnvironment<StringConstant> assumeBinaryExpression(
			ValueEnvironment<StringConstant> environment,
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
		if (operator == ComparisonEq.INSTANCE) {
			if (left instanceof Identifier) {
				if (!canProcess(right, src, oracle))
					// the expression does not have a string value, we do not
					// assume anything on it
					return environment;
				StringConstant eval = eval(environment, right, src, oracle);
				if (eval.isBottom())
					return environment.bottom();
				// If eval is TOP, the rhs is unknown. Any abstract value of lhs
				// satisfies lhs == TOP, so the lhs abstract value is preserved
				// and no refinement is needed.
				if (!eval.isTop())
					return environment.putState((Identifier) left, eval);
			} else if (right instanceof Identifier) {
				if (!canProcess(left, src, oracle))
					// the expression does not have a string value, we do not
					// assume anything on it
					return environment;
				StringConstant eval = eval(environment, left, src, oracle);
				if (eval.isBottom())
					return environment.bottom();
				// Same reasoning as above, symmetric case.
				if (!eval.isTop())
					return environment.putState((Identifier) right, eval);
			}
		}
		return environment;
	}

	@Override
	public StringConstant top() {
		return StringConstant.TOP;
	}

	@Override
	public StringConstant bottom() {
		return StringConstant.BOTTOM;
	}

	private StringConstant generate(
			Set<BinaryExpression> constraints,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (constraints == null)
			return StringConstant.BOTTOM;

		for (BinaryExpression expr : constraints)
			if (expr.getLeft() instanceof Constant) {
				String val = ((Constant) expr.getLeft()).getValue().toString();
				if (expr.getOperator() instanceof ComparisonEq)
					return new StringConstant(val);
			}

		return StringConstant.TOP;
	}

	@Override
	public Satisfiability containsChar(
			StringConstant current,
			char c)
			throws SemanticException {
		if (current.isBottom())
			return Satisfiability.BOTTOM;
		if (current.isTop())
			return Satisfiability.UNKNOWN;
		return current.value.indexOf(c) >= 0 ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
	}

	@Override
	public IntInterval length(
			StringConstant current)
			throws SemanticException {
		if (current.isBottom())
			return IntInterval.BOTTOM;
		if (current.isTop())
			return IntInterval.TOP;
		int l = current.value.length();
		return new IntInterval(l, l);
	}

	@Override
	public IntInterval indexOf(
			StringConstant current,
			StringConstant s)
			throws SemanticException {
		if (current.isBottom() || s.isBottom())
			return IntInterval.BOTTOM;
		if (current.isTop() || s.isTop())
			return IntInterval.TOP;
		int i = current.value.indexOf(s.value);
		return new IntInterval(i, i);
	}

	@Override
	public StringConstant substring(
			StringConstant current,
			long begin,
			long end)
			throws SemanticException {
		if (current.isBottom())
			return StringConstant.BOTTOM;
		if (current.isTop())
			return StringConstant.TOP;
		if (begin < 0 || end > current.value.length() || begin > end)
			return StringConstant.BOTTOM;
		return new StringConstant(current.value.substring((int) begin, (int) end));
	}

	@Override
	public Set<BinaryExpression> constraints(
			ValueDomain<?> requesting,
			ValueEnvironment<StringConstant> state,
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
				StringConstant value = eval(state, arg, pp, oracle);
				if (value.isTop())
					return ValueDomain.makeRangeConstraints(
							pp.getProgram().getTypes().getIntegerType(),
							0,
							null,
							e,
							pp);
				if (value.isBottom())
					return null;
				return ValueDomain.makeEqConstraint(
						pp.getProgram().getTypes().getIntegerType(),
						value.value.length(),
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
					|| operator == StringLastIndexOf.INSTANCE
					|| operator == ValueComparison.INSTANCE) {
				StringConstant left = eval(state, (ValueExpression) ((BinaryExpression) e).getLeft(), pp, oracle);
				StringConstant right = eval(state, (ValueExpression) ((BinaryExpression) e).getRight(), pp, oracle);
				if (left.isTop() || right.isTop())
					if (operator == ValueComparison.INSTANCE)
						return ValueDomain.makeRangeConstraints(
								pp.getProgram().getTypes().getIntegerType(),
								-1,
								1,
								e,
								pp);
					else
						return ValueDomain.makeRangeConstraints(
								pp.getProgram().getTypes().getIntegerType(),
								-1,
								null,
								e,
								pp);
				if (left.isBottom() || right.isBottom())
					return null;
				int i;
				if (operator == StringIndexOfChar.INSTANCE)
					i = left.value.indexOf(right.value.charAt(0));
				else if (operator == StringLastIndexOfChar.INSTANCE)
					i = left.value.lastIndexOf(right.value.charAt(0));
				else if (operator == StringIndexOf.INSTANCE)
					i = left.value.indexOf(right.value);
				else if (operator == StringLastIndexOf.INSTANCE)
					i = left.value.lastIndexOf(right.value);
				else // operator == ValueComparison.INSTANCE
					i = left.value.compareTo(right.value);
				return ValueDomain.makeEqConstraint(
						pp.getProgram().getTypes().getIntegerType(),
						i,
						e,
						pp);
			}
		}

		if (e instanceof TernaryExpression) {
			TernaryOperator operator = ((TernaryExpression) e).getOperator();
			if (operator == StringIndexOfCharFromIndex.INSTANCE
					|| operator == StringIndexOfFromIndex.INSTANCE
					|| operator == StringLastIndexOfCharFromIndex.INSTANCE
					|| operator == StringLastIndexOfFromIndex.INSTANCE) {
				StringConstant left = eval(state, (ValueExpression) ((TernaryExpression) e).getLeft(), pp, oracle);
				StringConstant middle = eval(state, (ValueExpression) ((TernaryExpression) e).getMiddle(), pp, oracle);
				Set<BinaryExpression> constraints = oracle.constraints(
						this,
						(ValueExpression) ((TernaryExpression) e).getRight(),
						pp);
				IntegerConstant right = intDomain.generate(constraints, pp, oracle);
				if (left.isTop() || middle.isTop() || right.isTop())
					return ValueDomain.makeRangeConstraints(
							pp.getProgram().getTypes().getIntegerType(),
							-1,
							null,
							e,
							pp);
				if (left.isBottom() || middle.isBottom() || right.isBottom())
					return null;
				int i;
				if (operator == StringIndexOfCharFromIndex.INSTANCE)
					i = left.value.indexOf(middle.value.charAt(0), right.value);
				else if (operator == StringLastIndexOfCharFromIndex.INSTANCE)
					i = left.value.lastIndexOf(middle.value.charAt(0), right.value);
				else if (operator == StringIndexOfFromIndex.INSTANCE)
					i = left.value.indexOf(middle.value, right.value);
				else // operator == StringLastIndexOfFromIndex.INSTANCE
					i = left.value.lastIndexOf(middle.value, right.value);
				return ValueDomain.makeEqConstraint(
						pp.getProgram().getTypes().getIntegerType(),
						i,
						e,
						pp);
			}
			if (operator == StringStartsWithFromIndex.INSTANCE) {
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

		StringConstant value = eval(state, e, pp, oracle);
		if (value.isTop())
			return Collections.emptySet();
		if (value.isBottom())
			return null;
		return ValueDomain.makeEqConstraint(
				pp.getProgram().getTypes().getStringType(),
				value.value,
				e,
				pp);
	}

}
