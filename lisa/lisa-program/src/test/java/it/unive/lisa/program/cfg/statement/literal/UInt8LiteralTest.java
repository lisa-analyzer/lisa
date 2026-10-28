package it.unive.lisa.program.cfg.statement.literal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.testsupport.TestFixtures;
import it.unive.lisa.program.type.UInt8Type;
import org.junit.jupiter.api.Test;

public class UInt8LiteralTest {

	@Test
	public void reportsItsStaticTypeAndValue() {
		UInt8Literal lit = new UInt8Literal(TestFixtures.CFG, TestFixtures.LOCATION, (byte) 5);
		assertEquals(UInt8Type.INSTANCE, lit.getStaticType());
		assertEquals((byte) 5, lit.getValue());
	}

	@Test
	public void equalsHoldsForSameValueAndLocation() {
		UInt8Literal a = new UInt8Literal(TestFixtures.CFG, TestFixtures.LOCATION, (byte) 5);
		UInt8Literal b = new UInt8Literal(TestFixtures.CFG, TestFixtures.LOCATION, (byte) 5);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void equalsFailsForDifferentValue() {
		UInt8Literal a = new UInt8Literal(TestFixtures.CFG, TestFixtures.LOCATION, (byte) 5);
		UInt8Literal b = new UInt8Literal(TestFixtures.CFG, TestFixtures.LOCATION, (byte) 6);
		assertFalse(a.equals(b));
	}

	@Test
	public void equalsFailsForDifferentLocation() {
		UInt8Literal a = new UInt8Literal(TestFixtures.CFG, TestFixtures.LOCATION, (byte) 5);
		UInt8Literal b = new UInt8Literal(TestFixtures.CFG, new SourceCodeLocation("other", 2, 2), (byte) 5);
		assertFalse(a.equals(b));
	}

	@Test
	public void notEqualToItsSignedCounterpartWithTheSameValue() {
		UInt8Literal a = new UInt8Literal(TestFixtures.CFG, TestFixtures.LOCATION, (byte) 5);
		Int8Literal b = new Int8Literal(TestFixtures.CFG, TestFixtures.LOCATION, (byte) 5);
		assertFalse(a.equals(b));
	}

}
