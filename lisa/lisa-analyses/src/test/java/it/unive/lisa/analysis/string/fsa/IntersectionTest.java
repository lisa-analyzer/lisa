package it.unive.lisa.analysis.string.fsa;

import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.Test;

public class IntersectionTest {

	@Test
	public void test01() {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[5];
		SortedSet<State> states2 = new TreeSet<>();
		State[] st2 = new State[3];
		SortedSet<Transition> delta = new TreeSet<>();
		SortedSet<Transition> delta2 = new TreeSet<>();

		st[0] = new State(true, false);
		st[1] = new State(false, false);
		st[2] = new State(false, false);
		st[3] = new State(false, true);
		st[4] = new State(false, true);
		Collections.addAll(states, st);

		st2[0] = new State(true, false);
		st2[1] = new State(false, false);
		st2[2] = new State(false, true);
		Collections.addAll(states2, st2);

		delta.add(new Transition(st[0], st[1], "a"));
		delta.add(new Transition(st[0], st[2], "c"));
		delta.add(new Transition(st[1], st[3], "b"));
		delta.add(new Transition(st[2], st[4], "c"));

		delta2.add(new Transition(st2[0], st2[1], "a"));
		delta2.add(new Transition(st2[1], st2[2], "b"));

		// ab | cc
		Automaton a = new Automaton(states, delta);

		// ab
		Automaton a2 = new Automaton(states2, delta2);

		assertTrue(a.intersection(a2).isEqual(a2));
	}

	@Test
	public void test02() {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[4];
		SortedSet<State> states2 = new TreeSet<>();
		State[] st2 = new State[3];
		SortedSet<Transition> delta = new TreeSet<>();
		SortedSet<Transition> delta2 = new TreeSet<>();

		st[0] = new State(true, false);
		st[1] = new State(false, false);
		st[2] = new State(false, true);
		st[3] = new State(false, true);
		Collections.addAll(states, st);

		delta.add(new Transition(st[0], st[1], "a"));
		delta.add(new Transition(st[1], st[2], "c"));
		delta.add(new Transition(st[2], st[3], "b"));

		st2[0] = new State(true, false);
		st2[1] = new State(false, false);
		st2[2] = new State(false, true);
		Collections.addAll(states2, st2);

		delta2.add(new Transition(st2[0], st2[1], "a"));
		delta2.add(new Transition(st2[1], st2[2], "c"));

		Automaton a = new Automaton(states, delta);

		Automaton a2 = new Automaton(states2, delta2);

		assertTrue(a.intersection(a2).isEqual(a2));
	}
}
