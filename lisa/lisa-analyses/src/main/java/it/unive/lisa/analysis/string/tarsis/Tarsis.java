package it.unive.lisa.analysis.string.tarsis;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.combination.smash.SmashedSumStringDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.numeric.Interval;
import it.unive.lisa.analysis.value.StringAbstraction;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.lattices.string.tarsis.IndexFinder;
import it.unive.lisa.lattices.string.tarsis.RegexAutomaton;
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
import it.unive.lisa.type.BooleanType;
import it.unive.lisa.util.datastructures.automaton.CyclicAutomatonException;
import it.unive.lisa.util.datastructures.automaton.TransitionSymbol;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.lisa.util.numeric.MathNumberConversionException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A class that represent the Tarsis domain for strings, exploiting a
 * {@link RegexAutomaton}.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Tarsis
		implements
		StringAbstraction<ValueEnvironment<RegexAutomaton>>,
		SmashedSumStringDomain<RegexAutomaton> {

	/**
	 * The interval domain that we use to handle numerical constraints.
	 */
	private final Interval intDomain = new Interval();

	@Override
	public RegexAutomaton evalConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (constant.getValue() instanceof String)
			return RegexAutomaton.string((String) constant.getValue());
		return top();
	}

	@Override
	public RegexAutomaton evalPushAny(
			PushAny pushAny,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (pushAny instanceof PushFromConstraints)
			return generate(((PushFromConstraints) pushAny).getConstraints(), pp, oracle);
		return SmashedSumStringDomain.super.evalPushAny(pushAny, pp, oracle);
	}

	@Override
	public RegexAutomaton evalUnaryExpression(
			UnaryExpression expression,
			RegexAutomaton arg,
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
			return arg.reverse().toSingleInitalState();
		else if (operator == StringToLowerCase.INSTANCE)
			return arg.lowerCase();
		else if (operator == StringToUpperCase.INSTANCE)
			return arg.upperCase();
		else if (operator == StringTrim.INSTANCE)
			return arg.trim();

		return RegexAutomaton.TOP;
	}

	@Override
	public RegexAutomaton evalBinaryExpression(
			BinaryExpression expression,
			RegexAutomaton left,
			RegexAutomaton right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		BinaryOperator operator = expression.getOperator();

		if (oracle.hasWholeValueAnlysis()
				&& (operator == StringCharAt.INSTANCE
						|| operator == StringSubstringToEnd.INSTANCE)) {
			Set<BinaryExpression> constraints = oracle.constraints(this, (ValueExpression) expression.getRight(), pp);
			IntInterval val = intDomain.generate(constraints, pp, oracle);
			if (val.isBottom() || val.getHigh().compareTo(MathNumber.ZERO) < 0)
				return RegexAutomaton.BOTTOM;
			if (val.isTop() || val.highIsPlusInfinity())
				return RegexAutomaton.TOP;
			if (operator == StringCharAt.INSTANCE) {
				RegexAutomaton partial = RegexAutomaton.BOTTOM;
				RegexAutomaton temp;

				int minI, maxI;
				try {
					minI = val.lowIsMinusInfinity() ? 0 : val.getLow().toInt();
					if (minI < 0)
						minI = 0;
					maxI = val.getHigh().toInt();
				} catch (MathNumberConversionException e) {
					// should not happen
					throw new SemanticException("Cannot convert stirng indexof bound to int", e);
				}

				outer: for (int i = minI; i <= maxI; i++) {
					temp = partial.lub(substring(left, i, i + 1));

					if (temp.equals(partial))
						break outer;

					partial = temp;
					if (partial.isTop())
						break outer;
				}

				return partial;

			}
			if (operator == StringSubstringToEnd.INSTANCE) {
				IntInterval len = left.length();
				if (len.isBottom())
					return RegexAutomaton.BOTTOM;
				if (len.isTop() || len.highIsPlusInfinity())
					return RegexAutomaton.TOP;

				RegexAutomaton partial = RegexAutomaton.BOTTOM;
				RegexAutomaton temp;

				int minI, maxI, minJ, maxJ;
				try {
					minI = val.lowIsMinusInfinity() ? 0 : val.getLow().toInt();
					if (minI < 0)
						minI = 0;
					maxI = val.getHigh().toInt();
					minJ = len.getLow().toInt();
					maxJ = len.getHigh().toInt();
				} catch (MathNumberConversionException e) {
					// should not happen
					throw new SemanticException("Cannot convert stirng substring bound to int", e);
				}

				outer: for (int i = minI; i <= maxI; i++)
					for (int j = minJ; j <= maxJ; j++) {
						if (i < j)
							temp = partial.lub(substring(left, i, j));
						else if (i == j)
							temp = partial.lub(left.emptyString());
						else
							temp = RegexAutomaton.BOTTOM;

						if (temp.equals(partial))
							break outer;

						partial = temp;
						if (partial.isTop())
							break outer;
					}

				return partial;
			}
		}

		if (operator instanceof StringConcat)
			return left.concat(right);

		return RegexAutomaton.TOP;
	}

	@Override
	public RegexAutomaton evalTernaryExpression(
			TernaryExpression expression,
			RegexAutomaton left,
			RegexAutomaton middle,
			RegexAutomaton right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		TernaryOperator operator = expression.getOperator();

		if (left.isTop())
			return RegexAutomaton.TOP;

		if (oracle.hasWholeValueAnlysis() && operator == StringSubstring.INSTANCE) {
			Set<BinaryExpression> cM = oracle.constraints(this, (ValueExpression) expression.getMiddle(), pp);
			IntInterval mid = intDomain.generate(cM, pp, oracle);
			Set<BinaryExpression> cR = oracle.constraints(this, (ValueExpression) expression.getRight(), pp);
			IntInterval rig = intDomain.generate(cR, pp, oracle);
			if (mid.isBottom()
					|| mid.getHigh().compareTo(MathNumber.ZERO) < 0
					|| rig.isBottom()
					|| rig.getHigh().compareTo(MathNumber.ZERO) < 0)
				return RegexAutomaton.BOTTOM;
			if (mid.isTop()
					|| mid.highIsPlusInfinity()
					|| rig.isTop()
					|| rig.highIsPlusInfinity())
				return RegexAutomaton.TOP;

			RegexAutomaton partial = RegexAutomaton.BOTTOM;
			RegexAutomaton temp;

			int minI, maxI, minJ, maxJ;
			try {
				minI = mid.lowIsMinusInfinity() ? 0 : mid.getLow().toInt();
				if (minI < 0)
					minI = 0;
				maxI = mid.getHigh().toInt();
				minJ = rig.getLow().toInt();
				if (minJ < 0)
					minJ = 0;
				maxJ = rig.getHigh().toInt();
			} catch (MathNumberConversionException e) {
				// should not happen
				throw new SemanticException("Cannot convert stirng substring bound to int", e);
			}

			outer: for (int i = minI; i <= maxI; i++)
				for (int j = minJ; j <= maxJ; j++) {
					if (i < j)
						temp = partial.lub(substring(left, i, j));
					else if (i == j)
						temp = partial.lub(left.emptyString());
					else
						temp = RegexAutomaton.BOTTOM;

					if (temp.equals(partial))
						break outer;

					partial = temp;
					if (partial.isTop())
						break outer;
				}

			return partial;
		}

		if (right.isTop() || middle.isTop())
			return RegexAutomaton.TOP;

		if (operator instanceof StringReplace)
			// expensive per-path reasoning
			return RegexAutomaton.TOP;
		if (operator == StringReplaceAll.INSTANCE)
			return replace(left, middle, right);
		if (operator == StringReplaceFirst.INSTANCE)
			// no regex support
			return RegexAutomaton.TOP;

		return RegexAutomaton.TOP;
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(
			BinaryExpression expression,
			RegexAutomaton left,
			RegexAutomaton right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (left.isTop() || right.isTop())
			return Satisfiability.UNKNOWN;

		BinaryOperator operator = expression.getOperator();
		if (operator == ComparisonEq.INSTANCE)
			return eq(left, right);
		else if (operator == ComparisonNe.INSTANCE)
			return eq(left, right).negate();
		else if (operator == StringContains.INSTANCE)
			return contains(left, right);
		else if (operator == StringEndsWith.INSTANCE)
			// not handled for now
			return Satisfiability.UNKNOWN;
		else if (operator == StringEquals.INSTANCE)
			return eq(left, right);
		else if (operator == StringEqualsIgnoreCase.INSTANCE)
			return eq(left.lowerCase(), right.lowerCase());
		else if (operator == StringMatches.INSTANCE)
			return Satisfiability.UNKNOWN;
		else if (operator == StringStartsWith.INSTANCE)
			// not handled for now
			return Satisfiability.UNKNOWN;
		else if (operator == StringIsPrefixOf.INSTANCE)
			// not handled for now
			return Satisfiability.UNKNOWN;
		else if (operator == StringIsSuffixOf.INSTANCE)
			// not handled for now
			return Satisfiability.UNKNOWN;
		else
			return Satisfiability.UNKNOWN;
	}

	@Override
	public ValueEnvironment<RegexAutomaton> assumeBinaryExpression(
			ValueEnvironment<RegexAutomaton> environment,
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
				RegexAutomaton eval = eval(environment, right, src, oracle);
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
				RegexAutomaton eval = eval(environment, left, src, oracle);
				if (eval.isBottom())
					return environment.bottom();
				// Same reasoning as above, symmetric case.
				if (!eval.isTop())
					return environment.putState((Identifier) right, eval);
			}
		}
		return environment;
	}

	/**
	 * Semantics of {@link StringEquals} between {@code current} and
	 * {@code other}.
	 * 
	 * @param current the current automaton
	 * @param other   the other domain instance
	 * 
	 * @return the satisfiability result
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	public Satisfiability eq(
			RegexAutomaton current,
			RegexAutomaton other)
			throws SemanticException {
		if (current.glb(other).isBottom())
			return Satisfiability.NOT_SATISFIED;
		if (current.hasCycle() || other.hasCycle())
			return Satisfiability.UNKNOWN;
		Satisfiability res = Satisfiability.BOTTOM;
		try {
			for (String a : current.getLanguage())
				for (String b : other.getLanguage()) {
					res = res.lub(eq(a, b));
					if (res.isTop())
						return res;
				}
		} catch (CyclicAutomatonException e) {
			throw new SemanticException("The automaton is cyclic", e);
		}
		return res;
	}

	private static Satisfiability eq(
			String a,
			String b)
			throws SemanticException {
		if (a.isEmpty() && b.isEmpty())
			return Satisfiability.SATISFIED;
		if (a.isEmpty())
			return b.equals(TransitionSymbol.UNKNOWN_SYMBOL) ? Satisfiability.UNKNOWN : Satisfiability.NOT_SATISFIED;
		if (b.isEmpty())
			return a.equals(TransitionSymbol.UNKNOWN_SYMBOL) ? Satisfiability.UNKNOWN : Satisfiability.NOT_SATISFIED;
		if (a.equals(TransitionSymbol.UNKNOWN_SYMBOL) || b.equals(TransitionSymbol.UNKNOWN_SYMBOL))
			return Satisfiability.UNKNOWN;
		char a0 = a.charAt(0);
		char b0 = b.charAt(0);
		char top = TransitionSymbol.UNKNOWN_SYMBOL.charAt(0);
		if (a0 != b0 && a0 != top && b0 != top)
			return Satisfiability.NOT_SATISFIED;
		if (a0 == b0 && a0 != top)
			return eq(a.substring(1), b.substring(1));
		if (a0 == top || b0 == top)
			return Satisfiability.NOT_SATISFIED.lub(eq(a.substring(1), b.substring(1)))
					.lub(eq(a.substring(1), b))
					.lub(eq(a, b.substring(1)));
		// this should be unreachable
		return Satisfiability.UNKNOWN;
	}

	/**
	 * Semantics of {@link StringContains} between {@code current} and
	 * {@code other}.
	 * 
	 * @param current the current automaton
	 * @param other   the other domain instance
	 * 
	 * @return the satisfiability result
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	public Satisfiability contains(
			RegexAutomaton current,
			RegexAutomaton other)
			throws SemanticException {
		try {
			if (!current.hasCycle()
					&& !other.hasCycle()
					&& !current.acceptsTopEventually()
					&& !other.acceptsTopEventually()) {
				// we can compare languages
				boolean atLeastOne = false, all = true;
				for (String a : current.getLanguage())
					for (String b : other.getLanguage()) {
						boolean cont = a.contains(b);
						atLeastOne = atLeastOne || cont;
						all = all && cont;
					}

				if (all)
					return Satisfiability.SATISFIED;
				if (atLeastOne)
					return Satisfiability.UNKNOWN;
				return Satisfiability.NOT_SATISFIED;
			}

			if (!other.hasCycle() && other.getLanguage().size() == 1 && other.getLanguage().iterator().next().isEmpty())
				// the empty string is always contained
				return Satisfiability.SATISFIED;

			if (other.hasOnlyOnePath() && !other.acceptsTopEventually()) {
				Satisfiability allSat = Satisfiability.UNKNOWN;
				RegexAutomaton C = other.extractLongestString();
				String longest = C.getLanguage().iterator().next();
				RegexAutomaton withNoScc = current.minimize().makeAcyclic();
				SortedSet<String> lang = withNoScc.getLanguage();
				for (String a : lang)
					allSat = allSat.glb(contains(a, longest));

				if (!lang.isEmpty() && allSat == Satisfiability.SATISFIED)
					return allSat;
			}

			RegexAutomaton transformed = current.explode().factors();
			RegexAutomaton otherExploded = other.explode();
			if (otherExploded.intersection(transformed).acceptsEmptyLanguage())
				// we can explode since it does not matter how the inner strings
				// overlap
				return Satisfiability.NOT_SATISFIED;

		} catch (CyclicAutomatonException e) {
			// can safely ignore
		}
		return Satisfiability.UNKNOWN;
	}

	private Satisfiability contains(
			String other,
			String that) {
		if (!other.contains(TransitionSymbol.UNKNOWN_SYMBOL)) {
			if (other.contains(that))
				return Satisfiability.SATISFIED;
			return Satisfiability.NOT_SATISFIED;
		} else {
			String otherWithoutTops = other.replaceAll(TransitionSymbol.UNKNOWN_SYMBOL, "");
			if (otherWithoutTops.contains(that))
				return Satisfiability.SATISFIED;
			else
				return Satisfiability.UNKNOWN;
		}
	}

	@Override
	public RegexAutomaton substring(
			RegexAutomaton current,
			long begin,
			long end) {
		if (current.isTop() || current.isBottom())
			return current;

		RegexAutomaton[] array = current.toRegex()
				.substring((int) begin, (int) end)
				.parallelStream()
				.map(s -> RegexAutomaton.string(s))
				.toArray(RegexAutomaton[]::new);

		RegexAutomaton result = RegexAutomaton.emptyLang();

		for (int i = 0; i < array.length; i++)
			result = result.union(array[i]);
		return result;
	}

	@Override
	public IntInterval length(
			RegexAutomaton current)
			throws SemanticException {
		return current.length();
	}

	@Override
	public IntInterval indexOf(
			RegexAutomaton current,
			RegexAutomaton other)
			throws SemanticException {
		if (contains(current, other) == Satisfiability.NOT_SATISFIED)
			return new IntInterval(-1, -1);
		else if (current.hasCycle() || other.hasCycle() || other.acceptsTopEventually())
			return new IntInterval(MathNumber.MINUS_ONE, MathNumber.PLUS_INFINITY);
		Pair<Integer, Integer> interval;
		try {
			interval = IndexFinder.findIndexesOf(current, other);
		} catch (CyclicAutomatonException e) {
			throw new SemanticException("The automaton is cyclic", e);
		}
		return new IntInterval(interval.getLeft(), interval.getRight());
	}

	/**
	 * Yields the concatenation between two automata.
	 * 
	 * @param current the current automaton
	 * @param other   the other automaton
	 * 
	 * @return the concatenation between two automata
	 */
	public RegexAutomaton concat(
			RegexAutomaton current,
			RegexAutomaton other) {
		return current.concat(other);
	}

	/**
	 * Yields the replacement of occurrences of {@code search} inside
	 * {@code this} with {@code repl}.
	 * 
	 * @param target the current automaton
	 * @param search the domain instance containing the automaton to search
	 * @param repl   the domain instance containing the automaton to use as
	 *                   replacement
	 * 
	 * @return the automaton containing the replaced strings
	 */
	public RegexAutomaton replace(
			RegexAutomaton target,
			RegexAutomaton search,
			RegexAutomaton repl) {
		if (target.isBottom() || search.isBottom() || repl.isBottom())
			return RegexAutomaton.BOTTOM;

		try {
			return target.replace(search, repl);
		} catch (CyclicAutomatonException e) {
			return RegexAutomaton.TOP;
		}
	}

	@Override
	public Satisfiability containsChar(
			RegexAutomaton current,
			char c)
			throws SemanticException {
		if (current.isTop())
			return Satisfiability.UNKNOWN;
		if (current.isBottom())
			return Satisfiability.BOTTOM;

		return contains(current, RegexAutomaton.string(String.valueOf(c)));
	}

	/**
	 * Yields a new automaton recognizing each string of {@code this} automaton
	 * repeated k-times, with k belonging to {@code intv}.
	 * 
	 * @param current the current automaton
	 * @param intv    the interval
	 * 
	 * @return a new automaton recognizing each string of {@code this} automaton
	 *             repeated k-times, with k belonging to {@code intv}
	 * 
	 * @throws MathNumberConversionException if {@code intv} is iterated but is
	 *                                           not finite
	 */
	public RegexAutomaton repeat(
			RegexAutomaton current,
			IntInterval intv)
			throws MathNumberConversionException {
		if (current.isBottom())
			return current;
		else if (intv.isTop() || current.hasCycle())
			return current.star();
		else if (intv.isFinite()) {
			if (intv.isSingleton())
				return current.repeat(intv.getHigh().toLong());
			else {
				RegexAutomaton result = current.emptyLanguage();

				for (Long i : intv)
					result = result.union(current.repeat(i));
				return result;
			}
		} else
			return current.repeat(intv.getLow().toLong()).concat(current.star());
	}

	/**
	 * Yields a new automaton where trailing and leading whitespaces have been
	 * removed from {@code this}.
	 * 
	 * @param current the current automaton
	 * 
	 * @return a new automaton where trailing and leading whitespaces have been
	 *             removed from {@code this}
	 */
	public RegexAutomaton trim(
			RegexAutomaton current) {
		if (current.isBottom() || current.isTop())
			return current;

		return current.trim();
	}

	@Override
	public RegexAutomaton top() {
		return RegexAutomaton.TOP;
	}

	@Override
	public RegexAutomaton bottom() {
		return RegexAutomaton.BOTTOM;
	}

	private RegexAutomaton generate(
			Set<BinaryExpression> constraints,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (constraints == null)
			return bottom();

		String prefix = null, suffix = null;
		for (BinaryExpression expr : constraints)
			if (expr.getLeft() instanceof Constant && ((Constant) expr.getLeft()).getValue() instanceof String) {
				String val = (String) ((Constant) expr.getLeft()).getValue();
				if (expr.getOperator() instanceof ComparisonEq)
					return RegexAutomaton.TOP.singleString(val);
				else if (expr.getOperator() instanceof StringStartsWith)
					prefix = val;
				else if (expr.getOperator() instanceof StringEndsWith)
					suffix = val;
			}

		RegexAutomaton res = RegexAutomaton.TOP;
		if (prefix != null)
			res = res.singleString(prefix).concat(res);
		if (suffix != null)
			res = res.concat(res.singleString(suffix));
		return res;
	}

	@Override
	public Set<BinaryExpression> constraints(
			ValueDomain<?> requesting,
			ValueEnvironment<RegexAutomaton> state,
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
				RegexAutomaton value = eval(state, arg, pp, oracle);
				if (value.isTop())
					return ValueDomain.makeRangeConstraints(
							pp.getProgram().getTypes().getIntegerType(),
							0,
							null,
							e,
							pp);
				if (value.isBottom())
					return null;
				IntInterval length = value.length();
				try {
					return ValueDomain.makeRangeConstraints(
							pp.getProgram().getTypes().getIntegerType(),
							length.getLow().toInt(),
							length.highIsPlusInfinity() ? null : length.getHigh().toInt(),
							e,
							pp);
				} catch (MathNumberConversionException e1) {
					// should not happen
					throw new SemanticException("Cannot convert string length bound to int", e1);
				}
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
				RegexAutomaton left = eval(state, (ValueExpression) ((BinaryExpression) e).getLeft(), pp, oracle);
				RegexAutomaton right = eval(state, (ValueExpression) ((BinaryExpression) e).getRight(), pp, oracle);
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
				if (operator == StringIndexOfChar.INSTANCE || operator == StringIndexOf.INSTANCE) {
					IntInterval indexes = indexOf(left, right);
					try {
						return ValueDomain.makeRangeConstraints(
								pp.getProgram().getTypes().getIntegerType(),
								indexes.getLow().toInt(),
								indexes.highIsPlusInfinity() ? null : indexes.getHigh().toInt(),
								e,
								pp);
					} catch (MathNumberConversionException e1) {
						// should not happen
						throw new SemanticException("Cannot convert stirng indexof bound to int", e1);
					}
				} else if (operator == StringLastIndexOfChar.INSTANCE || operator == StringLastIndexOf.INSTANCE)
					// not handled for now
					return Collections.emptySet();
				else // operator == ValueComparison.INSTANCE
						// not handled for now
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
					|| operator == StringLastIndexOfFromIndex.INSTANCE) {
				// not handled for now
				return ValueDomain.makeRangeConstraints(
						pp.getProgram().getTypes().getIntegerType(),
						-1,
						null,
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

		RegexAutomaton value = eval(state, e, pp, oracle);
		if (value.isTop())
			return Collections.emptySet();
		if (value.isBottom())
			return null;
		if (!value.hasCycle() && !value.acceptsTopEventually()) {
			SortedSet<String> lang;
			try {
				lang = value.getLanguage();
			} catch (CyclicAutomatonException e1) {
				// should not happen
				throw new SemanticException("The automaton is cyclic", e1);
			}
			if (lang.size() == 1)
				return ValueDomain.makeEqConstraint(
						pp.getProgram().getTypes().getStringType(),
						lang.first(),
						e,
						pp);
			if (lang.size() == 0)
				return null;
			return Collections.emptySet();
		}

		String lcp = value.longestCommonPrefix();
		String lcs = value.reverse().longestCommonPrefix();

		Set<BinaryExpression> constr = new HashSet<>();
		BooleanType booleanType = pp.getProgram().getTypes().getBooleanType();
		constr.add(
				new BinaryExpression(
						booleanType,
						new Constant(pp.getProgram().getTypes().getStringType(), lcp, pp.getLocation()),
						e,
						StringIsPrefixOf.INSTANCE,
						e.getCodeLocation()));
		constr.add(
				new BinaryExpression(
						booleanType,
						new Constant(pp.getProgram().getTypes().getStringType(), lcs, pp.getLocation()),
						e,
						StringIsSuffixOf.INSTANCE,
						e.getCodeLocation()));
		// we could also add all substrings
		return constr;
	}
}
