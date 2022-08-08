package it.unive.lisa.analysis.string;

import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HasCycleTest {

	@Test
	public void test01() {
		Set<State> states = new HashSet<>();
		State[] st = new State[2];
		st[0] = new State(true, false);
		st[1] = new State(false, true);
		Collections.addAll(states, st);

		Set<Transition> transitions = new HashSet<>();
		transitions.add(new Transition(st[0], st[0], "a"));
		transitions.add(new Transition(st[0], st[0], "b"));
		transitions.add(new Transition(st[0], st[1], "b"));

		// accepts language {a^nb^m}^p
		Automaton nfa = new Automaton(states, transitions);
		assertTrue(nfa.hasCycle());
	}

	@Test
	public void test02() {
		Set<State> states = new HashSet<>();
		Set<Transition> tr = new HashSet<>();
		State[] st = new State[3];
		st[0] = new State(true, false);
		st[1] = new State(false, false);
		st[2] = new State(false, true);
		Collections.addAll(states, st);
		tr.add(new Transition(st[0], st[1], "a"));
		tr.add(new Transition(st[1], st[2], "b"));
		Automaton a = new Automaton(states, tr);
		assertFalse(a.hasCycle());
	}
}
