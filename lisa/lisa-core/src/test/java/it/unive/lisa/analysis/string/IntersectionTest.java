package it.unive.lisa.analysis.string;

import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class IntersectionTest {

	@Test
	public void test01() {
		Set<State> states = new HashSet<>();
		Set<State> states2 = new HashSet<>();
		State[] st = new State[4];
		State[] st2 = new State[4];
		st[0] = new State(true, false);
		st[1] = new State(false, false);
		st[2] = new State(false, false);
		st[3] = new State(false, true);
		Collections.addAll(states, st);

		st2[0] = new State(true, false);
		st2[1] = new State(false, false);
		st2[2] = new State(false, false);
		st2[3] = new State(false, true);
		Collections.addAll(states2, st2);

		Set<Transition> delta = new HashSet<>();
		Set<Transition> delta2 = new HashSet<>();
		delta.add(new Transition(st[0], st[1], "a"));
		delta.add(new Transition(st[0], st[2], "b"));
		delta.add(new Transition(st[1], st[3], "a"));
		delta.add(new Transition(st[2], st[3], "c"));

		delta2.add(new Transition(st[0], st[1], "b"));
		delta2.add(new Transition(st[0], st[2], "a"));
		delta2.add(new Transition(st[1], st[3], "a"));
		delta2.add(new Transition(st[2], st[3], "a"));

		Automaton a = new Automaton(states, delta);
		Automaton a2 = new Automaton(states2, delta2);

		Set<String> exp = new HashSet<>();
		exp.add("aa");

		assertEquals(exp, a.intersection(a2).getLanguage());
	}
}
