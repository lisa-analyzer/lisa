package it.unive.lisa.analysis.string.fsa;

import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

public class ComplementTest {

	@Test
	public void test01() {
		Set<State> states = new HashSet<>();
		Set<Transition> delta = new HashSet<>();
		State[] st = new State[3];
		st[0] = new State(true, false);
		st[1] = new State(false, false);
		st[2] = new State(false, true);
		Collections.addAll(states, st);

		delta.add(new Transition(st[0], st[1], "a"));
		delta.add(new Transition(st[1], st[2], "b"));
		Set<String> sigma = new HashSet<>();
		sigma.add("a");
		sigma.add("b");

		// ab
		Automaton a = new Automaton(states, delta);

		Set<State> expStates = new HashSet<>();
		Set<Transition> expDelta = new HashSet<>();
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
		Set<State> states = new HashSet<>();
		Set<Transition> delta = new HashSet<>();
		State[] st = new State[2];
		st[0] = new State(true, false);
		st[1] = new State(false, true);
		Collections.addAll(states, st);

		delta.add(new Transition(st[0], st[0], "a"));
		delta.add(new Transition(st[0], st[1], "b"));
		delta.add(new Transition(st[1], st[1], "b"));

		// ab+
		Automaton a = new Automaton(states, delta);

		Set<String> sigma = new HashSet<>();
		sigma.add("a");
		sigma.add("b");

		Set<State> expStates = new HashSet<>();
		Set<Transition> expDelta = new HashSet<>();
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
