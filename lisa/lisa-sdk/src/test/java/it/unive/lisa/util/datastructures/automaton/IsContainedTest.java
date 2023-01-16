package it.unive.lisa.util.datastructures.automaton;

import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.Test;

public class IsContainedTest {

	@Test
	public void selfTest() {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[2];
		SortedSet<Transition<TestSymbol>> delta = new TreeSet<>();

		st[0] = new State(0, true, false);
		st[1] = new State(1, false, true);
		Collections.addAll(states, st);

		delta.add(new Transition<>(st[0], st[1], new TestSymbol("a")));
		delta.add(new Transition<>(st[0], st[1], new TestSymbol("b")));

		TestAutomaton a = new TestAutomaton(states, delta);

		assertTrue(a.isContained(a));
	}

	@Test
	public void simpleTest() {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[2];
		SortedSet<State> states2 = new TreeSet<>();
		State[] st2 = new State[2];

		SortedSet<Transition<TestSymbol>> delta = new TreeSet<>();
		SortedSet<Transition<TestSymbol>> delta2 = new TreeSet<>();

		st[0] = new State(0, true, false);
		st[1] = new State(1, false, true);
		Collections.addAll(states, st);

		st2[0] = new State(2, true, false);
		st2[1] = new State(3, false, true);
		Collections.addAll(states2, st2);

		delta.add(new Transition<>(st[0], st[1], new TestSymbol("a")));
		delta.add(new Transition<>(st[0], st[1], new TestSymbol("b")));

		delta2.add(new Transition<>(st2[0], st2[1], new TestSymbol("a")));

		// a | b
		TestAutomaton a = new TestAutomaton(states, delta);

		// a
		TestAutomaton a2 = new TestAutomaton(states2, delta2);

		assertTrue(a2.isContained(a));
		assertTrue(!a.isContained(a2));
	}

	@Test
	public void test01() {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[5];
		SortedSet<State> states2 = new TreeSet<>();
		State[] st2 = new State[3];
		SortedSet<Transition<TestSymbol>> delta = new TreeSet<>();
		SortedSet<Transition<TestSymbol>> delta2 = new TreeSet<>();
		st[0] = new State(0, true, false);
		st[1] = new State(1, false, false);
		st[2] = new State(2, false, false);
		st[3] = new State(3, false, true);
		st[4] = new State(4, false, true);
		Collections.addAll(states, st);

		st2[0] = new State(5, true, false);
		st2[1] = new State(6, false, false);
		st2[2] = new State(7, false, true);
		Collections.addAll(states2, st2);

		delta.add(new Transition<>(st[0], st[1], new TestSymbol("a")));
		delta.add(new Transition<>(st[0], st[2], new TestSymbol("c")));
		delta.add(new Transition<>(st[1], st[3], new TestSymbol("b")));
		delta.add(new Transition<>(st[2], st[4], new TestSymbol("c")));

		delta2.add(new Transition<>(st2[0], st2[1], new TestSymbol("a")));
		delta2.add(new Transition<>(st2[1], st2[2], new TestSymbol("b")));

		// ab | cc
		TestAutomaton a = new TestAutomaton(states, delta);

		// ab
		TestAutomaton a2 = new TestAutomaton(states2, delta2);

		assertTrue(a2.isContained(a));
		assertTrue(!a.isContained(a2));
	}
}
