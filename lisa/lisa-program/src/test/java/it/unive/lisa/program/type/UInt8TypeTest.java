package it.unive.lisa.program.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import org.junit.jupiter.api.Test;

public class UInt8TypeTest {

	@Test
	public void reportsItsOwnProperties() {
		assertEquals(8, UInt8Type.INSTANCE.getNBits());
		assertTrue(UInt8Type.INSTANCE.isUnsigned());
		assertTrue(UInt8Type.INSTANCE.isIntegral());
		assertFalse(UInt8Type.INSTANCE.isSigned());
	}

	@Test
	public void equalsHoldsForItself() {
		assertEquals(UInt8Type.INSTANCE, UInt8Type.INSTANCE);
		assertEquals(UInt8Type.INSTANCE.hashCode(), UInt8Type.INSTANCE.hashCode());
	}

	@Test
	@SuppressWarnings("unlikely-arg-type")
	public void equalsFailsForDifferentBitWidthOrIntegralness() {
		assertFalse(UInt8Type.INSTANCE.equals(UInt16Type.INSTANCE));
		assertFalse(UInt8Type.INSTANCE.equals(Float32Type.INSTANCE));
		assertFalse(UInt8Type.INSTANCE.equals(BoolType.INSTANCE));
	}

	@Test
	@SuppressWarnings("unlikely-arg-type")
	public void equalsFailsAcrossSignedness() {
		// NumericType.sameNumericTypes() (which equals() delegates to) also
		// compares isUnsigned(), so same-width same-integralness types with
		// different signedness must not be equal
		assertFalse(UInt8Type.INSTANCE.equals(Int8Type.INSTANCE));
	}

	@Test
	public void canBeAssignedToNumericTypesAndUntypedOnly() {
		assertTrue(UInt8Type.INSTANCE.canBeAssignedTo(it.unive.lisa.type.Untyped.INSTANCE));
		assertTrue(UInt8Type.INSTANCE.canBeAssignedTo(Int64Type.INSTANCE));
		assertTrue(UInt8Type.INSTANCE.canBeAssignedTo(Float64Type.INSTANCE));
		assertFalse(UInt8Type.INSTANCE.canBeAssignedTo(BoolType.INSTANCE));
		assertFalse(UInt8Type.INSTANCE.canBeAssignedTo(StringType.INSTANCE));
	}

	@Test
	public void commonSupertypeWidensToTheLargerBitWidth() {
		assertEquals(UInt32Type.INSTANCE, UInt8Type.INSTANCE.commonSupertype(UInt32Type.INSTANCE));
		assertEquals(UInt8Type.INSTANCE, UInt8Type.INSTANCE.commonSupertype(UInt8Type.INSTANCE));
	}

	@Test
	public void commonSupertypePrefersLargerBitWidthEvenAcrossIntegralness() {
		// bit width is compared before integral-ness or sign, so a wider
		// float still wins over this narrower unsigned integer
		assertEquals(Float32Type.INSTANCE, UInt8Type.INSTANCE.commonSupertype(Float32Type.INSTANCE));
	}

	@Test
	public void commonSupertypePrefersSignedAtEqualBitWidthAndIntegralness() {
		assertEquals(Int8Type.INSTANCE, UInt8Type.INSTANCE.commonSupertype(Int8Type.INSTANCE));
		assertEquals(Int8Type.INSTANCE, Int8Type.INSTANCE.commonSupertype(UInt8Type.INSTANCE));
	}

	@Test
	public void commonSupertypeWithNonNumericIsUntyped() {
		assertEquals(it.unive.lisa.type.Untyped.INSTANCE, UInt8Type.INSTANCE.commonSupertype(BoolType.INSTANCE));
	}

	@Test
	public void allInstancesIsJustItself() {
		assertEquals(Collections.singleton(UInt8Type.INSTANCE), UInt8Type.INSTANCE.allInstances(null));
	}

	@Test
	public void toStringIdentifiesTheTypeAsUnsigned() {
		assertEquals("uint8", UInt8Type.INSTANCE.toString());
	}

}
