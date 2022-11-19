package it.unive.lisa.analysis.string.fsa;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

public class CommonAlphabetTest {

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

		// ab
		Automaton a = new Automaton(states, delta);

		Set<State> states2 = new HashSet<>();
		Set<Transition> delta2 = new HashSet<>();
		State[] st2 = new State[3];
		st2[0] = new State(true, false);
		st2[1] = new State(false, false);
		st2[2] = new State(false, true);
		Collections.addAll(states, st);

		delta.add(new Transition(st2[0], st2[1], "a"));
		delta.add(new Transition(st2[1], st2[2], "c"));

		Automaton a2 = new Automaton(states2, delta2);

		Set<String> sigma = new HashSet<>();
		sigma.add("a");
		sigma.add("b");
		sigma.add("c");

		assertEquals(a.commonAlphabet(a2), sigma);
	}

	@Test
	public void test02() {
		Set<State> states = new HashSet<>();
		Set<Transition> delta = new HashSet<>();
		State[] st = new State[4];
		st[0] = new State(true, false);
		st[1] = new State(false, false);
		st[2] = new State(false, true);
		st[3] = new State(false, true);
		Collections.addAll(states, st);

		delta.add(new Transition(st[0], st[1], "a"));
		delta.add(new Transition(st[1], st[2], "b"));
		delta.add(new Transition(st[1], st[3], "c"));

		// ab | ac
		Automaton a = new Automaton(states, delta);

		Set<State> states2 = new HashSet<>();
		Set<Transition> delta2 = new HashSet<>();
		State[] st2 = new State[3];
		st2[0] = new State(true, false);
		st2[1] = new State(false, false);
		st2[2] = new State(false, true);
		Collections.addAll(states, st);

		delta.add(new Transition(st2[0], st2[1], "a"));
		delta.add(new Transition(st2[1], st2[2], "c"));

		// ac
		Automaton a2 = new Automaton(states2, delta2);

		Set<String> sigma = new HashSet<>();
		sigma.add("a");
		sigma.add("b");
		sigma.add("c");

		assertEquals(a.commonAlphabet(a2), sigma);
	}
}