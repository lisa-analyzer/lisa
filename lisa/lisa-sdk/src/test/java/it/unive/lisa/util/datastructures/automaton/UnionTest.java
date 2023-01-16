package it.unive.lisa.util.datastructures.automaton;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.Test;

public class UnionTest {

	@Test
	public void test01() throws CyclicAutomatonException {
		SortedSet<State> states1 = new TreeSet<>();
		SortedSet<State> states2 = new TreeSet<>();
		State[] st1 = new State[3];
		State[] st2 = new State[2];
		st1[0] = new State(0, true, false);
		st1[1] = new State(1, false, false);
		st1[2] = new State(2, false, true);
		Collections.addAll(states1, st1);

		st2[0] = new State(0, true, false);
		st2[1] = new State(1, false, true);
		Collections.addAll(states2, st2);

		SortedSet<Transition<TestSymbol>> delta1 = new TreeSet<>();
		SortedSet<Transition<TestSymbol>> delta2 = new TreeSet<>();

		delta1.add(new Transition<>(st1[0], st1[1], new TestSymbol("a")));
		delta1.add(new Transition<>(st1[1], st1[2], new TestSymbol("b")));
		delta2.add(new Transition<>(st2[0], st2[1], new TestSymbol("a")));

		SortedSet<String> exp = new TreeSet<>();
		exp.add("ab");
		exp.add("a");

		TestAutomaton a1 = new TestAutomaton(states1, delta1);
		TestAutomaton a2 = new TestAutomaton(states2, delta2);

		assertEquals(exp, a1.union(a2).getLanguage());
	}

	@Test
	public void test02() throws CyclicAutomatonException {
		SortedSet<State> states = new TreeSet<>();
		State[] st1 = new State[3];
		st1[0] = new State(0, true, false);
		st1[1] = new State(1, false, false);
		st1[2] = new State(2, false, true);
		Collections.addAll(states, st1);

		SortedSet<Transition<TestSymbol>> delta = new TreeSet<>();
		delta.add(new Transition<>(st1[0], st1[1], new TestSymbol("a")));
		delta.add(new Transition<>(st1[1], st1[2], new TestSymbol("b")));

		TestAutomaton a1 = new TestAutomaton(states, delta);
		TestAutomaton a2 = new TestAutomaton(states, delta);

		SortedSet<String> exp = new TreeSet<>();
		exp.add("ab");

		assertEquals(exp, a1.union(a2).getLanguage());
	}

	@Test
	public void test03() throws CyclicAutomatonException {
		SortedSet<State> states1 = new TreeSet<>();
		SortedSet<State> states2 = new TreeSet<>();
		State[] st1 = new State[4];
		State[] st2 = new State[3];
		st1[0] = new State(0, true, false);
		st1[1] = new State(1, false, false);
		st1[2] = new State(2, false, false);
		st1[3] = new State(3, false, true);
		Collections.addAll(states1, st1);

		st2[0] = new State(0, true, false);
		st2[1] = new State(1, false, true);
		st2[2] = new State(2, false, true);
		Collections.addAll(states2, st2);

		SortedSet<Transition<TestSymbol>> delta1 = new TreeSet<>();
		SortedSet<Transition<TestSymbol>> delta2 = new TreeSet<>();
		delta1.add(new Transition<>(st1[0], st1[1], new TestSymbol("a")));
		delta1.add(new Transition<>(st1[0], st1[2], new TestSymbol("")));
		delta1.add(new Transition<>(st1[1], st1[3], new TestSymbol("b")));
		delta1.add(new Transition<>(st1[2], st1[3], new TestSymbol("a")));
		delta2.add(new Transition<>(st2[0], st2[1], new TestSymbol("c")));
		delta2.add(new Transition<>(st2[0], st2[2], new TestSymbol("b")));

		TestAutomaton a1 = new TestAutomaton(states1, delta1);
		TestAutomaton a2 = new TestAutomaton(states2, delta2);
		SortedSet<String> exp = new TreeSet<>();
		exp.add("ab");
		exp.add("a");
		exp.add("c");
		exp.add("b");
		assertEquals(exp, a1.union(a2).getLanguage());
	}
}
