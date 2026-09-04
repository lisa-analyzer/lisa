package it.unive.lisa.program.cfg.statement.literal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.testsupport.TestFixtures;
import it.unive.lisa.program.type.Int8Type;
import org.junit.jupiter.api.Test;

public class Int8LiteralTest {

	@Test
	public void reportsItsStaticTypeAndValue() {
		Int8Literal lit = new Int8Literal(TestFixtures.CFG, TestFixtures.LOCATION, (byte) 5);
		assertEquals(Int8Type.INSTANCE, lit.getStaticType());
		assertEquals((byte) 5, lit.getValue());
	}

	@Test
	public void equalsHoldsForSameValueAndLocation() {
		Int8Literal a = new Int8Literal(TestFixtures.CFG, TestFixtures.LOCATION, (byte) 5);
		Int8Literal b = new Int8Literal(TestFixtures.CFG, TestFixtures.LOCATION, (byte) 5);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void equalsFailsForDifferentValue() {
		Int8Literal a = new Int8Literal(TestFixtures.CFG, TestFixtures.LOCATION, (byte) 5);
		Int8Literal b = new Int8Literal(TestFixtures.CFG, TestFixtures.LOCATION, (byte) 6);
		assertFalse(a.equals(b));
	}

	@Test
	public void equalsFailsForDifferentLocation() {
		Int8Literal a = new Int8Literal(TestFixtures.CFG, TestFixtures.LOCATION, (byte) 5);
		Int8Literal b = new Int8Literal(TestFixtures.CFG, new SourceCodeLocation("other", 2, 2), (byte) 5);
		assertFalse(a.equals(b));
	}

}
