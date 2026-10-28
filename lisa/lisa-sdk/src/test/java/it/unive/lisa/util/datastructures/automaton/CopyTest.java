package it.unive.lisa.util.datastructures.automaton;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class CopyTest {

	@Test
	public void copyAcceptsTheSameLanguageAsTheOriginal() {
		TestAutomaton a = new TestAutomaton("ab");
		TestAutomaton copy = a.copy();

		assertTrue(copy.validateString("ab"));
		assertFalse(copy.validateString("a"));
		assertFalse(copy.validateString("abc"));
	}

	@Test
	public void copyIsIndependentFromTheOriginal() {
		TestAutomaton a = new TestAutomaton("ab");
		TestAutomaton copy = a.copy();

		// mutating the original after copying must not affect the copy:
		// extend the original so that it also accepts "abc"
		State last = a.getFinalStates().iterator().next();
		State extra = new State(100, false, true);
		a.addState(extra);
		a.addTransition(last, extra, new TestSymbol("c"));

		assertTrue(a.validateString("abc"));
		assertFalse(copy.validateString("abc"));
	}

}
