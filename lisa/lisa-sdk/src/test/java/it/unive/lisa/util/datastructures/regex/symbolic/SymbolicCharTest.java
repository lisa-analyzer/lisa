package it.unive.lisa.util.datastructures.regex.symbolic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class SymbolicCharTest {

	@Test
	public void isMatchesOnlyTheSameConcreteCharacter() {
		SymbolicChar c = new SymbolicChar('a');
		assertTrue(c.is('a'));
		assertFalse(c.is('b'));
	}

	@Test
	public void equalsAndHashCodeAreValueBased() {
		assertEquals(new SymbolicChar('a'), new SymbolicChar('a'));
		assertEquals(new SymbolicChar('a').hashCode(), new SymbolicChar('a').hashCode());
		assertNotEquals(new SymbolicChar('a'), new SymbolicChar('b'));
	}

	@Test
	public void asCharAndToStringRoundtrip() {
		SymbolicChar c = new SymbolicChar('x');
		assertEquals('x', c.asChar());
		assertEquals("x", c.toString());
	}

	@Test
	public void unknownCharNeverMatchesAnyConcreteCharacter() {
		SymbolicChar unknown = UnknownSymbolicChar.INSTANCE;
		assertFalse(unknown.is('a'));
		assertFalse(unknown.is(unknown.asChar()));
	}

}
