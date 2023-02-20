package it.unive.lisa.analysis.string.fsa;

import static org.junit.Assert.assertEquals;

import it.unive.lisa.analysis.SemanticDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.symbolic.value.operator.binary.StringContains;
import it.unive.lisa.util.datastructures.automaton.State;
import it.unive.lisa.util.datastructures.automaton.Transition;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.Test;

public class ContainsTest {

	@Test
	public void test01() throws SemanticException {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[4];
		st[0] = new State(0, true, false);
		st[1] = new State(1, false, false);
		st[2] = new State(2, false, false);
		st[3] = new State(3, false, true);
		Collections.addAll(states, st);

		SortedSet<Transition<StringSymbol>> delta = new TreeSet<>();
		delta.add(new Transition<>(st[0], st[1], new StringSymbol("a")));
		delta.add(new Transition<>(st[0], st[2], new StringSymbol("b")));
		delta.add(new Transition<>(st[1], st[3], new StringSymbol("a")));
		delta.add(new Transition<>(st[2], st[3], new StringSymbol("c")));

		SimpleAutomaton a = new SimpleAutomaton(states, delta);

		SortedSet<State> states2 = new TreeSet<>();
		State[] st2 = new State[2];
		st2[0] = new State(4, true, false);
		st2[1] = new State(5, false, true);
		Collections.addAll(states2, st2);

		SortedSet<Transition<StringSymbol>> delta2 = new TreeSet<>();
		delta2.add(new Transition<>(st2[0], st2[1], new StringSymbol("a")));

		SimpleAutomaton a2 = new SimpleAutomaton(states2, delta2);

		FSA fsa = new FSA(a);
		FSA fsa1 = new FSA(a2);

		assertEquals(SemanticDomain.Satisfiability.UNKNOWN,
				fsa.satisfiesBinaryExpression(StringContains.INSTANCE, fsa, fsa1, null));
	}

	@Test
	public void test02() throws SemanticException {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[4];
		st[0] = new State(0, true, false);
		st[1] = new State(1, false, false);
		st[2] = new State(2, false, false);
		st[3] = new State(3, false, true);
		Collections.addAll(states, st);

		SortedSet<Transition<StringSymbol>> delta = new TreeSet<>();
		delta.add(new Transition<>(st[0], st[1], new StringSymbol("a")));
		delta.add(new Transition<>(st[0], st[2], new StringSymbol("b")));
		delta.add(new Transition<>(st[1], st[3], new StringSymbol("a")));
		delta.add(new Transition<>(st[2], st[3], new StringSymbol("c")));

		SimpleAutomaton a = new SimpleAutomaton(states, delta);

		SortedSet<State> states2 = new TreeSet<>();
		State[] st2 = new State[2];
		st2[0] = new State(4, true, false);
		st2[1] = new State(5, false, true);
		Collections.addAll(states2, st2);

		SortedSet<Transition<StringSymbol>> delta2 = new TreeSet<>();
		delta2.add(new Transition<>(st2[0], st2[1], new StringSymbol("d")));

		SimpleAutomaton a2 = new SimpleAutomaton(states2, delta2);

		FSA fsa = new FSA(a);
		FSA fsa1 = new FSA(a2);

		assertEquals(SemanticDomain.Satisfiability.NOT_SATISFIED,
				fsa.satisfiesBinaryExpression(StringContains.INSTANCE, fsa, fsa1, null));
	}

	@Test
	public void test03() throws SemanticException {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[4];
		st[0] = new State(0, true, false);
		st[1] = new State(1, false, false);
		st[2] = new State(2, false, false);
		st[3] = new State(3, false, true);
		Collections.addAll(states, st);

		SortedSet<Transition<StringSymbol>> delta = new TreeSet<>();
		delta.add(new Transition<>(st[0], st[1], new StringSymbol("a")));
		delta.add(new Transition<>(st[0], st[2], new StringSymbol("b")));
		delta.add(new Transition<>(st[1], st[3], new StringSymbol("a")));
		delta.add(new Transition<>(st[2], st[3], new StringSymbol("c")));

		SimpleAutomaton a = new SimpleAutomaton(states, delta);

		SortedSet<State> states2 = new TreeSet<>();
		State[] st2 = new State[2];
		st2[0] = new State(4, true, false);
		st2[1] = new State(5, false, true);
		Collections.addAll(states2, st2);

		SortedSet<Transition<StringSymbol>> delta2 = new TreeSet<>();
		delta2.add(new Transition<>(st2[0], st2[1], StringSymbol.EPSILON));

		SimpleAutomaton a2 = new SimpleAutomaton(states2, delta2);

		FSA fsa = new FSA(a);
		FSA fsa1 = new FSA(a2);

		assertEquals(SemanticDomain.Satisfiability.SATISFIED,
				fsa.satisfiesBinaryExpression(StringContains.INSTANCE, fsa, fsa1, null));
	}

	@Test
	public void test04() throws SemanticException {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[4];
		st[0] = new State(0, true, false);
		st[1] = new State(1, false, false);
		st[2] = new State(2, false, false);
		st[3] = new State(3, false, true);
		Collections.addAll(states, st);

		SortedSet<Transition<StringSymbol>> delta = new TreeSet<>();
		delta.add(new Transition<>(st[0], st[1], new StringSymbol("a")));
		delta.add(new Transition<>(st[0], st[2], new StringSymbol("b")));
		delta.add(new Transition<>(st[1], st[3], new StringSymbol("a")));
		delta.add(new Transition<>(st[2], st[3], new StringSymbol("c")));

		SimpleAutomaton a = new SimpleAutomaton(states, delta);

		SortedSet<State> states2 = new TreeSet<>();
		State[] st2 = new State[3];
		st2[0] = new State(4, true, false);
		st2[1] = new State(5, false, true);
		st2[2] = new State(6, false, true);
		Collections.addAll(states2, st2);

		SortedSet<Transition<StringSymbol>> delta2 = new TreeSet<>();
		delta2.add(new Transition<>(st2[0], st2[1], new StringSymbol("a")));
		delta2.add(new Transition<>(st2[0], st2[2], new StringSymbol("d")));

		SimpleAutomaton a2 = new SimpleAutomaton(states2, delta2);

		FSA fsa = new FSA(a);
		FSA fsa1 = new FSA(a2);

		assertEquals(SemanticDomain.Satisfiability.UNKNOWN,
				fsa.satisfiesBinaryExpression(StringContains.INSTANCE, fsa, fsa1, null));
	}

	@Test
	public void test05() throws SemanticException {
		SortedSet<State> states = new TreeSet<>();
		State[] st = new State[4];
		st[0] = new State(0, true, false);
		st[1] = new State(1, false, false);
		st[2] = new State(2, false, false);
		st[3] = new State(3, false, true);
		Collections.addAll(states, st);

		SortedSet<Transition<StringSymbol>> delta = new TreeSet<>();
		delta.add(new Transition<>(st[0], st[1], new StringSymbol("a")));
		delta.add(new Transition<>(st[0], st[2], new StringSymbol("b")));
		delta.add(new Transition<>(st[1], st[3], new StringSymbol("a")));
		delta.add(new Transition<>(st[2], st[3], new StringSymbol("c")));

		SimpleAutomaton a = new SimpleAutomaton(states, delta);

		SortedSet<State> states2 = new TreeSet<>();
		State[] st2 = new State[1];
		st2[0] = new State(4, true, true);
		Collections.addAll(states2, st2);

		SortedSet<Transition<StringSymbol>> delta2 = new TreeSet<>();
		delta2.add(new Transition<>(st2[0], st2[0], new StringSymbol("a")));

		SimpleAutomaton a2 = new SimpleAutomaton(states2, delta2);

		FSA fsa = new FSA(a);
		FSA fsa1 = new FSA(a2);

		assertEquals(SemanticDomain.Satisfiability.UNKNOWN,
				fsa.satisfiesBinaryExpression(StringContains.INSTANCE, fsa, fsa1, null));
	}

}
