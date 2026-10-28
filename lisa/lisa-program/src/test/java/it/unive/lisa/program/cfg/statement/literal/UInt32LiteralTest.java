package it.unive.lisa.program.cfg.statement.literal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.testsupport.TestFixtures;
import it.unive.lisa.program.type.UInt32Type;
import org.junit.jupiter.api.Test;

public class UInt32LiteralTest {

	@Test
	public void reportsItsStaticTypeAndValue() {
		UInt32Literal lit = new UInt32Literal(TestFixtures.CFG, TestFixtures.LOCATION, 42);
		assertEquals(UInt32Type.INSTANCE, lit.getStaticType());
		assertEquals(42, lit.getValue());
	}

	@Test
	public void equalsHoldsForSameValueAndLocation() {
		UInt32Literal a = new UInt32Literal(TestFixtures.CFG, TestFixtures.LOCATION, 42);
		UInt32Literal b = new UInt32Literal(TestFixtures.CFG, TestFixtures.LOCATION, 42);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void equalsFailsForDifferentValue() {
		UInt32Literal a = new UInt32Literal(TestFixtures.CFG, TestFixtures.LOCATION, 42);
		UInt32Literal b = new UInt32Literal(TestFixtures.CFG, TestFixtures.LOCATION, 7);
		assertFalse(a.equals(b));
	}

	@Test
	public void equalsFailsForDifferentLocation() {
		UInt32Literal a = new UInt32Literal(TestFixtures.CFG, TestFixtures.LOCATION, 42);
		UInt32Literal b = new UInt32Literal(TestFixtures.CFG, new SourceCodeLocation("other", 2, 2), 42);
		assertFalse(a.equals(b));
	}

}
