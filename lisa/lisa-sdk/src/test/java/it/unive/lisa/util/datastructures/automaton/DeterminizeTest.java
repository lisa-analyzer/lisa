package it.unive.lisa.util.datastructures.automaton;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.Test;

public class DeterminizeTest {

	@Test
	public void testLNComplementStep1() {
		SortedSet<State> states = new TreeSet<>();
		SortedSet<Transition<TestSymbol>> delta = new TreeSet<>();
		State[] st = new State[2];
		st[0] = new State(0, true, false);
		st[1] = new State(1, false, true);
		Collections.addAll(states, st);

		delta.add(new Transition<>(st[0], st[1], new TestSymbol("a")));
		delta.add(new Transition<>(st[1], st[0], new TestSymbol("b")));

		TestAutomaton a = new TestAutomaton(states, delta);

		assertSame(a, a.determinize());
	}

	@Test
	public void testLNComplementStep4() {
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

		assertSame(a, a.determinize());
	}

	@Test
	public void testLNComplementStep6() {
		SortedSet<State> states = new TreeSet<>();
		SortedSet<Transition<TestSymbol>> delta = new TreeSet<>();
		State[] st = new State[3];
		st[0] = new State(0, true, true);
		st[1] = new State(1, false, false);
		st[2] = new State(2, true, false);
		Collections.addAll(states, st);

		delta.add(new Transition<>(st[0], st[1], new TestSymbol("b")));
		delta.add(new Transition<>(st[1], st[0], new TestSymbol("a")));
		delta.add(new Transition<>(st[2], st[0], new TestSymbol("b")));
		delta.add(new Transition<>(st[2], st[1], new TestSymbol("a")));
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

		assertEquals(exp, a.determinize());
	}

	@Test
	public void testLNComplementStep8() {
		SortedSet<State> states = new TreeSet<>();
		SortedSet<Transition<TestSymbol>> delta = new TreeSet<>();
		State[] st = new State[3];
		st[0] = new State(0, true, true);
		st[1] = new State(1, false, false);
		st[2] = new State(2, true, false);
		Collections.addAll(states, st);

		delta.add(new Transition<>(st[0], st[1], new TestSymbol("b")));
		delta.add(new Transition<>(st[1], st[0], new TestSymbol("a")));
		delta.add(new Transition<>(st[2], st[0], new TestSymbol("b")));
		delta.add(new Transition<>(st[2], st[1], new TestSymbol("a")));
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

		assertEquals(exp, a.determinize());
	}

	@Test
	public void testDfa() {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[5];
		st[0] = new State(0, true, false);
		st[1] = new State(1, false, false);
		st[2] = new State(2, false, false);
		st[3] = new State(3, false, false);
		st[4] = new State(4, false, true);
		Collections.addAll(states, st);

		SortedSet<Transition<TestSymbol>> transitions = new TreeSet<>();
		transitions.add(new Transition<>(st[0], st[0], new TestSymbol("a")));
		transitions.add(new Transition<>(st[0], st[1], new TestSymbol("b")));
		transitions.add(new Transition<>(st[1], st[0], new TestSymbol("a")));
		transitions.add(new Transition<>(st[1], st[1], new TestSymbol("b")));
		transitions.add(new Transition<>(st[1], st[2], new TestSymbol("c")));
		transitions.add(new Transition<>(st[2], st[3], new TestSymbol("b")));
		transitions.add(new Transition<>(st[3], st[4], new TestSymbol("a")));

		// accepts language {a^nb^m}^pcba
		TestAutomaton dfa = new TestAutomaton(states, transitions);
		assertSame(dfa, dfa.determinize());

	}

	// expected
	@Test
	public void testNfa() {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[2];
		st[0] = new State(0, true, false);
		st[1] = new State(1, false, true);
		Collections.addAll(states, st);

		SortedSet<Transition<TestSymbol>> transitions = new TreeSet<>();
		transitions.add(new Transition<>(st[0], st[0], new TestSymbol("a")));
		transitions.add(new Transition<>(st[0], st[0], new TestSymbol("b")));
		transitions.add(new Transition<>(st[0], st[1], new TestSymbol("b")));

		// accepts language {a^nb^m}^p
		TestAutomaton nfa = new TestAutomaton(states, transitions);

		SortedSet<State> expStates = new TreeSet<>();
		State[] st2 = new State[2];
		st2[0] = new State(0, true, false);
		st2[1] = new State(1, false, true);
		Collections.addAll(expStates, st2);

		SortedSet<Transition<TestSymbol>> expDelta = new TreeSet<>();
		expDelta.add(new Transition<>(st2[0], st2[0], new TestSymbol("a")));
		expDelta.add(new Transition<>(st2[0], st2[1], new TestSymbol("b")));
		expDelta.add(new Transition<>(st2[1], st2[0], new TestSymbol("a")));
		expDelta.add(new Transition<>(st2[1], st2[1], new TestSymbol("b")));

		// accepts language {a^nb^m}^p
		TestAutomaton expected = new TestAutomaton(expStates, expDelta);
		assertEquals(expected, nfa.determinize());
	}

	@Test
	public void testEpsNfa() {
		SortedSet<State> states = new TreeSet<>();
		SortedSet<Transition<TestSymbol>> delta = new TreeSet<>();
		State[] st = new State[11];
		st[0] = new State(0, true, false);
		st[1] = new State(1, false, false);
		st[2] = new State(2, false, false);
		st[3] = new State(3, false, false);
		st[4] = new State(4, false, false);
		st[5] = new State(5, false, false);
		st[6] = new State(6, false, false);
		st[7] = new State(7, false, false);
		st[8] = new State(8, false, false);
		st[9] = new State(9, false, false);
		st[10] = new State(10, false, true);
		Collections.addAll(states, st);

		delta.add(new Transition<>(st[0], st[1], new TestSymbol("")));
		delta.add(new Transition<>(st[0], st[7], new TestSymbol("")));
		delta.add(new Transition<>(st[1], st[2], new TestSymbol("")));
		delta.add(new Transition<>(st[1], st[4], new TestSymbol("")));
		delta.add(new Transition<>(st[2], st[3], new TestSymbol("a")));
		delta.add(new Transition<>(st[3], st[6], new TestSymbol("")));
		delta.add(new Transition<>(st[4], st[5], new TestSymbol("b")));
		delta.add(new Transition<>(st[5], st[6], new TestSymbol("")));
		delta.add(new Transition<>(st[6], st[1], new TestSymbol("")));
		delta.add(new Transition<>(st[6], st[7], new TestSymbol("")));
		delta.add(new Transition<>(st[7], st[8], new TestSymbol("a")));
		delta.add(new Transition<>(st[8], st[9], new TestSymbol("b")));
		delta.add(new Transition<>(st[9], st[10], new TestSymbol("b")));
		// {a ,b}^n abb
		TestAutomaton a = new TestAutomaton(states, delta);

		SortedSet<State> expStates = new TreeSet<>();
		SortedSet<Transition<TestSymbol>> expDelta = new TreeSet<>();
		State[] st2 = new State[5];
		st2[0] = new State(0, true, false);
		st2[1] = new State(1, false, false);
		st2[2] = new State(2, false, false);
		st2[3] = new State(3, false, false);
		st2[4] = new State(4, false, true);
		Collections.addAll(expStates, st2);

		expDelta.add(new Transition<>(st2[0], st2[1], new TestSymbol("a")));
		expDelta.add(new Transition<>(st2[0], st2[2], new TestSymbol("b")));
		expDelta.add(new Transition<>(st2[1], st2[1], new TestSymbol("a")));
		expDelta.add(new Transition<>(st2[1], st2[3], new TestSymbol("b")));
		expDelta.add(new Transition<>(st2[2], st2[1], new TestSymbol("a")));
		expDelta.add(new Transition<>(st2[2], st2[2], new TestSymbol("b")));
		expDelta.add(new Transition<>(st2[3], st2[1], new TestSymbol("a")));
		expDelta.add(new Transition<>(st2[3], st2[4], new TestSymbol("b")));
		expDelta.add(new Transition<>(st2[4], st2[1], new TestSymbol("a")));
		expDelta.add(new Transition<>(st2[4], st2[2], new TestSymbol("b")));
		// {a,b}^n abb
		TestAutomaton expected = new TestAutomaton(expStates, expDelta);

		assertEquals(expected, a.determinize());
	}

}
