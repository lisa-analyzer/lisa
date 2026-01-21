package it.unive.lisa.util.datastructures.automaton;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

public class AcceptsEmptyLanguageTest {

	@Test
	public void test01() {
		SortedSet<State> states = new TreeSet<>();
		SortedSet<Transition<TestSymbol>> delta = new TreeSet<>();
		State[] st = new State[2];
		st[0] = new State(0, true, false);
		st[1] = new State(1, false, false);
		Collections.addAll(states, st);

		delta.add(new Transition<>(st[0], st[1], new TestSymbol("a")));

		TestAutomaton a = new TestAutomaton(states, delta);

		assertTrue(a.acceptsEmptyLanguage());
	}

	@Test
	public void test02() {
		SortedSet<State> states = new TreeSet<>();
		SortedSet<Transition<TestSymbol>> delta = new TreeSet<>();
		State[] st = new State[2];
		st[0] = new State(0, true, false);
		st[1] = new State(1, false, true);
		Collections.addAll(states, st);

		delta.add(new Transition<>(st[0], st[0], new TestSymbol("a")));
		delta.add(new Transition<>(st[0], st[1], new TestSymbol("b")));
		delta.add(new Transition<>(st[1], st[1], new TestSymbol("b")));

		// ab+
		TestAutomaton a = new TestAutomaton(states, delta);

		assertFalse(a.acceptsEmptyLanguage());
	}

}
