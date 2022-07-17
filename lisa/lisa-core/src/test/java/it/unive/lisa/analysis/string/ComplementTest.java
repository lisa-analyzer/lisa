package it.unive.lisa.analysis.string;

import org.apache.commons.collections4.set.TransformedNavigableSet;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class ComplementTest {

	@Test
	public void test01() {
		Set<State> states = new HashSet<>();
		State[] st = new State[3];
		st[0] = new State(true, false);
		st[1] = new State(false, false);
		st[2] = new State(false, true);
		Collections.addAll(states, st);

		Set<Transition> delta = new HashSet<>();
		delta.add(new Transition(st[0], st[1], "a"));
		delta.add(new Transition(st[1], st[2], "b"));

		Set<String> exp = new HashSet<>();
		exp.add("");
		exp.add("a");
		exp.add("aa");
		exp.add("b");
		exp.add("aba");
		exp.add("abb");

		Automaton a = new Automaton(states, delta);

		assertEquals(a.complement().getLanguage(), exp);
	}
}
