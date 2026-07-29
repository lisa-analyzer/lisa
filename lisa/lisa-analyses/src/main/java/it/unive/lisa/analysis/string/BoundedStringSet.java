package it.unive.lisa.analysis.string;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.combination.smash.SmashedSumStringDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.numeric.Interval;
import it.unive.lisa.analysis.value.StringAbstraction;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.lattices.SetLattice;
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
import it.unive.lisa.util.numeric.MathNumber;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.lang3.StringUtils;

/**
 * A domain computing bounded set of strings, where the maximum number of
 * elements is defined by {@link #max_size}. If the number of elements exceeds
 * this limit, the set is considered to be top. The domain is defined
 * <a href="https://link.springer.com/chapter/10.1007/978-3-642-54807-9_12">in
 * this paper</a>.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class BoundedStringSet
		implements
		StringAbstraction<ValueEnvironment<BoundedStringSet.BSS>>,
		SmashedSumStringDomain<BoundedStringSet.BSS> {

	/**
	 * A bounded set of strings, where the maximum number of elements is defined
	 * by {@link #max_size}. If the number of elements exceeds this limit, the
	 * set is considered to be top. The domain is defined <a href=
	 * "https://link.springer.com/chapter/10.1007/978-3-642-54807-9_12">in this
	 * paper</a>.
	 *
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public class BSS
			extends
			SetLattice<BSS, String> {

		/**
		 * Builds the top abstract value.
		 */
		public BSS() {
			super(Collections.emptySet(), true);
		}

		private BSS(
				Set<String> elements) {
			super(elements.size() > max_size ? Collections.emptySet() : elements, elements.size() > max_size);
		}

		private BSS(
				Set<String> elements,
				boolean isTop) {
			super(elements, isTop);
		}

		@Override
		public BSS lubAux(
				BSS other)
				throws SemanticException {
			BSS lub = super.lubAux(other);
			if (lub.elements.size() > max_size)
				return top();
			return lub;
		}

		@Override
		public BSS top() {
			return new BSS(Collections.emptySet(), true);
		}

		@Override
		public BSS bottom() {
			return new BSS(Collections.emptySet(), false);
		}

		@Override
		public BSS mk(
				Set<String> set) {
			return new BSS(set, false);
		}
	}

	/**
	 * The maximum number of elements that instances of this domain can contain
	 * before being considered top.
	 */
	private final int max_size;

	private final Interval interval = new Interval();

	/**
	 * Builds the domain, using {@code 10} as the maximum number of strings to
	 * track before going to top.
	 */
	public BoundedStringSet() {
		this(10);
	}

	/**
	 * Builds the domain, using {@code max_size} as the maximum number of
	 * strings to track before going to top.
	 *
	 * @param max_size the maximum number of strings to track before going to
	 *                     top
	 */
	public BoundedStringSet(
			int max_size) {
		this.max_size = max_size;
	}

	@Override
	public BSS evalConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (constant.getValue() instanceof String) {
			String str = (String) constant.getValue();
			return new BSS(Collections.singleton(str));
		}

		return top();
	}

	@Override
	public BSS evalPushAny(
			PushAny pushAny,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (pushAny instanceof PushFromConstraints)
			return generate(((PushFromConstraints) pushAny).getConstraints(), pp, oracle);
		return SmashedSumStringDomain.super.evalPushAny(pushAny, pp, oracle);
	}

	private BSS forEach(
			BSS arg,
			BSS onNull,
			java.util.function.UnaryOperator<String> operator) {
		if (arg.isTop())
			return arg;

		Set<String> result = new TreeSet<>();
		for (String str : arg.elements) {
			String res = operator.apply(str);
			if (res != null)
				result.add(res);
			else if (onNull != null)
				return onNull;
		}
		return new BSS(result);
	}

	private BSS forEach(
			BSS arg,
			IntInterval val,
			BSS onNull,
			java.util.function.BiFunction<String, Long, String> operator) {
		if (arg.isTop())
			return arg;

		Set<String> result = new TreeSet<>();
		for (String str : arg.elements)
			for (long i : val) {
				String res = operator.apply(str, i);
				if (res != null)
					result.add(res);
				else if (onNull != null)
					return onNull;
			}
		return new BSS(result);
	}

	private BSS forEach(
			BSS left,
			BSS right,
			java.util.function.BinaryOperator<String> operator) {
		if (left.isTop() || right.isTop())
			return top();

		Set<String> result = new TreeSet<>();
		for (String ll : left.elements)
			for (String rr : right.elements) {
				String res = operator.apply(ll, rr);
				if (res == null)
					return top();
				result.add(res);
			}
		return new BSS(result);
	}

	/**
	 * A function that takes three arguments and produces a result. This is the
	 * three-arity specialization of {@link java.util.function.Function}.
	 *
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 * 
	 * @param <T> the type of the first argument to the function
	 * @param <U> the type of the second argument to the function
	 * @param <V> the type of the third argument to the function
	 * @param <R> the type of the result of the function
	 */
	@FunctionalInterface
	interface TernaryFunction<T, U, V, R> {

		/**
		 * Applies this function to the given arguments.
		 *
		 * @param left   the first function argument
		 * @param middle the second function argument
		 * @param right  the third function argument
		 * 
		 * @return the function result
		 */
		R apply(
				T left,
				U middle,
				V right);
	}

	private BSS forEach(
			BSS arg,
			IntInterval val1,
			IntInterval val2,
			BSS onNull,
			TernaryFunction<String, Long, Long, String> operator) {
		if (arg.isTop())
			return arg;

		Set<String> result = new TreeSet<>();
		for (String str : arg.elements)
			for (long i : val1)
				for (long j : val2) {
					String res = operator.apply(str, i, j);
					if (res != null)
						result.add(res);
					else if (onNull != null)
						return onNull;
				}
		return new BSS(result);
	}

	private BSS forEach(
			BSS left,
			BSS middle,
			BSS right,
			BSS onNull,
			TernaryFunction<String, String, String, String> operator) {
		if (left.isTop() || right.isTop())
			return top();

		Set<String> result = new TreeSet<>();
		for (String ll : left.elements)
			for (String mm : middle.elements)
				for (String rr : right.elements) {
					String res = operator.apply(ll, mm, rr);
					if (res != null)
						result.add(res);
					else if (onNull != null)
						return onNull;
				}
		return new BSS(result);
	}

	@Override
	public BSS evalUnaryExpression(
			UnaryExpression expression,
			BSS arg,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		UnaryOperator operator = expression.getOperator();

		if (oracle.hasWholeValueAnlysis() && operator == NumericToString.INSTANCE) {
			Set<BinaryExpression> constraints = oracle.constraints(this, expression, pp);
			return generate(constraints, pp, oracle);
		}

		return forEach(arg, top(), s -> {
			if (operator == StringReverse.INSTANCE)
				return StringUtils.reverse(s);
			else if (operator == StringToLowerCase.INSTANCE)
				return s.toLowerCase();
			else if (operator == StringToUpperCase.INSTANCE)
				return s.toUpperCase();
			else if (operator == StringTrim.INSTANCE)
				return s.trim();
			else
				return null;
		});
	}

	@Override
	public BSS evalBinaryExpression(
			BinaryExpression expression,
			BSS left,
			BSS right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		BinaryOperator operator = expression.getOperator();

		if (left.isTop())
			return top();

		if (oracle.hasWholeValueAnlysis()
				&& (operator == StringCharAt.INSTANCE
						|| operator == StringSubstringToEnd.INSTANCE)) {
			Set<BinaryExpression> constraints = oracle.constraints(this, (ValueExpression) expression.getRight(), pp);
			IntInterval val = interval.generate(constraints, pp, oracle);
			if (val.isBottom())
				return bottom();
			if (val.isTop() || val.isInfinite()
					|| val.size().multiply(new MathNumber(left.size())).compareTo(new MathNumber(max_size)) > 0)
				return top();
			return forEach(left, val, null, (
					s,
					i) -> {
				if (i < 0 || i > s.length())
					return null;
				if (operator == StringCharAt.INSTANCE)
					return String.valueOf(s.charAt(i.intValue()));
				else // if (operator == StringSubstringToEnd.INSTANCE)
					return s.substring(i.intValue());
			});
		}

		if (right.isTop())
			return top();

		if (operator instanceof StringConcat)
			return forEach(left, right, String::concat);

		return top();
	}

	@Override
	public BSS evalTernaryExpression(
			TernaryExpression expression,
			BSS left,
			BSS middle,
			BSS right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		TernaryOperator operator = expression.getOperator();

		if (left.isTop())
			return top();

		if (oracle.hasWholeValueAnlysis() && operator == StringSubstring.INSTANCE) {
			Set<BinaryExpression> cM = oracle.constraints(this, (ValueExpression) expression.getMiddle(), pp);
			IntInterval mid = interval.generate(cM, pp, oracle);
			Set<BinaryExpression> cR = oracle.constraints(this, (ValueExpression) expression.getRight(), pp);
			IntInterval rig = interval.generate(cR, pp, oracle);
			if (mid.isBottom() || rig.isBottom())
				return bottom();
			if (mid.isTop()
					|| rig.isTop()
					|| mid.isInfinite()
					|| rig.isInfinite()
					|| mid.size().multiply(rig.size()).multiply(new MathNumber(left.size()))
							.compareTo(new MathNumber(max_size)) > 0)
				return top();
			return forEach(left, mid, rig, null, (
					s,
					m,
					r) -> {
				if (m < 0 || r > s.length() || m > r)
					return null;
				return s.substring(m.intValue(), r.intValue());
			});
		}

		if (middle.isTop() || right.isTop())
			return top();

		if (expression.getOperator() == StringReplace.INSTANCE &&
				(middle.elements.size() != 1 || right.elements.size() != 1))
			// if we have more search/replace strings than one, we cannot
			// guarantee what replacement will happen
			return top();

		return forEach(left, middle, right, top(), (
				l,
				m,
				r) -> {
			if (operator instanceof StringReplace)
				return l.replace(m, r);
			else if (operator == StringReplaceAll.INSTANCE)
				return l.replaceAll(m, r);
			else if (operator == StringReplaceFirst.INSTANCE)
				return l.replaceFirst(m, r);
			else
				return null;
		});
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(
			BinaryExpression expression,
			BSS left,
			BSS right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (left.isTop() || right.isTop())
			return Satisfiability.UNKNOWN;

		Satisfiability result = Satisfiability.BOTTOM;
		BinaryOperator operator = expression.getOperator();
		for (String ll : left.elements)
			for (String rr : right.elements)
				if (operator == ComparisonEq.INSTANCE)
					result = result.lub(Satisfiability.fromBoolean(ll.equals(rr)));
				else if (operator == ComparisonNe.INSTANCE)
					result = result.lub(Satisfiability.fromBoolean(!ll.equals(rr)));
				else if (operator == StringContains.INSTANCE)
					result = result.lub(Satisfiability.fromBoolean(ll.contains(rr)));
				else if (operator == StringEndsWith.INSTANCE)
					result = result.lub(Satisfiability.fromBoolean(ll.endsWith(rr)));
				else if (operator == StringEquals.INSTANCE)
					result = result.lub(Satisfiability.fromBoolean(ll.equals(rr)));
				else if (operator == StringEqualsIgnoreCase.INSTANCE)
					result = result.lub(Satisfiability.fromBoolean(ll.equalsIgnoreCase(rr)));
				else if (operator == StringMatches.INSTANCE)
					result = result.lub(Satisfiability.fromBoolean(ll.matches(rr)));
				else if (operator == StringStartsWith.INSTANCE)
					result = result.lub(Satisfiability.fromBoolean(ll.startsWith(rr)));
				else if (operator == StringIsPrefixOf.INSTANCE)
					result = result.lub(Satisfiability.fromBoolean(rr.startsWith(ll)));
				else if (operator == StringIsSuffixOf.INSTANCE)
					result = result.lub(Satisfiability.fromBoolean(rr.endsWith(ll)));
				else
					return Satisfiability.UNKNOWN;

		return result;
	}

	@Override
	public Satisfiability satisfiesTernaryExpression(
			TernaryExpression expression,
			BSS left,
			BSS middle,
			BSS right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (left.isTop() || middle.isTop())
			return Satisfiability.UNKNOWN;

		if (oracle.hasWholeValueAnlysis() && expression.getOperator() == StringStartsWithFromIndex.INSTANCE) {
			Set<BinaryExpression> constraints = oracle.constraints(this, (ValueExpression) expression.getRight(), pp);
			IntInterval val = interval.generate(constraints, pp, oracle);
			if (val.isBottom())
				return Satisfiability.NOT_SATISFIED;
			if (val.isTop()
					|| val.isInfinite()
					|| val.size().multiply(new MathNumber(left.size())).compareTo(new MathNumber(max_size)) > 0)
				return Satisfiability.UNKNOWN;
			for (String str : left.elements)
				for (long i : val) {
					if (i < 0 || i > str.length())
						continue;
					for (String prefix : middle.elements)
						if (!str.startsWith(prefix, (int) i))
							return Satisfiability.UNKNOWN;
				}
			return Satisfiability.SATISFIED;
		}

		return Satisfiability.UNKNOWN;
	}

	@Override
	public ValueEnvironment<BSS> assumeBinaryExpression(
			ValueEnvironment<BSS> environment,
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
		if (operator != ComparisonEq.INSTANCE && operator != ComparisonNe.INSTANCE)
			return environment;

		ValueExpression left = (ValueExpression) expression.getLeft();
		ValueExpression right = (ValueExpression) expression.getRight();
		Identifier id;
		BSS rhsVal;
		if (left instanceof Identifier) {
			if (!canProcess(left, src, oracle))
				// the expression does not have a string value, we do not
				// assume anything on it
				return environment;
			id = (Identifier) left;
			rhsVal = eval(environment, right, src, oracle);
		} else if (right instanceof Identifier) {
			if (!canProcess(left, src, oracle))
				// the expression does not have a string value, we do not
				// assume anything on it
				return environment;
			id = (Identifier) right;
			rhsVal = eval(environment, left, src, oracle);
		} else {
			return environment;
		}

		if (rhsVal.isTop() || rhsVal.isBottom())
			return environment;

		BSS lhsVal = environment.getState(id);
		if (operator == ComparisonEq.INSTANCE) {
			BSS refined = lhsVal.glb(rhsVal);
			if (refined.isBottom())
				return environment.bottom();
			if (!refined.equals(lhsVal))
				return environment.putState(id, refined);
		} else {
			// ComparisonNe: remove rhsVal strings from lhsVal
			if (lhsVal.isTop() || lhsVal.isBottom())
				return environment;
			Set<String> diff = new HashSet<>(lhsVal.elements);
			diff.removeAll(rhsVal.elements);
			BSS refined = top().mk(diff);
			if (refined.isBottom())
				return environment.bottom();
			if (!refined.equals(lhsVal))
				return environment.putState(id, refined);
		}
		return environment;
	}

	@Override
	public BSS top() {
		return new BSS();
	}

	@Override
	public BSS bottom() {
		return new BSS(Collections.emptySet(), false);
	}

	private BSS generate(
			Set<BinaryExpression> constraints,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (constraints == null)
			return bottom();

		for (BinaryExpression expr : constraints)
			if (expr.getLeft() instanceof Constant) {
				String val = ((Constant) expr.getLeft()).getValue().toString();
				if (expr.getOperator() instanceof ComparisonEq)
					return new BSS(Collections.singleton(val));
			}

		return top();
	}

	@Override
	public Satisfiability containsChar(
			BSS current,
			char c)
			throws SemanticException {
		if (current.isTop())
			return Satisfiability.UNKNOWN;
		if (current.isBottom())
			return Satisfiability.BOTTOM;

		boolean all = true, one = false;
		for (String str : current.elements)
			if (str.indexOf(c) >= 0)
				one = true;
			else
				all = false;
		if (all)
			return Satisfiability.SATISFIED;
		if (one)
			return Satisfiability.UNKNOWN;
		return Satisfiability.NOT_SATISFIED;
	}

	@Override
	public IntInterval length(
			BSS current)
			throws SemanticException {
		if (current.isTop())
			return new IntInterval(MathNumber.ZERO, MathNumber.PLUS_INFINITY);
		if (current.isBottom())
			return null;

		int minLength = Integer.MAX_VALUE;
		int maxLength = 0;
		for (String str : current.elements) {
			int len = str.length();
			if (len < minLength)
				minLength = len;
			if (len > maxLength)
				maxLength = len;
		}

		return new IntInterval(minLength, maxLength);
	}

	@Override
	public IntInterval indexOf(
			BSS current,
			BSS other)
			throws SemanticException {
		if (current.isBottom() || other.isBottom())
			return null;
		if (current.isTop() || other.isTop())
			return new IntInterval(MathNumber.MINUS_ONE, MathNumber.PLUS_INFINITY);

		int minIndex = Integer.MAX_VALUE;
		int maxIndex = -1;

		for (String str : current.elements)
			for (String sub : other.elements) {
				int index = str.indexOf(sub);
				if (index >= 0) {
					if (index < minIndex)
						minIndex = index;
					if (index > maxIndex)
						maxIndex = index;
				}
			}

		if (minIndex == Integer.MAX_VALUE)
			return new IntInterval(MathNumber.MINUS_ONE, MathNumber.PLUS_INFINITY);
		return new IntInterval(minIndex, maxIndex);
	}

	@Override
	public BSS substring(
			BSS current,
			long begin,
			long end)
			throws SemanticException {
		if (current.isBottom() || current.isTop())
			return current;

		Set<String> result = new TreeSet<>();
		for (String str : current.elements) {
			if (end <= str.length())
				result.add(str.substring((int) begin, (int) end));
		}

		return new BSS(result);
	}

	@Override
	public Set<BinaryExpression> constraints(
			ValueDomain<?> requesting,
			ValueEnvironment<BSS> state,
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
				BSS value = eval(state, arg, pp, oracle);
				if (value.isTop())
					return ValueDomain.makeRangeConstraints(
							pp.getProgram().getTypes().getIntegerType(),
							0,
							null,
							e,
							pp);
				if (value.isBottom())
					return null;
				int minLength = Integer.MAX_VALUE, maxLength = -1;
				for (String str : value.elements) {
					int len = str.length();
					if (len < minLength)
						minLength = len;
					if (len > maxLength)
						maxLength = len;
				}
				return ValueDomain.makeRangeConstraints(
						pp.getProgram().getTypes().getIntegerType(),
						minLength,
						maxLength,
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
				BSS left = eval(state, (ValueExpression) ((BinaryExpression) e).getLeft(), pp, oracle);
				BSS right = eval(state, (ValueExpression) ((BinaryExpression) e).getRight(), pp, oracle);
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
				int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
				if (operator == StringIndexOfChar.INSTANCE)
					for (String str : left.elements)
						for (String sub : right.elements) {
							int i = str.indexOf(sub.charAt(0));
							if (i < min)
								min = i;
							if (i > max)
								max = i;
						}
				else if (operator == StringLastIndexOfChar.INSTANCE)
					for (String str : left.elements)
						for (String sub : right.elements) {
							int i = str.lastIndexOf(sub.charAt(0));
							if (i < min)
								min = i;
							if (i > max)
								max = i;
						}
				else if (operator == StringIndexOf.INSTANCE)
					for (String str : left.elements)
						for (String sub : right.elements) {
							int i = str.indexOf(sub);
							if (i < min)
								min = i;
							if (i > max)
								max = i;
						}
				else if (operator == StringLastIndexOf.INSTANCE)
					for (String str : left.elements)
						for (String sub : right.elements) {
							int i = str.lastIndexOf(sub);
							if (i < min)
								min = i;
							if (i > max)
								max = i;
						}
				else // operator == ValueComparison.INSTANCE
					for (String str : left.elements)
						for (String sub : right.elements) {
							int i = str.compareTo(sub);
							if (i < min)
								min = i;
							if (i > max)
								max = i;
						}
				return ValueDomain.makeRangeConstraints(
						pp.getProgram().getTypes().getIntegerType(),
						min,
						max,
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
				BSS left = eval(state, (ValueExpression) ((TernaryExpression) e).getLeft(), pp, oracle);
				BSS middle = eval(state, (ValueExpression) ((TernaryExpression) e).getMiddle(), pp, oracle);
				Set<BinaryExpression> constraints = oracle.constraints(
						this,
						(ValueExpression) ((TernaryExpression) e).getRight(),
						pp);
				IntInterval right = interval.generate(constraints, pp, oracle);
				if (left.isTop() || middle.isTop() || right.isTop() || right.isInfinite())
					return ValueDomain.makeRangeConstraints(
							pp.getProgram().getTypes().getIntegerType(),
							-1,
							null,
							e,
							pp);
				if (left.isBottom() || middle.isBottom() || right.isBottom())
					return null;
				int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
				if (operator == StringIndexOfCharFromIndex.INSTANCE)
					for (String str : left.elements)
						for (String sub : middle.elements)
							for (long idx : right) {
								if (idx < 0 || idx > str.length())
									continue;
								int i = str.indexOf(sub.charAt(0), (int) idx);
								if (i < min)
									min = i;
								if (i > max)
									max = i;
							}
				else if (operator == StringLastIndexOfCharFromIndex.INSTANCE)
					for (String str : left.elements)
						for (String sub : middle.elements)
							for (long idx : right) {
								if (idx < 0 || idx > str.length())
									continue;
								int i = str.lastIndexOf(sub.charAt(0), (int) idx);
								if (i < min)
									min = i;
								if (i > max)
									max = i;
							}
				else if (operator == StringIndexOfFromIndex.INSTANCE)
					for (String str : left.elements)
						for (String sub : middle.elements)
							for (long idx : right) {
								if (idx < 0 || idx > str.length())
									continue;
								int i = str.indexOf(sub, (int) idx);
								if (i < min)
									min = i;
								if (i > max)
									max = i;
							}
				else // operator == StringLastIndexOfFromIndex.INSTANCE
					for (String str : left.elements)
						for (String sub : middle.elements)
							for (long idx : right) {
								if (idx < 0 || idx > str.length())
									continue;
								int i = str.lastIndexOf(sub, (int) idx);
								if (i < min)
									min = i;
								if (i > max)
									max = i;
							}
				return ValueDomain.makeRangeConstraints(
						pp.getProgram().getTypes().getIntegerType(),
						min,
						max,
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

		BSS value = eval(state, e, pp, oracle);
		if (value.isTop() || value.elements.size() != 1)
			return Collections.emptySet();
		if (value.isBottom())
			return null;
		return ValueDomain.makeEqConstraint(
				pp.getProgram().getTypes().getStringType(),
				value.elements.iterator().next(),
				e,
				pp);
	}

}
