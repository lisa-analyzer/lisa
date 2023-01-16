package it.unive.lisa.util.datastructures.automaton;

import static org.junit.Assert.assertEquals;

import it.unive.lisa.util.datastructures.regex.Atom;
import it.unive.lisa.util.datastructures.regex.RegularExpression;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.Test;

public class ToRegexTest {

	@Test
	public void test01() {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[1];
		st[0] = new State(0, true, true);
		Collections.addAll(states, st);

		SortedSet<Transition<TestSymbol>> delta = new TreeSet<>();
		delta.add(new Transition<>(st[0], st[0], new TestSymbol("a")));

		TestAutomaton a = new TestAutomaton(states, delta);
		// a*
		RegularExpression exp = new Atom("a").star();
		assertEquals(exp, a.toRegex());
	}

	@Test
	public void test02() {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[2];
		st[0] = new State(0, true, false);
		st[1] = new State(1, false, true);
		Collections.addAll(states, st);

		SortedSet<Transition<TestSymbol>> delta = new TreeSet<>();
		delta.add(new Transition<>(st[0], st[0], new TestSymbol("c")));
		delta.add(new Transition<>(st[0], st[1], new TestSymbol("a")));
		delta.add(new Transition<>(st[1], st[0], new TestSymbol("b")));
		delta.add(new Transition<>(st[1], st[1], new TestSymbol("d")));

		TestAutomaton a = new TestAutomaton(states, delta);
		// (ad*b+c)*ad*
		RegularExpression exp = new Atom("c").or(
				new Atom("a").comp(new Atom("d").star().comp(new Atom("b"))))
				.star()
				.comp(new Atom("a").comp(new Atom("d").star()));
		assertEquals(exp, a.toRegex());
	}

	@Test
	public void test03() {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[3];

		st[0] = new State(0, true, false);
		st[1] = new State(1, false, true);
		st[2] = new State(2, false, true);
		Collections.addAll(states, st);

		SortedSet<Transition<TestSymbol>> delta = new TreeSet<>();
		delta.add(new Transition<>(st[0], st[1], new TestSymbol("a")));
		delta.add(new Transition<>(st[0], st[2], new TestSymbol("c")));

		TestAutomaton a = new TestAutomaton(states, delta);
		// a + c
		RegularExpression exp = new Atom("a").or(new Atom("c"));
		assertEquals(exp, a.toRegex());
	}

	@Test
	public void test04() {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[3];
		st[0] = new State(0, true, false);
		st[1] = new State(1, false, false);
		st[2] = new State(2, false, true);
		Collections.addAll(states, st);

		SortedSet<Transition<TestSymbol>> delta = new TreeSet<>();
		delta.add(new Transition<>(st[0], st[1], new TestSymbol("a")));
		delta.add(new Transition<>(st[1], st[2], new TestSymbol("b")));

		TestAutomaton a = new TestAutomaton(states, delta);
		// ab
		RegularExpression exp = new Atom("a").comp(new Atom("b"));
		assertEquals(exp, a.toRegex());
	}

	@Test
	public void test05() {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[5];
		SortedSet<Transition<TestSymbol>> delta = new TreeSet<>();

		st[0] = new State(0, true, false);
		st[1] = new State(1, false, false);
		st[2] = new State(2, false, false);
		st[3] = new State(3, false, true);
		st[4] = new State(4, false, true);
		Collections.addAll(states, st);

		delta.add(new Transition<>(st[0], st[1], new TestSymbol("a")));
		delta.add(new Transition<>(st[0], st[2], new TestSymbol("c")));
		delta.add(new Transition<>(st[1], st[3], new TestSymbol("b")));
		delta.add(new Transition<>(st[2], st[4], new TestSymbol("c")));

		TestAutomaton a = new TestAutomaton(states, delta);
		// ab + cc
		RegularExpression exp = new Atom("a").comp(new Atom("b")).or(new Atom("c").comp(new Atom("c")));
		assertEquals(exp, a.toRegex());
	}

	@Test
	public void test06() {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[4];
		st[0] = new State(0, true, false);
		st[1] = new State(1, false, false);
		st[2] = new State(2, false, true);
		st[3] = new State(3, false, true);
		Collections.addAll(states, st);

		SortedSet<Transition<TestSymbol>> delta = new TreeSet<>();
		delta.add(new Transition<>(st[0], st[1], new TestSymbol("")));
		delta.add(new Transition<>(st[1], st[2], new TestSymbol("a")));
		delta.add(new Transition<>(st[1], st[3], new TestSymbol("b")));
		delta.add(new Transition<>(st[2], st[2], new TestSymbol("c")));
		delta.add(new Transition<>(st[3], st[3], new TestSymbol("c")));

		TestAutomaton a = new TestAutomaton(states, delta);
		// ac* + bc*
		RegularExpression exp = new Atom("a").comp(new Atom("c").star()).or(new Atom("b").comp(new Atom("c").star()));
		assertEquals(exp, a.toRegex());
	}

	@Test
	public void test07() {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[4];
		st[0] = new State(0, true, false);
		st[1] = new State(1, false, false);
		st[2] = new State(2, false, false);
		st[3] = new State(3, false, true);
		Collections.addAll(states, st);

		SortedSet<Transition<TestSymbol>> delta = new TreeSet<>();
		delta.add(new Transition<>(st[0], st[1], new TestSymbol("a")));
		delta.add(new Transition<>(st[0], st[2], new TestSymbol("a")));
		delta.add(new Transition<>(st[1], st[3], new TestSymbol("b")));
		delta.add(new Transition<>(st[2], st[3], new TestSymbol("c")));

		TestAutomaton a = new TestAutomaton(states, delta);
		// ab + ac
		RegularExpression exp = new Atom("a").comp(new Atom("b")).or(new Atom("a").comp(new Atom("c")));
		assertEquals(exp, a.toRegex());
	}

	@Test
	public void test08() {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[2];
		st[0] = new State(0, true, false);
		st[1] = new State(1, false, true);
		Collections.addAll(states, st);

		SortedSet<Transition<TestSymbol>> delta = new TreeSet<>();
		delta.add(new Transition<>(st[0], st[1], new TestSymbol("a")));

		TestAutomaton a = new TestAutomaton(states, delta);
		// a
		RegularExpression exp = new Atom("a");
		assertEquals(exp, a.toRegex());
	}

	@Test
	public void test09() {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[2];
		st[0] = new State(0, true, false);
		st[1] = new State(1, false, true);
		Collections.addAll(states, st);

		SortedSet<Transition<TestSymbol>> delta = new TreeSet<>();
		delta.add(new Transition<>(st[0], st[1], new TestSymbol("")));

		TestAutomaton a = new TestAutomaton(states, delta);
		// epsilon
		RegularExpression exp = Atom.EPSILON;
		assertEquals(exp, a.toRegex());
	}

	@Test
	public void test10() {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[2];
		st[0] = new State(0, true, false);
		st[1] = new State(1, false, true);
		Collections.addAll(states, st);

		SortedSet<Transition<TestSymbol>> delta = new TreeSet<>();
		delta.add(new Transition<>(st[0], st[1], new TestSymbol("a")));
		delta.add(new Transition<>(st[1], st[1], new TestSymbol("a")));
		delta.add(new Transition<>(st[1], st[1], new TestSymbol("b")));

		TestAutomaton a = new TestAutomaton(states, delta);
		// a(a+b)*
		RegularExpression exp = new Atom("a").comp(new Atom("a").or(new Atom("b")).star());
		assertEquals(exp, a.toRegex());
	}
}
