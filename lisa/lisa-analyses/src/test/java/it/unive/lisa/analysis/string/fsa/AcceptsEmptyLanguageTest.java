package it.unive.lisa.analysis.string.fsa;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.Test;

public class AcceptsEmptyLanguageTest {

	@Test
	public void test01() {
		SortedSet<State> states = new TreeSet<>();
		SortedSet<Transition> delta = new TreeSet<>();
		State[] st = new State[2];
		st[0] = new State(true, false);
		st[1] = new State(false, false);
		Collections.addAll(states, st);

		delta.add(new Transition(st[0], st[1], "a"));

		Automaton a = new Automaton(states, delta);

		assertTrue(a.acceptsEmptyLanguage());
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

		assertFalse(a.acceptsEmptyLanguage());
	}
}
