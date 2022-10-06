package it.unive.lisa.analysis.string;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

public class GetLanguageTest {

	@Test
	public void test01() throws CyclicAutomatonException {
		Set<State> states = new HashSet<>();
		Set<Transition> tr = new HashSet<>();
		State[] st = new State[3];
		st[0] = new State(true, false);
		st[1] = new State(false, false);
		st[2] = new State(false, true);
		Collections.addAll(states, st);

		tr.add(new Transition(st[0], st[1], "a"));
		tr.add(new Transition(st[1], st[2], "b"));
		Automaton a = new Automaton(states, tr);
		Set<String> exp = new HashSet<>();
		exp.add("ab");
		assertEquals(exp, a.getLanguage());
	}

	@Test(expected = CyclicAutomatonException.class)
	public void test02() throws CyclicAutomatonException {
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
		assertEquals(nfa.getLanguage(), new HashSet<String>());
	}

	@Test
	public void test03() throws CyclicAutomatonException {
		Set<State> states = new HashSet<>();
		State[] st = new State[6];
		st[0] = new State(true, false);
		st[1] = new State(false, false);
		st[2] = new State(false, false);
		st[3] = new State(false, false);
		st[4] = new State(false, true);
		st[5] = new State(false, true);
		Collections.addAll(states, st);

		Set<Transition> delta = new HashSet<>();
		delta.add(new Transition(st[0], st[1], "a"));
		delta.add(new Transition(st[0], st[2], "b"));
		delta.add(new Transition(st[1], st[3], "c"));
		delta.add(new Transition(st[2], st[4], "d"));
		delta.add(new Transition(st[3], st[5], "d"));

		Automaton a = new Automaton(states, delta);
		Set<String> expected = new HashSet<>();
		expected.add("acd");
		expected.add("bd");

		assertEquals(expected, a.getLanguage());
	}

	@Test
	public void test04() throws CyclicAutomatonException {
		Set<State> states = new HashSet<>();
		State[] st = new State[5];
		st[0] = new State(true, false);
		st[1] = new State(false, false);
		st[2] = new State(false, false);
		st[3] = new State(false, true);
		st[4] = new State(false, true);
		Collections.addAll(states, st);

		Set<Transition> delta = new HashSet<>();
		delta.add(new Transition(st[0], st[1], ""));
		delta.add(new Transition(st[0], st[2], ""));
		delta.add(new Transition(st[1], st[3], "a"));
		delta.add(new Transition(st[2], st[4], "b"));

		Set<String> exp = new HashSet<>();
		exp.add("a");
		exp.add("b");

		Automaton a = new Automaton(states, delta);

		assertEquals(exp, a.getLanguage());
	}

	@Test
	public void test05() throws CyclicAutomatonException {
		Set<State> states = new HashSet<>();
		State[] st = new State[5];
		st[0] = new State(true, false);
		st[1] = new State(false, false);
		st[2] = new State(false, false);
		st[3] = new State(false, false);
		st[4] = new State(false, true);
		Collections.addAll(states, st);

		Set<Transition> delta = new HashSet<>();
		delta.add(new Transition(st[0], st[1], "a"));
		delta.add(new Transition(st[1], st[2], "b"));
		delta.add(new Transition(st[1], st[3], "a"));
		delta.add(new Transition(st[3], st[4], "c"));
		delta.add(new Transition(st[2], st[4], "c"));

		Set<String> exp = new HashSet<>();
		exp.add("abc");
		exp.add("aac");

		Automaton a = new Automaton(states, delta);
		assertEquals(a.getLanguage(), exp);
	}

	@Test
	public void test06() throws CyclicAutomatonException {
		Set<State> states = new HashSet<>();
		State[] st = new State[8];
		st[0] = new State(true, false);
		st[1] = new State(false, false);
		st[2] = new State(false, false);
		st[3] = new State(false, false);
		st[4] = new State(false, false);
		st[5] = new State(false, false);
		st[6] = new State(false, false);
		st[7] = new State(false, true);
		Collections.addAll(states, st);

		Set<Transition> delta = new HashSet<>();
		delta.add(new Transition(st[0], st[1], "a"));
		delta.add(new Transition(st[1], st[2], ""));
		delta.add(new Transition(st[2], st[3], "b"));
		delta.add(new Transition(st[2], st[4], "c"));
		delta.add(new Transition(st[3], st[5], ""));
		delta.add(new Transition(st[4], st[6], "d"));
		delta.add(new Transition(st[5], st[7], "c"));
		delta.add(new Transition(st[6], st[7], "c"));

		Set<String> exp = new HashSet<>();
		exp.add("abc");
		exp.add("acdc");

		Automaton a = new Automaton(states, delta);
		assertEquals(a.getLanguage(), exp);
	}

	@Test
	public void test07() throws CyclicAutomatonException {
		Set<State> states = new HashSet<>();
		State[] st = new State[4];
		st[0] = new State(true, false);
		st[1] = new State(false, true);
		st[2] = new State(false, false);
		st[3] = new State(false, true);
		Collections.addAll(states, st);

		Set<Transition> delta = new HashSet<>();
		delta.add(new Transition(st[0], st[1], "a"));
		delta.add(new Transition(st[1], st[2], "b"));
		delta.add(new Transition(st[2], st[3], "c"));

		Set<String> exp = new HashSet<>();
		exp.add("a");
		exp.add("abc");

		Automaton a = new Automaton(states, delta);
		assertEquals(exp, a.getLanguage());
	}
}
