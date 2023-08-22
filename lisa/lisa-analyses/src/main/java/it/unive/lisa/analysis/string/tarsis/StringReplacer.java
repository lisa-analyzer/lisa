package it.unive.lisa.analysis.string.tarsis;

import it.unive.lisa.util.datastructures.automaton.State;
import it.unive.lisa.util.datastructures.automaton.Transition;
import it.unive.lisa.util.datastructures.regex.Atom;
import it.unive.lisa.util.datastructures.regex.RegularExpression;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * An algorithm that replaces strings across all paths of an automaton.
 * 
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class StringReplacer {

	/**
	 * The target automaton
	 */
	private final RegexAutomaton origin;

	/**
	 * The string searching algorithm
	 */
	private final StringSearcher searcher;

	/**
	 * Builds the replacer. For this algorithm to work correctly, the target
	 * automaton is first exploded with a call to
	 * {@link RegexAutomaton#explode()}.
	 * 
	 * @param origin the target automaton
	 */
	public StringReplacer(RegexAutomaton origin) {
		this.origin = origin.explode();
		searcher = new StringSearcher(origin);
	}

	/**
	 * Yields a new automaton where every occurrence of {@code toReplace} have
	 * been replaced with {@code str}. If {@code must} is {@code true}, then
	 * this method effectively replaces {@code toReplace}. Otherwise, a
	 * may-replacement is perfomed, meaning that {@code toReplaced} is replaced
	 * with {@code toReplace || str}.
	 * 
	 * @param toReplace the string to replace
	 * @param str       the automaton to use as a replacement
	 * @param must      whether or not a must-replacement has to be made
	 * 
	 * @return the replaced automaton
	 */
	public RegexAutomaton replace(String toReplace, RegexAutomaton str, boolean must) {
		if (toReplace.isEmpty())
			return emptyStringReplace(str);

		Set<Vector<Transition<RegularExpression>>> replaceablePaths = searcher.searchInAllPaths(toReplace);

		if (replaceablePaths.isEmpty())
			return origin;

		RegexAutomaton replaced = must ? str : str.union(origin.singleString(toReplace));
		AtomicInteger counter = new AtomicInteger();

		for (Vector<Transition<RegularExpression>> path : replaceablePaths) {

			// start replacing inputs
			Set<State> statesToRemove = new HashSet<>();
			Set<Transition<RegularExpression>> edgesToRemove = new HashSet<>();
			for (int i = path.size() - 1; i >= 0; i--) {
				Transition<RegularExpression> t = path.get(i);
				if (i == path.size() - 1)
					// last step: just remove it;
					edgesToRemove.add(t);
				else
				// we need to check if there is a branch in the destination node
				// in that case, we keep both the transition and the node
				// otherwise, we can remove both of them
				if (origin.getOutgoingTransitionsFrom(t.getDestination()).size() < 2) {
					edgesToRemove.add(t);
					statesToRemove.add(t.getDestination());
				} else
					// we must stop since we found a branch
					break;
			}

			origin.removeTransitions(edgesToRemove);
			origin.removeStates(statesToRemove);

			// we add the new automaton
			Map<State, State> conversion = new HashMap<>();
			Set<State> states = new HashSet<>();
			Set<Transition<RegularExpression>> delta = new HashSet<>();

			Function<State, State> maker = s -> new State(counter.getAndIncrement(), false, false);
			for (State origin : replaced.getStates()) {
				State r = conversion.computeIfAbsent(origin, maker);
				states.add(r);
				for (Transition<RegularExpression> t : replaced.getOutgoingTransitionsFrom(origin)) {
					State dest = conversion.computeIfAbsent(t.getDestination(), maker);
					states.add(dest);
					delta.add(new Transition<>(r, dest, t.getSymbol()));
				}
			}

			states.forEach(origin::addState);
			delta.forEach(origin::addTransition);
			for (State s : replaced.getInitialStates())
				origin.addTransition(path.firstElement().getSource(), conversion.get(s), Atom.EPSILON);
			for (State f : replaced.getFinalStates())
				origin.addTransition(conversion.get(f), path.lastElement().getDestination(), Atom.EPSILON);
		}

		return origin;
	}

	private RegexAutomaton emptyStringReplace(RegexAutomaton str) {
		int maxId = origin.getStates().stream().mapToInt(s -> s.getId()).max().getAsInt();
		AtomicInteger counter = new AtomicInteger(maxId + 1);
		SortedSet<State> states = new TreeSet<>();
		SortedSet<Transition<RegularExpression>> delta = new TreeSet<>();

		// states will be a superset of the original ones,
		// except that all final states are tuned non-final:
		// all paths will end with `str`
		Map<State, State> mapper = new HashMap<>();
		origin.getStates().forEach(s -> mapper.put(s, new State(s.getId(), s.isInitial(), false)));
		states.addAll(mapper.values());
		

		Function<State, State> maker = s -> new State(counter.getAndIncrement(), false, false);

		for (Transition<RegularExpression> t : origin.getTransitions()) {
			Map<State, State> conversion = new HashMap<>();

			for (State origin : str.getStates()) {
				State r = conversion.computeIfAbsent(origin, maker);
				states.add(r);
				for (Transition<RegularExpression> tt : str.getOutgoingTransitionsFrom(origin)) {
					State dest = conversion.computeIfAbsent(tt.getDestination(), maker);
					states.add(dest);
					delta.add(new Transition<>(r, dest, tt.getSymbol()));
				}
			}

			for (State s : str.getInitialStates())
				delta.add(new Transition<>(mapper.get(t.getSource()), conversion.get(s), Atom.EPSILON));
			for (State f : str.getFinalStates())
				delta.add(new Transition<>(conversion.get(f), mapper.get(t.getDestination()), t.getSymbol()));
		}

		maker = s -> new State(counter.getAndIncrement(), false, s.isFinal());
		for (State f : origin.getFinalStates()) {
			Map<State, State> conversion = new HashMap<>();

			for (State origin : str.getStates()) {
				State r = conversion.computeIfAbsent(origin, maker);
				states.add(r);
				for (Transition<RegularExpression> tt : str.getOutgoingTransitionsFrom(origin)) {
					State dest = conversion.computeIfAbsent(tt.getDestination(), maker);
					states.add(dest);
					delta.add(new Transition<>(r, dest, tt.getSymbol()));
				}
			}

			for (State s : str.getInitialStates())
				delta.add(new Transition<>(f, conversion.get(s), Atom.EPSILON));
		}

		return new RegexAutomaton(states, delta);
	}
}
