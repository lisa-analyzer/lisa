package it.unive.lisa.program.cfg.statement.literal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.testsupport.TestFixtures;
import it.unive.lisa.program.type.Int64Type;
import org.junit.jupiter.api.Test;

public class Int64LiteralTest {

	@Test
	public void reportsItsStaticTypeAndValue() {
		Int64Literal lit = new Int64Literal(TestFixtures.CFG, TestFixtures.LOCATION, 42L);
		assertEquals(Int64Type.INSTANCE, lit.getStaticType());
		assertEquals(42L, lit.getValue());
	}

	@Test
	public void equalsHoldsForSameValueAndLocation() {
		Int64Literal a = new Int64Literal(TestFixtures.CFG, TestFixtures.LOCATION, 42L);
		Int64Literal b = new Int64Literal(TestFixtures.CFG, TestFixtures.LOCATION, 42L);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void equalsFailsForDifferentValue() {
		Int64Literal a = new Int64Literal(TestFixtures.CFG, TestFixtures.LOCATION, 42L);
		Int64Literal b = new Int64Literal(TestFixtures.CFG, TestFixtures.LOCATION, 7L);
		assertFalse(a.equals(b));
	}

	@Test
	public void equalsFailsForDifferentLocation() {
		Int64Literal a = new Int64Literal(TestFixtures.CFG, TestFixtures.LOCATION, 42L);
		Int64Literal b = new Int64Literal(TestFixtures.CFG, new SourceCodeLocation("other", 2, 2), 42L);
		assertFalse(a.equals(b));
	}

}
