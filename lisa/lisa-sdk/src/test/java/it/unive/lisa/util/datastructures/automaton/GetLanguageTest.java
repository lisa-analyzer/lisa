package it.unive.lisa.util.datastructures.automaton;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.Test;

public class GetLanguageTest {

	@Test
	public void test01()
			throws CyclicAutomatonException {
		SortedSet<State> states = new TreeSet<>();
		SortedSet<Transition<TestSymbol>> tr = new TreeSet<>();
		State[] st = new State[3];
		st[0] = new State(0, true, false);
		st[1] = new State(1, false, false);
		st[2] = new State(2, false, true);
		Collections.addAll(states, st);

		tr.add(new Transition<>(st[0], st[1], new TestSymbol("a")));
		tr.add(new Transition<>(st[1], st[2], new TestSymbol("b")));
		TestAutomaton a = new TestAutomaton(states, tr);
		SortedSet<String> exp = new TreeSet<>();
		exp.add("ab");
		assertEquals(exp, a.getLanguage());
	}

	@Test(expected = CyclicAutomatonException.class)
	public void test02()
			throws CyclicAutomatonException {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[2];
		st[0] = new State(0, true, false);
		st[1] = new State(1, false, true);
		Collections.addAll(states, st);

		SortedSet<Transition<TestSymbol>> transitions = new TreeSet<>();
		transitions.add(new Transition<>(st[0], st[0], new TestSymbol("a")));
		transitions.add(new Transition<>(st[0], st[0], new TestSymbol("b")));
		transitions.add(new Transition<>(st[0], st[1], new TestSymbol("b")));

		// accepts language {a^nb^m}^p
		TestAutomaton nfa = new TestAutomaton(states, transitions);
		assertEquals(nfa.getLanguage(), new TreeSet<String>());
	}

	@Test
	public void test03()
			throws CyclicAutomatonException {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[6];
		st[0] = new State(0, true, false);
		st[1] = new State(1, false, false);
		st[2] = new State(2, false, false);
		st[3] = new State(3, false, false);
		st[4] = new State(4, false, true);
		st[5] = new State(5, false, true);
		Collections.addAll(states, st);

		SortedSet<Transition<TestSymbol>> delta = new TreeSet<>();
		delta.add(new Transition<>(st[0], st[1], new TestSymbol("a")));
		delta.add(new Transition<>(st[0], st[2], new TestSymbol("b")));
		delta.add(new Transition<>(st[1], st[3], new TestSymbol("c")));
		delta.add(new Transition<>(st[2], st[4], new TestSymbol("d")));
		delta.add(new Transition<>(st[3], st[5], new TestSymbol("d")));

		TestAutomaton a = new TestAutomaton(states, delta);
		SortedSet<String> expected = new TreeSet<>();
		expected.add("acd");
		expected.add("bd");

		assertEquals(expected, a.getLanguage());
	}

	@Test
	public void test04()
			throws CyclicAutomatonException {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[5];
		st[0] = new State(0, true, false);
		st[1] = new State(1, false, false);
		st[2] = new State(2, false, false);
		st[3] = new State(3, false, true);
		st[4] = new State(4, false, true);
		Collections.addAll(states, st);

		SortedSet<Transition<TestSymbol>> delta = new TreeSet<>();
		delta.add(new Transition<>(st[0], st[1], new TestSymbol("")));
		delta.add(new Transition<>(st[0], st[2], new TestSymbol("")));
		delta.add(new Transition<>(st[1], st[3], new TestSymbol("a")));
		delta.add(new Transition<>(st[2], st[4], new TestSymbol("b")));

		SortedSet<String> exp = new TreeSet<>();
		exp.add("a");
		exp.add("b");

		TestAutomaton a = new TestAutomaton(states, delta);

		assertEquals(exp, a.getLanguage());
	}

	@Test
	public void test05()
			throws CyclicAutomatonException {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[5];
		st[0] = new State(0, true, false);
		st[1] = new State(1, false, false);
		st[2] = new State(2, false, false);
		st[3] = new State(3, false, false);
		st[4] = new State(4, false, true);
		Collections.addAll(states, st);

		SortedSet<Transition<TestSymbol>> delta = new TreeSet<>();
		delta.add(new Transition<>(st[0], st[1], new TestSymbol("a")));
		delta.add(new Transition<>(st[1], st[2], new TestSymbol("b")));
		delta.add(new Transition<>(st[1], st[3], new TestSymbol("a")));
		delta.add(new Transition<>(st[3], st[4], new TestSymbol("c")));
		delta.add(new Transition<>(st[2], st[4], new TestSymbol("c")));

		SortedSet<String> exp = new TreeSet<>();
		exp.add("abc");
		exp.add("aac");

		TestAutomaton a = new TestAutomaton(states, delta);
		assertEquals(a.getLanguage(), exp);
	}

	@Test
	public void test06()
			throws CyclicAutomatonException {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[8];
		st[0] = new State(0, true, false);
		st[1] = new State(1, false, false);
		st[2] = new State(2, false, false);
		st[3] = new State(3, false, false);
		st[4] = new State(4, false, false);
		st[5] = new State(5, false, false);
		st[6] = new State(6, false, false);
		st[7] = new State(7, false, true);
		Collections.addAll(states, st);

		SortedSet<Transition<TestSymbol>> delta = new TreeSet<>();
		delta.add(new Transition<>(st[0], st[1], new TestSymbol("a")));
		delta.add(new Transition<>(st[1], st[2], new TestSymbol("")));
		delta.add(new Transition<>(st[2], st[3], new TestSymbol("b")));
		delta.add(new Transition<>(st[2], st[4], new TestSymbol("c")));
		delta.add(new Transition<>(st[3], st[5], new TestSymbol("")));
		delta.add(new Transition<>(st[4], st[6], new TestSymbol("d")));
		delta.add(new Transition<>(st[5], st[7], new TestSymbol("c")));
		delta.add(new Transition<>(st[6], st[7], new TestSymbol("c")));

		SortedSet<String> exp = new TreeSet<>();
		exp.add("abc");
		exp.add("acdc");

		TestAutomaton a = new TestAutomaton(states, delta);
		assertEquals(a.getLanguage(), exp);
	}

	@Test
	public void test07()
			throws CyclicAutomatonException {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[4];
		st[0] = new State(0, true, false);
		st[1] = new State(1, false, true);
		st[2] = new State(2, false, false);
		st[3] = new State(3, false, true);
		Collections.addAll(states, st);

		SortedSet<Transition<TestSymbol>> delta = new TreeSet<>();
		delta.add(new Transition<>(st[0], st[1], new TestSymbol("a")));
		delta.add(new Transition<>(st[1], st[2], new TestSymbol("b")));
		delta.add(new Transition<>(st[2], st[3], new TestSymbol("c")));

		SortedSet<String> exp = new TreeSet<>();
		exp.add("a");
		exp.add("abc");

		TestAutomaton a = new TestAutomaton(states, delta);
		assertEquals(exp, a.getLanguage());
	}

}
