package it.unive.lisa.analysis.string.fsa;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

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

	@Test
	public void test03() {
		Set<State> states = new HashSet<>();
		Set<Transition> delta = new HashSet<>();
		State q = new State(true, true);
		delta.add(new Transition(q, q, "a"));
		states.add(q);

		Automaton a = new Automaton(states, delta);
		assertTrue(a.hasCycle());
	}
}
