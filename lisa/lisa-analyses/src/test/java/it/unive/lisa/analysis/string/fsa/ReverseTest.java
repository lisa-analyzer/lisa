package it.unive.lisa.analysis.string.fsa;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.Test;

public class ReverseTest {

	@Test
	public void test01() throws CyclicAutomatonException {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[4];
		st[0] = new State(true, false);
		st[1] = new State(false, false);
		st[2] = new State(false, false);
		st[3] = new State(false, true);
		Collections.addAll(states, st);

		SortedSet<Transition> delta = new TreeSet<>();
		delta.add(new Transition(st[0], st[1], "a"));
		delta.add(new Transition(st[1], st[2], "b"));
		delta.add(new Transition(st[2], st[3], "c"));

		SortedSet<String> exp = new TreeSet<>();
		exp.add("cba");
		Automaton a = new Automaton(states, delta);

		assertEquals(exp, a.reverse().getLanguage());
	}

	@Test
	public void test02() throws CyclicAutomatonException {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[5];
		st[0] = new State(true, false);
		st[1] = new State(false, false);
		st[2] = new State(false, false);
		st[3] = new State(false, true);
		st[4] = new State(false, true);
		Collections.addAll(states, st);

		SortedSet<Transition> delta = new TreeSet<>();
		delta.add(new Transition(st[0], st[1], "a"));
		delta.add(new Transition(st[0], st[2], "b"));
		delta.add(new Transition(st[1], st[3], "c"));
		delta.add(new Transition(st[2], st[4], "b"));

		SortedSet<String> exp = new TreeSet<>();
		exp.add("ca");
		exp.add("bb");
		Automaton a = new Automaton(states, delta);

		assertEquals(exp, a.reverse().getLanguage());
	}

	@Test
	public void test03() throws CyclicAutomatonException {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[7];
		st[0] = new State(true, false);
		st[1] = new State(true, false);
		st[2] = new State(false, false);
		st[3] = new State(false, false);
		st[4] = new State(false, false);
		st[5] = new State(false, true);
		st[6] = new State(false, true);
		Collections.addAll(states, st);

		SortedSet<Transition> delta = new TreeSet<>();
		delta.add(new Transition(st[0], st[2], "a"));
		delta.add(new Transition(st[1], st[3], "c"));
		delta.add(new Transition(st[2], st[4], ""));
		delta.add(new Transition(st[3], st[4], "b"));
		delta.add(new Transition(st[4], st[5], ""));
		delta.add(new Transition(st[4], st[6], "b"));

		SortedSet<String> exp = new TreeSet<>();
		exp.add("a");
		exp.add("bc");
		exp.add("ba");
		exp.add("bbc");
		Automaton a = new Automaton(states, delta);

		assertEquals(a.reverse().getLanguage(), exp);
	}
}
