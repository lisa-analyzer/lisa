package it.unive.lisa.program;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class SourceCodeLocationTest {

	@Test
	public void minusOneIsAcceptedAsTheUnknownLineAndColumnSentinel() {
		SourceCodeLocation loc = assertDoesNotThrow(() -> new SourceCodeLocation("f", -1, -1));
		assertEquals(-1, loc.getLine());
		assertEquals(-1, loc.getCol());
	}

	@Test
	public void nullIsAcceptedAsTheUnknownSourceFileSentinel() {
		SourceCodeLocation loc = assertDoesNotThrow(() -> new SourceCodeLocation(null, 1, 1));
		assertEquals("null", loc.getSourceFile());
	}

	@Test
	public void anyNegativeValueBelowMinusOneIsRejected() {
		assertThrows(IllegalArgumentException.class, () -> new SourceCodeLocation("f", -2, 1));
		assertThrows(IllegalArgumentException.class, () -> new SourceCodeLocation("f", 1, -2));
	}

	@Test
	public void windowsSeparatorsAreNormalizedToUnix() {
		SourceCodeLocation loc = new SourceCodeLocation("a\\b\\c.imp", 1, 1);
		assertEquals("a/b/c.imp", loc.getSourceFile());
	}

	@Test
	public void equalsAndHashCodeAreBasedOnAllThreeFields() {
		SourceCodeLocation a = new SourceCodeLocation("f", 1, 2);
		SourceCodeLocation b = new SourceCodeLocation("f", 1, 2);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertFalse(a.equals(new SourceCodeLocation("g", 1, 2)));
		assertFalse(a.equals(new SourceCodeLocation("f", 2, 2)));
		assertFalse(a.equals(new SourceCodeLocation("f", 1, 3)));
	}

	@Test
	public void compareToOrdersBySourceFileThenLineThenColumn() {
		SourceCodeLocation base = new SourceCodeLocation("b", 5, 5);
		assertTrue(new SourceCodeLocation("a", 5, 5).compareTo(base) < 0);
		assertTrue(new SourceCodeLocation("c", 5, 5).compareTo(base) > 0);
		assertTrue(new SourceCodeLocation("b", 4, 5).compareTo(base) < 0);
		assertTrue(new SourceCodeLocation("b", 5, 4).compareTo(base) < 0);
		assertEquals(0, base.compareTo(new SourceCodeLocation("b", 5, 5)));
	}

	@Test
	public void compareToTreatsNonSourceCodeLocationsAsGreater() {
		SourceCodeLocation loc = new SourceCodeLocation("f", 1, 1);
		assertTrue(loc.compareTo(SyntheticLocation.INSTANCE) < 0);
	}

	@Test
	public void toStringAndGetCodeLocationAreTheSameQuotedRepresentation() {
		SourceCodeLocation loc = new SourceCodeLocation("f", 1, 2);
		assertEquals("'f':1:2", loc.toString());
		assertEquals("'f':1:2", loc.getCodeLocation());
	}

}
