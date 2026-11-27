package it.unive.lisa.util.datastructures.regex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.util.datastructures.automaton.TestAutomaton;
import org.junit.jupiter.api.Test;

public class EmptySetTest {

	@Test
	public void recognizesNoString() {
		EmptySet e = EmptySet.INSTANCE;
		assertTrue(e.isEmpty());
		assertThrows(UnsupportedOperationException.class, () -> e.is("anything"));
		assertFalse(e.mayContain("a"));
		assertFalse(e.contains("a"));
		assertFalse(e.mayStartWith("a"));
		assertFalse(e.startsWith("a"));
		assertFalse(e.mayEndWith("a"));
		assertFalse(e.endsWith("a"));
	}

	@Test
	public void lengthsAreZero() {
		assertEquals(0, EmptySet.INSTANCE.maxLength());
		assertEquals(0, EmptySet.INSTANCE.minLength());
	}

	@Test
	public void isFixedUnderStructuralOperations() {
		EmptySet e = EmptySet.INSTANCE;
		assertSame(e, e.simplify());
		assertSame(e, e.reverse());
		assertSame(e, e.toLower());
		assertSame(e, e.toUpper());
		assertSame(e, e.trimLeft());
		assertSame(e, e.trimRight());
	}

	@Test
	public void repeatZeroTimesYieldsEpsilon() {
		// regression test: by definition L^0 = {epsilon} for every language
		// L, including the empty one - repeat(0) used to always return
		// EmptySet.INSTANCE regardless of n
		assertEquals(Atom.EPSILON, EmptySet.INSTANCE.repeat(0));
	}

	@Test
	public void repeatPositiveTimesStaysEmpty() {
		assertSame(EmptySet.INSTANCE, EmptySet.INSTANCE.repeat(1));
		assertSame(EmptySet.INSTANCE, EmptySet.INSTANCE.repeat(5));
	}

	@Test
	public void toAutomatonAcceptsNoStringAtAll() {
		TestAutomaton automaton = EmptySet.INSTANCE.toAutomaton(new TestAutomaton(""));
		assertTrue(automaton.acceptsEmptyLanguage());
	}

}
