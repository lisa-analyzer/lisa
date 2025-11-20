package it.unive.lisa.analysis.string.fsa;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import it.unive.lisa.lattices.string.fsa.SimpleAutomaton;
import it.unive.lisa.lattices.string.fsa.StringSymbol;
import it.unive.lisa.util.datastructures.automaton.State;
import it.unive.lisa.util.datastructures.automaton.Transition;
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
		st[0] = new State(0, true, false);
		st[1] = new State(1, false, false);
		st[2] = new State(2, false, false);
		st[3] = new State(3, false, false);
		st[4] = new State(4, false, true);
		Collections.addAll(states, st);

		SortedSet<Transition<StringSymbol>> transitions = new TreeSet<>();
		transitions.add(new Transition<>(st[0], st[0], new StringSymbol("a")));
		transitions.add(new Transition<>(st[0], st[1], new StringSymbol("b")));
		transitions.add(new Transition<>(st[1], st[0], new StringSymbol("a")));
		transitions.add(new Transition<>(st[1], st[1], new StringSymbol("b")));
		transitions.add(new Transition<>(st[1], st[2], new StringSymbol("c")));
		transitions.add(new Transition<>(st[2], st[3], new StringSymbol("b")));
		transitions.add(new Transition<>(st[3], st[4], new StringSymbol("a")));

		// accepts language {a^nb^m}^pcba
		SimpleAutomaton dfa = new SimpleAutomaton(states, transitions);

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
		st[0] = new State(0, true, false);
		st[1] = new State(1, false, true);
		Collections.addAll(states, st);

		SortedSet<Transition<StringSymbol>> transitions = new TreeSet<>();
		transitions.add(new Transition<>(st[0], st[0], new StringSymbol("a")));
		transitions.add(new Transition<>(st[0], st[0], new StringSymbol("b")));
		transitions.add(new Transition<>(st[0], st[1], new StringSymbol("b")));

		// accepts language {a^nb^m}^p
		SimpleAutomaton nfa = new SimpleAutomaton(states, transitions);

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
		st[0] = new State(0, true, false);
		st[1] = new State(1, false, false);
		st[2] = new State(2, false, false);
		st[3] = new State(3, false, false);
		st[4] = new State(4, false, false);
		st[5] = new State(5, false, false);
		st[6] = new State(6, false, false);
		st[7] = new State(7, false, false);
		st[8] = new State(8, false, false);
		st[9] = new State(9, false, false);
		st[10] = new State(10, false, true);
		Collections.addAll(states, st);

		SortedSet<Transition<StringSymbol>> transitions = new TreeSet<>();
		transitions.add(new Transition<>(st[0], st[1], StringSymbol.EPSILON));
		transitions.add(new Transition<>(st[0], st[7], StringSymbol.EPSILON));
		transitions.add(new Transition<>(st[1], st[2], StringSymbol.EPSILON));
		transitions.add(new Transition<>(st[1], st[4], StringSymbol.EPSILON));
		transitions.add(new Transition<>(st[2], st[3], new StringSymbol("a")));
		transitions.add(new Transition<>(st[3], st[6], StringSymbol.EPSILON));
		transitions.add(new Transition<>(st[4], st[5], new StringSymbol("b")));
		transitions.add(new Transition<>(st[5], st[6], StringSymbol.EPSILON));
		transitions.add(new Transition<>(st[6], st[1], StringSymbol.EPSILON));
		transitions.add(new Transition<>(st[6], st[7], StringSymbol.EPSILON));
		transitions.add(new Transition<>(st[7], st[8], new StringSymbol("a")));
		transitions.add(new Transition<>(st[8], st[9], new StringSymbol("b")));
		transitions.add(new Transition<>(st[9], st[10], new StringSymbol("b")));

		// accepts {a^nb^m}^pabb
		SimpleAutomaton enfa = new SimpleAutomaton(states, transitions);

		assertTrue(enfa.validateString("abababb"));
		assertTrue(enfa.validateString("abbbaabb"));

		assertFalse(enfa.validateString("abbaab"));
		assertFalse(enfa.validateString("baaabba"));
	}

}
