package it.unive.lisa.analysis.string.tarsis;

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

/**
 * A class that describes an generic automaton(dfa, nfa, epsilon nfa) using an
 * alphabet of strings, extended with a special symbol for statically unknown
 * ones. Transition symbols are {@link RegularExpression}s.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class RegexAutomaton extends Automaton<RegexAutomaton, RegularExpression> {

	/**
	 * Builds a {@link RegexAutomaton} recognizing the top string, that is, with
	 * a single transition recognizing {@link TopAtom}.
	 * 
	 * @return the automaton
	 */
	public static RegexAutomaton topString() {
		State q0 = new State(0, true, false);
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
	public static RegexAutomaton string(String string) {
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
	public static RegexAutomaton string(SymbolicString s) {
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
	public static RegexAutomaton strings(String... strings) {
		RegexAutomaton a = emptyLang();

		for (String s : strings)
			a = a.union(string(s));

		return a;
	}

	@Override
	public RegexAutomaton singleString(String string) {
		return string(string);
	}

	@Override
	public RegexAutomaton unknownString() {
		return topString();
	}

	@Override
	public RegexAutomaton emptyLanguage() {
		return emptyLanguage();
	}

	@Override
	public RegexAutomaton emptyString() {
		return emptyStr();
	}

	@Override
	public RegexAutomaton from(SortedSet<State> states, SortedSet<Transition<RegularExpression>> transitions) {
		return new RegexAutomaton(states, transitions);
	}

	@Override
	public RegularExpression epsilon() {
		return Atom.EPSILON;
	}

	@Override
	public RegularExpression concat(RegularExpression first, RegularExpression second) {
		return first.comp(second);
	}

	@Override
	public RegularExpression symbolToRegex(RegularExpression symbol) {
		return symbol;
	}

	/**
	 * Builds a new automaton with given {@code states} and {@code transitions}.
	 *
	 * @param states      the set of states of the new automaton
	 * @param transitions the set of the transitions of the new automaton
	 */
	public RegexAutomaton(SortedSet<State> states, SortedSet<Transition<RegularExpression>> transitions) {
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

	private Set<Vector<State>> findMergableStatesInPath(List<State> v) {
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
	public RegexAutomaton replace(RegexAutomaton toReplace, RegexAutomaton str) throws CyclicAutomatonException {
		Collection<RegexAutomaton> automata = new ArrayList<>();
		boolean isSingleString = toReplace.getLanguage().size() == 1;
		for (String s : toReplace.getLanguage())
			automata.add(new StringReplacer(this).replace(s, str, isSingleString).collapse());

		if (automata.size() == 1)
			return automata.iterator().next();

		return union(automata.toArray(new RegexAutomaton[automata.size()]));
	}

	private RegexAutomaton union(RegexAutomaton... automata) {
		RegexAutomaton result = emptyLanguage();

		for (RegexAutomaton a : automata)
			result = a.union(result);

		return result;
	}
}
