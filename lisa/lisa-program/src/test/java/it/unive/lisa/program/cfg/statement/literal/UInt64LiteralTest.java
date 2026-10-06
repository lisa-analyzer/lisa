package it.unive.lisa.program.cfg.statement.literal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.testsupport.TestFixtures;
import it.unive.lisa.program.type.UInt64Type;
import org.junit.jupiter.api.Test;

public class UInt64LiteralTest {

	@Test
	public void reportsItsStaticTypeAndValue() {
		UInt64Literal lit = new UInt64Literal(TestFixtures.CFG, TestFixtures.LOCATION, 42L);
		assertEquals(UInt64Type.INSTANCE, lit.getStaticType());
		assertEquals(42L, lit.getValue());
	}

	@Test
	public void equalsHoldsForSameValueAndLocation() {
		UInt64Literal a = new UInt64Literal(TestFixtures.CFG, TestFixtures.LOCATION, 42L);
		UInt64Literal b = new UInt64Literal(TestFixtures.CFG, TestFixtures.LOCATION, 42L);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void equalsFailsForDifferentValue() {
		UInt64Literal a = new UInt64Literal(TestFixtures.CFG, TestFixtures.LOCATION, 42L);
		UInt64Literal b = new UInt64Literal(TestFixtures.CFG, TestFixtures.LOCATION, 7L);
		assertFalse(a.equals(b));
	}

	@Test
	public void equalsFailsForDifferentLocation() {
		UInt64Literal a = new UInt64Literal(TestFixtures.CFG, TestFixtures.LOCATION, 42L);
		UInt64Literal b = new UInt64Literal(TestFixtures.CFG, new SourceCodeLocation("other", 2, 2), 42L);
		assertFalse(a.equals(b));
	}

}
