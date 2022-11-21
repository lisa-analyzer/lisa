package it.unive.lisa.analysis.string.fsa;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.Test;

public class HasCycleTest {

	@Test
	public void test01() {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[2];
		st[0] = new State(true, false);
		st[1] = new State(false, true);
		Collections.addAll(states, st);

		SortedSet<Transition> transitions = new TreeSet<>();
		transitions.add(new Transition(st[0], st[0], "a"));
		transitions.add(new Transition(st[0], st[0], "b"));
		transitions.add(new Transition(st[0], st[1], "b"));

		// accepts language {a^nb^m}^p
		Automaton nfa = new Automaton(states, transitions);
		assertTrue(nfa.hasCycle());
	}

	@Test
	public void test02() {
		SortedSet<State> states = new TreeSet<>();
		SortedSet<Transition> tr = new TreeSet<>();
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
		SortedSet<State> states = new TreeSet<>();
		SortedSet<Transition> delta = new TreeSet<>();
		State q = new State(true, true);
		delta.add(new Transition(q, q, "a"));
		states.add(q);

		Automaton a = new Automaton(states, delta);
		assertTrue(a.hasCycle());
	}
}
