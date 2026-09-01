package it.unive.lisa.program.cfg.statement.literal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.testsupport.TestFixtures;
import it.unive.lisa.program.type.Int16Type;
import org.junit.jupiter.api.Test;

public class Int16LiteralTest {

	@Test
	public void reportsItsStaticTypeAndValue() {
		Int16Literal lit = new Int16Literal(TestFixtures.CFG, TestFixtures.LOCATION, (short) 500);
		assertEquals(Int16Type.INSTANCE, lit.getStaticType());
		assertEquals((short) 500, lit.getValue());
	}

	@Test
	public void equalsHoldsForSameValueAndLocation() {
		Int16Literal a = new Int16Literal(TestFixtures.CFG, TestFixtures.LOCATION, (short) 500);
		Int16Literal b = new Int16Literal(TestFixtures.CFG, TestFixtures.LOCATION, (short) 500);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void equalsFailsForDifferentValue() {
		Int16Literal a = new Int16Literal(TestFixtures.CFG, TestFixtures.LOCATION, (short) 500);
		Int16Literal b = new Int16Literal(TestFixtures.CFG, TestFixtures.LOCATION, (short) 600);
		assertFalse(a.equals(b));
	}

	@Test
	public void equalsFailsForDifferentLocation() {
		Int16Literal a = new Int16Literal(TestFixtures.CFG, TestFixtures.LOCATION, (short) 500);
		Int16Literal b = new Int16Literal(TestFixtures.CFG, new SourceCodeLocation("other", 2, 2), (short) 500);
		assertFalse(a.equals(b));
	}

}
