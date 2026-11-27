package it.unive.lisa.program.cfg.statement.literal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.testsupport.TestFixtures;
import it.unive.lisa.program.type.BoolType;
import org.junit.jupiter.api.Test;

public class TrueLiteralTest {

	@Test
	public void reportsItsStaticTypeAndValue() {
		TrueLiteral lit = new TrueLiteral(TestFixtures.CFG, TestFixtures.LOCATION);
		assertEquals(BoolType.INSTANCE, lit.getStaticType());
		assertEquals(Boolean.TRUE, lit.getValue());
	}

	@Test
	public void equalsHoldsForSameLocation() {
		TrueLiteral a = new TrueLiteral(TestFixtures.CFG, TestFixtures.LOCATION);
		TrueLiteral b = new TrueLiteral(TestFixtures.CFG, TestFixtures.LOCATION);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void equalsFailsForDifferentLocation() {
		TrueLiteral a = new TrueLiteral(TestFixtures.CFG, TestFixtures.LOCATION);
		TrueLiteral b = new TrueLiteral(TestFixtures.CFG, new SourceCodeLocation("other", 2, 2));
		assertFalse(a.equals(b));
	}

}
