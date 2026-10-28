package it.unive.lisa.analysis.symbols;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class NameSymbolTest {

	@Test
	public void getNameReturnsTheConstructorArgument() {
		assertEquals("x", new NameSymbol("x").getName());
	}

	@Test
	public void equalsIsReflexiveAndBasedOnTheName() {
		NameSymbol a1 = new NameSymbol("a");
		NameSymbol a2 = new NameSymbol("a");
		NameSymbol b = new NameSymbol("b");
		assertTrue(a1.equals(a1));
		assertEquals(a1, a2);
		assertEquals(a1.hashCode(), a2.hashCode());
		assertNotEquals(a1, b);
	}

	@Test
	public void equalsIsFalseAgainstNullAndOtherSymbolKinds() {
		NameSymbol a = new NameSymbol("a");
		assertFalse(a.equals(null));
		assertNotEquals(a, new QualifierSymbol("a"));
		assertNotEquals(a, new QualifiedNameSymbol("q", "a"));
	}

	@Test
	public void toStringMentionsTheName() {
		assertTrue(new NameSymbol("x").toString().contains("x"));
	}

}
