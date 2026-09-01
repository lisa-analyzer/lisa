package it.unive.lisa.program.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import org.junit.jupiter.api.Test;

public class StringTypeTest {

	@Test
	public void equalsHoldsForItselfAndAnyStringTypeImplementor() {
		assertEquals(StringType.INSTANCE, StringType.INSTANCE);
		assertEquals(StringType.INSTANCE.hashCode(), StringType.INSTANCE.hashCode());
	}

	@Test
	public void equalsFailsForNonStringTypes() {
		assertFalse(StringType.INSTANCE.equals(Int32Type.INSTANCE));
		assertFalse(StringType.INSTANCE.equals(BoolType.INSTANCE));
	}

	@Test
	public void canBeAssignedToStringTypesAndUntypedOnly() {
		assertTrue(StringType.INSTANCE.canBeAssignedTo(StringType.INSTANCE));
		assertTrue(StringType.INSTANCE.canBeAssignedTo(it.unive.lisa.type.Untyped.INSTANCE));
		assertFalse(StringType.INSTANCE.canBeAssignedTo(Int32Type.INSTANCE));
		assertFalse(StringType.INSTANCE.canBeAssignedTo(BoolType.INSTANCE));
	}

	@Test
	public void commonSupertypeIsItselfForStringTypesAndUntypedOtherwise() {
		assertEquals(StringType.INSTANCE, StringType.INSTANCE.commonSupertype(StringType.INSTANCE));
		assertEquals(it.unive.lisa.type.Untyped.INSTANCE, StringType.INSTANCE.commonSupertype(Int32Type.INSTANCE));
		assertEquals(it.unive.lisa.type.Untyped.INSTANCE, StringType.INSTANCE.commonSupertype(BoolType.INSTANCE));
	}

	@Test
	public void allInstancesIsJustItself() {
		assertEquals(Collections.singleton(StringType.INSTANCE), StringType.INSTANCE.allInstances(null));
	}

	@Test
	public void toStringIdentifiesTheType() {
		assertEquals("string", StringType.INSTANCE.toString());
	}

}
