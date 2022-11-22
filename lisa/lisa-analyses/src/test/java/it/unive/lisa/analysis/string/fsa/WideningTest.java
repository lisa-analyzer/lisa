package it.unive.lisa.analysis.string.fsa;

import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.Test;

public class WideningTest {

	@Test
	public void test01() {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[5];
		st[0] = new State(true, true);
		st[1] = new State(false, true);
		st[2] = new State(false, true);
		st[3] = new State(false, true);
		st[4] = new State(false, true);

		Collections.addAll(states, st);

		SortedSet<Transition> delta = new TreeSet<>();
		delta.add(new Transition(st[0], st[1], "a"));
		delta.add(new Transition(st[1], st[2], "a"));
		delta.add(new Transition(st[2], st[3], "a"));
		delta.add(new Transition(st[3], st[4], "a"));

		Automaton a = new Automaton(states, delta);

		SortedSet<State> expStates = new TreeSet<>();
		State q = new State(true, true);
		expStates.add(q);
		SortedSet<Transition> expDelta = new TreeSet<>();
		expDelta.add(new Transition(q, q, "a"));

		Automaton exp = new Automaton(expStates, expDelta);

		assertTrue(a.widening(2).isEqual(exp));
		assertTrue(a.widening(5).isEqual(a));
	}
}
