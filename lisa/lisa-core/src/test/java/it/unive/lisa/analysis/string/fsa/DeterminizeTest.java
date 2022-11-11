package it.unive.lisa.analysis.string.fsa;

import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

public class DeterminizeTest {

	@Test
	public void testDfa() {
		Set<State> states = new HashSet<>();
		State[] st = new State[5];
		st[0] = new State(true, false);
		st[1] = new State(false, false);
		st[2] = new State(false, false);
		st[3] = new State(false, false);
		st[4] = new State(false, true);
		Collections.addAll(states, st);

		Set<Transition> transitions = new HashSet<>();
		transitions.add(new Transition(st[0], st[0], "a"));
		transitions.add(new Transition(st[0], st[1], "b"));
		transitions.add(new Transition(st[1], st[0], "a"));
		transitions.add(new Transition(st[1], st[1], "b"));
		transitions.add(new Transition(st[1], st[2], "c"));
		transitions.add(new Transition(st[2], st[3], "b"));
		transitions.add(new Transition(st[3], st[4], "a"));

		// accepts language {a^nb^m}^pcba
		Automaton dfa = new Automaton(states, transitions);
		assertTrue(dfa.isEqual(dfa.determinize()));

	}

	// expected
	@Test
	public void testNfa() {
		Set<State> states = new HashSet<>();
		State[] st = new State[2];
		st[0] = new State(true, false);
		st[1] = new State(false, true);
		Collections.addAll(states, st);

		Set<Transition> transitions = new HashSet<>();
		transitions.add(new Transition(st[0], st[0], "a"));
		transitions.add(new Transition(st[0], st[0], "b"));
		transitions.add(new Transition(st[0], st[1], "b"));

		// accepts language {a^nb^m}^p
		Automaton nfa = new Automaton(states, transitions);

		Set<State> expStates = new HashSet<>();
		State[] st2 = new State[2];
		st2[0] = new State(true, false);
		st2[1] = new State(false, true);
		Collections.addAll(expStates, st2);

		Set<Transition> expDelta = new HashSet<>();
		expDelta.add(new Transition(st2[0], st2[0], "a"));
		expDelta.add(new Transition(st2[0], st2[1], "b"));
		expDelta.add(new Transition(st2[1], st2[0], "a"));
		expDelta.add(new Transition(st2[1], st2[1], "b"));

		// accepts language {a^nb^m}^p
		Automaton expected = new Automaton(expStates, expDelta);
		assertTrue(expected.isEqual(nfa.determinize()));
	}

	@Test
	public void testEpsNfa() {
		Set<State> states = new HashSet<>();
		Set<Transition> delta = new HashSet<>();
		State[] st = new State[11];
		st[0] = new State(true, false);
		st[1] = new State(false, false);
		st[2] = new State(false, false);
		st[3] = new State(false, false);
		st[4] = new State(false, false);
		st[5] = new State(false, false);
		st[6] = new State(false, false);
		st[7] = new State(false, false);
		st[8] = new State(false, false);
		st[9] = new State(false, false);
		st[10] = new State(false, true);
		Collections.addAll(states, st);

		delta.add(new Transition(st[0], st[1], ""));
		delta.add(new Transition(st[0], st[7], ""));
		delta.add(new Transition(st[1], st[2], ""));
		delta.add(new Transition(st[1], st[4], ""));
		delta.add(new Transition(st[2], st[3], "a"));
		delta.add(new Transition(st[3], st[6], ""));
		delta.add(new Transition(st[4], st[5], "b"));
		delta.add(new Transition(st[5], st[6], ""));
		delta.add(new Transition(st[6], st[1], ""));
		delta.add(new Transition(st[6], st[7], ""));
		delta.add(new Transition(st[7], st[8], "a"));
		delta.add(new Transition(st[8], st[9], "b"));
		delta.add(new Transition(st[9], st[10], "b"));
		// {a ,b}^n abb
		Automaton a = new Automaton(states, delta);

		Set<State> expStates = new HashSet<>();
		Set<Transition> expDelta = new HashSet<>();
		State[] st2 = new State[5];
		st2[0] = new State(true, false);
		st2[1] = new State(false, false);
		st2[2] = new State(false, false);
		st2[3] = new State(false, false);
		st2[4] = new State(false, true);
		Collections.addAll(expStates, st2);

		expDelta.add(new Transition(st2[0], st2[1], "a"));
		expDelta.add(new Transition(st2[0], st2[2], "b"));
		expDelta.add(new Transition(st2[1], st2[1], "a"));
		expDelta.add(new Transition(st2[1], st2[3], "b"));
		expDelta.add(new Transition(st2[2], st2[1], "a"));
		expDelta.add(new Transition(st2[2], st2[2], "b"));
		expDelta.add(new Transition(st2[3], st2[1], "a"));
		expDelta.add(new Transition(st2[3], st2[4], "b"));
		expDelta.add(new Transition(st2[4], st2[1], "a"));
		expDelta.add(new Transition(st2[4], st2[2], "b"));
		// {a,b}^n abb
		Automaton expected = new Automaton(expStates, expDelta);

		assertTrue(expected.isEqual(a.determinize()));
	}
}
