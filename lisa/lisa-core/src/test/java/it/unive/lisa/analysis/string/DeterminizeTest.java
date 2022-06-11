package it.unive.lisa.analysis.string;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

public class DeterminizeTest {

	@Test
	public void testDfa() {
		Set<State> states = new HashSet<>();
		State[] st = new State[5];
		State s;
		for (int i = 0; i < 5; ++i) {
			if (i == 0)
				s = new State(i, true, false);

			else if (i == 4)
				s = new State(i, false, true);

			else
				s = new State(i, false, false);

			st[i] = s;
			states.add(s);
		}
		Set<Transition> transitions = new HashSet<>();
		transitions.add(new Transition(st[0], st[0], 'a'));
		transitions.add(new Transition(st[0], st[1], 'b'));
		transitions.add(new Transition(st[1], st[0], 'a'));
		transitions.add(new Transition(st[1], st[1], 'b'));
		transitions.add(new Transition(st[1], st[2], 'c'));
		transitions.add(new Transition(st[2], st[3], 'b'));
		transitions.add(new Transition(st[3], st[4], 'a'));

		// accepts language {a^nb^m}^pcba
		Automaton dfa = new Automaton(states, transitions);
		assertEquals(dfa, dfa.determinize());

	}

	// TODO: capire perché fallisce, dal debug ottengo esattamente l'automa
	// expected
	@Test
	public void testNfa() {
		Set<State> states = new HashSet<>();
		State[] st = new State[2];
		st[0] = new State(0, true, false);
		st[1] = new State(1, false, true);

		states.add(st[0]);
		states.add(st[1]);

		Set<Transition> transitions = new HashSet<>();
		transitions.add(new Transition(st[0], st[0], 'a'));
		transitions.add(new Transition(st[0], st[0], 'b'));
		transitions.add(new Transition(st[0], st[1], 'b'));

		// accepts language {a^nb^m}^p
		Automaton nfa = new Automaton(states, transitions);

		Set<State> expStates = new HashSet<>();
		State q0 = new State(0, true, false);
		State q1 = new State(1, false, true);
		Set<Transition> expDelta = new HashSet<>();
		expDelta.add(new Transition(q0, q0, 'a'));
		expDelta.add(new Transition(q0, q1, 'b'));
		expDelta.add(new Transition(q1, q0, 'a'));
		expDelta.add(new Transition(q1, q1, 'b'));

		// accepts language {a^nb^m}^p
		Automaton expected = new Automaton(expStates, expDelta);
		assertEquals(expected, nfa.determinize());

	}

	@Test
	public void testEpsNfa() {
		Set<State> states = new HashSet<>();
		Set<Transition> delta = new HashSet<>();
		State q0 = new State(0, true, false);
		State q1 = new State(1, false, false);
		State q2 = new State(2, false, false);
		State q3 = new State(3, false, false);
		State q4 = new State(4, false, false);
		State q5 = new State(5, false, false);
		State q6 = new State(6, false, false);
		State q7 = new State(7, false, false);
		State q8 = new State(8, false, false);
		State q9 = new State(9, false, false);
		State q10 = new State(10, false, true);
		states.add(q0);
		states.add(q1);
		states.add(q2);
		states.add(q3);
		states.add(q4);
		states.add(q5);
		states.add(q6);
		states.add(q7);
		states.add(q8);
		states.add(q9);
		states.add(q10);
		delta.add(new Transition(q0, q1, ' '));
		delta.add(new Transition(q0, q7, ' '));
		delta.add(new Transition(q1, q2, ' '));
		delta.add(new Transition(q1, q4, ' '));
		delta.add(new Transition(q2, q3, 'a'));
		delta.add(new Transition(q3, q6, ' '));
		delta.add(new Transition(q4, q5, 'b'));
		delta.add(new Transition(q5, q6, ' '));
		delta.add(new Transition(q6, q1, ' '));
		delta.add(new Transition(q6, q7, ' '));
		delta.add(new Transition(q7, q8, 'a'));
		delta.add(new Transition(q8, q9, 'b'));
		delta.add(new Transition(q9, q10, 'b'));
		// {a ,b}^n abb
		Automaton a = new Automaton(states, delta);

		Set<State> expStates = new HashSet<>();
		Set<Transition> expDelta = new HashSet<>();
		State s0 = new State(0, true, false);
		State s1 = new State(1, false, false);
		State s2 = new State(2, false, false);
		State s3 = new State(3, false, false);
		State s4 = new State(4, false, true);
		expStates.add(s0);
		expStates.add(s1);
		expStates.add(s2);
		expStates.add(s3);
		expStates.add(s4);
		expDelta.add(new Transition(s0, s1, 'a'));
		expDelta.add(new Transition(s0, s2, 'b'));
		expDelta.add(new Transition(s1, s1, 'a'));
		expDelta.add(new Transition(s1, s3, 'b'));
		expDelta.add(new Transition(s2, s1, 'a'));
		expDelta.add(new Transition(s2, s2, 'b'));
		expDelta.add(new Transition(s3, s1, 'a'));
		expDelta.add(new Transition(s3, s4, 'b'));
		expDelta.add(new Transition(s4, s1, 'a'));
		expDelta.add(new Transition(s4, s2, 'b'));
		// {a,b}^n abb
		Automaton expected = new Automaton(expStates, expDelta);

		assertEquals(expected, a.determinize());
	}

}
