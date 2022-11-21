package it.unive.lisa.analysis.string.fsa;

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
		SortedSet<Transition> thisDelta = new TreeSet<>();
		SortedSet<Transition> otherDelta = new TreeSet<>();
		st[0] = new State(true, false);
		st[1] = new State(false, false);
		st[2] = new State(false, false);
		st[3] = new State(false, true);
		Collections.addAll(thisStates, st);

		st2[0] = new State(true, false);
		st2[1] = new State(false, false);
		st2[2] = new State(false, true);
		Collections.addAll(otherStates, st2);

		thisDelta.add(new Transition(st[0], st[1], "a"));
		thisDelta.add(new Transition(st[0], st[2], "b"));
		thisDelta.add(new Transition(st[1], st[3], "c"));
		thisDelta.add(new Transition(st[2], st[3], "c"));

		otherDelta.add(new Transition(st2[0], st2[1], "a"));
		otherDelta.add(new Transition(st2[0], st2[1], "b"));
		otherDelta.add(new Transition(st2[1], st2[2], "c"));

		Automaton thisAutomaton = new Automaton(thisStates, thisDelta);
		Automaton otherAutomaton = new Automaton(otherStates, otherDelta);

		assertTrue(thisAutomaton.isEqual(otherAutomaton));
	}

	@Test
	public void twoNotEqualStrings() {
		SortedSet<State> thisStates = new TreeSet<>();
		State[] st = new State[2];

		SortedSet<State> otherStates = new TreeSet<>();
		State[] st2 = new State[2];

		SortedSet<Transition> thisDelta = new TreeSet<>();
		SortedSet<Transition> otherDelta = new TreeSet<>();

		st[0] = new State(true, false);
		st[1] = new State(false, true);
		Collections.addAll(thisStates, st);

		st2[0] = new State(true, false);
		st2[1] = new State(false, true);
		Collections.addAll(otherStates, st2);

		thisDelta.add(new Transition(st[0], st[1], "a"));
		otherDelta.add(new Transition(st2[0], st2[1], "b"));

		// a
		Automaton thisAutomaton = new Automaton(thisStates, thisDelta);

		// b
		Automaton otherAutomaton = new Automaton(otherStates, otherDelta);
		assertTrue(!thisAutomaton.isEqual(otherAutomaton));
	}

	@Test
	public void twoEqualsStringsWithEpsilon() {
		SortedSet<State> thisStates = new TreeSet<>();
		State[] st = new State[3];

		SortedSet<State> otherStates = new TreeSet<>();
		State[] st2 = new State[2];

		SortedSet<Transition> thisDelta = new TreeSet<>();
		SortedSet<Transition> otheDelta = new TreeSet<>();

		st[0] = new State(true, false);
		st[1] = new State(false, false);
		st[2] = new State(false, true);
		Collections.addAll(thisStates, st);

		st2[0] = new State(true, false);
		st2[1] = new State(false, true);
		Collections.addAll(otherStates, st2);

		thisDelta.add(new Transition(st[0], st[1], "a"));
		thisDelta.add(new Transition(st[1], st[2], ""));

		otheDelta.add(new Transition(st2[0], st2[1], "a"));

		// a (but with epsilon)
		Automaton thisAutomaton = new Automaton(thisStates, thisDelta);

		// a
		Automaton otherAutomaton = new Automaton(otherStates, otheDelta);
		assertTrue(thisAutomaton.isEqual(otherAutomaton));
	}

	@Test
	public void twoNotEqualsStringsWithEpsilon() {
		SortedSet<State> thisStates = new TreeSet<>();
		State[] st = new State[3];

		SortedSet<State> otherStates = new TreeSet<>();
		State[] st2 = new State[2];

		SortedSet<Transition> thisDelta = new TreeSet<>();
		SortedSet<Transition> otherDelta = new TreeSet<>();

		st[0] = new State(true, false);
		st[1] = new State(false, false);
		st[2] = new State(false, true);
		Collections.addAll(thisStates, st);

		st2[0] = new State(true, false);
		st2[1] = new State(false, true);
		Collections.addAll(otherStates, st2);

		thisDelta.add(new Transition(st[0], st[1], "b"));
		thisDelta.add(new Transition(st[1], st[2], ""));

		otherDelta.add(new Transition(st2[0], st2[1], "a"));

		// a (but with epsilon)
		Automaton thisAutomaton = new Automaton(thisStates, thisDelta);

		// a
		Automaton otherAutomaton = new Automaton(otherStates, otherDelta);
		assertTrue(!thisAutomaton.isEqual(otherAutomaton));
	}

	@Test
	public void nonDeterministicAutomaton() {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[4];

		SortedSet<State> states2 = new TreeSet<>();
		State[] st2 = new State[2];

		SortedSet<Transition> delta = new TreeSet<>();
		SortedSet<Transition> delta2 = new TreeSet<>();

		st[0] = new State(true, false);
		st[1] = new State(false, false);
		st[2] = new State(false, false);
		st[3] = new State(false, true);
		Collections.addAll(states, st);

		st2[0] = new State(true, false);
		st2[1] = new State(false, true);
		Collections.addAll(states2, st2);

		delta.add(new Transition(st[0], st[1], "a"));
		delta.add(new Transition(st[1], st[3], "b"));
		delta.add(new Transition(st[0], st[2], "a"));
		delta.add(new Transition(st[2], st[3], "b"));

		delta2.add(new Transition(st2[0], st2[1], "a"));

		// ab (but with two paths, hence non-deterministic)
		Automaton a = new Automaton(states, delta);

		// a
		Automaton a2 = new Automaton(states2, delta2);
		assertTrue(!a.isEqual(a2));
	}
}
