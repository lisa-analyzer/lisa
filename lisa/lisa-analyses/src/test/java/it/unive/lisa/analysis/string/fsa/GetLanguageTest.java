package it.unive.lisa.analysis.string.fsa;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.Test;

public class GetLanguageTest {

	@Test
	public void test01() throws CyclicAutomatonException {
		SortedSet<State> states = new TreeSet<>();
		SortedSet<Transition> tr = new TreeSet<>();
		State[] st = new State[3];
		st[0] = new State(true, false);
		st[1] = new State(false, false);
		st[2] = new State(false, true);
		Collections.addAll(states, st);

		tr.add(new Transition(st[0], st[1], "a"));
		tr.add(new Transition(st[1], st[2], "b"));
		Automaton a = new Automaton(states, tr);
		SortedSet<String> exp = new TreeSet<>();
		exp.add("ab");
		assertEquals(exp, a.getLanguage());
	}

	@Test(expected = CyclicAutomatonException.class)
	public void test02() throws CyclicAutomatonException {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[2];
		st[0] = new State(true, false);
		st[1] = new State(false, true);
		Collections.addAll(states, st);

		SortedSet<Transition> transitions = new TreeSet<>();
		transitions.add(new Transition(st[0], st[0], "a"));
		transitions.add(new Transition(st[0], st[0], "b"));
		transitions.add(new Transition(st[0], st[1], "b"));

		// accepts language {a^nb^m}^p
		Automaton nfa = new Automaton(states, transitions);
		assertEquals(nfa.getLanguage(), new TreeSet<String>());
	}

	@Test
	public void test03() throws CyclicAutomatonException {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[6];
		st[0] = new State(true, false);
		st[1] = new State(false, false);
		st[2] = new State(false, false);
		st[3] = new State(false, false);
		st[4] = new State(false, true);
		st[5] = new State(false, true);
		Collections.addAll(states, st);

		SortedSet<Transition> delta = new TreeSet<>();
		delta.add(new Transition(st[0], st[1], "a"));
		delta.add(new Transition(st[0], st[2], "b"));
		delta.add(new Transition(st[1], st[3], "c"));
		delta.add(new Transition(st[2], st[4], "d"));
		delta.add(new Transition(st[3], st[5], "d"));

		Automaton a = new Automaton(states, delta);
		SortedSet<String> expected = new TreeSet<>();
		expected.add("acd");
		expected.add("bd");

		assertEquals(expected, a.getLanguage());
	}

	@Test
	public void test04() throws CyclicAutomatonException {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[5];
		st[0] = new State(true, false);
		st[1] = new State(false, false);
		st[2] = new State(false, false);
		st[3] = new State(false, true);
		st[4] = new State(false, true);
		Collections.addAll(states, st);

		SortedSet<Transition> delta = new TreeSet<>();
		delta.add(new Transition(st[0], st[1], ""));
		delta.add(new Transition(st[0], st[2], ""));
		delta.add(new Transition(st[1], st[3], "a"));
		delta.add(new Transition(st[2], st[4], "b"));

		SortedSet<String> exp = new TreeSet<>();
		exp.add("a");
		exp.add("b");

		Automaton a = new Automaton(states, delta);

		assertEquals(exp, a.getLanguage());
	}

	@Test
	public void test05() throws CyclicAutomatonException {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[5];
		st[0] = new State(true, false);
		st[1] = new State(false, false);
		st[2] = new State(false, false);
		st[3] = new State(false, false);
		st[4] = new State(false, true);
		Collections.addAll(states, st);

		SortedSet<Transition> delta = new TreeSet<>();
		delta.add(new Transition(st[0], st[1], "a"));
		delta.add(new Transition(st[1], st[2], "b"));
		delta.add(new Transition(st[1], st[3], "a"));
		delta.add(new Transition(st[3], st[4], "c"));
		delta.add(new Transition(st[2], st[4], "c"));

		SortedSet<String> exp = new TreeSet<>();
		exp.add("abc");
		exp.add("aac");

		Automaton a = new Automaton(states, delta);
		assertEquals(a.getLanguage(), exp);
	}

	@Test
	public void test06() throws CyclicAutomatonException {
		SortedSet<State> states = new TreeSet<>();
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

		SortedSet<Transition> delta = new TreeSet<>();
		delta.add(new Transition(st[0], st[1], "a"));
		delta.add(new Transition(st[1], st[2], ""));
		delta.add(new Transition(st[2], st[3], "b"));
		delta.add(new Transition(st[2], st[4], "c"));
		delta.add(new Transition(st[3], st[5], ""));
		delta.add(new Transition(st[4], st[6], "d"));
		delta.add(new Transition(st[5], st[7], "c"));
		delta.add(new Transition(st[6], st[7], "c"));

		SortedSet<String> exp = new TreeSet<>();
		exp.add("abc");
		exp.add("acdc");

		Automaton a = new Automaton(states, delta);
		assertEquals(a.getLanguage(), exp);
	}

	@Test
	public void test07() throws CyclicAutomatonException {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[4];
		st[0] = new State(true, false);
		st[1] = new State(false, true);
		st[2] = new State(false, false);
		st[3] = new State(false, true);
		Collections.addAll(states, st);

		SortedSet<Transition> delta = new TreeSet<>();
		delta.add(new Transition(st[0], st[1], "a"));
		delta.add(new Transition(st[1], st[2], "b"));
		delta.add(new Transition(st[2], st[3], "c"));

		SortedSet<String> exp = new TreeSet<>();
		exp.add("a");
		exp.add("abc");

		Automaton a = new Automaton(states, delta);
		assertEquals(exp, a.getLanguage());
	}
}
