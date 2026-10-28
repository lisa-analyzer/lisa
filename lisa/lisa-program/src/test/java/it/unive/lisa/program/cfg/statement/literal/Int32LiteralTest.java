package it.unive.lisa.program.cfg.statement.literal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.testsupport.TestFixtures;
import it.unive.lisa.program.type.Int32Type;
import org.junit.jupiter.api.Test;

public class Int32LiteralTest {

	@Test
	public void reportsItsStaticTypeAndValue() {
		Int32Literal lit = new Int32Literal(TestFixtures.CFG, TestFixtures.LOCATION, 42);
		assertEquals(Int32Type.INSTANCE, lit.getStaticType());
		assertEquals(42, lit.getValue());
	}

	@Test
	public void equalsHoldsForSameValueAndLocation() {
		Int32Literal a = new Int32Literal(TestFixtures.CFG, TestFixtures.LOCATION, 42);
		Int32Literal b = new Int32Literal(TestFixtures.CFG, TestFixtures.LOCATION, 42);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void equalsFailsForDifferentValue() {
		Int32Literal a = new Int32Literal(TestFixtures.CFG, TestFixtures.LOCATION, 42);
		Int32Literal b = new Int32Literal(TestFixtures.CFG, TestFixtures.LOCATION, 7);
		assertFalse(a.equals(b));
	}

	@Test
	public void equalsFailsForDifferentLocation() {
		Int32Literal a = new Int32Literal(TestFixtures.CFG, TestFixtures.LOCATION, 42);
		Int32Literal b = new Int32Literal(TestFixtures.CFG, new SourceCodeLocation("other", 2, 2), 42);
		assertFalse(a.equals(b));
	}

	@Test
	public void toStringReflectsTheValue() {
		Int32Literal lit = new Int32Literal(TestFixtures.CFG, TestFixtures.LOCATION, 42);
		assertTrue(lit.toString().contains("42"));
	}

	@Test
	public void notEqualToADifferentLiteralClassWithTheSameValue() {
		Int32Literal a = new Int32Literal(TestFixtures.CFG, TestFixtures.LOCATION, 42);
		Int64Literal b = new Int64Literal(TestFixtures.CFG, TestFixtures.LOCATION, 42L);
		assertNotEquals(a, b);
	}

}
