package it.unive.lisa.program.cfg.statement.literal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.testsupport.TestFixtures;
import it.unive.lisa.program.type.Float64Type;
import org.junit.jupiter.api.Test;

public class Float64LiteralTest {

	@Test
	public void reportsItsStaticTypeAndValue() {
		Float64Literal lit = new Float64Literal(TestFixtures.CFG, TestFixtures.LOCATION, 3.5d);
		assertEquals(Float64Type.INSTANCE, lit.getStaticType());
		assertEquals(3.5d, lit.getValue());
	}

	@Test
	public void equalsHoldsForSameValueAndLocation() {
		Float64Literal a = new Float64Literal(TestFixtures.CFG, TestFixtures.LOCATION, 3.5d);
		Float64Literal b = new Float64Literal(TestFixtures.CFG, TestFixtures.LOCATION, 3.5d);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void equalsFailsForDifferentValue() {
		Float64Literal a = new Float64Literal(TestFixtures.CFG, TestFixtures.LOCATION, 3.5d);
		Float64Literal b = new Float64Literal(TestFixtures.CFG, TestFixtures.LOCATION, 4.5d);
		assertFalse(a.equals(b));
	}

	@Test
	public void equalsFailsForDifferentLocation() {
		Float64Literal a = new Float64Literal(TestFixtures.CFG, TestFixtures.LOCATION, 3.5d);
		Float64Literal b = new Float64Literal(TestFixtures.CFG, new SourceCodeLocation("other", 2, 2), 3.5d);
		assertFalse(a.equals(b));
	}

}
