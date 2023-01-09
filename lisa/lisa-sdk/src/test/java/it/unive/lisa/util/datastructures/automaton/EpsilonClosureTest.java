package it.unive.lisa.util.datastructures.automaton;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.Test;

public class EpsilonClosureTest {

	@Test
	public void testEpsClosure001() {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[3];
		st[0] = new State(0, true, false);
		st[1] = new State(1, false, false);
		st[2] = new State(2, false, true);
		Collections.addAll(states, st);

		SortedSet<Transition<TestSymbol>> delta = new TreeSet<>();
		delta.add(new Transition<>(st[0], st[1], new TestSymbol("")));
		delta.add(new Transition<>(st[1], st[2], new TestSymbol("b")));
		delta.add(new Transition<>(st[1], st[2], new TestSymbol("a")));

		// a | b
		TestAutomaton a = new TestAutomaton(states, delta);
		SortedSet<State> expected = new TreeSet<State>();
		expected.add(st[0]);
		expected.add(st[1]);

		assertEquals(expected, a.epsilonClosure(a.getInitialStates()));
	}

	@Test
	public void testEpsClosure002() {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[4];
		st[0] = new State(0, true, false);
		st[1] = new State(1, false, false);
		st[2] = new State(2, false, false);
		st[3] = new State(3, false, true);
		Collections.addAll(states, st);

		SortedSet<Transition<TestSymbol>> delta = new TreeSet<>();
		delta.add(new Transition<>(st[0], st[1], new TestSymbol("")));
		delta.add(new Transition<>(st[1], st[2], new TestSymbol("")));
		delta.add(new Transition<>(st[2], st[3], new TestSymbol("b")));
		delta.add(new Transition<>(st[2], st[3], new TestSymbol("a")));
		delta.add(new Transition<>(st[2], st[3], new TestSymbol("c")));

		// a | b | c
		TestAutomaton a = new TestAutomaton(states, delta);
		SortedSet<State> expected = new TreeSet<>();
		expected.add(st[0]);
		expected.add(st[1]);
		expected.add(st[2]);

		assertEquals(expected, a.epsilonClosure(a.getInitialStates()));
	}

	@Test
	public void testEpsClosure003() {
		SortedSet<State> states = new TreeSet<>();
		State q0 = new State(0, true, true);
		states.add(q0);

		SortedSet<Transition<TestSymbol>> delta = new TreeSet<>();
		delta.add(new Transition<>(q0, q0, new TestSymbol("a")));

		// a*
		TestAutomaton a = new TestAutomaton(states, delta);
		SortedSet<State> expected = new TreeSet<>();
		expected.add(q0);

		assertEquals(expected, a.epsilonClosure(a.getInitialStates()));
	}

	@Test
	public void testEpsClosure004() {
		SortedSet<State> states = new TreeSet<>();
		State q0 = new State(0, true, true);
		states.add(q0);

		SortedSet<Transition<TestSymbol>> delta = new TreeSet<>();
		delta.add(new Transition<>(q0, q0, new TestSymbol("a")));
		delta.add(new Transition<>(q0, q0, new TestSymbol("")));

		// eps | a*
		TestAutomaton a = new TestAutomaton(states, delta);
		SortedSet<State> expected = new TreeSet<>();
		expected.add(q0);

		assertEquals(expected, a.epsilonClosure(a.getInitialStates()));
	}

	@Test
	public void testEpsClosure005() {
		SortedSet<State> states = new TreeSet<>();
		State q0 = new State(0, true, true);
		states.add(q0);

		SortedSet<Transition<TestSymbol>> delta = new TreeSet<>();
		delta.add(new Transition<>(q0, q0, new TestSymbol("a")));
		delta.add(new Transition<>(q0, q0, new TestSymbol("b")));
		delta.add(new Transition<>(q0, q0, new TestSymbol("")));

		// (a | b | eps)
		TestAutomaton a = new TestAutomaton(states, delta);
		SortedSet<State> expected = new TreeSet<>();
		expected.add(q0);

		assertEquals(expected, a.epsilonClosure(a.getInitialStates()));
	}

	@Test
	public void testEpsClosure006() {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[2];
		st[0] = new State(0, true, false);
		st[1] = new State(1, false, true);
		Collections.addAll(states, st);

		SortedSet<Transition<TestSymbol>> delta = new TreeSet<>();
		delta.add(new Transition<>(st[0], st[1], new TestSymbol("a")));
		delta.add(new Transition<>(st[0], st[1], new TestSymbol("b")));
		delta.add(new Transition<>(st[0], st[1], new TestSymbol("")));
		delta.add(new Transition<>(st[1], st[1], new TestSymbol("a")));
		delta.add(new Transition<>(st[1], st[1], new TestSymbol("b")));
		delta.add(new Transition<>(st[1], st[1], new TestSymbol("")));

		// (a | b | eps)+
		TestAutomaton a = new TestAutomaton(states, delta);
		SortedSet<State> expected = new TreeSet<>();
		expected.add(st[0]);
		expected.add(st[1]);

		assertEquals(expected, a.epsilonClosure(a.getInitialStates()));
	}

}
