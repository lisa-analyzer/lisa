package it.unive.lisa.util.datastructures.automaton;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class CaseConversionTest {

	@Test
	public void lowerCaseAcceptsOnlyTheLowerCasedLanguage() {
		TestAutomaton a = new TestAutomaton("AB");
		TestAutomaton lower = a.lowerCase();

		assertTrue(lower.validateString("ab"));
		assertFalse(lower.validateString("AB"));
		assertFalse(lower.validateString("Ab"));
	}

	@Test
	public void upperCaseAcceptsOnlyTheUpperCasedLanguage() {
		TestAutomaton a = new TestAutomaton("ab");
		TestAutomaton upper = a.upperCase();

		assertTrue(upper.validateString("AB"));
		assertFalse(upper.validateString("ab"));
		assertFalse(upper.validateString("Ab"));
	}

}
