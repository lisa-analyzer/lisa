package it.unive.lisa.util.datastructures.automaton;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.Test;

public class TotalizeTest {

	@Test
	public void testLNComplementStep2() {
		SortedSet<State> states = new TreeSet<>();
		SortedSet<Transition<TestSymbol>> delta = new TreeSet<>();
		State[] st = new State[2];
		st[0] = new State(0, true, false);
		st[1] = new State(1, false, true);
		Collections.addAll(states, st);

		delta.add(new Transition<>(st[0], st[1], new TestSymbol("a")));
		delta.add(new Transition<>(st[1], st[0], new TestSymbol("b")));
		SortedSet<TestSymbol> sigma = new TreeSet<>();
		sigma.add(new TestSymbol("a"));
		sigma.add(new TestSymbol("b"));

		TestAutomaton a = new TestAutomaton(states, delta);

		SortedSet<State> expStates = new TreeSet<>();
		SortedSet<Transition<TestSymbol>> expDelta = new TreeSet<>();
		State[] expSt = new State[3];
		expSt[0] = new State(0, true, false);
		expSt[1] = new State(1, false, true);
		expSt[2] = new State(2, false, false);
		Collections.addAll(expStates, expSt);

		expDelta.add(new Transition<>(expSt[0], expSt[1], new TestSymbol("a")));
		expDelta.add(new Transition<>(expSt[1], expSt[0], new TestSymbol("b")));
		expDelta.add(new Transition<>(expSt[0], expSt[2], new TestSymbol("b")));
		expDelta.add(new Transition<>(expSt[1], expSt[2], new TestSymbol("a")));
		expDelta.add(new Transition<>(expSt[2], expSt[2], new TestSymbol("a")));
		expDelta.add(new Transition<>(expSt[2], expSt[2], new TestSymbol("b")));

		TestAutomaton exp = new TestAutomaton(expStates, expDelta);

		assertEquals(exp, a.totalize(sigma));
	}
}
