package it.unive.lisa.util.datastructures.automaton;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.Test;

public class MinimizeTest {

	@Test
	public void testLNComplementStepMinimization() {
		SortedSet<State> states = new TreeSet<>();
		SortedSet<Transition<TestSymbol>> delta = new TreeSet<>();
		State[] st = new State[3];
		st[0] = new State(0, true, true);
		st[1] = new State(1, false, false);
		st[2] = new State(2, false, true);
		Collections.addAll(states, st);

		delta.add(new Transition<>(st[0], st[1], new TestSymbol("a")));
		delta.add(new Transition<>(st[1], st[0], new TestSymbol("b")));
		delta.add(new Transition<>(st[0], st[2], new TestSymbol("b")));
		delta.add(new Transition<>(st[1], st[2], new TestSymbol("a")));
		delta.add(new Transition<>(st[2], st[2], new TestSymbol("a")));
		delta.add(new Transition<>(st[2], st[2], new TestSymbol("b")));

		TestAutomaton a = new TestAutomaton(states, delta);

		SortedSet<State> expStates = new TreeSet<>();
		SortedSet<Transition<TestSymbol>> expDelta = new TreeSet<>();
		State[] expSt = new State[3];
		expSt[0] = new State(0, true, true);
		expSt[1] = new State(1, false, false);
		expSt[2] = new State(2, false, true);
		Collections.addAll(expStates, expSt);

		expDelta.add(new Transition<>(expSt[0], expSt[1], new TestSymbol("a")));
		expDelta.add(new Transition<>(expSt[1], expSt[0], new TestSymbol("b")));
		expDelta.add(new Transition<>(expSt[0], expSt[2], new TestSymbol("b")));
		expDelta.add(new Transition<>(expSt[1], expSt[2], new TestSymbol("a")));
		expDelta.add(new Transition<>(expSt[2], expSt[2], new TestSymbol("a")));
		expDelta.add(new Transition<>(expSt[2], expSt[2], new TestSymbol("b")));

		TestAutomaton exp = new TestAutomaton(expStates, expDelta);

		assertEquals(exp, a.minimize());
	}

	@Test
	public void test01() {
		SortedSet<State> states = new TreeSet<>();
		SortedSet<Transition<TestSymbol>> delta = new TreeSet<>();
		State[] st = new State[4];
		st[0] = new State(0, true, false);
		st[1] = new State(1, false, false);
		st[2] = new State(2, false, false);
		st[3] = new State(3, false, true);
		Collections.addAll(states, st);

		delta.add(new Transition<>(st[0], st[1], new TestSymbol("a")));
		delta.add(new Transition<>(st[0], st[2], new TestSymbol("a")));
		delta.add(new Transition<>(st[1], st[3], new TestSymbol("b")));
		delta.add(new Transition<>(st[2], st[3], new TestSymbol("c")));

		// ab | ac
		TestAutomaton nfa = new TestAutomaton(states, delta);

		SortedSet<State> expStates = new TreeSet<>();
		SortedSet<Transition<TestSymbol>> expDelta = new TreeSet<>();
		State[] expSt = new State[3];
		expSt[0] = new State(0, true, false);
		expSt[1] = new State(1, false, false);
		expSt[2] = new State(2, false, true);
		Collections.addAll(expStates, expSt);

		expDelta.add(new Transition<>(expSt[0], expSt[1], new TestSymbol("a")));
		expDelta.add(new Transition<>(expSt[1], expSt[2], new TestSymbol("b")));
		expDelta.add(new Transition<>(expSt[1], expSt[2], new TestSymbol("c")));

		TestAutomaton exp = new TestAutomaton(expStates, expDelta);

		assertEquals(exp, nfa.minimize());
	}

	@Test
	public void test02() {
		SortedSet<State> states = new TreeSet<>();
		SortedSet<Transition<TestSymbol>> delta = new TreeSet<>();
		State[] st = new State[3];
		st[0] = new State(0, true, false);
		st[1] = new State(1, false, false);
		st[2] = new State(2, false, true);
		Collections.addAll(states, st);

		delta.add(new Transition<>(st[0], st[0], new TestSymbol("a")));
		delta.add(new Transition<>(st[0], st[1], new TestSymbol("a")));
		delta.add(new Transition<>(st[0], st[1], new TestSymbol("b")));
		delta.add(new Transition<>(st[1], st[2], new TestSymbol("c")));

		// a+c | a*bc
		TestAutomaton a = new TestAutomaton(states, delta);

		SortedSet<State> expStates = new TreeSet<>();
		SortedSet<Transition<TestSymbol>> expDelta = new TreeSet<>();
		State[] expSt = new State[4];
		expSt[0] = new State(0, true, false);
		expSt[1] = new State(1, false, false);
		expSt[2] = new State(2, false, false);
		expSt[3] = new State(3, false, true);
		Collections.addAll(expStates, expSt);

		expDelta.add(new Transition<>(expSt[0], expSt[1], new TestSymbol("a")));
		expDelta.add(new Transition<>(expSt[0], expSt[2], new TestSymbol("b")));
		expDelta.add(new Transition<>(expSt[1], expSt[1], new TestSymbol("a")));
		expDelta.add(new Transition<>(expSt[1], expSt[2], new TestSymbol("b")));
		expDelta.add(new Transition<>(expSt[1], expSt[3], new TestSymbol("c")));
		expDelta.add(new Transition<>(expSt[2], expSt[3], new TestSymbol("c")));

		TestAutomaton exp = new TestAutomaton(expStates, expDelta);

		assertEquals(exp, a.minimize());
	}

	@Test
	public void test03() {
		SortedSet<State> states = new TreeSet<>();
		SortedSet<Transition<TestSymbol>> delta = new TreeSet<>();
		State[] st = new State[5];
		st[0] = new State(0, true, true);
		st[1] = new State(1, false, false);
		st[2] = new State(2, false, false);
		st[3] = new State(3, false, true);
		st[4] = new State(4, false, true);
		Collections.addAll(states, st);

		delta.add(new Transition<>(st[0], st[0], new TestSymbol("c")));
		delta.add(new Transition<>(st[0], st[1], new TestSymbol("a")));
		delta.add(new Transition<>(st[0], st[2], new TestSymbol("b")));
		delta.add(new Transition<>(st[1], st[3], new TestSymbol("a")));
		delta.add(new Transition<>(st[2], st[4], new TestSymbol("b")));

		// c*aa | c*bb
		TestAutomaton a = new TestAutomaton(states, delta);

		SortedSet<State> expStates = new TreeSet<>();
		SortedSet<Transition<TestSymbol>> expDelta = new TreeSet<>();
		State[] expSt = new State[4];
		expSt[0] = new State(0, true, true);
		expSt[1] = new State(1, false, false);
		expSt[2] = new State(2, false, false);
		expSt[3] = new State(3, false, true);
		Collections.addAll(expStates, expSt);

		expDelta.add(new Transition<>(expSt[0], expSt[0], new TestSymbol("c")));
		expDelta.add(new Transition<>(expSt[0], expSt[1], new TestSymbol("a")));
		expDelta.add(new Transition<>(expSt[0], expSt[2], new TestSymbol("b")));
		expDelta.add(new Transition<>(expSt[1], expSt[3], new TestSymbol("a")));
		expDelta.add(new Transition<>(expSt[2], expSt[3], new TestSymbol("b")));

		TestAutomaton exp = new TestAutomaton(expStates, expDelta);

		assertEquals(exp, a.minimize());
	}

	@Test
	public void returnThisIfMinimized() {
		SortedSet<State> states = new TreeSet<>();
		SortedSet<Transition<TestSymbol>> delta = new TreeSet<>();
		State[] st = new State[3];
		st[0] = new State(0, true, false);
		st[1] = new State(1, false, false);
		st[2] = new State(2, false, true);
		Collections.addAll(states, st);

		delta.add(new Transition<>(st[0], st[1], new TestSymbol("a")));
		delta.add(new Transition<>(st[1], st[2], new TestSymbol("a")));

		// aa
		TestAutomaton a = new TestAutomaton(states, delta);

		a = a.minimize();

		assertSame(a, a.minimize());
	}
}
