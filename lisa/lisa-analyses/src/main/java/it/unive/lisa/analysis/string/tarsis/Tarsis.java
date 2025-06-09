package it.unive.lisa.analysis.string.tarsis;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.numeric.Interval;
import it.unive.lisa.analysis.string.SmashedSumStringDomain;
import it.unive.lisa.analysis.string.fsa.FSA;
import it.unive.lisa.analysis.string.fsa.SimpleAutomaton;
import it.unive.lisa.analysis.string.fsa.StringSymbol;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.StringConcat;
import it.unive.lisa.symbolic.value.operator.binary.StringContains;
import it.unive.lisa.symbolic.value.operator.binary.StringEquals;
import it.unive.lisa.symbolic.value.operator.ternary.StringReplace;
import it.unive.lisa.symbolic.value.operator.ternary.TernaryOperator;
import it.unive.lisa.util.datastructures.automaton.CyclicAutomatonException;
import it.unive.lisa.util.datastructures.automaton.State;
import it.unive.lisa.util.datastructures.automaton.Transition;
import it.unive.lisa.util.datastructures.automaton.TransitionSymbol;
import it.unive.lisa.util.datastructures.regex.RegularExpression;
import it.unive.lisa.util.datastructures.regex.TopAtom;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.lisa.util.numeric.MathNumberConversionException;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A class that represent the Tarsis domain for strings, exploiting a
 * {@link RegexAutomaton}.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Tarsis implements SmashedSumStringDomain<Tarsis> {

	/**
	 * Top element of the domain
	 */
	private static final Tarsis TOP = new Tarsis();

	/**
	 * Top element of the domain
	 */
	private static final Tarsis BOTTOM = new Tarsis(RegexAutomaton.emptyLang());

	/**
	 * Maximum widening threshold, or default threshold if there is no
	 * difference in the size of the two automata.
	 */
	public static final int WIDENING_CAP = 5;

	/**
	 * Used to store the string representation
	 */
	private final RegexAutomaton a;

	/**
	 * Creates a new Tarsis object representing the TOP element.
	 */
	public Tarsis() {
		// top
		this.a = RegexAutomaton.topString();
	}

	/**
	 * Creates a new FSA object using a {@link SimpleAutomaton}.
	 * 
	 * @param a the {@link SimpleAutomaton} used for object construction.
	 */
	public Tarsis(
			RegexAutomaton a) {
		this.a = a;
	}

	/**
	 * Yields the {@link RegexAutomaton} backing this domain element.
	 * 
	 * @return the automaton
	 */
	public RegexAutomaton getAutomaton() {
		return a;
	}

	@Override
	public Tarsis lubAux(
			Tarsis other)
			throws SemanticException {
		return new Tarsis(this.a.union(other.a));
	}

	@Override
	public Tarsis glbAux(
			Tarsis other)
			throws SemanticException {
		return new Tarsis(this.a.intersection(other.a));
	}

	/**
	 * Yields the size of this string, that is, the number of states of the
	 * underlying automaton.
	 * 
	 * @return the size of this string
	 */
	public int size() {
		return a.getStates().size();
	}

	private int getSizeDiffCapped(
			Tarsis other) {
		int size = size();
		int otherSize = other.size();
		if (size > otherSize)
			return Math.min(size - otherSize, WIDENING_CAP);
		else if (size < otherSize)
			return Math.min(otherSize - size, WIDENING_CAP);
		else
			return WIDENING_CAP;
	}

	@Override
	public Tarsis wideningAux(
			Tarsis other)
			throws SemanticException {
		return new Tarsis(this.a.union(other.a).widening(getSizeDiffCapped(other)));
	}

	@Override
	public boolean lessOrEqualAux(
			Tarsis other)
			throws SemanticException {
		return this.a.isContained(other.a);
	}

	@Override
	public boolean equals(
			Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		Tarsis fsa = (Tarsis) o;
		return Objects.equals(a, fsa.a);
	}

	@Override
	public int hashCode() {
		return Objects.hash(a);
	}

	@Override
	public Tarsis top() {
		return TOP;
	}

	@Override
	public Tarsis bottom() {
		return BOTTOM;
	}

	@Override
	public boolean isBottom() {
		return !isTop() && this.a.acceptsEmptyLanguage();
	}

	@Override
	public StructuredRepresentation representation() {
		if (isBottom())
			return Lattice.bottomRepresentation();
		else if (isTop())
			return Lattice.topRepresentation();

		return new StringRepresentation(this.a.toRegex().simplify());
	}

	@Override
	public Tarsis evalNonNullConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (constant.getValue() instanceof String)
			return new Tarsis(RegexAutomaton.string((String) constant.getValue()));
		return top();
	}

	@Override
	public Tarsis evalBinaryExpression(
			BinaryOperator operator,
			Tarsis left,
			Tarsis right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (operator == StringConcat.INSTANCE)
			return new Tarsis(left.a.concat(right.a));
		return top();
	}

	@Override
	public Tarsis evalTernaryExpression(
			TernaryOperator operator,
			Tarsis left,
			Tarsis middle,
			Tarsis right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (operator == StringReplace.INSTANCE)
			return left.replace(middle, right);
		return TOP;
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(
			BinaryOperator operator,
			Tarsis left,
			Tarsis right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (operator == StringContains.INSTANCE)
			return left.contains(right);
		if (operator == StringEquals.INSTANCE)
			return left.eq(right);
		return Satisfiability.UNKNOWN;
	}

	/**
	 * Semantics of {@link StringEquals} between {@code this} and {@code other}.
	 * 
	 * @param other the other domain instance
	 * 
	 * @return the satisfiability result
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	public Satisfiability eq(
			Tarsis other)
			throws SemanticException {
		if (glb(other).isBottom())
			return Satisfiability.NOT_SATISFIED;
		if (this.a.hasCycle() || other.a.hasCycle())
			return Satisfiability.UNKNOWN;
		Satisfiability res = Satisfiability.BOTTOM;
		try {
			for (String a : this.a.getLanguage())
				for (String b : other.a.getLanguage()) {
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
			return Satisfiability.NOT_SATISFIED
					.lub(eq(a.substring(1), b.substring(1)))
					.lub(eq(a.substring(1), b))
					.lub(eq(a, b.substring(1)));
		// this should be unreachable
		return Satisfiability.UNKNOWN;
	}

	/**
	 * Semantics of {@link StringContains} between {@code this} and
	 * {@code other}.
	 * 
	 * @param other the other domain instance
	 * 
	 * @return the satisfiability result
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	public Satisfiability contains(
			Tarsis other)
			throws SemanticException {
		try {
			if (!a.hasCycle()
					&& !other.a.hasCycle()
					&& !a.acceptsTopEventually()
					&& !other.a.acceptsTopEventually()) {
				// we can compare languages
				boolean atLeastOne = false, all = true;
				for (String a : a.getLanguage())
					for (String b : other.a.getLanguage()) {
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

			if (!other.a.hasCycle() && other.a.getLanguage().size() == 1
					&& other.a.getLanguage().iterator().next().isEmpty())
				// the empty string is always contained
				return Satisfiability.SATISFIED;

			if (other.a.hasOnlyOnePath() && !other.a.acceptsTopEventually()) {
				Satisfiability allSat = Satisfiability.UNKNOWN;
				RegexAutomaton C = other.a.extractLongestString();
				String longest = C.getLanguage().iterator().next();
				RegexAutomaton withNoScc = a.minimize().makeAcyclic();
				SortedSet<String> lang = withNoScc.getLanguage();
				for (String a : lang)
					allSat = allSat.glb(contains(a, longest));

				if (!lang.isEmpty() && allSat == Satisfiability.SATISFIED)
					return allSat;
			}

			RegexAutomaton transformed = a.explode().factors();
			RegexAutomaton otherExploded = other.a.explode();
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
	public Tarsis substring(
			long begin,
			long end) {
		if (isTop() || isBottom())
			return this;

		RegexAutomaton[] array = this.a.toRegex().substring((int) begin, (int) end)
				.parallelStream()
				.map(s -> RegexAutomaton.string(s)).toArray(RegexAutomaton[]::new);

		RegexAutomaton result = RegexAutomaton.emptyLang();

		for (int i = 0; i < array.length; i++)
			result = result.union(array[i]);
		return new Tarsis(result);
	}

	@Override
	public IntInterval length() {
		int max = a.lenghtOfLongestString();
		int min = a.toRegex().minLength();
		return new IntInterval(Integer.valueOf(min), max == Integer.MAX_VALUE ? null : max);
	}

	@Override
	public IntInterval indexOf(
			Tarsis s)
			throws SemanticException {
		if (contains(s) == Satisfiability.NOT_SATISFIED)
			return new IntInterval(-1, -1);
		else if (a.hasCycle() || s.a.hasCycle() || s.a.acceptsTopEventually())
			return new IntInterval(MathNumber.MINUS_ONE, MathNumber.PLUS_INFINITY);
		Pair<Integer, Integer> interval;
		try {
			interval = IndexFinder.findIndexesOf(a, s.a);
		} catch (CyclicAutomatonException e) {
			throw new SemanticException("The automaton is cyclic", e);
		}
		return new IntInterval(interval.getLeft(), interval.getRight());
	}

	/**
	 * Yields the concatenation between two automata.
	 * 
	 * @param other the other automaton
	 * 
	 * @return the concatenation between two automata
	 */
	public Tarsis concat(
			Tarsis other) {
		return new Tarsis(this.a.concat(other.a));
	}

	/**
	 * Yields the replacement of occurrences of {@code search} inside
	 * {@code this} with {@code repl}.
	 * 
	 * @param search the domain instance containing the automaton to search
	 * @param repl   the domain instance containing the automaton to use as
	 *                   replacement
	 * 
	 * @return the domain instance containing the replaced automaton
	 */
	public Tarsis replace(
			Tarsis search,
			Tarsis repl) {
		if (isBottom() || search.isBottom() || repl.isBottom())
			return bottom();

		try {
			return new Tarsis(this.a.replace(search.a, repl.a));
		} catch (CyclicAutomatonException e) {
			return TOP;
		}
	}

	/**
	 * Converts this domain instance to one of {@link FSA}, that uses single
	 * characters as transition symbols.
	 * 
	 * @return the converted domain instance
	 */
	public FSA toFSA() {
		RegexAutomaton exploded = this.a.minimize().explode();
		SortedSet<Transition<StringSymbol>> fsaDelta = new TreeSet<>();

		if (!this.a.acceptsTopEventually()) {
			for (Transition<RegularExpression> t : exploded.getTransitions())
				fsaDelta.add(new Transition<>(t.getSource(), t.getDestination(),
						new StringSymbol(t.getSymbol().toString())));

			return new FSA(new SimpleAutomaton(exploded.getStates(), fsaDelta));
		}

		SortedSet<State> fsaStates = new TreeSet<>(exploded.getStates());

		for (Transition<RegularExpression> t : exploded.getTransitions()) {
			if (t.getSymbol() != TopAtom.INSTANCE)
				fsaDelta.add(new Transition<>(t.getSource(), t.getDestination(),
						new StringSymbol(t.getSymbol().toString())));
			else {
				for (char c = 32; c <= 123; c++)
					fsaDelta.add(new Transition<>(t.getSource(), t.getDestination(), new StringSymbol(c)));
				fsaDelta.add(new Transition<>(t.getSource(), t.getDestination(), StringSymbol.EPSILON));
			}
		}

		SimpleAutomaton fsa = new SimpleAutomaton(fsaStates, fsaDelta).minimize();
		return new FSA(fsa);
	}

	@Override
	public Satisfiability containsChar(
			char c)
			throws SemanticException {
		if (isTop())
			return Satisfiability.UNKNOWN;
		if (isBottom())
			return Satisfiability.BOTTOM;

		return satisfiesBinaryExpression(StringContains.INSTANCE, this,
				new Tarsis(RegexAutomaton.string(String.valueOf(c))), null, null);
	}

	/**
	 * Yields a new Tarsis's instance recognizing each string of {@code this}
	 * automaton repeated k-times, with k belonging to {@code intv}.
	 * 
	 * @param intv the interval
	 * 
	 * @return a new Tarsis's instance recognizing each string of {@code this}
	 *             automaton repeated k-times, with k belonging to {@code intv}
	 * 
	 * @throws MathNumberConversionException if {@code intv} is iterated but is
	 *                                           not finite
	 */
	public Tarsis repeat(
			Interval intv)
			throws MathNumberConversionException {
		if (isBottom())
			return this;
		else if (intv.isTop() || a.hasCycle())
			return new Tarsis(a.star());
		else if (intv.interval.isFinite()) {
			if (intv.interval.isSingleton())
				return new Tarsis(a.repeat(intv.interval.getHigh().toLong()));
			else {
				RegexAutomaton result = a.emptyLanguage();

				for (Long i : intv.interval)
					result = result.union(a.repeat(i));
				return new Tarsis(result);
			}
		} else
			return new Tarsis(a.repeat(intv.interval.getLow().toLong()).concat(a.star()));
	}

	/**
	 * Yields a new Tarsis's instance where trailing and leading whitespaces
	 * have been removed from {@code this}.
	 * 
	 * @return a new Tarsis's instance where trailing and leading whitespaces
	 *             have been removed from {@code this}
	 */
	public Tarsis trim() {
		if (isBottom() || isTop())
			return this;

		return new Tarsis(this.a.trim());
	}
}
