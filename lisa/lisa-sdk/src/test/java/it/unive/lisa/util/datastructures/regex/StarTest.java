package it.unive.lisa.util.datastructures.regex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.util.datastructures.automaton.TestAutomaton;
import org.junit.jupiter.api.Test;

public class StarTest {

	@Test
	public void toStringOmitsParenthesesForSingleCharAtomsAndOrs() {
		assertEquals("a*", new Star(new Atom("a")).toString());
		assertEquals("(a + b)*", new Star(new Or(new Atom("a"), new Atom("b"))).toString());
		assertEquals("(ab)*", new Star(new Atom("ab")).toString());
	}

	@Test
	public void lengthsAreUnboundedAboveAndZeroBelow() {
		Star s = new Star(new Atom("ab"));
		assertEquals(0, s.minLength());
		assertEquals(Integer.MAX_VALUE, s.maxLength());
	}

	@Test
	public void simplifyCollapsesEpsilonStarToEpsilon() {
		assertEquals(Atom.EPSILON, new Star(Atom.EPSILON).simplify());
	}

	@Test
	public void simplifyCollapsesEmptySetStarToEpsilon() {
		assertEquals(Atom.EPSILON, new Star(EmptySet.INSTANCE).simplify());
	}

	@Test
	public void simplifyCollapsesDoubleStarToASingleStar() {
		Star inner = new Star(new Atom("a"));
		assertEquals(inner, new Star(inner).simplify());
	}

	@Test
	public void repeatZeroTimesYieldsEpsilon() {
		// regression test: by definition L^0 = {epsilon}, but repeat(long)
		// used to always return "this" (the full starred language)
		// regardless of n
		assertEquals(Atom.EPSILON, new Star(new Atom("a")).repeat(0));
	}

	@Test
	public void repeatPositiveTimesIsStillTheSameStar() {
		// r*.r* = r*, so repeating a star any positive number of times
		// yields the same language
		Star s = new Star(new Atom("a"));
		assertSame(s, s.repeat(1));
		assertSame(s, s.repeat(4));
	}

	@Test
	public void toAutomatonAcceptsZeroOrMoreRepetitions() {
		TestAutomaton automaton = new Star(new Atom("ab")).toAutomaton(new TestAutomaton(""));
		assertTrue(automaton.validateString(""));
		assertTrue(automaton.validateString("ab"));
		assertTrue(automaton.validateString("abab"));
		assertFalse(automaton.validateString("a"));
	}

	@Test
	public void containsOnlyRecognizesTheEmptyString() {
		Star s = new Star(new Atom("ab"));
		assertTrue(s.contains(""));
		assertFalse(s.contains("ab"));
	}

	@Test
	public void mayContainDetectsRepeatedOccurrences() {
		Star s = new Star(new Atom("ab"));
		assertTrue(s.mayContain("ab"));
		assertTrue(s.mayContain("abab"));
		assertFalse(s.mayContain("ac"));
	}

}
