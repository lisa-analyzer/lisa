package it.unive.lisa.analysis.string.tarsis;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.combination.constraints.WholeValueStringDomain;
import it.unive.lisa.analysis.combination.smash.SmashedSumStringDomain;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.lattices.string.tarsis.IndexFinder;
import it.unive.lisa.lattices.string.tarsis.RegexAutomaton;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.symbolic.value.operator.binary.StringConcat;
import it.unive.lisa.symbolic.value.operator.binary.StringContains;
import it.unive.lisa.symbolic.value.operator.binary.StringEquals;
import it.unive.lisa.symbolic.value.operator.ternary.StringReplace;
import it.unive.lisa.type.BooleanType;
import it.unive.lisa.util.datastructures.automaton.CyclicAutomatonException;
import it.unive.lisa.util.datastructures.automaton.TransitionSymbol;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.lisa.util.numeric.MathNumberConversionException;
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
public class Tarsis implements SmashedSumStringDomain<RegexAutomaton>, WholeValueStringDomain<RegexAutomaton> {

	@Override
	public RegexAutomaton evalNonNullConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (constant.getValue() instanceof String)
			return RegexAutomaton.string((String) constant.getValue());
		return top();
	}

	@Override
	public RegexAutomaton evalBinaryExpression(
			BinaryExpression expression,
			RegexAutomaton left,
			RegexAutomaton right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (expression.getOperator() == StringConcat.INSTANCE)
			return left.concat(right);
		return top();
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
		if (expression.getOperator() == StringReplace.INSTANCE)
			return replace(left, middle, right);
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
		BinaryOperator operator = expression.getOperator();
		if (operator == StringContains.INSTANCE)
			return contains(left, right);
		if (operator == StringEquals.INSTANCE)
			return eq(left, right);
		return Satisfiability.UNKNOWN;
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
	public RegexAutomaton substring(
			RegexAutomaton current,
			Set<BinaryExpression> a1,
			Set<BinaryExpression> a2,
			ProgramPoint pp)
			throws SemanticException {
		if (current.isBottom() || a1 == null || a2 == null)
			return RegexAutomaton.BOTTOM;

		Integer minI = null, maxI = null;
		for (BinaryExpression expr : a1)
			if (expr.getLeft() instanceof Constant && ((Constant) expr.getLeft()).getValue() instanceof Integer) {
				Integer val = (Integer) ((Constant) expr.getLeft()).getValue();
				if (expr.getOperator() instanceof ComparisonEq)
					minI = maxI = val;
				else if (expr.getOperator() instanceof ComparisonGe)
					maxI = val;
				else if (expr.getOperator() instanceof ComparisonLe)
					minI = val;
			}
		if (minI == null || minI < 0)
			minI = 0;
		if (maxI != null && maxI < minI)
			maxI = minI;

		Integer minJ = null, maxJ = null;
		for (BinaryExpression expr : a2)
			if (expr.getLeft() instanceof Constant && ((Constant) expr.getLeft()).getValue() instanceof Integer) {
				Integer val = (Integer) ((Constant) expr.getLeft()).getValue();
				if (expr.getOperator() instanceof ComparisonEq)
					minJ = maxJ = val;
				else if (expr.getOperator() instanceof ComparisonGe)
					maxJ = val;
				else if (expr.getOperator() instanceof ComparisonLe)
					minJ = val;
			}
		if (minJ == null || minJ < 0)
			minJ = 0;
		if (maxJ != null && maxJ < minJ)
			maxJ = minJ;

		if (maxI == null || maxJ == null)
			return RegexAutomaton.TOP;

		RegexAutomaton partial = RegexAutomaton.BOTTOM;
		RegexAutomaton temp;

		outer: for (int i = minI; i <= maxI; i++)
			for (int j = minJ; j <= maxJ; j++) {
				if (i < j)
					temp = partial.lub(substring(current, i, j));
				else if (i == j)
					temp = partial.lub(current.emptyString());
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

	@Override
	public Set<BinaryExpression> indexOf_constr(
			BinaryExpression expression,
			RegexAutomaton current,
			RegexAutomaton other,
			ProgramPoint pp)
			throws SemanticException {
		if (current.isBottom() || other.isBottom())
			return null;

		IntInterval indexes = indexOf(current, other);
		BooleanType booleanType = pp.getProgram().getTypes().getBooleanType();

		Set<BinaryExpression> constr = new HashSet<>();
		try {
			constr.add(
				new BinaryExpression(
					booleanType,
					new Constant(
						pp.getProgram().getTypes().getIntegerType(),
						indexes.getLow().toInt(),
						pp.getLocation()),
					expression,
					ComparisonLe.INSTANCE,
					pp.getLocation()));
			if (indexes.getHigh().isFinite())
				constr.add(
					new BinaryExpression(
						booleanType,
						new Constant(
							pp.getProgram().getTypes().getIntegerType(),
							indexes.getHigh().toInt(),
							pp.getLocation()),
						expression,
						ComparisonGe.INSTANCE,
						pp.getLocation()));
		} catch (MathNumberConversionException e1) {
			throw new SemanticException("Cannot convert stirng indexof bound to int", e1);
		}
		return constr;
	}

	@Override
	public RegexAutomaton top() {
		return RegexAutomaton.TOP;
	}

	@Override
	public RegexAutomaton bottom() {
		return RegexAutomaton.BOTTOM;
	}

}
