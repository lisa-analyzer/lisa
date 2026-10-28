package it.unive.lisa.program.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import org.junit.jupiter.api.Test;

public class UInt16TypeTest {

	@Test
	public void reportsItsOwnProperties() {
		assertEquals(16, UInt16Type.INSTANCE.getNBits());
		assertTrue(UInt16Type.INSTANCE.isUnsigned());
		assertTrue(UInt16Type.INSTANCE.isIntegral());
		assertFalse(UInt16Type.INSTANCE.isSigned());
	}

	@Test
	public void equalsHoldsForItself() {
		assertEquals(UInt16Type.INSTANCE, UInt16Type.INSTANCE);
		assertEquals(UInt16Type.INSTANCE.hashCode(), UInt16Type.INSTANCE.hashCode());
	}

	@Test
	@SuppressWarnings("unlikely-arg-type")
	public void equalsFailsForDifferentBitWidthOrIntegralness() {
		assertFalse(UInt16Type.INSTANCE.equals(UInt32Type.INSTANCE));
		assertFalse(UInt16Type.INSTANCE.equals(Float32Type.INSTANCE));
		assertFalse(UInt16Type.INSTANCE.equals(BoolType.INSTANCE));
	}

	@Test
	@SuppressWarnings("unlikely-arg-type")
	public void equalsFailsAcrossSignedness() {
		// NumericType.sameNumericTypes() (which equals() delegates to) also
		// compares isUnsigned(), so same-width same-integralness types with
		// different signedness must not be equal
		assertFalse(UInt16Type.INSTANCE.equals(Int16Type.INSTANCE));
	}

	@Test
	public void canBeAssignedToNumericTypesAndUntypedOnly() {
		assertTrue(UInt16Type.INSTANCE.canBeAssignedTo(it.unive.lisa.type.Untyped.INSTANCE));
		assertTrue(UInt16Type.INSTANCE.canBeAssignedTo(Int64Type.INSTANCE));
		assertTrue(UInt16Type.INSTANCE.canBeAssignedTo(Float64Type.INSTANCE));
		assertFalse(UInt16Type.INSTANCE.canBeAssignedTo(BoolType.INSTANCE));
		assertFalse(UInt16Type.INSTANCE.canBeAssignedTo(StringType.INSTANCE));
	}

	@Test
	public void commonSupertypeWidensToTheLargerBitWidth() {
		assertEquals(UInt64Type.INSTANCE, UInt16Type.INSTANCE.commonSupertype(UInt64Type.INSTANCE));
		assertEquals(UInt16Type.INSTANCE, UInt16Type.INSTANCE.commonSupertype(UInt8Type.INSTANCE));
	}

	@Test
	public void commonSupertypePrefersLargerBitWidthEvenAcrossIntegralness() {
		// bit width is compared before integral-ness or sign, so a wider
		// float still wins over this narrower unsigned integer
		assertEquals(Float32Type.INSTANCE, UInt16Type.INSTANCE.commonSupertype(Float32Type.INSTANCE));
	}

	@Test
	public void commonSupertypePrefersSignedAtEqualBitWidthAndIntegralness() {
		assertEquals(Int16Type.INSTANCE, UInt16Type.INSTANCE.commonSupertype(Int16Type.INSTANCE));
		assertEquals(Int16Type.INSTANCE, Int16Type.INSTANCE.commonSupertype(UInt16Type.INSTANCE));
	}

	@Test
	public void commonSupertypeWithNonNumericIsUntyped() {
		assertEquals(it.unive.lisa.type.Untyped.INSTANCE, UInt16Type.INSTANCE.commonSupertype(StringType.INSTANCE));
	}

	@Test
	public void allInstancesIsJustItself() {
		assertEquals(Collections.singleton(UInt16Type.INSTANCE), UInt16Type.INSTANCE.allInstances(null));
	}

	@Test
	public void toStringIdentifiesTheTypeAsUnsigned() {
		assertEquals("uint16", UInt16Type.INSTANCE.toString());
	}

}
