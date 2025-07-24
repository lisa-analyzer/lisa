package it.unive.lisa.lattices.string.tarsis;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.combination.constraints.WholeValueElement;
import it.unive.lisa.analysis.string.fsa.FSA;
import it.unive.lisa.lattices.string.fsa.SimpleAutomaton;
import it.unive.lisa.lattices.string.fsa.StringSymbol;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.symbolic.value.operator.binary.StringEndsWith;
import it.unive.lisa.symbolic.value.operator.binary.StringStartsWith;
import it.unive.lisa.symbolic.value.operator.unary.StringLength;
import it.unive.lisa.type.BooleanType;
import it.unive.lisa.util.datastructures.automaton.Automaton;
import it.unive.lisa.util.datastructures.automaton.CyclicAutomatonException;
import it.unive.lisa.util.datastructures.automaton.State;
import it.unive.lisa.util.datastructures.automaton.Transition;
import it.unive.lisa.util.datastructures.regex.Atom;
import it.unive.lisa.util.datastructures.regex.RegularExpression;
import it.unive.lisa.util.datastructures.regex.TopAtom;
import it.unive.lisa.util.datastructures.regex.symbolic.SymbolicChar;
import it.unive.lisa.util.datastructures.regex.symbolic.SymbolicString;
import it.unive.lisa.util.datastructures.regex.symbolic.UnknownSymbolicChar;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumberConversionException;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A class that describes an generic automaton(dfa, nfa, epsilon nfa) using an
 * alphabet of strings, extended with a special symbol for statically unknown
 * ones. Transition symbols are {@link RegularExpression}s.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class RegexAutomaton
		extends
		Automaton<RegexAutomaton, RegularExpression>
		implements
		BaseLattice<RegexAutomaton>,
		WholeValueElement<RegexAutomaton> {

	/**
	 * Builds a {@link RegexAutomaton} recognizing the top string, that is, with
	 * a single transition recognizing {@link TopAtom}.
	 * 
	 * @return the automaton
	 */
	public static RegexAutomaton topString() {
		State q0 = new State(0, true, true);
		State q1 = new State(1, false, true);

		SortedSet<State> states = new TreeSet<>();
		states.add(q0);
		states.add(q1);

		SortedSet<Transition<RegularExpression>> delta = new TreeSet<>();
		delta.add(new Transition<>(q0, q1, TopAtom.INSTANCE));

		RegexAutomaton result = new RegexAutomaton(states, delta);
		result.deterministic = Optional.of(true);
		result.minimized = Optional.of(true);
		return result;
	}

	/**
	 * Builds a {@link RegexAutomaton} recognizing the empty language.
	 * 
	 * @return the automaton
	 */
	public static RegexAutomaton emptyLang() {
		SortedSet<State> newStates = new TreeSet<>();
		State initialState = new State(0, true, false);
		newStates.add(initialState);

		RegexAutomaton result = new RegexAutomaton(newStates, Collections.emptySortedSet());
		result.deterministic = Optional.of(true);
		result.minimized = Optional.of(true);
		return result;
	}

	/**
	 * Builds a {@link RegexAutomaton} recognizing the given string.
	 * 
	 * @param string the string to recognize
	 * 
	 * @return the automaton
	 */
	public static RegexAutomaton string(
			String string) {
		State q0 = new State(0, true, false);
		State q1 = new State(1, false, true);

		SortedSet<State> states = new TreeSet<>();
		states.add(q0);
		states.add(q1);

		SortedSet<Transition<RegularExpression>> delta = new TreeSet<>();
		delta.add(new Transition<>(q0, q1, new Atom(string)));

		RegexAutomaton result = new RegexAutomaton(states, delta);
		result.deterministic = Optional.of(true);
		result.minimized = Optional.of(true);
		return result;
	}

	/**
	 * Builds a {@link RegexAutomaton} recognizing the given string.
	 * 
	 * @param s the string to recognize
	 * 
	 * @return the automaton
	 */
	public static RegexAutomaton string(
			SymbolicString s) {
		List<RegexAutomaton> result = new ArrayList<>();
		String collector = "";
		for (SymbolicChar ch : s.collapseTopChars()) {
			if (ch instanceof UnknownSymbolicChar) {
				if (!collector.isEmpty())
					result.add(string(collector));

				collector = "";
				result.add(topString());
			} else
				collector += ch.asChar();
		}

		if (!collector.isEmpty())
			result.add(string(collector));

		if (result.isEmpty())
			return emptyStr();

		if (result.size() == 1)
			return result.get(0);

		RegexAutomaton r = result.get(0);
		for (int i = 1; i < result.size(); i++)
			r = r.concat(result.get(i));

		return r;
	}

	/**
	 * Builds a {@link RegexAutomaton} recognizing the empty string.
	 * 
	 * @return the automaton
	 */
	public static RegexAutomaton emptyStr() {
		State q0 = new State(0, true, false);
		State q1 = new State(1, false, true);

		SortedSet<State> states = new TreeSet<>();
		states.add(q0);
		states.add(q1);

		SortedSet<Transition<RegularExpression>> delta = new TreeSet<>();
		delta.add(new Transition<>(q0, q1, Atom.EPSILON));

		RegexAutomaton result = new RegexAutomaton(states, delta);
		result.deterministic = Optional.of(true);
		result.minimized = Optional.of(true);
		return result;
	}

	/**
	 * Builds a {@link RegexAutomaton} recognizing the given strings.
	 * 
	 * @param strings the strings to recognize
	 * 
	 * @return the automaton
	 */
	public static RegexAutomaton strings(
			String... strings) {
		RegexAutomaton a = emptyLang();

		for (String s : strings)
			a = a.union(string(s));

		return a;
	}

	@Override
	public RegexAutomaton singleString(
			String string) {
		return string(string);
	}

	@Override
	public RegexAutomaton unknownString() {
		return topString();
	}

	@Override
	public RegexAutomaton emptyLanguage() {
		return emptyLang();
	}

	@Override
	public RegexAutomaton emptyString() {
		return emptyStr();
	}

	@Override
	public RegexAutomaton from(
			SortedSet<State> states,
			SortedSet<Transition<RegularExpression>> transitions) {
		return new RegexAutomaton(states, transitions);
	}

	@Override
	public RegularExpression epsilon() {
		return Atom.EPSILON;
	}

	@Override
	public RegularExpression concat(
			RegularExpression first,
			RegularExpression second) {
		return first.comp(second);
	}

	@Override
	public RegularExpression symbolToRegex(
			RegularExpression symbol) {
		return symbol;
	}

	/**
	 * Builds a new empty automaton.
	 */
	public RegexAutomaton() {
		super();
	}

	/**
	 * Builds a new automaton with given {@code states} and {@code transitions}.
	 *
	 * @param states      the set of states of the new automaton
	 * @param transitions the set of the transitions of the new automaton
	 */
	public RegexAutomaton(
			SortedSet<State> states,
			SortedSet<Transition<RegularExpression>> transitions) {
		super(states, transitions);
	}

	/**
	 * Yields {@code true} if and only if there is at least one transition in
	 * this automaton that recognizes a top string.
	 * 
	 * @return {@code true} if that condition holds
	 */
	public boolean acceptsTopEventually() {
		removeUnreachableStates();
		for (Transition<RegularExpression> t : transitions)
			if (t.getSymbol() instanceof TopAtom)
				return true;

		return false;
	}

	/**
	 * Yields a new automaton that is built by exploding this one, that is, by
	 * ensuring that each transition recognizes regular expressions of at most
	 * one character (excluding the ones recognizing the top string). <br>
	 * <br>
	 * <b>This automaton is never modified by this method</b>.
	 * 
	 * @return the exploded automaton
	 */
	public RegexAutomaton explode() {
		SortedSet<State> exStates = new TreeSet<>();
		SortedSet<Transition<RegularExpression>> exTransitions = new TreeSet<>();
		int counter = 0;
		Map<State, State> mapping = new HashMap<>();

		for (State origin : states) {
			State st = new State(counter++, origin.isInitial(), origin.isFinal());
			State replaced = mapping.computeIfAbsent(origin, s -> st);
			exStates.add(replaced);
			for (Transition<RegularExpression> t : getOutgoingTransitionsFrom(origin)) {
				State st1 = new State(counter++, t.getDestination().isInitial(), t.getDestination().isFinal());
				State dest = mapping.computeIfAbsent(t.getDestination(), s -> st1);
				exStates.add(dest);

				if (t.getSymbol().maxLength() < 2)
					exTransitions.add(new Transition<>(replaced, dest, t.getSymbol()));
				else {
					RegularExpression[] regexes = t.getSymbol().explode();
					State last = replaced;
					for (RegularExpression regex : regexes)
						if (regex == regexes[regexes.length - 1])
							exTransitions.add(new Transition<>(last, dest, regex));
						else {
							State temp = new State(counter++, false, false);
							exStates.add(temp);
							exTransitions.add(new Transition<>(last, temp, regex));
							last = temp;
						}
				}
			}
		}

		return new RegexAutomaton(exStates, exTransitions).minimize();
	}

	@Override
	public RegexAutomaton intersection(
			RegexAutomaton other) {
		if (this == other)
			return this;

		int code = 0;
		Map<State, Pair<State, State>> stateMapping = new HashMap<>();
		SortedSet<State> newStates = new TreeSet<>();
		SortedSet<Transition<RegularExpression>> newDelta = new TreeSet<Transition<RegularExpression>>();

		for (State s1 : states)
			for (State s2 : other.states) {
				State s = new State(code++, s1.isInitial() && s2.isInitial(), s1.isFinal() && s2.isFinal());
				stateMapping.put(s, Pair.of(s1, s2));
				newStates.add(s);
			}

		for (Transition<RegularExpression> t1 : getTransitions()) {
			for (Transition<RegularExpression> t2 : other.getTransitions()) {
				State from = getStateFromPair(stateMapping, Pair.of(t1.getSource(), t2.getSource()));
				State to = getStateFromPair(stateMapping, Pair.of(t1.getDestination(), t2.getDestination()));

				if (t1.getSymbol().equals(t2.getSymbol()))
					newDelta.add(new Transition<RegularExpression>(from, to, t1.getSymbol()));
				else if (t1.getSymbol() == TopAtom.INSTANCE && t2.getSymbol() != TopAtom.INSTANCE)
					newDelta.add(new Transition<RegularExpression>(from, to, t2.getSymbol()));
				else if (t1.getSymbol() != TopAtom.INSTANCE && t2.getSymbol() == TopAtom.INSTANCE)
					newDelta.add(new Transition<RegularExpression>(from, to, t1.getSymbol()));
			}
		}

		RegexAutomaton result = from(newStates, newDelta).minimize();
		return result;
	}

	/**
	 * Yields a new automaton that is built by collapsing {@code this}, that is,
	 * by merging together subsequent states that are never the root of a
	 * branch, the destination of a loop, or that have at least one outgoing
	 * transition recognizing the top string.<br>
	 * <br>
	 * <b>{@code this} is never modified by this method</b>.
	 * 
	 * @return the collapsed automaton
	 */
	public RegexAutomaton collapse() {
		HashSet<Vector<State>> collected = new HashSet<>();

		Set<List<State>> paths = getAllPaths();
		if (paths.isEmpty())
			return this;

		for (List<State> v : paths)
			collected.addAll(findMergableStatesInPath(v));

		if (collected.isEmpty())
			return this;

		RegexAutomaton collapsed = copy();
		Set<Transition<RegularExpression>> edgesToRemove = new HashSet<>();
		Set<State> statesToRemove = new HashSet<>();
		for (Vector<State> v : collected) {
			String accumulated = "";
			if (v.size() == 1)
				statesToRemove.add(v.firstElement());
			else
				for (int i = 0; i < v.size() - 1; i++) {
					State from = v.get(i);
					State to = v.get(i + 1);
					Transition<RegularExpression> t = getAllTransitionsConnecting(from, to).iterator().next();
					accumulated += ((Atom) t.getSymbol()).toString();
					edgesToRemove.add(t);
					statesToRemove.add(from);
					statesToRemove.add(to);
				}

			Transition<RegularExpression> in = collapsed.getIngoingTransitionsFrom(v.firstElement()).iterator().next();
			edgesToRemove.add(in);
			accumulated = ((Atom) in.getSymbol()).toString() + accumulated;
			Transition<RegularExpression> out = collapsed.getOutgoingTransitionsFrom(v.lastElement()).iterator().next();
			edgesToRemove.add(out);
			accumulated += ((Atom) out.getSymbol()).toString();

			collapsed.addTransition(in.getSource(), out.getDestination(), new Atom(accumulated));
		}

		collapsed.removeTransitions(edgesToRemove);
		collapsed.removeStates(statesToRemove);
		return collapsed.minimize();
	}

	private Set<Vector<State>> findMergableStatesInPath(
			List<State> v) {
		Set<Vector<State>> collected = new HashSet<>();
		if (v.size() == 1)
			return collected;

		Vector<State> sequence = new Vector<>();
		boolean collecting = false;
		Set<Transition<RegularExpression>> tmp;
		for (int i = 0; i < v.size() - 1; i++) {
			State from = v.get(i);
			State to = v.get(i + 1);
			if (getAllTransitionsConnecting(from, to).size() != 1) {
				// more than one edge connecting the nodes: this is an or
				if (collecting) {
					collecting = false;
					collected.add(sequence);
					sequence = new Vector<>();
				}
			} else if (getIngoingTransitionsFrom(to).size() != 1) {
				// more than one edge reaching `to`: this is the join of an or
				if (collecting) {
					collecting = false;
					collected.add(sequence);
					sequence = new Vector<>();
				}
			} else if ((tmp = getOutgoingTransitionsFrom(to)).size() == 1
					&& !(tmp.iterator().next().getSymbol() instanceof TopAtom)) {
				// reading just a symbol that is not top!
				sequence.add(to);
				if (!collecting)
					collecting = true;
			} else if (collecting) {
				collecting = false;
				collected.add(sequence);
				sequence = new Vector<>();
			}
		}

		return collected;
	}

	/**
	 * Yields a new automaton where all occurrences of strings recognized by
	 * {@code toReplace} are replaced with the automaton {@code str}, assuming
	 * that {@code toReplace} is finite (i.e., no loops nor top-transitions).
	 * The resulting automaton is then collapsed.<br>
	 * <br>
	 * If {@code toReplace} recognizes a single string, than this method
	 * performs a must-replacement, meaning that the string recognized by
	 * {@code toReplace} will effectively be replaced. Otherwise, occurrences of
	 * strings of {@code toReplace} are not replaced in the resulting automaton:
	 * instead, a branching will be introduced to model an or between the
	 * original string of {@code toReplace} and the whole {@code str}. <br>
	 * <br>
	 * <b>{@code this} is never modified by this method</b>.
	 * 
	 * @param toReplace the automaton recognizing the strings to replace
	 * @param str       the automaton that must be used as replacement
	 * 
	 * @return the replaced automaton
	 * 
	 * @throws CyclicAutomatonException if {@code toReplace} contains loops
	 */
	public RegexAutomaton replace(
			RegexAutomaton toReplace,
			RegexAutomaton str)
			throws CyclicAutomatonException {
		Collection<RegexAutomaton> automata = new ArrayList<>();
		boolean isSingleString = toReplace.getLanguage().size() == 1;
		for (String s : toReplace.getLanguage())
			automata.add(new StringReplacer(this).replace(s, str, isSingleString).collapse());

		if (automata.size() == 1)
			return automata.iterator().next();

		return union(automata.toArray(new RegexAutomaton[automata.size()]));
	}

	private RegexAutomaton union(
			RegexAutomaton... automata) {
		RegexAutomaton result = emptyLanguage();

		for (RegexAutomaton a : automata)
			result = a.union(result);

		return result;
	}

	/**
	 * Yields an automaton that corresponds to the {@code n}-time concatenation
	 * of {@code this}.
	 * 
	 * @param n the number of repetitions
	 * 
	 * @return an automaton that corresponds to the {@code n}-time concatenation
	 *             of {@code this}
	 */
	public RegexAutomaton repeat(
			long n) {
		if (n == 0)
			return emptyString();
		return toRegex().simplify().repeat(n).toAutomaton(this).minimize();
	}

	/**
	 * Yields a new automaton where leading whitespaces have been removed from
	 * {@code this}.
	 * 
	 * @return a new automaton where leading whitespaces have been removed from
	 *             {@code this}
	 */
	public RegexAutomaton trimLeft() {
		return this.toRegex().trimLeft().simplify().toAutomaton(this);
	}

	/**
	 * Yields a new automaton where trailing whitespaces have been removed from
	 * {@code this}.
	 * 
	 * @return a new automaton where trailing whitespaces have been removed from
	 *             {@code this}
	 */
	public RegexAutomaton trimRight() {
		return this.toRegex().trimRight().simplify().toAutomaton(this);
	}

	/**
	 * Yields a new automaton where trailing and leading whitespaces have been
	 * removed from {@code this}.
	 * 
	 * @return a new automaton where trailing and leading whitespaces have been
	 *             removed from {@code this}
	 */
	public RegexAutomaton trim() {
		return this.toRegex().trimRight().simplify().trimLeft().simplify().toAutomaton(this);
	}

	/**
	 * Top element of the domain.
	 */
	public static final RegexAutomaton TOP = topString();

	/**
	 * Top element of the domain.
	 */
	public static final RegexAutomaton BOTTOM = RegexAutomaton.emptyLang();

	/**
	 * Maximum widening threshold, or default threshold if there is no
	 * difference in the size of the two automata.
	 */
	private static final int WIDENING_CAP = 5;

	@Override
	public RegexAutomaton top() {
		return TOP;
	}

	@Override
	public RegexAutomaton bottom() {
		return BOTTOM;
	}

	@Override
	public boolean isBottom() {
		return !isTop() && acceptsEmptyLanguage();
	}

	@Override
	public StructuredRepresentation representation() {
		if (isBottom())
			return Lattice.bottomRepresentation();
		else if (isTop())
			return Lattice.topRepresentation();

		return new StringRepresentation(toRegex().simplify());
	}

	@Override
	public RegexAutomaton lubAux(
			RegexAutomaton other)
			throws SemanticException {
		return this.union(other);
	}

	@Override
	public RegexAutomaton glbAux(
			RegexAutomaton other)
			throws SemanticException {
		return this.intersection(other);
	}

	private int getSizeDiffCapped(
			RegexAutomaton other) {
		int size = getStates().size();
		int otherSize = other.getStates().size();
		if (size > otherSize)
			return Math.min(size - otherSize, WIDENING_CAP);
		else if (size < otherSize)
			return Math.min(otherSize - size, WIDENING_CAP);
		else
			return WIDENING_CAP;
	}

	@Override
	public RegexAutomaton wideningAux(
			RegexAutomaton other)
			throws SemanticException {
		return this.union(other).widening(getSizeDiffCapped(other));
	}

	@Override
	public boolean lessOrEqualAux(
			RegexAutomaton other)
			throws SemanticException {
		return this.isContained(other);
	}

	@Override
	public Set<BinaryExpression> constraints(
			ValueExpression e,
			ProgramPoint pp)
			throws SemanticException {
		if (isBottom())
			return null;

		BooleanType booleanType = pp.getProgram().getTypes().getBooleanType();
		UnaryExpression strlen = new UnaryExpression(
				pp.getProgram().getTypes().getIntegerType(),
				e,
				StringLength.INSTANCE,
				pp.getLocation());

		if (isTop())
			return Collections
					.singleton(
							new BinaryExpression(
									booleanType,
									new Constant(pp.getProgram().getTypes().getIntegerType(), 0, pp.getLocation()),
									strlen,
									ComparisonLe.INSTANCE,
									e.getCodeLocation()));

		try {
			if (!hasCycle() && getLanguage().size() == 1) {
				String str = getLanguage().iterator().next();
				return Set
						.of(
								new BinaryExpression(
										booleanType,
										new Constant(
												pp.getProgram().getTypes().getIntegerType(),
												str.length(),
												pp.getLocation()),
										strlen,
										ComparisonLe.INSTANCE,
										e.getCodeLocation()),
								new BinaryExpression(
										booleanType,
										new Constant(
												pp.getProgram().getTypes().getIntegerType(),
												str.length(),
												pp.getLocation()),
										strlen,
										ComparisonGe.INSTANCE,
										e.getCodeLocation()),
								new BinaryExpression(
										booleanType,
										new Constant(pp.getProgram().getTypes().getStringType(), str, pp.getLocation()),
										e,
										ComparisonEq.INSTANCE,
										e.getCodeLocation()));
			}
		} catch (CyclicAutomatonException e1) {
			// should be unreachable, since we check for cycles before
			throw new SemanticException("The automaton is cyclic", e1);
		}

		IntInterval length = length();
		String lcp = longestCommonPrefix();
		String lcs = reverse().longestCommonPrefix();

		Set<BinaryExpression> constr = new HashSet<>();
		try {
			constr
					.add(
							new BinaryExpression(
									booleanType,
									new Constant(
											pp.getProgram().getTypes().getIntegerType(),
											length.getLow().toInt(),
											pp.getLocation()),
									strlen,
									ComparisonLe.INSTANCE,
									e.getCodeLocation()));
			if (length.getHigh().isFinite())
				constr
						.add(
								new BinaryExpression(
										booleanType,
										new Constant(
												pp.getProgram().getTypes().getIntegerType(),
												length.getHigh().toInt(),
												pp.getLocation()),
										strlen,
										ComparisonGe.INSTANCE,
										e.getCodeLocation()));
		} catch (MathNumberConversionException e1) {
			throw new SemanticException("Cannot convert stirng length bound to int", e1);
		}

		constr
				.add(
						new BinaryExpression(
								booleanType,
								new Constant(pp.getProgram().getTypes().getStringType(), lcp, pp.getLocation()),
								e,
								StringStartsWith.INSTANCE,
								e.getCodeLocation()));
		constr
				.add(
						new BinaryExpression(
								booleanType,
								new Constant(pp.getProgram().getTypes().getStringType(), lcs, pp.getLocation()),
								e,
								StringEndsWith.INSTANCE,
								e.getCodeLocation()));
		return constr;
	}

	@Override
	public RegexAutomaton generate(
			Set<BinaryExpression> constraints,
			ProgramPoint pp)
			throws SemanticException {
		if (constraints == null)
			return bottom();

		String prefix = null, suffix = null;
		for (BinaryExpression expr : constraints)
			if (expr.getLeft() instanceof Constant && ((Constant) expr.getLeft()).getValue() instanceof String) {
				String val = (String) ((Constant) expr.getLeft()).getValue();
				if (expr.getOperator() instanceof ComparisonEq)
					return singleString(val);
				else if (expr.getOperator() instanceof StringStartsWith)
					prefix = val;
				else if (expr.getOperator() instanceof StringEndsWith)
					suffix = val;
			}

		RegexAutomaton res = TOP;
		if (prefix != null)
			res = singleString(prefix).concat(res);
		if (suffix != null)
			res = res.concat(singleString(suffix));
		return res;
	}

	/**
	 * Converts this domain instance to one used by {@link FSA}
	 * ({@link SimpleAutomaton}), that uses single characters as transition
	 * symbols.
	 * 
	 * @return the converted domain instance
	 */
	public SimpleAutomaton toSimpleAutomaton() {
		RegexAutomaton exploded = minimize().explode();
		SortedSet<Transition<StringSymbol>> fsaDelta = new TreeSet<>();

		if (!acceptsTopEventually()) {
			for (Transition<RegularExpression> t : exploded.getTransitions())
				fsaDelta
						.add(
								new Transition<>(
										t.getSource(),
										t.getDestination(),
										new StringSymbol(t.getSymbol().toString())));

			return new SimpleAutomaton(exploded.getStates(), fsaDelta);
		}

		SortedSet<State> fsaStates = new TreeSet<>(exploded.getStates());

		for (Transition<RegularExpression> t : exploded.getTransitions()) {
			if (t.getSymbol() != TopAtom.INSTANCE)
				fsaDelta
						.add(
								new Transition<>(
										t.getSource(),
										t.getDestination(),
										new StringSymbol(t.getSymbol().toString())));
			else {
				for (char c = 32; c <= 123; c++)
					fsaDelta.add(new Transition<>(t.getSource(), t.getDestination(), new StringSymbol(c)));
				fsaDelta.add(new Transition<>(t.getSource(), t.getDestination(), StringSymbol.EPSILON));
			}
		}

		return new SimpleAutomaton(fsaStates, fsaDelta).minimize();
	}

}
