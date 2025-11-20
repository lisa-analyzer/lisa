package it.unive.lisa.util.datastructures.automaton;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.Test;

public class ConcatTest {

	@Test
	public void test01() {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[2];
		st[0] = new State(0, true, false);
		st[1] = new State(1, false, true);
		Collections.addAll(states, st);

		SortedSet<Transition<TestSymbol>> delta = new TreeSet<>();
		delta.add(new Transition<>(st[0], st[1], new TestSymbol("a")));

		SortedSet<State> states2 = new TreeSet<>();
		State[] st2 = new State[2];
		st2[0] = new State(0, true, false);
		st2[1] = new State(1, false, true);
		Collections.addAll(states2, st2);

		SortedSet<Transition<TestSymbol>> delta2 = new TreeSet<>();
		delta2.add(new Transition<>(st2[0], st2[1], new TestSymbol("b")));

		TestAutomaton a = new TestAutomaton(states, delta);
		TestAutomaton a2 = new TestAutomaton(states2, delta2);

		SortedSet<State> expStates = new TreeSet<>();
		State[] expSt = new State[3];
		expSt[0] = new State(0, true, false);
		expSt[1] = new State(1, false, false);
		expSt[2] = new State(2, false, true);
		Collections.addAll(expStates, expSt);

		SortedSet<Transition<TestSymbol>> expDelta = new TreeSet<>();
		expDelta.add(new Transition<>(expSt[0], expSt[1], new TestSymbol("a")));
		expDelta.add(new Transition<>(expSt[1], expSt[2], new TestSymbol("b")));

		TestAutomaton exp = new TestAutomaton(expStates, expDelta);

		assertEquals(exp, a.concat(a2));
	}

	@Test
	public void test02() {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[3];
		st[0] = new State(0, true, false);
		st[1] = new State(1, false, true);
		st[2] = new State(2, false, true);
		Collections.addAll(states, st);

		SortedSet<Transition<TestSymbol>> delta = new TreeSet<>();
		delta.add(new Transition<>(st[0], st[1], new TestSymbol("a")));
		delta.add(new Transition<>(st[0], st[2], new TestSymbol("b")));

		SortedSet<State> states2 = new TreeSet<>();
		State[] st2 = new State[2];
		st2[0] = new State(0, true, false);
		st2[1] = new State(1, false, true);
		Collections.addAll(states2, st2);

		SortedSet<Transition<TestSymbol>> delta2 = new TreeSet<>();
		delta2.add(new Transition<>(st2[0], st2[1], new TestSymbol("c")));

		TestAutomaton a = new TestAutomaton(states, delta);
		TestAutomaton a2 = new TestAutomaton(states2, delta2);

		SortedSet<State> expStates = new TreeSet<>();
		State[] expSt = new State[3];
		expSt[0] = new State(2, true, false);
		expSt[1] = new State(1, false, false);
		expSt[2] = new State(0, false, true);
		Collections.addAll(expStates, expSt);

		SortedSet<Transition<TestSymbol>> expDelta = new TreeSet<>();
		expDelta.add(new Transition<>(expSt[0], expSt[1], new TestSymbol("a")));
		expDelta.add(new Transition<>(expSt[0], expSt[1], new TestSymbol("b")));
		expDelta.add(new Transition<>(expSt[1], expSt[2], new TestSymbol("c")));

		TestAutomaton exp = new TestAutomaton(expStates, expDelta);

		assertEquals(exp, a.concat(a2));
	}

}
