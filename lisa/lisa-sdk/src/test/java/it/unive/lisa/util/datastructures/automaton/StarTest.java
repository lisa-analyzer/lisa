package it.unive.lisa.util.datastructures.automaton;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class StarTest {

	@Test
	public void starAcceptsAnyNumberOfRepetitionsIncludingZero() {
		TestAutomaton ab = new TestAutomaton("ab");
		TestAutomaton starred = ab.star();

		assertTrue(starred.validateString(""));
		assertTrue(starred.validateString("ab"));
		assertTrue(starred.validateString("abab"));
		assertTrue(starred.validateString("ababab"));

		assertFalse(starred.validateString("a"));
		assertFalse(starred.validateString("aba"));
		assertFalse(starred.validateString("abb"));
	}

}
