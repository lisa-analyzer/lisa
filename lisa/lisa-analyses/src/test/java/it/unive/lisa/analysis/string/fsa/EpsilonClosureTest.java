package it.unive.lisa.analysis.string.fsa;

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
		st[0] = new State(true, false);
		st[1] = new State(false, false);
		st[2] = new State(false, true);
		Collections.addAll(states, st);

		SortedSet<Transition> delta = new TreeSet<>();
		delta.add(new Transition(st[0], st[1], ""));
		delta.add(new Transition(st[1], st[2], "b"));
		delta.add(new Transition(st[1], st[2], "a"));

		// a | b
		Automaton a = new Automaton(states, delta);
		SortedSet<State> expected = new TreeSet<State>();
		expected.add(st[0]);
		expected.add(st[1]);

		assertEquals(expected, a.epsClosure());
	}

	@Test
	public void testEpsClosure002() {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[4];
		st[0] = new State(true, false);
		st[1] = new State(false, false);
		st[2] = new State(false, false);
		st[3] = new State(false, true);
		Collections.addAll(states, st);

		SortedSet<Transition> delta = new TreeSet<>();
		delta.add(new Transition(st[0], st[1], ""));
		delta.add(new Transition(st[1], st[2], ""));
		delta.add(new Transition(st[2], st[3], "b"));
		delta.add(new Transition(st[2], st[3], "a"));
		delta.add(new Transition(st[2], st[3], "c"));

		// a | b | c
		Automaton a = new Automaton(states, delta);
		SortedSet<State> expected = new TreeSet<>();
		expected.add(st[0]);
		expected.add(st[1]);
		expected.add(st[2]);

		assertEquals(expected, a.epsClosure());
	}

	@Test
	public void testEpsClosure003() {
		SortedSet<State> states = new TreeSet<>();
		State q0 = new State(true, true);
		states.add(q0);

		SortedSet<Transition> delta = new TreeSet<>();
		delta.add(new Transition(q0, q0, "a"));

		// a*
		Automaton a = new Automaton(states, delta);
		SortedSet<State> expected = new TreeSet<>();
		expected.add(q0);

		assertEquals(expected, a.epsClosure());
	}

	@Test
	public void testEpsClosure004() {
		SortedSet<State> states = new TreeSet<>();
		State q0 = new State(true, true);
		states.add(q0);

		SortedSet<Transition> delta = new TreeSet<>();
		delta.add(new Transition(q0, q0, "a"));
		delta.add(new Transition(q0, q0, ""));

		// eps | a*
		Automaton a = new Automaton(states, delta);
		SortedSet<State> expected = new TreeSet<>();
		expected.add(q0);

		assertEquals(expected, a.epsClosure());
	}

	@Test
	public void testEpsClosure005() {
		SortedSet<State> states = new TreeSet<>();
		State q0 = new State(true, true);
		states.add(q0);

		SortedSet<Transition> delta = new TreeSet<>();
		delta.add(new Transition(q0, q0, "a"));
		delta.add(new Transition(q0, q0, "b"));
		delta.add(new Transition(q0, q0, ""));

		// (a | b | eps)
		Automaton a = new Automaton(states, delta);
		SortedSet<State> expected = new TreeSet<>();
		expected.add(q0);

		assertEquals(expected, a.epsClosure());
	}

	@Test
	public void testEpsClosure006() {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[2];
		st[0] = new State(true, false);
		st[1] = new State(false, true);
		Collections.addAll(states, st);

		SortedSet<Transition> delta = new TreeSet<>();
		delta.add(new Transition(st[0], st[1], "a"));
		delta.add(new Transition(st[0], st[1], "b"));
		delta.add(new Transition(st[0], st[1], ""));
		delta.add(new Transition(st[1], st[1], "a"));
		delta.add(new Transition(st[1], st[1], "b"));
		delta.add(new Transition(st[1], st[1], ""));

		// (a | b | eps)+
		Automaton a = new Automaton(states, delta);
		SortedSet<State> expected = new TreeSet<>();
		expected.add(st[0]);
		expected.add(st[1]);

		assertEquals(expected, a.epsClosure());
	}

}
