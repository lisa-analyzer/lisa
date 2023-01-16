package it.unive.lisa.util.datastructures.automaton;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.Test;

public class HasCycleTest {

	@Test
	public void test01() {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[2];
		st[0] = new State(0, true, false);
		st[1] = new State(1, false, true);
		Collections.addAll(states, st);

		SortedSet<Transition<TestSymbol>> transitions = new TreeSet<>();
		transitions.add(new Transition<>(st[0], st[0], new TestSymbol("a")));
		transitions.add(new Transition<>(st[0], st[0], new TestSymbol("b")));
		transitions.add(new Transition<>(st[0], st[1], new TestSymbol("b")));

		// accepts language {a^nb^m}^p
		TestAutomaton nfa = new TestAutomaton(states, transitions);
		assertTrue(nfa.hasCycle());
	}

	@Test
	public void test02() {
		SortedSet<State> states = new TreeSet<>();
		SortedSet<Transition<TestSymbol>> tr = new TreeSet<>();
		State[] st = new State[3];
		st[0] = new State(0, true, false);
		st[1] = new State(1, false, false);
		st[2] = new State(2, false, true);
		Collections.addAll(states, st);
		tr.add(new Transition<>(st[0], st[1], new TestSymbol("a")));
		tr.add(new Transition<>(st[1], st[2], new TestSymbol("b")));
		TestAutomaton a = new TestAutomaton(states, tr);
		assertFalse(a.hasCycle());
	}

	@Test
	public void test03() {
		SortedSet<State> states = new TreeSet<>();
		SortedSet<Transition<TestSymbol>> delta = new TreeSet<>();
		State q = new State(0, true, true);
		delta.add(new Transition<>(q, q, new TestSymbol("a")));
		states.add(q);

		TestAutomaton a = new TestAutomaton(states, delta);
		assertTrue(a.hasCycle());
	}
}
