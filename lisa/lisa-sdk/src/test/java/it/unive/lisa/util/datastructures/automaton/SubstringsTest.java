package it.unive.lisa.util.datastructures.automaton;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class SubstringsTest {

	@Test
	public void prefixAcceptsExactlyThePrefixesOfTheLanguage() {
		TestAutomaton prefix = new TestAutomaton("ab").prefix();

		assertTrue(prefix.validateString(""));
		assertTrue(prefix.validateString("a"));
		assertTrue(prefix.validateString("ab"));

		assertFalse(prefix.validateString("b"));
		assertFalse(prefix.validateString("abc"));
	}

	@Test
	public void suffixAcceptsExactlyTheSuffixesOfTheLanguage() {
		TestAutomaton suffix = new TestAutomaton("ab").suffix();

		assertTrue(suffix.validateString(""));
		assertTrue(suffix.validateString("b"));
		assertTrue(suffix.validateString("ab"));

		assertFalse(suffix.validateString("a"));
		assertFalse(suffix.validateString("cab"));
	}

	@Test
	public void factorsAcceptsExactlyTheSubstringsOfTheLanguage() {
		TestAutomaton factors = new TestAutomaton("ab").factors();

		assertTrue(factors.validateString(""));
		assertTrue(factors.validateString("a"));
		assertTrue(factors.validateString("b"));
		assertTrue(factors.validateString("ab"));

		assertFalse(factors.validateString("ba"));
		assertFalse(factors.validateString("abc"));
	}

}
