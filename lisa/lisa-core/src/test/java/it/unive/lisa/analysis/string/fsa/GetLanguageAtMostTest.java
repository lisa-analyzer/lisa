package it.unive.lisa.analysis.string.fsa;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class GetLanguageAtMostTest {

	// TODO: add more tests
	@Test
	public void tets01() {
		Set<String> expected = new HashSet<>();
		expected.add("");
		expected.add("a");
		expected.add("aa");
		expected.add("aaa");

		State q0 = new State(true, true);
		Set<State> sts = new HashSet<>();
		sts.add(q0);

		Transition t = new Transition(q0, q0, "a");
		Set<Transition> delta = new HashSet<>();
		delta.add(t);

		Automaton a = new Automaton(sts, delta);
		assertEquals(expected, a.getLanguageAtMost(3));
	}

	@Test
	public void test02() {
		Set<String> expected = new HashSet<>();
		expected.add("");
		expected.add("a");
		expected.add("ab");
		expected.add("abc");

		Set<State> states = new HashSet<>();
		Set<Transition> delta = new HashSet<>();
		State[] st = new State[5];
		st[0] = new State(true, false);
		st[1] = new State(false, false);
		st[2] = new State(false, false);
		st[3] = new State(false, false);
		st[4] = new State(false, true);
		Collections.addAll(states, st);

		delta.add(new Transition(st[0], st[1], "a"));
		delta.add(new Transition(st[1], st[2], "b"));
		delta.add(new Transition(st[2], st[3], "c"));
		delta.add(new Transition(st[3], st[4], "d"));

		Automaton a = new Automaton(states, delta);
		assertEquals(expected, a.getLanguageAtMost(3));
	}

	@Test
	public void test03() {
		Set<String> expected = new HashSet<>();
		expected.add("");
		expected.add("b");
		expected.add("bc");
		expected.add("bca");
		expected.add("c");
		expected.add("cb");
		expected.add("cbb");

		Set<State> states = new HashSet<>();
		Set<Transition> delta = new HashSet<>();
		State[] st = new State[5];
		st[0] = new State(true, false);
		st[1] = new State(false, false);
		st[2] = new State(false, false);
		st[3] = new State(false, true);
		st[4] = new State(false, true);
		Collections.addAll(states, st);

		delta.add(new Transition(st[0], st[1], "a"));
		delta.add(new Transition(st[1], st[2], "b"));
		delta.add(new Transition(st[1], st[3], "c"));
		delta.add(new Transition(st[2], st[4], "c"));
		delta.add(new Transition(st[3], st[3], "b"));
		delta.add(new Transition(st[4], st[4], "a"));

		Automaton a = new Automaton(states, delta);
		assertEquals(expected, a.getLanguageAtMost(st[1], 3));
	}

}
