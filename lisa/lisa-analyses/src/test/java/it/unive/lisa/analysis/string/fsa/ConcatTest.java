package it.unive.lisa.analysis.string.fsa;

import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.Test;

public class ConcatTest {

	@Test
	public void test01() {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[2];
		st[0] = new State(true, false);
		st[1] = new State(false, true);
		Collections.addAll(states, st);

		SortedSet<Transition> delta = new TreeSet<>();
		delta.add(new Transition(st[0], st[1], "a"));

		SortedSet<State> states2 = new TreeSet<>();
		State[] st2 = new State[2];
		st2[0] = new State(true, false);
		st2[1] = new State(false, true);
		Collections.addAll(states2, st2);

		SortedSet<Transition> delta2 = new TreeSet<>();
		delta2.add(new Transition(st2[0], st2[1], "b"));

		Automaton a = new Automaton(states, delta);
		Automaton a2 = new Automaton(states2, delta2);

		SortedSet<State> expStates = new TreeSet<>();
		State[] expSt = new State[3];
		expSt[0] = new State(true, false);
		expSt[1] = new State(false, false);
		expSt[2] = new State(false, true);
		Collections.addAll(expStates, expSt);

		SortedSet<Transition> expDelta = new TreeSet<>();
		expDelta.add(new Transition(expSt[0], expSt[1], "a"));
		expDelta.add(new Transition(expSt[1], expSt[2], "b"));

		Automaton exp = new Automaton(expStates, expDelta);

		assertTrue(a.concat(a2).isEqual(exp));
	}

	@Test
	public void test02() {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[3];
		st[0] = new State(true, false);
		st[1] = new State(false, true);
		st[2] = new State(false, true);
		Collections.addAll(states, st);

		SortedSet<Transition> delta = new TreeSet<>();
		delta.add(new Transition(st[0], st[1], "a"));
		delta.add(new Transition(st[0], st[2], "b"));

		SortedSet<State> states2 = new TreeSet<>();
		State[] st2 = new State[2];
		st2[0] = new State(true, false);
		st2[1] = new State(false, true);
		Collections.addAll(states2, st2);

		SortedSet<Transition> delta2 = new TreeSet<>();
		delta2.add(new Transition(st2[0], st2[1], "c"));

		Automaton a = new Automaton(states, delta);
		Automaton a2 = new Automaton(states2, delta2);

		SortedSet<State> expStates = new TreeSet<>();
		State[] expSt = new State[3];
		expSt[0] = new State(true, false);
		expSt[1] = new State(false, false);
		expSt[2] = new State(false, true);
		Collections.addAll(expStates, expSt);

		SortedSet<Transition> expDelta = new TreeSet<>();
		expDelta.add(new Transition(expSt[0], expSt[1], "a"));
		expDelta.add(new Transition(expSt[0], expSt[1], "b"));
		expDelta.add(new Transition(expSt[1], expSt[2], "c"));

		Automaton exp = new Automaton(expStates, expDelta);

		assertTrue(a.concat(a2).isEqual(exp));
	}
}
