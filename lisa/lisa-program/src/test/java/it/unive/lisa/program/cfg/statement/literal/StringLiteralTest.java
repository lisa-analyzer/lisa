package it.unive.lisa.program.cfg.statement.literal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.testsupport.TestFixtures;
import it.unive.lisa.program.type.StringType;
import org.junit.jupiter.api.Test;

public class StringLiteralTest {

	@Test
	public void reportsItsStaticTypeAndValue() {
		StringLiteral lit = new StringLiteral(TestFixtures.CFG, TestFixtures.LOCATION, "hello");
		assertEquals(StringType.INSTANCE, lit.getStaticType());
		assertEquals("hello", lit.getValue());
	}

	@Test
	public void equalsHoldsForSameValueAndLocation() {
		StringLiteral a = new StringLiteral(TestFixtures.CFG, TestFixtures.LOCATION, "hello");
		StringLiteral b = new StringLiteral(TestFixtures.CFG, TestFixtures.LOCATION, "hello");
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void equalsFailsForDifferentValue() {
		StringLiteral a = new StringLiteral(TestFixtures.CFG, TestFixtures.LOCATION, "hello");
		StringLiteral b = new StringLiteral(TestFixtures.CFG, TestFixtures.LOCATION, "world");
		assertFalse(a.equals(b));
	}

	@Test
	public void equalsFailsForDifferentLocation() {
		StringLiteral a = new StringLiteral(TestFixtures.CFG, TestFixtures.LOCATION, "hello");
		StringLiteral b = new StringLiteral(TestFixtures.CFG, new SourceCodeLocation("other", 2, 2), "hello");
		assertFalse(a.equals(b));
	}

	@Test
	public void toStringWrapsTheValueInQuotes() {
		StringLiteral lit = new StringLiteral(TestFixtures.CFG, TestFixtures.LOCATION, "hello");
		assertEquals("\"hello\"", lit.toString());
	}

}
