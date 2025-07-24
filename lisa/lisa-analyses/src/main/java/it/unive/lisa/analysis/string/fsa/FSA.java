package it.unive.lisa.analysis.string.fsa;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.combination.smash.SmashedSumStringDomain;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.lattices.string.fsa.SimpleAutomaton;
import it.unive.lisa.lattices.string.fsa.StringSymbol;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.operator.binary.StringConcat;
import it.unive.lisa.symbolic.value.operator.binary.StringContains;
import it.unive.lisa.symbolic.value.operator.ternary.StringReplace;
import it.unive.lisa.util.collections.workset.FIFOWorkingSet;
import it.unive.lisa.util.collections.workset.WorkingSet;
import it.unive.lisa.util.datastructures.automaton.CyclicAutomatonException;
import it.unive.lisa.util.datastructures.automaton.State;
import it.unive.lisa.util.datastructures.automaton.Transition;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.lisa.util.numeric.MathNumberConversionException;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * A class that represent the Finite State Automaton domain for strings,
 * exploiting a {@link SimpleAutomaton}.<br>
 * <br>
 * <b>Caution:</b> the FSA domain is buggy and requires lots of resources, to
 * the point where it might be hard to debug also on relatively small samples.
 * Use with caution.
 *
 * @author <a href="mailto:simone.leoni2@studenti.unipr.it">Simone Leoni</a>
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
public class FSA
		implements
		SmashedSumStringDomain<SimpleAutomaton> {

	@Override
	public SimpleAutomaton evalNonNullConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (constant.getValue() instanceof String)
			return new SimpleAutomaton((String) constant.getValue());
		return SimpleAutomaton.TOP;
	}

	@Override
	public SimpleAutomaton evalBinaryExpression(
			BinaryExpression expression,
			SimpleAutomaton left,
			SimpleAutomaton right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (expression.getOperator() == StringConcat.INSTANCE)
			return left.concat(right);
		return SimpleAutomaton.TOP;
	}

	@Override
	public SimpleAutomaton evalTernaryExpression(
			TernaryExpression expression,
			SimpleAutomaton left,
			SimpleAutomaton middle,
			SimpleAutomaton right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (expression.getOperator() == StringReplace.INSTANCE)
			try {
				return left.replace(middle, right);
			} catch (CyclicAutomatonException e) {
				return SimpleAutomaton.TOP;
			}
		return SimpleAutomaton.TOP;
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(
			BinaryExpression expression,
			SimpleAutomaton left,
			SimpleAutomaton right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (expression.getOperator() == StringContains.INSTANCE)
			return contains(left, right);
		return Satisfiability.UNKNOWN;
	}

	@Override
	public SimpleAutomaton substring(
			SimpleAutomaton current,
			long begin,
			long end)
			throws SemanticException {
		if (current.isTop() || current.isBottom())
			return current;

		if (!current.hasCycle()) {
			SimpleAutomaton result = current.emptyLanguage();
			try {
				for (String s : current.getLanguage()) {
					if (begin < s.length() && end < s.length())
						result = result.union(new SimpleAutomaton(s.substring((int) begin, (int) end)));
					else
						result = result.union(new SimpleAutomaton(""));

					return result;
				}
			} catch (CyclicAutomatonException e) {
				throw new SemanticException("The automaton is cyclic", e);
			}
		}

		SimpleAutomaton[] array = current
				.toRegex()
				.substring((int) begin, (int) end)
				.parallelStream()
				.map(s -> new SimpleAutomaton(s.toString()))
				.toArray(SimpleAutomaton[]::new);

		SimpleAutomaton result = current.emptyLanguage();

		for (int i = 0; i < array.length; i++)
			result = result.union(array[i]);
		return result;
	}

	/**
	 * Yields the {@link IntInterval} containing the minimum and maximum length
	 * of the given abstract value.
	 * 
	 * @param current the current automaton
	 * 
	 * @return the minimum and maximum length of this abstract value
	 */
	public IntInterval length(
			SimpleAutomaton current) {
		return current.length();
	}

	@Override
	public IntInterval indexOf(
			SimpleAutomaton current,
			SimpleAutomaton s)
			throws SemanticException {
		if (current.hasCycle())
			return mkInterval(-1, null);

		if (!current.hasCycle() && !s.hasCycle()) {
			Set<String> first, second;
			try {
				first = current.getLanguage();
				second = s.getLanguage();
			} catch (CyclicAutomatonException e) {
				throw new SemanticException("The automaton is cyclic", e);
			}

			IntInterval result = null;
			for (String f1 : first) {
				for (String f2 : second) {
					IntInterval partial;

					if (f1.contains(f2)) {
						int i = f1.indexOf(f2);
						partial = mkInterval(i, i);
					} else {
						partial = mkInterval(-1, -1);
					}
					result = result == null ? partial : mkInterval(partial, result);
				}
			}

			return result;
		}

		HashSet<Integer> indexesOf = new HashSet<>();
		for (State q : current.getStates()) {
			SimpleAutomaton build = current.factorsChangingInitialState(q);
			if (!build.intersection(s).acceptsEmptyLanguage())
				indexesOf.add(current.maximumPath(q, q).size() - 1);
		}

		// No state in the automaton can read search
		if (indexesOf.isEmpty())
			return mkInterval(-1, -1);
		else if (s.recognizesExactlyOneString() && current.recognizesExactlyOneString())
			return mkInterval(
					indexesOf.stream().mapToInt(i -> i).min().getAsInt(),
					indexesOf.stream().mapToInt(i -> i).max().getAsInt());
		else
			return mkInterval(-1, indexesOf.stream().mapToInt(i -> i).max().getAsInt());
	}

	/**
	 * Yields the concatenation between two automata.
	 * 
	 * @param current the current automaton
	 * @param other   the other automaton
	 * 
	 * @return the concatenation between two automata
	 */
	public SimpleAutomaton concat(
			SimpleAutomaton current,
			SimpleAutomaton other) {
		return current.concat(other);
	}

	private IntInterval mkInterval(
			Integer min,
			Integer max) {
		return new IntInterval(min, max);
	}

	private IntInterval mkInterval(
			IntInterval first,
			IntInterval second) {
		MathNumber newLow = first.getLow().min(second.getLow());
		MathNumber newHigh = first.getHigh().max(second.getHigh());
		return new IntInterval(newLow, newHigh);
	}

	/**
	 * Yields if the given automaton recognizes strings recognized by the other
	 * automaton.
	 * 
	 * @param current the current automaton
	 * @param other   the other automaton
	 * 
	 * @return if the given automaton recognizes strings recognized by the other
	 *             automaton
	 */
	public Satisfiability contains(
			SimpleAutomaton current,
			SimpleAutomaton other) {
		if (other.isEqualTo(current.emptyString()))
			return Satisfiability.SATISFIED;
		else if (current.factors().intersection(other).acceptsEmptyLanguage())
			return Satisfiability.NOT_SATISFIED;
		else
			try {
				Set<String> rightLang = other.getLanguage();
				Set<String> leftLang = current.getLanguage();
				// right accepts only the empty string
				if (rightLang.size() == 1 && rightLang.contains(""))
					return Satisfiability.SATISFIED;

				// we can compare languages
				boolean atLeastOne = false, all = true;
				for (String a : leftLang)
					for (String b : rightLang) {
						boolean cont = a.contains(b);
						atLeastOne = atLeastOne || cont;
						all = all && cont;
					}

				if (all)
					return Satisfiability.SATISFIED;
				if (atLeastOne)
					return Satisfiability.UNKNOWN;
				return Satisfiability.NOT_SATISFIED;
			} catch (CyclicAutomatonException e) {
				return Satisfiability.UNKNOWN;
			}
	}

	/**
	 * Yields the replacement of occurrences of {@code search} inside
	 * {@code this} with {@code repl}.
	 * 
	 * @param current the current automaton
	 * @param search  the domain instance containing the automaton to search
	 * @param repl    the domain instance containing the automaton to use as
	 *                    replacement
	 * 
	 * @return the automaton containing the replaced strings
	 */
	public SimpleAutomaton replace(
			SimpleAutomaton current,
			SimpleAutomaton search,
			SimpleAutomaton repl) {
		try {
			return current.replace(search, repl);
		} catch (CyclicAutomatonException e) {
			return SimpleAutomaton.TOP;
		}
	}

	@Override
	public Satisfiability containsChar(
			SimpleAutomaton current,
			char c)
			throws SemanticException {
		if (current.isTop())
			return Satisfiability.UNKNOWN;
		if (current.isBottom())
			return Satisfiability.BOTTOM;
		if (!current.hasCycle()) {
			Satisfiability sat = Satisfiability.BOTTOM;
			try {
				for (String s : current.getLanguage())
					if (s.contains(String.valueOf(c)))
						sat = sat.lub(Satisfiability.SATISFIED);
					else
						sat = sat.lub(Satisfiability.NOT_SATISFIED);
			} catch (CyclicAutomatonException e) {
				// can ignore thanks to the guard
			}
			return sat;
		}

		WorkingSet<State> ws = FIFOWorkingSet.mk();
		Set<State> visited = new TreeSet<>();

		for (State q : current.getInitialStates())
			ws.push(q);

		while (!ws.isEmpty()) {
			State top = ws.pop();
			for (Transition<StringSymbol> tr : current.getOutgoingTransitionsFrom(top)) {
				if (tr.getSymbol().getSymbol().equals(String.valueOf(c)))
					return Satisfiability.SATISFIED;
			}
			visited.add(top);

			for (Transition<StringSymbol> tr : current.getOutgoingTransitionsFrom(top))
				if (visited.contains(tr.getDestination()))
					return Satisfiability.UNKNOWN;
				else
					ws.push(tr.getDestination());
		}

		return Satisfiability.NOT_SATISFIED;
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
	public SimpleAutomaton trim(
			SimpleAutomaton current) {
		if (current.isBottom() || current.isTop())
			return current;
		return current.trim();
	}

	/**
	 * Yields a new automaton recognizing each string of {@code this} automaton
	 * repeated k-times, with k belonging to {@code intv}.
	 * 
	 * @param current the current automaton
	 * @param i       the interval
	 * 
	 * @return a new automaton recognizing each string of {@code this} automaton
	 *             repeated k-times, with k belonging to {@code intv}
	 * 
	 * @throws MathNumberConversionException if {@code intv} is iterated but is
	 *                                           not finite
	 */
	public SimpleAutomaton repeat(
			SimpleAutomaton current,
			IntInterval i)
			throws MathNumberConversionException {
		if (current.isBottom() || current.isTop())
			return current;
		return current.repeat(i);
	}

	@Override
	public SimpleAutomaton top() {
		return SimpleAutomaton.TOP;
	}

	@Override
	public SimpleAutomaton bottom() {
		return SimpleAutomaton.TOP.bottom();
	}

}
