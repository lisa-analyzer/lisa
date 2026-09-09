package it.unive.lisa.program.cfg.statement.literal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.testsupport.TestFixtures;
import it.unive.lisa.program.type.UInt16Type;
import org.junit.jupiter.api.Test;

public class UInt16LiteralTest {

	@Test
	public void reportsItsStaticTypeAndValue() {
		UInt16Literal lit = new UInt16Literal(TestFixtures.CFG, TestFixtures.LOCATION, (short) 500);
		assertEquals(UInt16Type.INSTANCE, lit.getStaticType());
		assertEquals((short) 500, lit.getValue());
	}

	@Test
	public void equalsHoldsForSameValueAndLocation() {
		UInt16Literal a = new UInt16Literal(TestFixtures.CFG, TestFixtures.LOCATION, (short) 500);
		UInt16Literal b = new UInt16Literal(TestFixtures.CFG, TestFixtures.LOCATION, (short) 500);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void equalsFailsForDifferentValue() {
		UInt16Literal a = new UInt16Literal(TestFixtures.CFG, TestFixtures.LOCATION, (short) 500);
		UInt16Literal b = new UInt16Literal(TestFixtures.CFG, TestFixtures.LOCATION, (short) 600);
		assertFalse(a.equals(b));
	}

	@Test
	public void equalsFailsForDifferentLocation() {
		UInt16Literal a = new UInt16Literal(TestFixtures.CFG, TestFixtures.LOCATION, (short) 500);
		UInt16Literal b = new UInt16Literal(TestFixtures.CFG, new SourceCodeLocation("other", 2, 2), (short) 500);
		assertFalse(a.equals(b));
	}

}
