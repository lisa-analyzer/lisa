package it.unive.lisa.program.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import org.junit.jupiter.api.Test;

public class BoolTypeTest {

	@Test
	public void equalsHoldsForItselfAndAnyBooleanTypeImplementor() {
		assertEquals(BoolType.INSTANCE, BoolType.INSTANCE);
		assertEquals(BoolType.INSTANCE.hashCode(), BoolType.INSTANCE.hashCode());
	}

	@Test
	public void equalsFailsForNonBooleanTypes() {
		assertFalse(BoolType.INSTANCE.equals(Int32Type.INSTANCE));
		assertFalse(BoolType.INSTANCE.equals(StringType.INSTANCE));
	}

	@Test
	public void canBeAssignedToBooleanTypesAndUntypedOnly() {
		assertTrue(BoolType.INSTANCE.canBeAssignedTo(BoolType.INSTANCE));
		assertTrue(BoolType.INSTANCE.canBeAssignedTo(it.unive.lisa.type.Untyped.INSTANCE));
		assertFalse(BoolType.INSTANCE.canBeAssignedTo(Int32Type.INSTANCE));
		assertFalse(BoolType.INSTANCE.canBeAssignedTo(StringType.INSTANCE));
	}

	@Test
	public void commonSupertypeIsItselfForBooleanTypesAndUntypedOtherwise() {
		assertEquals(BoolType.INSTANCE, BoolType.INSTANCE.commonSupertype(BoolType.INSTANCE));
		assertEquals(it.unive.lisa.type.Untyped.INSTANCE, BoolType.INSTANCE.commonSupertype(Int32Type.INSTANCE));
		assertEquals(it.unive.lisa.type.Untyped.INSTANCE, BoolType.INSTANCE.commonSupertype(StringType.INSTANCE));
	}

	@Test
	public void allInstancesIsJustItself() {
		assertEquals(Collections.singleton(BoolType.INSTANCE), BoolType.INSTANCE.allInstances(null));
	}

	@Test
	public void toStringIdentifiesTheType() {
		assertEquals("bool", BoolType.INSTANCE.toString());
	}

}
