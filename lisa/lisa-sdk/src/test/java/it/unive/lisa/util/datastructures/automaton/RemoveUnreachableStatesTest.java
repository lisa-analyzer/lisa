package it.unive.lisa.util.datastructures.automaton;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

public class RemoveUnreachableStatesTest {

	@Test
	public void unreachableStatesAreDropped() {
		SortedSet<State> states = new TreeSet<>();
		State q0 = new State(0, true, true);
		State q1 = new State(1, false, false);
		Collections.addAll(states, q0, q1);

		// q1 is not reachable from q0
		TestAutomaton a = new TestAutomaton(states, new TreeSet<>());

		TestAutomaton cleaned = a.removeUnreachableStates();
		assertEquals(1, cleaned.getStates().size());
		assertFalse(cleaned.getStates().contains(q1));
		assertTrue(cleaned.getStates().contains(q0));
	}

	@Test
	public void reachableAutomatonIsUnchanged() {
		TestAutomaton a = new TestAutomaton("ab");
		TestAutomaton cleaned = a.removeUnreachableStates();
		assertEquals(a.getStates().size(), cleaned.getStates().size());
		assertTrue(cleaned.validateString("ab"));
	}

}
