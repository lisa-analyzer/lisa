package it.unive.lisa.analysis.string.fsa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.Test;

public class MinimizeTest {

	@Test
	public void test01() {
		SortedSet<State> states = new TreeSet<>();
		SortedSet<Transition> delta = new TreeSet<>();
		State[] st = new State[4];
		st[0] = new State(true, false);
		st[1] = new State(false, false);
		st[2] = new State(false, false);
		st[3] = new State(false, true);
		Collections.addAll(states, st);

		delta.add(new Transition(st[0], st[1], "a"));
		delta.add(new Transition(st[0], st[2], "a"));
		delta.add(new Transition(st[1], st[3], "b"));
		delta.add(new Transition(st[2], st[3], "c"));

		// ab | ac
		Automaton nfa = new Automaton(states, delta);

		assertTrue(nfa.isEqual(nfa.minimize()));
	}

	@Test
	public void test02() {
		SortedSet<State> states = new TreeSet<>();
		SortedSet<Transition> delta = new TreeSet<>();
		State[] st = new State[3];
		st[0] = new State(true, false);
		st[1] = new State(false, false);
		st[2] = new State(false, true);
		Collections.addAll(states, st);

		delta.add(new Transition(st[0], st[0], "a"));
		delta.add(new Transition(st[0], st[1], "a"));
		delta.add(new Transition(st[0], st[1], "b"));
		delta.add(new Transition(st[1], st[2], "c"));

		// a+c | bc
		Automaton a = new Automaton(states, delta);

		assertTrue(a.isEqual(a.minimize()));
	}

	@Test
	public void test03() {
		SortedSet<State> states = new TreeSet<>();
		SortedSet<Transition> delta = new TreeSet<>();
		State[] st = new State[5];
		st[0] = new State(true, true);
		st[1] = new State(false, false);
		st[2] = new State(false, false);
		st[3] = new State(false, true);
		st[4] = new State(false, true);
		Collections.addAll(states, st);

		delta.add(new Transition(st[0], st[0], "e"));
		delta.add(new Transition(st[0], st[1], "a"));
		delta.add(new Transition(st[0], st[2], "b"));
		delta.add(new Transition(st[1], st[3], "a"));
		delta.add(new Transition(st[2], st[4], "b"));

		// e* | aa | bb
		Automaton a = new Automaton(states, delta);

		assertTrue(a.isEqual(a.minimize()));
	}

	@Test
	public void returnThisIfMinimized() {
		SortedSet<State> states = new TreeSet<>();
		SortedSet<Transition> delta = new TreeSet<>();
		State[] st = new State[3];
		st[0] = new State(true, false);
		st[1] = new State(false, false);
		st[2] = new State(false, true);
		Collections.addAll(states, st);

		delta.add(new Transition(st[0], st[1], "a"));
		delta.add(new Transition(st[1], st[2], "a"));

		// aa
		Automaton a = new Automaton(states, delta);

		a = a.minimize();

		assertEquals(a, a.minimize());
	}
}
