package it.unive.lisa.analysis.string;

import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class UnionTest {

	@Test
	public void test01() {
		Set<State> states1 = new HashSet<>();
		Set<State> states2 = new HashSet<>();
		State[] st1 = new State[3];
		State[] st2 = new State[2];
		st1[0] = new State(true, false);
		st1[1] = new State(false, false);
		st1[2] = new State(false, true);
		Collections.addAll(states1, st1);

		st2[0] = new State(true, false);
		st2[1] = new State(false, true);
		Collections.addAll(states2, st2);

		Set<Transition> delta1 = new HashSet<>();
		Set<Transition> delta2 = new HashSet<>();

		delta1.add(new Transition(st1[0], st1[1], "a"));
		delta1.add(new Transition(st1[1], st1[2], "b"));
		delta2.add(new Transition(st2[0], st2[1], "a"));

		Set<String> exp = new HashSet<>();
		exp.add("ab");
		exp.add("a");

		Automaton a1 = new Automaton(states1, delta1);
		Automaton a2 = new Automaton(states2, delta2);

		assertEquals(a1.union(a2).getLanguage(), exp);
	}

	@Test
	public void test02() {
		Set<State> states = new HashSet<>();
		State[] st1 = new State[3];
		st1[0] = new State(true, false);
		st1[1] = new State(false, false);
		st1[2] = new State(false, true);
		Collections.addAll(states, st1);

    	Set<Transition> delta = new HashSet<>();
		delta.add(new Transition(st1[0], st1[1], "a"));
		delta.add(new Transition(st1[1], st1[2], "b"));

		Automaton a1 = new Automaton(states, delta);
		Automaton a2 = new Automaton(states, delta);

		Set<String> exp = new HashSet<>();
		exp.add("ab");

		assertEquals(a1.union(a2).getLanguage(), exp);
	}

	@Test
	public void test03() {
		Set<State> states1 = new HashSet<>();
		Set<State> states2 = new HashSet<>();
		State[] st1 = new State[4];
		State[] st2 = new State[3];
		st1[0] = new State(true, false);
		st1[1] = new State(false, false);
		st1[2] = new State(false, false);
		st1[3] = new State(false, true);
		Collections.addAll(states1, st1);

		st2[0] = new State(true, false);
		st2[1] = new State(false, true);
		st2[2] = new State(false, true);
		Collections.addAll(states2, st2);

		Set<Transition> delta1 = new HashSet<>();
		Set<Transition> delta2 = new HashSet<>();
		delta1.add(new Transition(st1[0], st1[1], "a"));
		delta1.add(new Transition(st1[0], st1[2], ""));
		delta1.add(new Transition(st1[1], st1[3], "b"));
		delta1.add(new Transition(st1[2], st1[3], "a"));
		delta2.add(new Transition(st2[0], st2[1], "c"));
		delta2.add(new Transition(st2[0], st2[2], "b"));

		Automaton a1 = new Automaton(states1, delta1);
		Automaton a2 = new Automaton(states2, delta2);
		Set<String> exp = new HashSet<>();
		exp.add("ab");
		exp.add("a");
		exp.add("c");
		exp.add("b");
		assertEquals(exp, a1.union(a2).getLanguage());
	}
}
