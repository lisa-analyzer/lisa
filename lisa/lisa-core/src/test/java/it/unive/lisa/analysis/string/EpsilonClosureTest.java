package it.unive.lisa.analysis.string;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

public class EpsilonClosureTest {

	@Test
	public void testEpsClosure001() {
		Set<State> states = new HashSet<>();
		State q0 = new State(0, true, false);
		State q1 = new State(1, false, false);
		State q2 = new State(2, false, true);
		states.add(q0);
		states.add(q1);
		states.add(q2);

		Set<Transition> delta = new HashSet<>();
		delta.add(new Transition(q0, q1, ""));
		delta.add(new Transition(q1, q2, "b"));
		delta.add(new Transition(q1, q2, "a"));

		// a | b
		Automaton a = new Automaton(states, delta);
		Set<State> expected = new HashSet<State>();
		expected.add(q0);
		expected.add(q1);

		assertEquals(expected, a.epsClosure());
	}

	@Test
	public void testEpsClosure002() {
		Set<State> states = new HashSet<>();
		State q0 = new State(0, true, false);
		State q1 = new State(1, false, false);
		State q2 = new State(2, false, false);
		State q3 = new State(3, false, true);
		states.add(q0);
		states.add(q1);
		states.add(q2);

		Set<Transition> delta = new HashSet<>();
		delta.add(new Transition(q0, q1, ""));
		delta.add(new Transition(q1, q2, ""));
		delta.add(new Transition(q2, q3, "b"));
		delta.add(new Transition(q2, q3, "a"));
		delta.add(new Transition(q2, q3, "c"));

		// a | b | c
		Automaton a = new Automaton(states, delta);
		Set<State> expected = new HashSet<>();
		expected.add(q0);
		expected.add(q1);
		expected.add(q2);

		assertEquals(expected, a.epsClosure());
	}

	@Test
	public void testEpsClosure003() {
		Set<State> states = new HashSet<>();
		State q0 = new State(0, true, true);
		states.add(q0);

		Set<Transition> delta = new HashSet<>();
		delta.add(new Transition(q0, q0, "a"));

		// a*
		Automaton a = new Automaton(states, delta);
		Set<State> expected = new HashSet<>();
		expected.add(q0);

		assertEquals(expected, a.epsClosure());
	}

	@Test
	public void testEpsClosure004() {
		Set<State> states = new HashSet<>();
		State q0 = new State(0, true, true);
		states.add(q0);

		Set<Transition> delta = new HashSet<>();
		delta.add(new Transition(q0, q0, "a"));
		delta.add(new Transition(q0, q0, ""));

		// eps | a*
		Automaton a = new Automaton(states, delta);
		Set<State> expected = new HashSet<>();
		expected.add(q0);

		assertEquals(expected, a.epsClosure());
	}

	@Test
	public void testEpsClosure005() {
		Set<State> states = new HashSet<>();
		State q0 = new State(0, true, true);
		states.add(q0);

		Set<Transition> delta = new HashSet<>();
		delta.add(new Transition(q0, q0, "a"));
		delta.add(new Transition(q0, q0, "b"));
		delta.add(new Transition(q0, q0, ""));

		// (a | b | eps)
		Automaton a = new Automaton(states, delta);
		Set<State> expected = new HashSet<>();
		expected.add(q0);

		assertEquals(expected, a.epsClosure());
	}

	@Test
	public void testEpsClosure006() {
		Set<State> states = new HashSet<>();
		State q0 = new State(0, true, false);
		State q1 = new State(1, false, true);
		states.add(q0);
		states.add(q1);

		Set<Transition> delta = new HashSet<>();
		delta.add(new Transition(q0, q1, "a"));
		delta.add(new Transition(q0, q1, "b"));
		delta.add(new Transition(q0, q1, ""));
		delta.add(new Transition(q1, q1, "a"));
		delta.add(new Transition(q1, q1, "b"));
		delta.add(new Transition(q1, q1, ""));

		// (a | b | eps)+
		Automaton a = new Automaton(states, delta);
		Set<State> expected = new HashSet<>();
		expected.add(q0);
		expected.add(q1);

		assertEquals(expected, a.epsClosure());
	}

}
