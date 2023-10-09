package it.unive.lisa.analysis.string.tarsis;

import it.unive.lisa.util.datastructures.automaton.State;
import it.unive.lisa.util.datastructures.automaton.Transition;
import it.unive.lisa.util.datastructures.regex.RegularExpression;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

/**
 * An algorithm that searches strings across all paths of an automaton.
 * 
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class StringSearcher {

	/**
	 * The target automaton
	 */
	private final RegexAutomaton automaton;

	/**
	 * The string to search
	 */
	private String searchString;

	/**
	 * True if and only if we are currently matching characters
	 */
	private boolean matching;

	/**
	 * Builds the searcher. For this algorithm to work correctly, the automaton
	 * passed as parameter must be first exploded with a call to
	 * {@link RegexAutomaton#explode()}.
	 * 
	 * @param origin the target automaton
	 */
	public StringSearcher(
			RegexAutomaton origin) {
		automaton = origin;
		searchString = null;
		matching = false;
	}

	/**
	 * Yields a set containing all the sequences of transitions that recognize
	 * the given string.
	 * 
	 * @param toSearch the string to search
	 * 
	 * @return the set of sequences of transitions
	 */
	public Set<Vector<Transition<RegularExpression>>> searchInAllPaths(
			String toSearch) {
		Set<Vector<Transition<RegularExpression>>> collected = new HashSet<>();

		Set<List<State>> paths = automaton.getAllPaths();

		if (paths.size() == 0)
			return collected;

		for (List<State> v : paths)
			collected.addAll(searchInPath(v, toSearch));

		return collected;
	}

	@SuppressWarnings("unchecked")
	private Set<Vector<Transition<RegularExpression>>> searchInPath(
			List<State> v,
			String toSearch) {

		Set<Vector<Transition<RegularExpression>>> collected = new HashSet<>();
		if (v.size() == 1 && toSearch.length() == 1)
			return handleSelfLoop(v, collected);

		Vector<Transition<RegularExpression>> path = new Vector<>();
		resetSearchState(path, toSearch);
		for (int i = 0; i < v.size() - 1; i++) {
			State from = v.get(i);
			State to = v.get(i + 1);
			Set<Transition<RegularExpression>> transitions = automaton.getAllTransitionsConnecting(from, to);

			if (transitions.size() == 0)
				continue;

			boolean matched = false;
			for (Transition<RegularExpression> t : transitions) {
				if (matching)
					if (t.getSymbol().is(searchString.substring(0, 1))) {
						// we found a matching char
						advanceSearch(path, t);
						matched = true;
					} else {
						resetSearchState(path, toSearch);
						if (t.getSymbol().is(searchString.substring(0, 1))) {
							startSearch(path, t);
							matched = true;
						}
					}
				else if (t.getSymbol().is(searchString.substring(0, 1))) {
					// we found the beginning of the string
					startSearch(path, t);
					matched = true;
				}

				if (matched)
					// we break since we do not care about the other transitions
					// between these two nodes
					break;
			}

			if (searchString.isEmpty()) {
				collected.add((Vector<Transition<RegularExpression>>) path.clone());
				resetSearchState(path, toSearch);
			}

		}

		return collected;
	}

	private Set<Vector<Transition<RegularExpression>>> handleSelfLoop(
			List<State> v,
			Set<Vector<Transition<RegularExpression>>> collected) {
		// self loop!
		Set<Transition<RegularExpression>> transitions = automaton.getAllTransitionsConnecting(v.get(0), v.get(0));

		if (transitions.size() == 0)
			return collected;

		for (Transition<RegularExpression> t : transitions)
			if (t.getSymbol().is(searchString.substring(0, 1))) {
				Vector<Transition<RegularExpression>> result = new Vector<>();
				result.add(t);
				collected.add(result);
			}

		return collected;
	}

	private void advanceSearch(
			Vector<Transition<RegularExpression>> path,
			Transition<RegularExpression> t) {
		searchString = searchString.substring(1);
		path.add(t);
	}

	private void startSearch(
			Vector<Transition<RegularExpression>> path,
			Transition<RegularExpression> t) {
		matching = true;
		advanceSearch(path, t);
	}

	private void resetSearchState(
			Vector<Transition<RegularExpression>> path,
			String toSearch) {
		matching = false;
		searchString = toSearch;
		path.clear();
	}
}
