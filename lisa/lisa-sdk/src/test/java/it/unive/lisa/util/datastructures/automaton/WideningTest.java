package it.unive.lisa.util.datastructures.automaton;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

public class WideningTest {

	@Test
	public void test01() {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[5];
		st[0] = new State(0, true, true);
		st[1] = new State(1, false, true);
		st[2] = new State(2, false, true);
		st[3] = new State(3, false, true);
		st[4] = new State(4, false, true);

		Collections.addAll(states, st);

		SortedSet<Transition<TestSymbol>> delta = new TreeSet<>();
		delta.add(new Transition<>(st[0], st[1], new TestSymbol("a")));
		delta.add(new Transition<>(st[1], st[2], new TestSymbol("a")));
		delta.add(new Transition<>(st[2], st[3], new TestSymbol("a")));
		delta.add(new Transition<>(st[3], st[4], new TestSymbol("a")));

		TestAutomaton a = new TestAutomaton(states, delta);

		SortedSet<State> expStates = new TreeSet<>();
		State q = new State(0, true, true);
		expStates.add(q);
		SortedSet<Transition<TestSymbol>> expDelta = new TreeSet<>();
		expDelta.add(new Transition<>(q, q, new TestSymbol("a")));

		TestAutomaton exp = new TestAutomaton(expStates, expDelta);

		assertEquals(exp, a.widening(2));
		assertTrue(a.isEqualTo(a.widening(5)));
	}

}
