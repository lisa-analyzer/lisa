package it.unive.lisa.analysis.string.fsa;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.Test;

public class ValidateStringTest {

	// testing dfa
	@Test
	public void testDfa() {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[5];
		st[0] = new State(true, false);
		st[1] = new State(false, false);
		st[2] = new State(false, false);
		st[3] = new State(false, false);
		st[4] = new State(false, true);
		Collections.addAll(states, st);

		SortedSet<Transition> transitions = new TreeSet<>();
		transitions.add(new Transition(st[0], st[0], "a"));
		transitions.add(new Transition(st[0], st[1], "b"));
		transitions.add(new Transition(st[1], st[0], "a"));
		transitions.add(new Transition(st[1], st[1], "b"));
		transitions.add(new Transition(st[1], st[2], "c"));
		transitions.add(new Transition(st[2], st[3], "b"));
		transitions.add(new Transition(st[3], st[4], "a"));

		// accepts language {a^nb^m}^pcba
		Automaton dfa = new Automaton(states, transitions);

		assertTrue(dfa.validateString("aabbababcba"));
		assertTrue(dfa.validateString("abababbbbaabcba"));

		assertFalse(dfa.validateString("baababacba"));
		assertFalse(dfa.validateString("aabbabacba"));
		assertFalse(dfa.validateString("cabcba"));
	}

	// testing nfa
	@Test
	public void testNfa() {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[2];
		st[0] = new State(true, false);
		st[1] = new State(false, true);
		Collections.addAll(states, st);

		SortedSet<Transition> transitions = new TreeSet<>();
		transitions.add(new Transition(st[0], st[0], "a"));
		transitions.add(new Transition(st[0], st[0], "b"));
		transitions.add(new Transition(st[0], st[1], "b"));

		// accepts language {a^nb^m}^p
		Automaton nfa = new Automaton(states, transitions);

		assertTrue(nfa.validateString("ababababababb"));
		assertTrue(nfa.validateString("babababababab"));

		assertFalse(nfa.validateString("abbabaaaba"));
		assertFalse(nfa.validateString("baababbaaa"));
	}

	// testing epsilon nfa
	@Test
	public void testEpsNfa() {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[11];
		st[0] = new State(true, false);
		st[1] = new State(false, false);
		st[2] = new State(false, false);
		st[3] = new State(false, false);
		st[4] = new State(false, false);
		st[5] = new State(false, false);
		st[6] = new State(false, false);
		st[7] = new State(false, false);
		st[8] = new State(false, false);
		st[9] = new State(false, false);
		st[10] = new State(false, true);
		Collections.addAll(states, st);

		SortedSet<Transition> transitions = new TreeSet<>();
		transitions.add(new Transition(st[0], st[1], ""));
		transitions.add(new Transition(st[0], st[7], ""));
		transitions.add(new Transition(st[1], st[2], ""));
		transitions.add(new Transition(st[1], st[4], ""));
		transitions.add(new Transition(st[2], st[3], "a"));
		transitions.add(new Transition(st[3], st[6], ""));
		transitions.add(new Transition(st[4], st[5], "b"));
		transitions.add(new Transition(st[5], st[6], ""));
		transitions.add(new Transition(st[6], st[1], ""));
		transitions.add(new Transition(st[6], st[7], ""));
		transitions.add(new Transition(st[7], st[8], "a"));
		transitions.add(new Transition(st[8], st[9], "b"));
		transitions.add(new Transition(st[9], st[10], "b"));

		// accepts {a^nb^m}^pabb
		Automaton enfa = new Automaton(states, transitions);

		assertTrue(enfa.validateString("abababb"));
		assertTrue(enfa.validateString("abbbaabb"));

		assertFalse(enfa.validateString("abbaab"));
		assertFalse(enfa.validateString("baaabba"));
	}

}
