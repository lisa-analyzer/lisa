package it.unive.lisa.util.datastructures.regex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class TopAtomTest {

	@Test
	public void representsAnUnboundedUnknownSequence() {
		TopAtom top = TopAtom.INSTANCE;
		assertFalse(top.isEmpty());
		assertEquals(0, top.minLength());
		assertEquals(Integer.MAX_VALUE, top.maxLength());
	}

	@Test
	public void mayContainAnythingButOnlyDefinitelyContainsTheEmptyString() {
		TopAtom top = TopAtom.INSTANCE;
		assertTrue(top.mayContain("anything"));
		assertTrue(top.contains(""));
		assertFalse(top.contains("a"));

		assertTrue(top.mayStartWith("a"));
		assertTrue(top.startsWith(""));
		assertFalse(top.startsWith("a"));

		assertTrue(top.mayEndWith("a"));
		assertTrue(top.endsWith(""));
		assertFalse(top.endsWith("a"));
	}

	@Test
	public void isNeverEqualToAConcreteString() {
		assertFalse(TopAtom.INSTANCE.is("anything"));
		assertFalse(TopAtom.INSTANCE.is(""));
	}

	@Test
	public void isFixedUnderCaseAndReversal() {
		assertSame(TopAtom.INSTANCE, TopAtom.INSTANCE.reverse());
		assertSame(TopAtom.INSTANCE, TopAtom.INSTANCE.toLower());
		assertSame(TopAtom.INSTANCE, TopAtom.INSTANCE.toUpper());
	}

	@Test
	public void topAsSingleCharHasLengthExactlyOne() {
		RegularExpression singleChar = TopAtom.INSTANCE.topAsSingleChar();
		assertEquals(1, singleChar.minLength());
		assertEquals(1, singleChar.maxLength());
	}

	@Test
	public void topAsEmptyStringIsEpsilon() {
		assertEquals(Atom.EPSILON, TopAtom.INSTANCE.topAsEmptyString());
	}

}
