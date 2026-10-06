package it.unive.lisa.program.cfg.statement.literal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.testsupport.TestFixtures;
import it.unive.lisa.program.type.BoolType;
import org.junit.jupiter.api.Test;

public class FalseLiteralTest {

	@Test
	public void reportsItsStaticTypeAndValue() {
		FalseLiteral lit = new FalseLiteral(TestFixtures.CFG, TestFixtures.LOCATION);
		assertEquals(BoolType.INSTANCE, lit.getStaticType());
		assertEquals(Boolean.FALSE, lit.getValue());
	}

	@Test
	public void equalsHoldsForSameLocation() {
		FalseLiteral a = new FalseLiteral(TestFixtures.CFG, TestFixtures.LOCATION);
		FalseLiteral b = new FalseLiteral(TestFixtures.CFG, TestFixtures.LOCATION);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void equalsFailsForDifferentLocation() {
		FalseLiteral a = new FalseLiteral(TestFixtures.CFG, TestFixtures.LOCATION);
		FalseLiteral b = new FalseLiteral(TestFixtures.CFG, new SourceCodeLocation("other", 2, 2));
		assertFalse(a.equals(b));
	}

	@Test
	public void notEqualToTrueLiteralAtTheSameLocation() {
		FalseLiteral a = new FalseLiteral(TestFixtures.CFG, TestFixtures.LOCATION);
		TrueLiteral b = new TrueLiteral(TestFixtures.CFG, TestFixtures.LOCATION);
		assertNotEquals(a, b);
	}

}
