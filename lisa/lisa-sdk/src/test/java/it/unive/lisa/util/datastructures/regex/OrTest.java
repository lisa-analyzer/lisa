package it.unive.lisa.util.datastructures.regex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.util.datastructures.automaton.TestAutomaton;
import org.junit.jupiter.api.Test;

public class OrTest {

	@Test
	public void constructorCanonicallyOrdersTheOperands() {
		// regardless of construction order, the smaller operand (by
		// RegularExpression#compareTo) always ends up first, making Or
		// deterministic / order-insensitive
		Or ab = new Or(new Atom("a"), new Atom("b"));
		Or ba = new Or(new Atom("b"), new Atom("a"));
		assertEquals(ab, ba);
		assertEquals(new Atom("a"), ab.getFirst());
		assertEquals(new Atom("b"), ab.getSecond());
	}

	@Test
	public void toStringWrapsInParenthesesWithAPlus() {
		assertEquals("(a + b)", new Or(new Atom("a"), new Atom("b")).toString());
	}

	@Test
	public void isAtomicOnlyWhenBothOperandsAreAtoms() {
		assertTrue(new Or(new Atom("a"), new Atom("b")).isAtomic());
		assertFalse(new Or(new Atom("a"), new Star(new Atom("b"))).isAtomic());
	}

	@Test
	public void isEmptyOnlyWhenBothOperandsAreEmpty() {
		assertTrue(new Or(Atom.EPSILON, Atom.EPSILON).isEmpty());
		assertFalse(new Or(Atom.EPSILON, new Atom("a")).isEmpty());
	}

	@Test
	public void lengthsAreMinAndMaxOfTheOperands() {
		Or or = new Or(new Atom("a"), new Atom("abc"));
		assertEquals(1, or.minLength());
		assertEquals(3, or.maxLength());
	}

	@Test
	public void simplifyCollapsesIdenticalOperands() {
		assertEquals(new Atom("a"), new Or(new Atom("a"), new Atom("a")).simplify());
	}

	@Test
	public void simplifyAbsorbsEmptySet() {
		assertEquals(new Atom("a"), new Or(new Atom("a"), EmptySet.INSTANCE).simplify());
		assertEquals(new Atom("a"), new Or(EmptySet.INSTANCE, new Atom("a")).simplify());
	}

	@Test
	public void simplifyMergesEpsilonWithStarOfTheSameOperand() {
		// epsilon + a* = a*
		Star aStar = new Star(new Atom("a"));
		assertEquals(aStar, new Or(Atom.EPSILON, aStar).simplify());
	}

	@Test
	public void mayContainIsTheDisjunctionOfTheOperands() {
		Or or = new Or(new Atom("ab"), new Atom("cd"));
		assertTrue(or.mayContain("ab"));
		assertTrue(or.mayContain("cd"));
		assertFalse(or.mayContain("ef"));
	}

	@Test
	public void containsIsTheConjunctionOfTheOperands() {
		// only strings guaranteed to be present in BOTH branches are
		// "definitely" contained
		Or or = new Or(new Atom("xax"), new Atom("yay"));
		assertTrue(or.contains("a"));
		assertFalse(or.contains("x"));
	}

	@Test
	public void toAutomatonAcceptsTheUnionOfTheLanguages() {
		Or or = new Or(new Atom("a"), new Atom("b"));
		TestAutomaton automaton = or.toAutomaton(new TestAutomaton(""));
		assertTrue(automaton.validateString("a"));
		assertTrue(automaton.validateString("b"));
		assertFalse(automaton.validateString("c"));
		assertFalse(automaton.validateString("ab"));
	}

	@Test
	public void repeatDistributesOverBothOperands() {
		Or or = new Or(new Atom("a"), new Atom("b"));
		TestAutomaton automaton = or.repeat(2).toAutomaton(new TestAutomaton(""));
		assertTrue(automaton.validateString("aa"));
		assertTrue(automaton.validateString("bb"));
		assertFalse(automaton.validateString("ab"));
	}

}
