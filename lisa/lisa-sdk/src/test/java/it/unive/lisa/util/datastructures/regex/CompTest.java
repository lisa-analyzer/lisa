package it.unive.lisa.util.datastructures.regex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.util.datastructures.automaton.TestAutomaton;
import org.junit.jupiter.api.Test;

public class CompTest {

	@Test
	public void toStringConcatenatesTheOperands() {
		assertEquals("ab", new Comp(new Atom("a"), new Atom("b")).toString());
	}

	@Test
	public void isEmptyOnlyWhenBothOperandsAreEmpty() {
		assertTrue(new Comp(Atom.EPSILON, Atom.EPSILON).isEmpty());
		assertFalse(new Comp(new Atom("a"), Atom.EPSILON).isEmpty());
		assertFalse(new Comp(Atom.EPSILON, new Atom("b")).isEmpty());
	}

	@Test
	public void lengthsAreTheSumOfTheOperands() {
		Comp c = new Comp(new Atom("ab"), new Atom("c"));
		assertEquals(3, c.maxLength());
		assertEquals(3, c.minLength());
	}

	@Test
	public void lengthOverflowsToInfinity() {
		Comp c = new Comp(new Atom("a"), new Star(new Atom("b")));
		assertEquals(Integer.MAX_VALUE, c.maxLength());
	}

	@Test
	public void simplifyRemovesEpsilonOperands() {
		assertEquals(new Atom("a"), new Comp(new Atom("a"), Atom.EPSILON).simplify());
		assertEquals(new Atom("a"), new Comp(Atom.EPSILON, new Atom("a")).simplify());
	}

	@Test
	public void simplifyAbsorbsEmptySet() {
		assertEquals(EmptySet.INSTANCE, new Comp(new Atom("a"), EmptySet.INSTANCE).simplify());
		assertEquals(EmptySet.INSTANCE, new Comp(EmptySet.INSTANCE, new Atom("a")).simplify());
	}

	@Test
	public void simplifyMergesEqualStars() {
		Star aStar = new Star(new Atom("a"));
		assertEquals(aStar, new Comp(aStar, aStar).simplify());
	}

	@Test
	public void toAutomatonAcceptsExactlyTheConcatenatedLanguage() {
		Comp c = new Comp(new Atom("a"), new Atom("b"));
		TestAutomaton automaton = c.toAutomaton(new TestAutomaton(""));
		assertTrue(automaton.validateString("ab"));
		assertFalse(automaton.validateString("a"));
		assertFalse(automaton.validateString("b"));
	}

	@Test
	public void reverseSwapsAndReversesTheOperands() {
		Comp c = new Comp(new Atom("ab"), new Atom("cd"));
		assertEquals(new Comp(new Atom("dc"), new Atom("ba")), c.reverse());
	}

	@Test
	public void repeatBuildsTheSimplifiedConcatenation() {
		Comp c = new Comp(new Atom("a"), new Atom("b"));
		assertEquals("abab", c.repeat(2).toString());
		assertEquals(Atom.EPSILON, c.repeat(0));
	}

}
