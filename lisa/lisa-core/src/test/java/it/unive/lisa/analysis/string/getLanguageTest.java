package it.unive.lisa.analysis.string;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;

public class getLanguageTest {

	@Test
	public void test01() {
		Set<State> st = new HashSet<>();
		Set<Transition> tr = new HashSet<>();
		State q0 = new State(true, false);
		State q1 = new State(false, false);
		State q2 = new State(false, true);
		st.add(q0);
		st.add(q1);
		st.add(q2);
		tr.add(new Transition(q0, q1, "a"));
		tr.add(new Transition(q1, q2, "b"));
		Automaton a = new Automaton(st, tr);
		Set<String> exp = new HashSet<>();
		exp.add("ab");
		assertEquals(exp, a.getLanguage());
	}

	@Test
	public void test02() {
		Set<State> states = new HashSet<>();
		State[] st = new State[2];
		st[0] = new State(true, false);
		st[1] = new State(false, true);

		states.add(st[0]);
		states.add(st[1]);

		Set<Transition> transitions = new HashSet<>();
		transitions.add(new Transition(st[0], st[0], "a"));
		transitions.add(new Transition(st[0], st[0], "b"));
		transitions.add(new Transition(st[0], st[1], "b"));

		// accepts language {a^nb^m}^p
		Automaton nfa = new Automaton(states, transitions);
		assertEquals(nfa.getLanguage(), new HashSet<String>());
	}
}
