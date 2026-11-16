package it.unive.lisa.util.datastructures.automaton;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

public class FactorsChangingInitialStateTest {

	@Test
	public void resultAcceptsFactorsStartingAtTheGivenState() {
		// a -> b -> c, made initial at the "b" state: the result must accept
		// "c" (the only factor starting from the new initial state)
		SortedSet<State> states = new TreeSet<>();
		State q0 = new State(0, true, false);
		State q1 = new State(1, false, false);
		State q2 = new State(2, false, true);
		Collections.addAll(states, q0, q1, q2);

		SortedSet<Transition<TestSymbol>> delta = new TreeSet<>();
		delta.add(new Transition<>(q0, q1, new TestSymbol("a")));
		delta.add(new Transition<>(q1, q2, new TestSymbol("b")));

		TestAutomaton a = new TestAutomaton(states, delta);

		// s is equal by value to q1, but is a distinct object instance: the
		// state must be identified by value (State#equals), not by
		// reference, since nothing in the documented contract requires
		// passing back one of this automaton's own State instances
		State s = new State(1, false, false);
		TestAutomaton factors = a.factorsChangingInitialState(s);

		assertTrue(factors.validateString("b"));
	}

}
