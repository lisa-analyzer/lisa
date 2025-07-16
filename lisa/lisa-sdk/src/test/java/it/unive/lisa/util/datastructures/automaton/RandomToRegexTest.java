package it.unive.lisa.util.datastructures.automaton;

import static it.unive.lisa.util.datastructures.automaton.TestUtil.generateAutomaton;
import static it.unive.lisa.util.datastructures.automaton.TestUtil.randomChar;
import static org.junit.Assert.assertTrue;

import it.unive.lisa.util.datastructures.regex.RegularExpression;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("This sometimes causes OOM in GitHub actions")
public class RandomToRegexTest {

	private final SortedSet<State> states = new TreeSet<>();

	private final Map<Integer, State> mapping = new HashMap<>();

	@Before
	public void initialize() {
		State q0 = new State(0, true, false);
		states.add(q0);
		mapping.put(0, q0);

		State q1 = new State(1, false, false);
		states.add(q1);
		mapping.put(1, q1);

		State q2 = new State(2, false, true);
		states.add(q2);
		mapping.put(2, q2);

		State q3 = new State(3, false, false);
		states.add(q3);
		mapping.put(3, q3);

		State q4 = new State(4, false, true);
		states.add(q4);
		mapping.put(4, q4);
	}

	private static void check(
			TestAutomaton a) {
		RegularExpression fromRegex = a.toRegex().simplify();
		TestAutomaton revert = fromRegex.toAutomaton(a);
		assertTrue(
				a + " is different from " + revert,
				a.isEqualTo(revert));
	}

	/**
	 * #states: 5 #final-states: 2 #transitions: 1 for each state #automata: 100
	 * #sizeofchar: 2
	 */
	@Test
	public void toRegexTest001() {
		int numberOfTransitionsForEachState = 1;
		int numberOfGeneratedAutomata = 100;
		int sizeOfChar = 2;

		for (int k = 0; k < numberOfGeneratedAutomata; k++)
			check(generateAutomaton(states, mapping, numberOfTransitionsForEachState, sizeOfChar));
	}

	/**
	 * #states: 5 #final-states: 2 #transitions: 2 for each state #automata: 50
	 * #sizeofchar: 2
	 */
	@Test
	public void toRegexTest002() {
		int numberOfTransitionsForEachState = 2;
		int numberOfGeneratedAutomata = 50;
		int sizeOfChar = 2;

		for (int k = 0; k < numberOfGeneratedAutomata; k++)
			check(generateAutomaton(states, mapping, numberOfTransitionsForEachState, sizeOfChar));
	}

	/**
	 * #states: 5 #final-states: 2 #transitions: 2 for each state and 1
	 * self-loop #automata: 50 #sizeofchar: 2
	 */
	@Test
	public void toRegexTest003() {
		int numberOfTransitionsForEachState = 2;
		int numberOfGeneratedAutomata = 50;
		int sizeOfChar = 2;

		for (int k = 0; k < numberOfGeneratedAutomata; k++) {
			TestAutomaton gen = generateAutomaton(states, mapping, numberOfTransitionsForEachState, sizeOfChar);
			for (State s : states)
				gen.addTransition(new Transition<>(s, s, randomChar(sizeOfChar)));
			check(gen);
		}
	}

	/**
	 * #states: 5 #final-states: 2 #transitions: 3 for each state #automata: 50
	 * #sizeofchar: 2
	 */
	@Test
	public void toRegexTest004() {
		int numberOfTransitionsForEachState = 3;
		int numberOfGeneratedAutomata = 50;
		int sizeOfChar = 2;

		for (int k = 0; k < numberOfGeneratedAutomata; k++)
			check(generateAutomaton(states, mapping, numberOfTransitionsForEachState, sizeOfChar));
	}

}
