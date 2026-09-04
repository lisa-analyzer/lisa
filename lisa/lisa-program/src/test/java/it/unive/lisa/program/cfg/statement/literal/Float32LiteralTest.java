package it.unive.lisa.program.cfg.statement.literal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.testsupport.TestFixtures;
import it.unive.lisa.program.type.Float32Type;
import org.junit.jupiter.api.Test;

public class Float32LiteralTest {

	@Test
	public void reportsItsStaticTypeAndValue() {
		Float32Literal lit = new Float32Literal(TestFixtures.CFG, TestFixtures.LOCATION, 3.5f);
		assertEquals(Float32Type.INSTANCE, lit.getStaticType());
		assertEquals(3.5f, lit.getValue());
	}

	@Test
	public void equalsHoldsForSameValueAndLocation() {
		Float32Literal a = new Float32Literal(TestFixtures.CFG, TestFixtures.LOCATION, 3.5f);
		Float32Literal b = new Float32Literal(TestFixtures.CFG, TestFixtures.LOCATION, 3.5f);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void equalsFailsForDifferentValue() {
		Float32Literal a = new Float32Literal(TestFixtures.CFG, TestFixtures.LOCATION, 3.5f);
		Float32Literal b = new Float32Literal(TestFixtures.CFG, TestFixtures.LOCATION, 4.5f);
		assertFalse(a.equals(b));
	}

	@Test
	public void equalsFailsForDifferentLocation() {
		Float32Literal a = new Float32Literal(TestFixtures.CFG, TestFixtures.LOCATION, 3.5f);
		Float32Literal b = new Float32Literal(TestFixtures.CFG, new SourceCodeLocation("other", 2, 2), 3.5f);
		assertFalse(a.equals(b));
	}

}
