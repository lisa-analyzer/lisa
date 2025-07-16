package it.unive.lisa.util.datastructures.automaton;

import java.util.Map;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;

public class TestUtil {

	private static final Random random = new Random();

	public static TestAutomaton build(
			State... states) {
		SortedSet<State> ss = new TreeSet<>();
		for (State q : states)
			ss.add(q);

		return new TestAutomaton(ss, new TreeSet<>());
	}

	@SafeVarargs
	public static TestAutomaton addEdges(
			TestAutomaton a,
			Transition<TestSymbol>... transitions) {
		for (Transition<TestSymbol> t : transitions)
			a.addTransition(t);

		return a;
	}

	public static TestSymbol randomChar(
			int count) {
		String ALPHA_NUMERIC_STRING = "0123456789qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM";
		int character = (int) (Math.random() * ALPHA_NUMERIC_STRING.length());
		return new TestSymbol(String.valueOf(ALPHA_NUMERIC_STRING.charAt(character)));
	}

	public static TestAutomaton generateAutomaton(
			SortedSet<State> states,
			Map<Integer, State> mapping,
			int numberOfTransitionsForEachState,
			int charLen) {
		TestAutomaton a = null;

		do {
			SortedSet<Transition<TestSymbol>> delta = new TreeSet<>();

			for (State s : states)
				for (int i = 0; i < numberOfTransitionsForEachState; i++)
					delta.add(new Transition<>(s, mapping.get(random.nextInt(states.size())), randomChar(charLen)));

			a = new TestAutomaton(states, delta);
		} while (a.acceptsEmptyLanguage());

		return a;
	}

}
