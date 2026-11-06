package it.unive.lisa.program.cfg.statement.literal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.program.testsupport.TestFixtures;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.program.type.StringType;
import it.unive.lisa.type.TypeTokenType;
import org.junit.jupiter.api.Test;

public class TypeLiteralTest {

	@Test
	public void reportsItsValueAndAStaticTypeWrappingIt() {
		TypeLiteral lit = new TypeLiteral(TestFixtures.CFG, TestFixtures.LOCATION, Int32Type.INSTANCE);
		assertEquals(Int32Type.INSTANCE, lit.getValue());
		assertTrue(lit.getStaticType() instanceof TypeTokenType);
		assertEquals(java.util.Set.of(Int32Type.INSTANCE), ((TypeTokenType) lit.getStaticType()).getTypes());
	}

	@Test
	public void equalsHoldsForSameValueAndLocation() {
		TypeLiteral a = new TypeLiteral(TestFixtures.CFG, TestFixtures.LOCATION, Int32Type.INSTANCE);
		TypeLiteral b = new TypeLiteral(TestFixtures.CFG, TestFixtures.LOCATION, Int32Type.INSTANCE);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void equalsFailsForDifferentWrappedType() {
		TypeLiteral a = new TypeLiteral(TestFixtures.CFG, TestFixtures.LOCATION, Int32Type.INSTANCE);
		TypeLiteral b = new TypeLiteral(TestFixtures.CFG, TestFixtures.LOCATION, StringType.INSTANCE);
		assertFalse(a.equals(b));
	}

}
