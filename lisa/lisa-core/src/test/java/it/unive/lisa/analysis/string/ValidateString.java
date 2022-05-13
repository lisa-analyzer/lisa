package it.unive.lisa.analysis.string;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class ValidateString {

	// testing dfa
	@Test
	public void testDfa() {
		Set<State> states = new HashSet<>();
		Set<State> initialStates = new HashSet<>();
		Set<State> finalStates = new HashSet<>();
		State[] st = new State[5];
		State s;
		for(int i = 0; i < 5; ++i) {
			if(i == 0) {
				s = new State(i, true, false);
				initialStates.add(s);
			}
			else if(i == 4) {
				s = new State(i, false, true); 
				finalStates.add(s);
			}
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

		Automaton dfa = new Automaton(states, transitions, initialStates, finalStates);

		assertTrue(dfa.validateString("aabbababcba"));
		assertTrue(dfa.validateString("abababbbbaabcba"));

		assertFalse(dfa.validateString("baababacba"));
		assertFalse(dfa.validateString("aabbabacba"));
		assertFalse(dfa.validateString("cabcba"));
	}

	// testing nfa
	@Test
	public void testNfa() {
		Set<State> states = new HashSet<>();
		Set<State> initialStates = new HashSet<>();
		Set<State> finalStates = new HashSet<>();
		State[] st = new State[6];
		State s;
		for(int i = 0; i < 6; ++i) {
			if(i == 0) {
				s = new State(i, true, false);
				initialStates.add(s);
			}
			else if(i == 1 || i == 2) {
				s = new State(i, false, true); 
				finalStates.add(s);
			}
			else 
				s = new State(i, false, false);

			st[i] = s;
			states.add(s);
		}
		Set<Transition> transitions = new HashSet<>();
		transitions.add(new Transition(st[0], st[0], 'a'));
		transitions.add(new Transition(st[0], st[2], 'a'));
		transitions.add(new Transition(st[1], st[3], 'a'));
		transitions.add(new Transition(st[2], st[5], 'b'));
		transitions.add(new Transition(st[3], st[4], 'a'));
		transitions.add(new Transition(st[4], st[1], 'b'));
		transitions.add(new Transition(st[5], st[2], 'a'));

		Automaton dfa = new Automaton(states, transitions, initialStates, finalStates);

		assertTrue(dfa.validateString("ababababababa"));
		assertTrue(dfa.validateString("ababababababab"));

		assertFalse(dfa.validateString("bababababab"));
		assertFalse(dfa.validateString("abababababb"));
	}

	// testing epsilon nfa
	@Test
	public void testEpsNfa() {

	}

}
