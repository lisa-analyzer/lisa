package it.unive.lisa.analysis.string.fsa;

import static org.junit.Assert.assertEquals;

import it.unive.lisa.TestParameterProvider;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.program.type.StringType;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.StringContains;
import it.unive.lisa.util.datastructures.automaton.State;
import it.unive.lisa.util.datastructures.automaton.Transition;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.Test;

public class ContainsTest {

	private final SemanticOracle oracle = TestParameterProvider.provideParam(null, SemanticOracle.class);

	private final BinaryExpression expr = new BinaryExpression(
			BoolType.INSTANCE,
			new Variable(StringType.INSTANCE, "x", SyntheticLocation.INSTANCE),
			new Variable(StringType.INSTANCE, "y", SyntheticLocation.INSTANCE),
			StringContains.INSTANCE,
			SyntheticLocation.INSTANCE);

	@Test
	public void test01()
			throws SemanticException {
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

		FSA domain = new FSA();

		assertEquals(Satisfiability.UNKNOWN, domain.satisfiesBinaryExpression(expr, a, a2, null, oracle));
	}

	@Test
	public void test02()
			throws SemanticException {
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

		FSA domain = new FSA();

		assertEquals(Satisfiability.NOT_SATISFIED, domain.satisfiesBinaryExpression(expr, a, a2, null, oracle));
	}

	@Test
	public void test03()
			throws SemanticException {
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

		FSA domain = new FSA();

		assertEquals(Satisfiability.SATISFIED, domain.satisfiesBinaryExpression(expr, a, a2, null, oracle));
	}

	@Test
	public void test04()
			throws SemanticException {
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

		FSA domain = new FSA();

		assertEquals(Satisfiability.UNKNOWN, domain.satisfiesBinaryExpression(expr, a, a2, null, oracle));
	}

	@Test
	public void test05()
			throws SemanticException {
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

		FSA domain = new FSA();

		assertEquals(Satisfiability.UNKNOWN, domain.satisfiesBinaryExpression(expr, a, a2, null, oracle));
	}

}
