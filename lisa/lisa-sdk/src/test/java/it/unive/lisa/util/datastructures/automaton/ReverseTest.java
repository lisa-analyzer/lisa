package it.unive.lisa.util.datastructures.automaton;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.Test;

public class ReverseTest {

	@Test
	public void testLNComplementStep5() {
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
		expSt[2] = new State(2, true, false);
		Collections.addAll(expStates, expSt);

		expDelta.add(new Transition<>(expSt[0], expSt[1], new TestSymbol("b")));
		expDelta.add(new Transition<>(expSt[1], expSt[0], new TestSymbol("a")));
		expDelta.add(new Transition<>(expSt[2], expSt[0], new TestSymbol("b")));
		expDelta.add(new Transition<>(expSt[2], expSt[1], new TestSymbol("a")));
		expDelta.add(new Transition<>(expSt[2], expSt[2], new TestSymbol("a")));
		expDelta.add(new Transition<>(expSt[2], expSt[2], new TestSymbol("b")));

		TestAutomaton exp = new TestAutomaton(expStates, expDelta);

		assertEquals(exp, a.reverse());
	}

	@Test
	public void testLNComplementStep7() {
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
		expSt[2] = new State(2, true, false);
		Collections.addAll(expStates, expSt);

		expDelta.add(new Transition<>(expSt[0], expSt[1], new TestSymbol("b")));
		expDelta.add(new Transition<>(expSt[1], expSt[0], new TestSymbol("a")));
		expDelta.add(new Transition<>(expSt[2], expSt[0], new TestSymbol("b")));
		expDelta.add(new Transition<>(expSt[2], expSt[1], new TestSymbol("a")));
		expDelta.add(new Transition<>(expSt[2], expSt[2], new TestSymbol("a")));
		expDelta.add(new Transition<>(expSt[2], expSt[2], new TestSymbol("b")));

		TestAutomaton exp = new TestAutomaton(expStates, expDelta);

		assertEquals(exp, a.reverse());
	}

	@Test
	public void test01()
			throws CyclicAutomatonException {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[4];
		st[0] = new State(0, true, false);
		st[1] = new State(1, false, false);
		st[2] = new State(2, false, false);
		st[3] = new State(3, false, true);
		Collections.addAll(states, st);

		SortedSet<Transition<TestSymbol>> delta = new TreeSet<>();
		delta.add(new Transition<>(st[0], st[1], new TestSymbol("a")));
		delta.add(new Transition<>(st[1], st[2], new TestSymbol("b")));
		delta.add(new Transition<>(st[2], st[3], new TestSymbol("c")));

		SortedSet<String> exp = new TreeSet<>();
		exp.add("cba");
		TestAutomaton a = new TestAutomaton(states, delta);

		assertEquals(exp, a.reverse().getLanguage());
	}

	@Test
	public void test02()
			throws CyclicAutomatonException {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[5];
		st[0] = new State(0, true, false);
		st[1] = new State(1, false, false);
		st[2] = new State(2, false, false);
		st[3] = new State(3, false, true);
		st[4] = new State(4, false, true);
		Collections.addAll(states, st);

		SortedSet<Transition<TestSymbol>> delta = new TreeSet<>();
		delta.add(new Transition<>(st[0], st[1], new TestSymbol("a")));
		delta.add(new Transition<>(st[0], st[2], new TestSymbol("b")));
		delta.add(new Transition<>(st[1], st[3], new TestSymbol("c")));
		delta.add(new Transition<>(st[2], st[4], new TestSymbol("b")));

		SortedSet<String> exp = new TreeSet<>();
		exp.add("ca");
		exp.add("bb");
		TestAutomaton a = new TestAutomaton(states, delta);

		assertEquals(exp, a.reverse().getLanguage());
	}

	@Test
	public void test03()
			throws CyclicAutomatonException {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[7];
		st[0] = new State(0, true, false);
		st[1] = new State(1, true, false);
		st[2] = new State(2, false, false);
		st[3] = new State(3, false, false);
		st[4] = new State(4, false, false);
		st[5] = new State(5, false, true);
		st[6] = new State(6, false, true);
		Collections.addAll(states, st);

		SortedSet<Transition<TestSymbol>> delta = new TreeSet<>();
		delta.add(new Transition<>(st[0], st[2], new TestSymbol("a")));
		delta.add(new Transition<>(st[1], st[3], new TestSymbol("c")));
		delta.add(new Transition<>(st[2], st[4], new TestSymbol("")));
		delta.add(new Transition<>(st[3], st[4], new TestSymbol("b")));
		delta.add(new Transition<>(st[4], st[5], new TestSymbol("")));
		delta.add(new Transition<>(st[4], st[6], new TestSymbol("b")));

		SortedSet<String> exp = new TreeSet<>();
		exp.add("a");
		exp.add("bc");
		exp.add("ba");
		exp.add("bbc");
		TestAutomaton a = new TestAutomaton(states, delta);

		assertEquals(a.reverse().getLanguage(), exp);
	}

}
