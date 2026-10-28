package it.unive.lisa.util.datastructures.regex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.util.datastructures.automaton.TestAutomaton;
import org.junit.jupiter.api.Test;

public class AtomTest {

	@Test
	public void equalsAndHashCodeAreValueBased() {
		assertEquals(new Atom("ab"), new Atom("ab"));
		assertEquals(new Atom("ab").hashCode(), new Atom("ab").hashCode());
		assertNotEquals(new Atom("ab"), new Atom("ac"));
	}

	@Test
	public void emptyAtomIsEpsilon() {
		assertTrue(new Atom("").isEmpty());
		assertTrue(new Atom("").isEpsilon());
		assertTrue(Atom.EPSILON.isEpsilon());
		assertFalse(new Atom("a").isEpsilon());
	}

	@Test
	public void isChecksForExactStringMatch() {
		assertTrue(new Atom("ab").is("ab"));
		assertFalse(new Atom("ab").is("a"));
		assertFalse(new Atom("ab").is("abc"));
	}

	@Test
	public void lengthsMatchTheUnderlyingString() {
		assertEquals(2, new Atom("ab").maxLength());
		assertEquals(2, new Atom("ab").minLength());
		assertEquals(0, Atom.EPSILON.maxLength());
	}

	@Test
	public void substringPredicatesMatchJavaStringSemantics() {
		Atom a = new Atom("hello");
		assertTrue(a.contains("ell"));
		assertTrue(a.mayContain("ell"));
		assertFalse(a.contains("xyz"));

		assertTrue(a.startsWith("he"));
		assertTrue(a.mayStartWith("he"));
		assertFalse(a.startsWith("el"));

		assertTrue(a.endsWith("lo"));
		assertTrue(a.mayEndWith("lo"));
		assertFalse(a.endsWith("el"));
	}

	@Test
	public void reverseFlipsTheString() {
		assertEquals(new Atom("cba"), new Atom("abc").reverse());
	}

	@Test
	public void caseConversion() {
		assertEquals(new Atom("ab"), new Atom("AB").toLower());
		assertEquals(new Atom("AB"), new Atom("ab").toUpper());
	}

	@Test
	public void trimming() {
		assertEquals(new Atom("ab  "), new Atom("  ab  ").trimLeft());
		assertEquals(new Atom("  ab"), new Atom("  ab  ").trimRight());
		assertEquals(Atom.EPSILON, new Atom("   ").trimLeft());
		assertEquals(Atom.EPSILON, new Atom("   ").trimRight());
	}

	@Test
	public void simplifyIsANoOp() {
		Atom a = new Atom("ab");
		assertSame(a, a.simplify());
	}

	@Test
	public void repeatBuildsTheSimplifiedConcatenation() {
		// this is a regression test: repeat(n) used to discard the result of
		// simplify(), returning the raw nested Comp chain (with a leftover,
		// unsimplified epsilon operand from the accumulator's seed value)
		// instead of the simplified structure with the epsilon eliminated
		RegularExpression repeated = new Atom("a").repeat(3);
		assertEquals("aaa", repeated.toString());
	}

	@Test
	public void repeatZeroTimesYieldsEpsilon() {
		assertEquals(Atom.EPSILON, new Atom("a").repeat(0));
	}

	@Test
	public void explodeSplitsIntoSingleCharacterAtoms() {
		RegularExpression[] exploded = new Atom("ab").explode();
		assertEquals(2, exploded.length);
		assertEquals(new Atom("a"), exploded[0]);
		assertEquals(new Atom("b"), exploded[1]);
	}

	@Test
	public void toAutomatonAcceptsExactlyTheAtomString() {
		TestAutomaton automaton = new Atom("ab").toAutomaton(new TestAutomaton(""));
		assertTrue(automaton.validateString("ab"));
		assertFalse(automaton.validateString("a"));
		assertFalse(automaton.validateString(""));
	}

	@Test
	public void epsilonAtomToAutomatonAcceptsOnlyTheEmptyString() {
		TestAutomaton automaton = Atom.EPSILON.toAutomaton(new TestAutomaton(""));
		assertTrue(automaton.validateString(""));
		assertFalse(automaton.validateString("a"));
	}

}
