package it.unive.lisa.analysis.string;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class ValidateString {

	// testing dfa
	@Test
	public void dfa() {
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

		// accepts {a^n b^m}^p cba
		Automaton dfa = new Automaton(states, transitions, initialStates, finalStates);

		assertTrue(dfa.validateString("aabbababcba"));
		assertTrue(dfa.validateString("abababbbbaabcba"));

		assertFalse(dfa.validateString("baababacba"));
		assertFalse(dfa.validateString("aabbabacba"));
		assertFalse(dfa.validateString("cabcba"));
	}

	@Test
	public void dfa1() {
		Set<State> states = new HashSet<>();
		Set<State> initialStates = new HashSet<>();
		Set<State> finalStates = new HashSet<>();
		State[] st = new State[7];
		State s;
		for(int i = 0; i < 7; ++i) {
			if(i == 0) {
				s = new State(i, true, false);
				initialStates.add(s);
			}
			else if(i == 5 || i == 6) {
				s = new State(i, false, true); 
				finalStates.add(s);
			}
			else 
				s = new State(i, false, false);

			st[i] = s;
			states.add(s);
		}
		Set<Transition> transitions = new HashSet<>();
		transitions.add(new Transition(st[0], st[1], 'a'));
		transitions.add(new Transition(st[1], st[2], 'b'));
		transitions.add(new Transition(st[2], st[3], 'c'));
		transitions.add(new Transition(st[3], st[3], 'd'));
		transitions.add(new Transition(st[3], st[4], 'c'));
		transitions.add(new Transition(st[4], st[5], 'b'));
		transitions.add(new Transition(st[5], st[5], 'b'));
		transitions.add(new Transition(st[5], st[6], 'a'));
		transitions.add(new Transition(st[6], st[6], 'a'));


		// accepts abcd^ncb^na^n
		Automaton dfa = new Automaton(states, transitions, initialStates, finalStates);

		assertTrue(dfa.validateString("abcdddcbbaa"));
		assertTrue(dfa.validateString("abcdddcbbbb"));

		assertFalse(dfa.validateString("abcdcdbba"));
		assertFalse(dfa.validateString("abcdcbbab"));
	}

	// testing nfa
	@Test
	public void nfa() {
		
	}

	@Test
	public void nfa1() {

	}

	// testing epsilon nfa
	@Test
	public void enfa() {

	}

	@Test
	public void enfa1() {

	}
}
