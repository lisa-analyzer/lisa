package it.unive.lisa.util.datastructures.automaton;

import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.Test;

public class IsEqualTest {

	@Test
	public void test01() {
		SortedSet<State> thisStates = new TreeSet<>();
		State[] st = new State[4];
		SortedSet<State> otherStates = new TreeSet<>();
		State[] st2 = new State[3];
		SortedSet<Transition<TestSymbol>> thisDelta = new TreeSet<>();
		SortedSet<Transition<TestSymbol>> otherDelta = new TreeSet<>();
		st[0] = new State(0, true, false);
		st[1] = new State(1, false, false);
		st[2] = new State(2, false, false);
		st[3] = new State(3, false, true);
		Collections.addAll(thisStates, st);

		st2[0] = new State(4, true, false);
		st2[1] = new State(5, false, false);
		st2[2] = new State(6, false, true);
		Collections.addAll(otherStates, st2);

		thisDelta.add(new Transition<>(st[0], st[1], new TestSymbol("a")));
		thisDelta.add(new Transition<>(st[0], st[2], new TestSymbol("b")));
		thisDelta.add(new Transition<>(st[1], st[3], new TestSymbol("c")));
		thisDelta.add(new Transition<>(st[2], st[3], new TestSymbol("c")));

		otherDelta.add(new Transition<>(st2[0], st2[1], new TestSymbol("a")));
		otherDelta.add(new Transition<>(st2[0], st2[1], new TestSymbol("b")));
		otherDelta.add(new Transition<>(st2[1], st2[2], new TestSymbol("c")));

		TestAutomaton thisTestAutomaton = new TestAutomaton(thisStates, thisDelta);
		TestAutomaton otherTestAutomaton = new TestAutomaton(otherStates, otherDelta);

		assertTrue(thisTestAutomaton.isEqualTo(otherTestAutomaton));
	}

	@Test
	public void twoNotEqualStrings() {
		SortedSet<State> thisStates = new TreeSet<>();
		State[] st = new State[2];

		SortedSet<State> otherStates = new TreeSet<>();
		State[] st2 = new State[2];

		SortedSet<Transition<TestSymbol>> thisDelta = new TreeSet<>();
		SortedSet<Transition<TestSymbol>> otherDelta = new TreeSet<>();

		st[0] = new State(0, true, false);
		st[1] = new State(1, false, true);
		Collections.addAll(thisStates, st);

		st2[0] = new State(2, true, false);
		st2[1] = new State(3, false, true);
		Collections.addAll(otherStates, st2);

		thisDelta.add(new Transition<>(st[0], st[1], new TestSymbol("a")));
		otherDelta.add(new Transition<>(st2[0], st2[1], new TestSymbol("b")));

		// a
		TestAutomaton thisTestAutomaton = new TestAutomaton(thisStates, thisDelta);

		// b
		TestAutomaton otherTestAutomaton = new TestAutomaton(otherStates, otherDelta);
		assertTrue(!thisTestAutomaton.isEqualTo(otherTestAutomaton));
	}

	@Test
	public void twoEqualsStringsWithEpsilon() {
		SortedSet<State> thisStates = new TreeSet<>();
		State[] st = new State[3];

		SortedSet<State> otherStates = new TreeSet<>();
		State[] st2 = new State[2];

		SortedSet<Transition<TestSymbol>> thisDelta = new TreeSet<>();
		SortedSet<Transition<TestSymbol>> otheDelta = new TreeSet<>();

		st[0] = new State(0, true, false);
		st[1] = new State(1, false, false);
		st[2] = new State(2, false, true);
		Collections.addAll(thisStates, st);

		st2[0] = new State(3, true, false);
		st2[1] = new State(4, false, true);
		Collections.addAll(otherStates, st2);

		thisDelta.add(new Transition<>(st[0], st[1], new TestSymbol("a")));
		thisDelta.add(new Transition<>(st[1], st[2], new TestSymbol("")));

		otheDelta.add(new Transition<>(st2[0], st2[1], new TestSymbol("a")));

		// a (but with epsilon)
		TestAutomaton thisTestAutomaton = new TestAutomaton(thisStates, thisDelta);

		// a
		TestAutomaton otherTestAutomaton = new TestAutomaton(otherStates, otheDelta);
		assertTrue(thisTestAutomaton.isEqualTo(otherTestAutomaton));
	}

	@Test
	public void twoNotEqualsStringsWithEpsilon() {
		SortedSet<State> thisStates = new TreeSet<>();
		State[] st = new State[3];

		SortedSet<State> otherStates = new TreeSet<>();
		State[] st2 = new State[2];

		SortedSet<Transition<TestSymbol>> thisDelta = new TreeSet<>();
		SortedSet<Transition<TestSymbol>> otherDelta = new TreeSet<>();

		st[0] = new State(0, true, false);
		st[1] = new State(1, false, false);
		st[2] = new State(2, false, true);
		Collections.addAll(thisStates, st);

		st2[0] = new State(3, true, false);
		st2[1] = new State(4, false, true);
		Collections.addAll(otherStates, st2);

		thisDelta.add(new Transition<>(st[0], st[1], new TestSymbol("b")));
		thisDelta.add(new Transition<>(st[1], st[2], new TestSymbol("")));

		otherDelta.add(new Transition<>(st2[0], st2[1], new TestSymbol("a")));

		// a (but with epsilon)
		TestAutomaton thisTestAutomaton = new TestAutomaton(thisStates, thisDelta);

		// a
		TestAutomaton otherTestAutomaton = new TestAutomaton(otherStates, otherDelta);
		assertTrue(!thisTestAutomaton.isEqualTo(otherTestAutomaton));
	}

	@Test
	public void nonDeterministicTestAutomaton() {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[4];

		SortedSet<State> states2 = new TreeSet<>();
		State[] st2 = new State[2];

		SortedSet<Transition<TestSymbol>> delta = new TreeSet<>();
		SortedSet<Transition<TestSymbol>> delta2 = new TreeSet<>();

		st[0] = new State(0, true, false);
		st[1] = new State(1, false, false);
		st[2] = new State(2, false, false);
		st[3] = new State(3, false, true);
		Collections.addAll(states, st);

		st2[0] = new State(4, true, false);
		st2[1] = new State(5, false, true);
		Collections.addAll(states2, st2);

		delta.add(new Transition<>(st[0], st[1], new TestSymbol("a")));
		delta.add(new Transition<>(st[1], st[3], new TestSymbol("b")));
		delta.add(new Transition<>(st[0], st[2], new TestSymbol("a")));
		delta.add(new Transition<>(st[2], st[3], new TestSymbol("b")));

		delta2.add(new Transition<>(st2[0], st2[1], new TestSymbol("a")));

		// ab (but with two paths, hence non-deterministic)
		TestAutomaton a = new TestAutomaton(states, delta);

		// a
		TestAutomaton a2 = new TestAutomaton(states2, delta2);
		assertTrue(!a.isEqualTo(a2));
	}

}
