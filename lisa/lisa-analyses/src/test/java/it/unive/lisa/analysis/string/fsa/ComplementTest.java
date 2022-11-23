package it.unive.lisa.analysis.string.fsa;

import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.Test;

public class ComplementTest {

	@Test
	public void test01() {
		SortedSet<State> states = new TreeSet<>();
		SortedSet<Transition> delta = new TreeSet<>();
		State[] st = new State[3];
		st[0] = new State(true, false);
		st[1] = new State(false, false);
		st[2] = new State(false, true);
		Collections.addAll(states, st);

		delta.add(new Transition(st[0], st[1], "a"));
		delta.add(new Transition(st[1], st[2], "b"));
		SortedSet<String> sigma = new TreeSet<>();
		sigma.add("a");
		sigma.add("b");

		// ab
		Automaton a = new Automaton(states, delta);

		SortedSet<State> expStates = new TreeSet<>();
		SortedSet<Transition> expDelta = new TreeSet<>();
		State[] expSt = new State[4];
		expSt[0] = new State(true, true);
		expSt[1] = new State(false, true);
		expSt[2] = new State(false, false);
		expSt[3] = new State(false, true);
		Collections.addAll(expStates, expSt);

		expDelta.add(new Transition(expSt[0], expSt[1], "a"));
		expDelta.add(new Transition(expSt[0], expSt[3], "b"));
		expDelta.add(new Transition(expSt[1], expSt[2], "b"));
		expDelta.add(new Transition(expSt[1], expSt[3], "a"));
		expDelta.add(new Transition(expSt[2], expSt[3], "a"));
		expDelta.add(new Transition(expSt[2], expSt[3], "b"));
		expDelta.add(new Transition(expSt[3], expSt[3], "a"));
		expDelta.add(new Transition(expSt[3], expSt[3], "b"));

		Automaton exp = new Automaton(expStates, expDelta);

		assertTrue(a.complement(sigma).isEqual(exp));
	}

	@Test
	public void test02() {
		SortedSet<State> states = new TreeSet<>();
		SortedSet<Transition> delta = new TreeSet<>();
		State[] st = new State[2];
		st[0] = new State(true, false);
		st[1] = new State(false, true);
		Collections.addAll(states, st);

		delta.add(new Transition(st[0], st[0], "a"));
		delta.add(new Transition(st[0], st[1], "b"));
		delta.add(new Transition(st[1], st[1], "b"));

		// ab+
		Automaton a = new Automaton(states, delta);

		SortedSet<String> sigma = new TreeSet<>();
		sigma.add("a");
		sigma.add("b");

		SortedSet<State> expStates = new TreeSet<>();
		SortedSet<Transition> expDelta = new TreeSet<>();
		State[] expSt = new State[3];
		expSt[0] = new State(true, true);
		expSt[1] = new State(false, false);
		expSt[2] = new State(false, true);
		Collections.addAll(expStates, expSt);

		expDelta.add(new Transition(expSt[0], expSt[0], "a"));
		expDelta.add(new Transition(expSt[0], expSt[1], "b"));
		expDelta.add(new Transition(expSt[1], expSt[1], "b"));
		expDelta.add(new Transition(expSt[1], expSt[2], "a"));
		expDelta.add(new Transition(expSt[2], expSt[2], "a"));
		expDelta.add(new Transition(expSt[2], expSt[2], "b"));

		Automaton exp = new Automaton(expStates, expDelta);

		assertTrue(a.complement(sigma).isEqual(exp));
	}
}
