package it.unive.lisa.program.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import org.junit.jupiter.api.Test;

public class UInt32TypeTest {

	@Test
	public void reportsItsOwnProperties() {
		assertEquals(32, UInt32Type.INSTANCE.getNBits());
		assertTrue(UInt32Type.INSTANCE.isUnsigned());
		assertTrue(UInt32Type.INSTANCE.isIntegral());
		assertFalse(UInt32Type.INSTANCE.isSigned());
	}

	@Test
	public void equalsHoldsForItself() {
		assertEquals(UInt32Type.INSTANCE, UInt32Type.INSTANCE);
		assertEquals(UInt32Type.INSTANCE.hashCode(), UInt32Type.INSTANCE.hashCode());
	}

	@Test
	public void equalsFailsForDifferentBitWidthOrIntegralness() {
		assertFalse(UInt32Type.INSTANCE.equals(UInt64Type.INSTANCE));
		assertFalse(UInt32Type.INSTANCE.equals(Float32Type.INSTANCE));
		assertFalse(UInt32Type.INSTANCE.equals(BoolType.INSTANCE));
	}

	@Test
	public void equalsFailsAcrossSignedness() {
		// NumericType.sameNumericTypes() (which equals() delegates to) also
		// compares isUnsigned(), so same-width same-integralness types with
		// different signedness must not be equal
		assertFalse(UInt32Type.INSTANCE.equals(Int32Type.INSTANCE));
	}

	@Test
	public void canBeAssignedToNumericTypesAndUntypedOnly() {
		assertTrue(UInt32Type.INSTANCE.canBeAssignedTo(it.unive.lisa.type.Untyped.INSTANCE));
		assertTrue(UInt32Type.INSTANCE.canBeAssignedTo(Int8Type.INSTANCE));
		assertTrue(UInt32Type.INSTANCE.canBeAssignedTo(Float64Type.INSTANCE));
		assertFalse(UInt32Type.INSTANCE.canBeAssignedTo(BoolType.INSTANCE));
		assertFalse(UInt32Type.INSTANCE.canBeAssignedTo(StringType.INSTANCE));
	}

	@Test
	public void commonSupertypeWidensToTheLargerBitWidth() {
		assertEquals(UInt64Type.INSTANCE, UInt32Type.INSTANCE.commonSupertype(UInt64Type.INSTANCE));
		assertEquals(UInt32Type.INSTANCE, UInt32Type.INSTANCE.commonSupertype(UInt16Type.INSTANCE));
	}

	@Test
	public void commonSupertypePrefersNonIntegralAtEqualBitWidth() {
		assertEquals(Float32Type.INSTANCE, UInt32Type.INSTANCE.commonSupertype(Float32Type.INSTANCE));
		assertEquals(Float32Type.INSTANCE, Float32Type.INSTANCE.commonSupertype(UInt32Type.INSTANCE));
	}

	@Test
	public void commonSupertypePrefersSignedAtEqualBitWidthAndIntegralness() {
		assertEquals(Int32Type.INSTANCE, UInt32Type.INSTANCE.commonSupertype(Int32Type.INSTANCE));
		assertEquals(Int32Type.INSTANCE, Int32Type.INSTANCE.commonSupertype(UInt32Type.INSTANCE));
	}

	@Test
	public void commonSupertypeWithNonNumericIsUntyped() {
		assertEquals(it.unive.lisa.type.Untyped.INSTANCE, UInt32Type.INSTANCE.commonSupertype(BoolType.INSTANCE));
	}

	@Test
	public void allInstancesIsJustItself() {
		assertEquals(Collections.singleton(UInt32Type.INSTANCE), UInt32Type.INSTANCE.allInstances(null));
	}

	@Test
	public void toStringIdentifiesTheTypeAsUnsigned() {
		assertEquals("uint32", UInt32Type.INSTANCE.toString());
	}

}
