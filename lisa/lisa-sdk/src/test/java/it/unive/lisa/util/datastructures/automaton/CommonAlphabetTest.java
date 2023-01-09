package it.unive.lisa.util.datastructures.automaton;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.Test;

public class CommonAlphabetTest {

	@Test
	public void test01() {
		SortedSet<State> states = new TreeSet<>();
		SortedSet<Transition<TestSymbol>> delta = new TreeSet<>();
		State[] st = new State[3];
		st[0] = new State(0, true, false);
		st[1] = new State(1, false, false);
		st[2] = new State(2, false, true);
		Collections.addAll(states, st);

		delta.add(new Transition<>(st[0], st[1], new TestSymbol("a")));
		delta.add(new Transition<>(st[1], st[2], new TestSymbol("b")));

		// ab
		TestAutomaton a = new TestAutomaton(states, delta);

		SortedSet<State> states2 = new TreeSet<>();
		SortedSet<Transition<TestSymbol>> delta2 = new TreeSet<>();
		State[] st2 = new State[3];
		st2[0] = new State(0, true, false);
		st2[1] = new State(1, false, false);
		st2[2] = new State(2, false, true);
		Collections.addAll(states, st);

		delta.add(new Transition<>(st2[0], st2[1], new TestSymbol("a")));
		delta.add(new Transition<>(st2[1], st2[2], new TestSymbol("c")));

		TestAutomaton a2 = new TestAutomaton(states2, delta2);

		SortedSet<TestSymbol> sigma = new TreeSet<>();
		sigma.add(new TestSymbol("a"));
		sigma.add(new TestSymbol("b"));
		sigma.add(new TestSymbol("c"));

		assertEquals(a.commonAlphabet(a2), sigma);
	}

	@Test
	public void test02() {
		SortedSet<State> states = new TreeSet<>();
		SortedSet<Transition<TestSymbol>> delta = new TreeSet<>();
		State[] st = new State[4];
		st[0] = new State(0, true, false);
		st[1] = new State(1, false, false);
		st[2] = new State(2, false, true);
		st[3] = new State(3, false, true);
		Collections.addAll(states, st);

		delta.add(new Transition<>(st[0], st[1], new TestSymbol("a")));
		delta.add(new Transition<>(st[1], st[2], new TestSymbol("b")));
		delta.add(new Transition<>(st[1], st[3], new TestSymbol("c")));

		// ab | ac
		TestAutomaton a = new TestAutomaton(states, delta);

		SortedSet<State> states2 = new TreeSet<>();
		SortedSet<Transition<TestSymbol>> delta2 = new TreeSet<>();
		State[] st2 = new State[3];
		st2[0] = new State(0, true, false);
		st2[1] = new State(1, false, false);
		st2[2] = new State(2, false, true);
		Collections.addAll(states, st);

		delta.add(new Transition<>(st2[0], st2[1], new TestSymbol("a")));
		delta.add(new Transition<>(st2[1], st2[2], new TestSymbol("c")));

		// ac
		TestAutomaton a2 = new TestAutomaton(states2, delta2);

		SortedSet<TestSymbol> sigma = new TreeSet<>();
		sigma.add(new TestSymbol("a"));
		sigma.add(new TestSymbol("b"));
		sigma.add(new TestSymbol("c"));

		assertEquals(a.commonAlphabet(a2), sigma);
	}
}
