package it.unive.lisa.analysis.string.fsa;

import static org.junit.Assert.assertEquals;

import it.unive.lisa.analysis.SemanticDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.symbolic.value.operator.binary.StringContains;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.Test;

public class ContainsTest {

	@Test
	public void test01() throws SemanticException {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[4];
		st[0] = new State(true, false);
		st[1] = new State(false, false);
		st[2] = new State(false, false);
		st[3] = new State(false, true);
		Collections.addAll(states, st);

		SortedSet<Transition> delta = new TreeSet<>();
		delta.add(new Transition(st[0], st[1], "a"));
		delta.add(new Transition(st[0], st[2], "b"));
		delta.add(new Transition(st[1], st[3], "a"));
		delta.add(new Transition(st[2], st[3], "c"));

		Automaton a = new Automaton(states, delta);

		SortedSet<State> states2 = new TreeSet<>();
		State[] st2 = new State[2];
		st2[0] = new State(true, false);
		st2[1] = new State(false, true);
		Collections.addAll(states2, st2);

		SortedSet<Transition> delta2 = new TreeSet<>();
		delta2.add(new Transition(st2[0], st2[1], "a"));

		Automaton a2 = new Automaton(states2, delta2);

		FSA fsa = new FSA(a);
		FSA fsa1 = new FSA(a2);

		assertEquals(fsa.satisfiesBinaryExpression(StringContains.INSTANCE, fsa, fsa1, null),
				SemanticDomain.Satisfiability.SATISFIED);
	}

	@Test
	public void test02() throws SemanticException {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[4];
		st[0] = new State(true, false);
		st[1] = new State(false, false);
		st[2] = new State(false, false);
		st[3] = new State(false, true);
		Collections.addAll(states, st);

		SortedSet<Transition> delta = new TreeSet<>();
		delta.add(new Transition(st[0], st[1], "a"));
		delta.add(new Transition(st[0], st[2], "b"));
		delta.add(new Transition(st[1], st[3], "a"));
		delta.add(new Transition(st[2], st[3], "c"));

		Automaton a = new Automaton(states, delta);

		SortedSet<State> states2 = new TreeSet<>();
		State[] st2 = new State[2];
		st2[0] = new State(true, false);
		st2[1] = new State(false, true);
		Collections.addAll(states2, st2);

		SortedSet<Transition> delta2 = new TreeSet<>();
		delta2.add(new Transition(st2[0], st2[1], "d"));

		Automaton a2 = new Automaton(states2, delta2);

		FSA fsa = new FSA(a);
		FSA fsa1 = new FSA(a2);

		assertEquals(fsa.satisfiesBinaryExpression(StringContains.INSTANCE, fsa, fsa1, null),
				SemanticDomain.Satisfiability.NOT_SATISFIED);
	}

	@Test
	public void test03() throws SemanticException {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[4];
		st[0] = new State(true, false);
		st[1] = new State(false, false);
		st[2] = new State(false, false);
		st[3] = new State(false, true);
		Collections.addAll(states, st);

		SortedSet<Transition> delta = new TreeSet<>();
		delta.add(new Transition(st[0], st[1], "a"));
		delta.add(new Transition(st[0], st[2], "b"));
		delta.add(new Transition(st[1], st[3], "a"));
		delta.add(new Transition(st[2], st[3], "c"));

		Automaton a = new Automaton(states, delta);

		SortedSet<State> states2 = new TreeSet<>();
		State[] st2 = new State[2];
		st2[0] = new State(true, false);
		st2[1] = new State(false, true);
		Collections.addAll(states2, st2);

		SortedSet<Transition> delta2 = new TreeSet<>();
		delta2.add(new Transition(st2[0], st2[1], ""));

		Automaton a2 = new Automaton(states2, delta2);

		FSA fsa = new FSA(a);
		FSA fsa1 = new FSA(a2);

		assertEquals(fsa.satisfiesBinaryExpression(StringContains.INSTANCE, fsa, fsa1, null),
				SemanticDomain.Satisfiability.SATISFIED);
	}

	@Test
	public void test04() throws SemanticException {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[4];
		st[0] = new State(true, false);
		st[1] = new State(false, false);
		st[2] = new State(false, false);
		st[3] = new State(false, true);
		Collections.addAll(states, st);

		SortedSet<Transition> delta = new TreeSet<>();
		delta.add(new Transition(st[0], st[1], "a"));
		delta.add(new Transition(st[0], st[2], "b"));
		delta.add(new Transition(st[1], st[3], "a"));
		delta.add(new Transition(st[2], st[3], "c"));

		Automaton a = new Automaton(states, delta);

		SortedSet<State> states2 = new TreeSet<>();
		State[] st2 = new State[3];
		st2[0] = new State(true, false);
		st2[1] = new State(false, true);
		st2[2] = new State(false, true);
		Collections.addAll(states2, st2);

		SortedSet<Transition> delta2 = new TreeSet<>();
		delta2.add(new Transition(st2[0], st2[1], "a"));
		delta2.add(new Transition(st2[0], st2[2], "d"));

		Automaton a2 = new Automaton(states2, delta2);

		FSA fsa = new FSA(a);
		FSA fsa1 = new FSA(a2);

		assertEquals(fsa.satisfiesBinaryExpression(StringContains.INSTANCE, fsa, fsa1, null),
				SemanticDomain.Satisfiability.UNKNOWN);
	}

	@Test
	public void test05() throws SemanticException {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[4];
		st[0] = new State(true, false);
		st[1] = new State(false, false);
		st[2] = new State(false, false);
		st[3] = new State(false, true);
		Collections.addAll(states, st);

		SortedSet<Transition> delta = new TreeSet<>();
		delta.add(new Transition(st[0], st[1], "a"));
		delta.add(new Transition(st[0], st[2], "b"));
		delta.add(new Transition(st[1], st[3], "a"));
		delta.add(new Transition(st[2], st[3], "c"));

		Automaton a = new Automaton(states, delta);

		SortedSet<State> states2 = new TreeSet<>();
		State[] st2 = new State[1];
		st2[0] = new State(true, true);
		Collections.addAll(states2, st2);

		SortedSet<Transition> delta2 = new TreeSet<>();
		delta2.add(new Transition(st2[0], st2[0], "a"));

		Automaton a2 = new Automaton(states2, delta2);

		FSA fsa = new FSA(a);
		FSA fsa1 = new FSA(a2);

		assertEquals(fsa.satisfiesBinaryExpression(StringContains.INSTANCE, fsa, fsa1, null),
				SemanticDomain.Satisfiability.UNKNOWN);
	}

}
